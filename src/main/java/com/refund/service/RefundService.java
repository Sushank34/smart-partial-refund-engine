package com.refund.service;

import com.refund.domain.Order;
import com.refund.domain.PaymentSplit;
import com.refund.domain.Refund;
import com.refund.domain.RefundAllocation;
import com.refund.domain.RefundStatus;
import com.refund.exception.ApiException;
import com.refund.repository.OrderRepository;
import com.refund.repository.RefundRepository;
import com.refund.web.dto.CreateOrderRequest;
import com.refund.web.dto.CreateRefundRequest;
import com.refund.web.dto.PaymentSplitRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates the refund use cases: recording orders, calculating + persisting refunds,
 * and serving history. The proportional / multi-currency maths lives in {@link RefundCalculator};
 * this class handles validation, persistence, and the order balance.
 */
@Service
public class RefundService {

    private final OrderRepository orderRepository;
    private final RefundRepository refundRepository;

    public RefundService(OrderRepository orderRepository, RefundRepository refundRepository) {
        this.orderRepository = orderRepository;
        this.refundRepository = refundRepository;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest req) {
        BigDecimal rate = resolveRate(req.displayCurrency(), req.processingCurrency(), req.exchangeRate());

        List<PaymentSplit> splits = new ArrayList<>();
        for (PaymentSplitRequest p : req.payments()) {
            splits.add(new PaymentSplit(p.method(), Money.toMinor(p.amount())));
        }

        Order order = new Order("ord_" + shortId(), req.displayCurrency().toUpperCase(),
                req.processingCurrency().toUpperCase(), rate, splits, Instant.now());
        return orderRepository.save(order);
    }

    public List<Order> listOrders() {
        return orderRepository.findAll();
    }

    public Order getOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("ORDER_NOT_FOUND", "No order with id " + orderId + "."));
    }

    public List<Refund> getHistory(String orderId) {
        getOrder(orderId); // 404 if the order does not exist
        return refundRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
    }

    /** Result of a refund request: the refund, and whether it was an idempotent replay of a prior one. */
    public record RefundOutcome(Refund refund, boolean replayed) {
    }

    /** Convenience overload with no idempotency key (used by the seeder and tests). */
    @Transactional
    public Refund requestRefund(String orderId, CreateRefundRequest req) {
        return requestRefund(orderId, req, null).refund();
    }

    @Transactional
    public RefundOutcome requestRefund(String orderId, CreateRefundRequest req, String idempotencyKey) {
        // Idempotent replay: a retry with a key we've already processed returns the original refund,
        // never a duplicate. The unique DB constraint is the backstop if two retries race.
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = refundRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return new RefundOutcome(existing.get(), true);
            }
        }

        Order order = getOrder(orderId);
        long requestedMinor = Money.toMinor(req.amount());

        // Maximum-refundable enforcement: never refund more than what remains on the order.
        long remaining = order.refundableRemainingMinor();
        if (requestedMinor > remaining) {
            throw ApiException.unprocessable("EXCEEDS_REFUNDABLE", String.format(
                    "Refund of %s exceeds the remaining refundable balance of %s.",
                    Money.toMajor(requestedMinor), Money.toMajor(remaining)));
        }

        RefundCalculator.Breakdown breakdown =
                RefundCalculator.calculate(requestedMinor, order.getSplits(), order.getExchangeRate());

        List<RefundAllocation> allocations = new ArrayList<>();
        List<String> violations = new ArrayList<>();
        for (RefundCalculator.Allocation a : breakdown.allocations()) {
            allocations.add(new RefundAllocation(a.method(), a.displayMinor(), a.processingMinor()));
            long min = a.method().minRefundMinor();
            if (a.displayMinor() > 0 && a.displayMinor() < min) {
                violations.add(String.format("%s allocation %s is below the %s minimum for this method",
                        a.method(), Money.toMajor(a.displayMinor()), Money.toMajor(min)));
            }
        }

        RefundStatus status = violations.isEmpty() ? RefundStatus.COMPLETED : RefundStatus.FLAGGED;
        String note = violations.isEmpty() ? null : String.join("; ", violations);

        Refund refund = new Refund("rf_" + shortId(), orderId, idempotencyKey, requestedMinor,
                breakdown.requestedProcessingMinor(), order.getDisplayCurrency(), order.getProcessingCurrency(),
                order.getExchangeRate(), req.reasonCode(), status, note, allocations, Instant.now());
        refund = refundRepository.save(refund);

        order.applyRefund(requestedMinor);
        orderRepository.save(order);

        return new RefundOutcome(refund, false);
    }

    /** Default the rate to 1 for same-currency orders; otherwise require a positive rate. */
    private BigDecimal resolveRate(String display, String processing, BigDecimal provided) {
        boolean sameCurrency = display.equalsIgnoreCase(processing);
        if (provided == null) {
            if (sameCurrency) {
                return BigDecimal.ONE;
            }
            throw ApiException.unprocessable("EXCHANGE_RATE_REQUIRED",
                    "exchangeRate is required when display and processing currencies differ.");
        }
        if (provided.signum() <= 0) {
            throw ApiException.unprocessable("INVALID_EXCHANGE_RATE", "exchangeRate must be positive.");
        }
        return provided;
    }

    private static String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
