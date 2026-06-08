package com.refund.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.refund.domain.Order;
import com.refund.domain.OrderStatus;
import com.refund.domain.PaymentMethod;
import com.refund.domain.ReasonCode;
import com.refund.domain.Refund;
import com.refund.exception.ApiException;
import com.refund.web.dto.CreateOrderRequest;
import com.refund.web.dto.CreateRefundRequest;
import com.refund.web.dto.PaymentSplitRequest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RefundServiceTest {

    @Autowired
    private RefundService service;

    private Order newOrder() {
        return service.createOrder(new CreateOrderRequest("USD", "PEN", new BigDecimal("3.75"),
                List.of(new PaymentSplitRequest(PaymentMethod.WALLET, new BigDecimal("600.00")),
                        new PaymentSplitRequest(PaymentMethod.CREDIT_CARD, new BigDecimal("400.00")))));
    }

    @Test
    void recordsEveryPartialRefundInHistory() {
        Order order = newOrder();

        service.requestRefund(order.getId(), new CreateRefundRequest(new BigDecimal("100.00"), ReasonCode.CUSTOMER_REQUEST));
        service.requestRefund(order.getId(), new CreateRefundRequest(new BigDecimal("50.00"), ReasonCode.DAMAGED_GOODS));
        service.requestRefund(order.getId(), new CreateRefundRequest(new BigDecimal("25.00"), ReasonCode.PARTIAL_CANCELLATION));

        List<Refund> history = service.getHistory(order.getId());
        assertEquals(3, history.size());
        assertEquals(17500, service.getOrder(order.getId()).getTotalRefundedMinor());
    }

    @Test
    void rejectsRefundExceedingRemainingBalance() {
        Order order = newOrder(); // $1000 total
        service.requestRefund(order.getId(), new CreateRefundRequest(new BigDecimal("800.00"), ReasonCode.CUSTOMER_REQUEST));

        ApiException ex = assertThrows(ApiException.class, () -> service.requestRefund(order.getId(),
                new CreateRefundRequest(new BigDecimal("250.00"), ReasonCode.CUSTOMER_REQUEST)));
        assertEquals("EXCEEDS_REFUNDABLE", ex.getCode());
    }

    @Test
    void flipsToFullyRefundedWhenBalanceReachesZero() {
        Order order = newOrder();
        service.requestRefund(order.getId(), new CreateRefundRequest(new BigDecimal("1000.00"), ReasonCode.PARTIAL_CANCELLATION));
        assertEquals(OrderStatus.FULLY_REFUNDED, service.getOrder(order.getId()).getStatus());
    }

    @Test
    void unknownOrderIsNotFound() {
        ApiException ex = assertThrows(ApiException.class, () -> service.getOrder("ord_does_not_exist"));
        assertEquals("ORDER_NOT_FOUND", ex.getCode());
    }
}
