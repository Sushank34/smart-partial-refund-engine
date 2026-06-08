package com.refund.web.dto;

import com.refund.domain.PaymentMethod;
import com.refund.domain.ReasonCode;
import com.refund.domain.Refund;
import com.refund.domain.RefundStatus;
import com.refund.service.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Refund view returned by the refund and history endpoints. Shows the requested amount in
 * both currencies and the per-method breakdown, also in both currencies.
 */
public record RefundResponse(
        String id,
        String orderId,
        ReasonCode reasonCode,
        RefundStatus status,
        String note,
        String displayCurrency,
        String processingCurrency,
        BigDecimal exchangeRate,
        BigDecimal requestedDisplayAmount,
        BigDecimal requestedProcessingAmount,
        List<AllocationView> allocations,
        Instant createdAt) {

    /** One payment method's slice of the refund, in both currencies. */
    public record AllocationView(
            PaymentMethod method,
            BigDecimal displayAmount,
            BigDecimal processingAmount,
            boolean instant,
            int settlementDays) {
    }

    public static RefundResponse from(Refund refund) {
        List<AllocationView> views = refund.getAllocations().stream()
                .map(a -> new AllocationView(
                        a.getMethod(),
                        Money.toMajor(a.getDisplayAmountMinor()),
                        Money.toMajor(a.getProcessingAmountMinor()),
                        a.getMethod().isInstant(),
                        a.getMethod().settlementDays()))
                .toList();
        return new RefundResponse(
                refund.getId(),
                refund.getOrderId(),
                refund.getReasonCode(),
                refund.getStatus(),
                refund.getNote(),
                refund.getDisplayCurrency(),
                refund.getProcessingCurrency(),
                refund.getExchangeRate(),
                Money.toMajor(refund.getRequestedAmountMinor()),
                Money.toMajor(refund.getRequestedProcessingMinor()),
                views,
                refund.getCreatedAt());
    }
}
