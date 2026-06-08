package com.refund.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The portion of a refund routed back to one payment method, expressed in both
 * the display currency and the processing currency (using the order's historical rate).
 * All amounts are in minor units (cents).
 */
@Entity
@Table(name = "refund_allocations")
public class RefundAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    private long displayAmountMinor;

    private long processingAmountMinor;

    protected RefundAllocation() {
    }

    public RefundAllocation(PaymentMethod method, long displayAmountMinor, long processingAmountMinor) {
        this.method = method;
        this.displayAmountMinor = displayAmountMinor;
        this.processingAmountMinor = processingAmountMinor;
    }

    public Long getId() {
        return id;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public long getDisplayAmountMinor() {
        return displayAmountMinor;
    }

    public long getProcessingAmountMinor() {
        return processingAmountMinor;
    }
}
