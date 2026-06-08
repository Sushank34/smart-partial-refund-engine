package com.refund.domain;

/** Lifecycle of an order with respect to refunds. */
public enum OrderStatus {
    /** Still has refundable balance remaining. */
    ACTIVE,
    /** The full order amount has been refunded. */
    FULLY_REFUNDED
}
