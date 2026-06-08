package com.refund.domain;

/** Why a refund was issued — recorded on every refund for audit and reporting. */
public enum ReasonCode {
    PARTIAL_CANCELLATION,
    ITEM_OUT_OF_STOCK,
    DAMAGED_GOODS,
    CUSTOMER_REQUEST,
    OTHER
}
