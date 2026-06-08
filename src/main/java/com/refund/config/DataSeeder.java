package com.refund.config;

import com.refund.domain.Order;
import com.refund.domain.PaymentMethod;
import com.refund.domain.PaymentSplit;
import com.refund.domain.ReasonCode;
import com.refund.repository.OrderRepository;
import com.refund.service.Money;
import com.refund.service.RefundService;
import com.refund.web.dto.CreateRefundRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds a demonstration dataset on startup: 16 orders spanning single, two, and three-plus
 * payment methods across five currency pairs (USD→PEN, USD→COP, EUR→USD, USD→BOB, and
 * same-currency), with a couple of orders pre-loaded with partial refunds to exercise the
 * audit trail.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final OrderRepository orderRepository;
    private final RefundService refundService;

    public DataSeeder(OrderRepository orderRepository, RefundService refundService) {
        this.orderRepository = orderRepository;
        this.refundService = refundService;
    }

    @Override
    public void run(String... args) {
        // --- 5 single-method orders ---
        seed("ord_1001", "USD", "USD", "1", split(PaymentMethod.WALLET, "500.00"));
        seed("ord_1002", "USD", "PEN", "3.75", split(PaymentMethod.CREDIT_CARD, "1200.00"));
        seed("ord_1003", "PEN", "PEN", "1", split(PaymentMethod.BANK_TRANSFER, "850.00"));
        seed("ord_1004", "USD", "COP", "3950.50", split(PaymentMethod.DEBIT_CARD, "50.00"));
        seed("ord_1005", "EUR", "USD", "1.08", split(PaymentMethod.WALLET, "2000.00"));

        // --- 5 two-method orders ---
        seed("ord_1006", "USD", "PEN", "3.75",
                split(PaymentMethod.WALLET, "700.00"), split(PaymentMethod.CREDIT_CARD, "300.00"));
        seed("ord_1007", "USD", "USD", "1",
                split(PaymentMethod.BANK_TRANSFER, "1200.00"), split(PaymentMethod.CREDIT_CARD, "300.00"));
        seed("ord_1008", "USD", "COP", "3950.50",
                split(PaymentMethod.WALLET, "150.00"), split(PaymentMethod.DEBIT_CARD, "100.00"));
        seed("ord_1009", "EUR", "USD", "1.08",
                split(PaymentMethod.CREDIT_CARD, "1500.00"), split(PaymentMethod.WALLET, "1500.00"));
        seed("ord_1010", "PEN", "PEN", "1",
                split(PaymentMethod.WALLET, "540.00"), split(PaymentMethod.BANK_TRANSFER, "60.00"));

        // --- 5 three-plus-method orders ---
        seed("ord_1011", "USD", "PEN", "3.75",
                split(PaymentMethod.WALLET, "500.00"), split(PaymentMethod.CREDIT_CARD, "300.00"),
                split(PaymentMethod.BANK_TRANSFER, "200.00"));
        seed("ord_1012", "USD", "COP", "3950.50",
                split(PaymentMethod.WALLET, "800.00"), split(PaymentMethod.CREDIT_CARD, "700.00"),
                split(PaymentMethod.DEBIT_CARD, "500.00"));
        seed("ord_1013", "USD", "USD", "1",
                split(PaymentMethod.WALLET, "1250.00"), split(PaymentMethod.CREDIT_CARD, "1250.00"),
                split(PaymentMethod.DEBIT_CARD, "1250.00"), split(PaymentMethod.BANK_TRANSFER, "1250.00"));
        // Even three-way split — the classic case where naive rounding loses a cent.
        seed("ord_1014", "EUR", "USD", "1.08",
                split(PaymentMethod.WALLET, "100.00"), split(PaymentMethod.CREDIT_CARD, "100.00"),
                split(PaymentMethod.DEBIT_CARD, "100.00"));
        seed("ord_1015", "USD", "PEN", "3.75",
                split(PaymentMethod.WALLET, "150.00"), split(PaymentMethod.CREDIT_CARD, "100.00"),
                split(PaymentMethod.BANK_TRANSFER, "83.33"));

        // Bolivia (BOB) — completes the four operating markets (PEN, COP, USD, BOB).
        seed("ord_1016", "USD", "BOB", "6.90",
                split(PaymentMethod.WALLET, "180.00"), split(PaymentMethod.CREDIT_CARD, "120.00"));

        // --- Pre-existing partial refunds, to demonstrate the audit trail ---
        refundService.requestRefund("ord_1006",
                new CreateRefundRequest(new BigDecimal("100.00"), ReasonCode.CUSTOMER_REQUEST));
        refundService.requestRefund("ord_1011",
                new CreateRefundRequest(new BigDecimal("300.00"), ReasonCode.PARTIAL_CANCELLATION));
        refundService.requestRefund("ord_1011",
                new CreateRefundRequest(new BigDecimal("150.00"), ReasonCode.ITEM_OUT_OF_STOCK));

        log.info("Seeded {} demo orders.", orderRepository.count());
    }

    private void seed(String id, String displayCcy, String processingCcy, String rate, PaymentSplit... splits) {
        Order order = new Order(id, displayCcy, processingCcy, new BigDecimal(rate), List.of(splits), Instant.now());
        orderRepository.save(order);
    }

    private static PaymentSplit split(PaymentMethod method, String amount) {
        return new PaymentSplit(method, Money.toMinor(new BigDecimal(amount)));
    }
}
