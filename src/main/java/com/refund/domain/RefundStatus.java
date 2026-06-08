package com.refund.domain;

/** Outcome of a refund request. */
public enum RefundStatus {
    /** Calculated, persisted, and within all payment-method constraints. */
    COMPLETED,
    /** Processed, but one or more allocations breached a payment-method constraint (see note). */
    FLAGGED
}
