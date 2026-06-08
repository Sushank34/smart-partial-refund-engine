package com.refund.service;

import com.refund.domain.PaymentMethod;
import com.refund.domain.PaymentSplit;
import com.refund.exception.ApiException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Pure, side-effect-free refund maths. Given a refund amount, the order's payment splits
 * and its historical exchange rate, it produces an exact per-method breakdown in both the
 * display and processing currencies.
 *
 * <h2>Why this is correct to the cent</h2>
 * Splitting an amount proportionally almost always produces fractional cents. Naively
 * rounding each share independently loses or invents money (e.g. three-way split of $0.10).
 * We instead use the <b>largest-remainder method</b> (a.k.a. Hamilton apportionment):
 * floor every share, then hand the leftover cents one at a time to the methods with the
 * largest truncated fractions. This guarantees the shares sum <i>exactly</i> to the input.
 *
 * <p>The same routine is applied independently to the display amount and to the processing
 * amount, so each currency's allocations also sum exactly to that currency's refund total —
 * no drift between the two currencies.
 */
public final class RefundCalculator {

    private RefundCalculator() {
    }

    /** One method's slice of a refund, in both currencies (minor units). */
    public record Allocation(PaymentMethod method, long displayMinor, long processingMinor) {
    }

    /** Full breakdown of a single refund. */
    public record Breakdown(long requestedDisplayMinor, long requestedProcessingMinor,
                            List<Allocation> allocations) {
    }

    /**
     * Distribute {@code requestedDisplayMinor} across the given payment splits in proportion
     * to how much each method originally funded, then convert each slice to the processing
     * currency using {@code exchangeRate}.
     */
    public static Breakdown calculate(long requestedDisplayMinor, List<PaymentSplit> splits,
                                      BigDecimal exchangeRate) {
        if (requestedDisplayMinor <= 0) {
            throw ApiException.unprocessable("INVALID_AMOUNT", "Refund amount must be positive.");
        }
        if (splits == null || splits.isEmpty()) {
            throw ApiException.unprocessable("NO_PAYMENT_SPLITS", "Order has no payment splits to refund to.");
        }

        long[] weights = splits.stream().mapToLong(PaymentSplit::getAmountMinor).toArray();

        // Processing-currency total for the whole refund, rounded once.
        long requestedProcessingMinor = BigDecimal.valueOf(requestedDisplayMinor)
                .multiply(exchangeRate)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

        long[] displayShares = allocateProportional(requestedDisplayMinor, weights);
        long[] processingShares = allocateProportional(requestedProcessingMinor, weights);

        List<Allocation> allocations = new ArrayList<>(splits.size());
        for (int i = 0; i < splits.size(); i++) {
            allocations.add(new Allocation(splits.get(i).getMethod(), displayShares[i], processingShares[i]));
        }
        return new Breakdown(requestedDisplayMinor, requestedProcessingMinor, allocations);
    }

    /**
     * Split {@code amount} across {@code weights} proportionally using the largest-remainder
     * method. The returned shares always sum exactly to {@code amount}.
     *
     * <p>Package-private and {@code static} so it can be unit-tested directly.
     */
    static long[] allocateProportional(long amount, long[] weights) {
        long totalWeight = 0;
        for (long w : weights) {
            if (w < 0) {
                throw ApiException.unprocessable("INVALID_SPLIT", "Payment split amounts cannot be negative.");
            }
            totalWeight += w;
        }
        if (totalWeight <= 0) {
            throw ApiException.unprocessable("INVALID_SPLIT", "Payment splits must have a positive total.");
        }

        long[] shares = new long[weights.length];
        long[] remainders = new long[weights.length];
        long distributed = 0;
        for (int i = 0; i < weights.length; i++) {
            long numerator = Math.multiplyExact(amount, weights[i]);
            shares[i] = numerator / totalWeight;          // floor
            remainders[i] = numerator % totalWeight;       // fractional part, scaled by totalWeight
            distributed += shares[i];
        }

        // Leftover cents (always < number of weights). Hand them to the largest remainders;
        // ties broken by larger original weight, then lowest index — fully deterministic.
        long leftover = amount - distributed;
        List<Integer> order = new ArrayList<>(weights.length);
        for (int i = 0; i < weights.length; i++) {
            order.add(i);
        }
        order.sort(Comparator
                .comparingLong((Integer i) -> remainders[i]).reversed()
                .thenComparing(Comparator.comparingLong((Integer i) -> weights[i]).reversed())
                .thenComparingInt(i -> i));

        for (int k = 0; k < leftover; k++) {
            shares[order.get(k)]++;
        }
        return shares;
    }
}
