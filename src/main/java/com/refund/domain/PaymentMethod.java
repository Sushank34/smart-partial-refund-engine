package com.refund.domain;

/**
 * Supported payment instruments and their refund constraints.
 *
 * <p>{@code minRefundMinor} is the smallest amount (in minor units / cents of the order's
 * display currency) a single allocation to this method may carry. Wallets accept any amount
 * and settle instantly; card and bank refunds have a floor and a settlement delay.
 *
 * <p>Assumption: minimums are expressed in the order's display currency for this prototype.
 * A production system would hold a per-currency minimum table.
 */
public enum PaymentMethod {

    WALLET(0L, true, 0),
    CREDIT_CARD(100L, false, 5),
    DEBIT_CARD(100L, false, 3),
    BANK_TRANSFER(100L, false, 3);

    private final long minRefundMinor;
    private final boolean instant;
    private final int settlementDays;

    PaymentMethod(long minRefundMinor, boolean instant, int settlementDays) {
        this.minRefundMinor = minRefundMinor;
        this.instant = instant;
        this.settlementDays = settlementDays;
    }

    public long minRefundMinor() {
        return minRefundMinor;
    }

    public boolean isInstant() {
        return instant;
    }

    public int settlementDays() {
        return settlementDays;
    }
}
