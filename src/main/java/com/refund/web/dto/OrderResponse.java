package com.refund.web.dto;

import com.refund.domain.Order;
import com.refund.domain.OrderStatus;
import com.refund.domain.PaymentMethod;
import com.refund.service.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Order view returned by the create and fetch endpoints. All amounts in major units. */
public record OrderResponse(
        String id,
        String displayCurrency,
        String processingCurrency,
        BigDecimal exchangeRate,
        BigDecimal totalAmount,
        BigDecimal totalRefunded,
        BigDecimal refundableRemaining,
        OrderStatus status,
        List<Split> payments,
        Instant createdAt) {

    public record Split(PaymentMethod method, BigDecimal amount) {
    }

    public static OrderResponse from(Order order) {
        List<Split> splits = order.getSplits().stream()
                .map(s -> new Split(s.getMethod(), Money.toMajor(s.getAmountMinor())))
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getDisplayCurrency(),
                order.getProcessingCurrency(),
                order.getExchangeRate(),
                Money.toMajor(order.getTotalAmountMinor()),
                Money.toMajor(order.getTotalRefundedMinor()),
                Money.toMajor(order.refundableRemainingMinor()),
                order.getStatus(),
                splits,
                order.getCreatedAt());
    }
}
