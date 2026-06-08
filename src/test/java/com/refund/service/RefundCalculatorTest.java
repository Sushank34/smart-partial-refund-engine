package com.refund.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.refund.domain.PaymentMethod;
import com.refund.domain.PaymentSplit;
import com.refund.exception.ApiException;
import com.refund.service.RefundCalculator.Allocation;
import com.refund.service.RefundCalculator.Breakdown;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class RefundCalculatorTest {

    private static List<PaymentSplit> splits(Object... methodAmountPairs) {
        List<PaymentSplit> list = new java.util.ArrayList<>();
        for (int i = 0; i < methodAmountPairs.length; i += 2) {
            list.add(new PaymentSplit((PaymentMethod) methodAmountPairs[i], (long) (int) methodAmountPairs[i + 1]));
        }
        return list;
    }

    private static long sumDisplay(Breakdown b) {
        return b.allocations().stream().mapToLong(Allocation::displayMinor).sum();
    }

    private static long sumProcessing(Breakdown b) {
        return b.allocations().stream().mapToLong(Allocation::processingMinor).sum();
    }

    @Test
    void splitsSixtyFortyExactly() {
        // $300.00 refund on a 60/40 order → $180 wallet, $120 card.
        Breakdown b = RefundCalculator.calculate(30000,
                splits(PaymentMethod.WALLET, 60000, PaymentMethod.CREDIT_CARD, 40000), BigDecimal.ONE);

        assertEquals(18000, b.allocations().get(0).displayMinor());
        assertEquals(12000, b.allocations().get(1).displayMinor());
        assertEquals(30000, sumDisplay(b));
    }

    @Test
    void neverLosesACentOnAwkwardThreeWaySplit() {
        // $0.10 across three equal methods cannot divide evenly; must still total exactly 10c.
        Breakdown b = RefundCalculator.calculate(10,
                splits(PaymentMethod.WALLET, 100, PaymentMethod.CREDIT_CARD, 100, PaymentMethod.DEBIT_CARD, 100),
                BigDecimal.ONE);

        assertEquals(10, sumDisplay(b));
        // Largest-remainder hands the extra cents to the earliest methods: 4 / 3 / 3.
        assertEquals(4, b.allocations().get(0).displayMinor());
        assertEquals(3, b.allocations().get(1).displayMinor());
        assertEquals(3, b.allocations().get(2).displayMinor());
    }

    @Test
    void sumInvariantHoldsForManyAmountsAndWeights() {
        long[] weights = {70000, 23333, 6667};
        for (long amount = 1; amount <= 5000; amount++) {
            long[] shares = RefundCalculator.allocateProportional(amount, weights);
            long total = 0;
            for (long s : shares) {
                assertTrue(s >= 0, "share must be non-negative");
                total += s;
            }
            assertEquals(amount, total, "shares must sum to the requested amount for amount=" + amount);
        }
    }

    @Test
    void appliesHistoricalExchangeRateToBothCurrencies() {
        // $200.00 at 1 USD = 3.75 PEN → 750.00 PEN, split 60/40.
        Breakdown b = RefundCalculator.calculate(20000,
                splits(PaymentMethod.WALLET, 60000, PaymentMethod.CREDIT_CARD, 40000), new BigDecimal("3.75"));

        assertEquals(75000, b.requestedProcessingMinor());
        assertEquals(75000, sumProcessing(b));
        assertEquals(45000, b.allocations().get(0).processingMinor()); // 60% of 750.00
        assertEquals(30000, b.allocations().get(1).processingMinor()); // 40% of 750.00
    }

    @Test
    void processingAllocationsSumExactlyEvenWithRoundingRate() {
        // A rate that produces fractional cents everywhere; the per-method processing
        // amounts must still sum to the rounded processing total.
        Breakdown b = RefundCalculator.calculate(33333,
                splits(PaymentMethod.WALLET, 15000, PaymentMethod.CREDIT_CARD, 10000,
                        PaymentMethod.BANK_TRANSFER, 8333), new BigDecimal("3950.50"));

        assertEquals(33333, sumDisplay(b));
        assertEquals(b.requestedProcessingMinor(), sumProcessing(b));
    }

    @Test
    void singleMethodGetsTheWholeAmount() {
        Breakdown b = RefundCalculator.calculate(12345,
                splits(PaymentMethod.WALLET, 99999), new BigDecimal("1.08"));

        assertEquals(12345, b.allocations().get(0).displayMinor());
        assertEquals(1, b.allocations().size());
    }

    @Test
    void rejectsNonPositiveAmount() {
        assertThrows(ApiException.class, () -> RefundCalculator.calculate(0,
                splits(PaymentMethod.WALLET, 100), BigDecimal.ONE));
    }
}
