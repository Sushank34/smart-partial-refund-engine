package com.refund.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One payment instrument used to pay for an order, and how much of the order
 * total (in display-currency minor units) flowed through it. The set of splits
 * on an order defines the proportions a refund is distributed back across.
 */
@Entity
@Table(name = "payment_splits")
public class PaymentSplit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    /** Amount paid through this method, in display-currency minor units (cents). */
    private long amountMinor;

    protected PaymentSplit() {
    }

    public PaymentSplit(PaymentMethod method, long amountMinor) {
        this.method = method;
        this.amountMinor = amountMinor;
    }

    public Long getId() {
        return id;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public long getAmountMinor() {
        return amountMinor;
    }
}
