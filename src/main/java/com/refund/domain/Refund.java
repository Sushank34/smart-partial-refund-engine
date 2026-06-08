package com.refund.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * An immutable audit record of one partial (or full) refund: what was requested,
 * why, when, and exactly how it was distributed across payment methods and currencies.
 * Multiple refunds may exist for one order; together they form its refund history.
 */
@Entity
@Table(name = "refunds")
public class Refund {

    @Id
    private String id;

    private String orderId;

    /** Requested amount in display-currency minor units. */
    private long requestedAmountMinor;

    /** Requested amount converted to processing-currency minor units at the order's rate. */
    private long requestedProcessingMinor;

    // Currency context snapshotted at refund time, so this audit record stands on its own
    // even if the order is later amended.
    private String displayCurrency;

    private String processingCurrency;

    private BigDecimal exchangeRate;

    @Enumerated(EnumType.STRING)
    private ReasonCode reasonCode;

    @Enumerated(EnumType.STRING)
    private RefundStatus status;

    /** Human-readable note, populated when the refund is FLAGGED. */
    private String note;

    private Instant createdAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "refund_id")
    private List<RefundAllocation> allocations = new ArrayList<>();

    protected Refund() {
    }

    public Refund(String id, String orderId, long requestedAmountMinor, long requestedProcessingMinor,
                  String displayCurrency, String processingCurrency, BigDecimal exchangeRate,
                  ReasonCode reasonCode, RefundStatus status, String note,
                  List<RefundAllocation> allocations, Instant createdAt) {
        this.id = id;
        this.orderId = orderId;
        this.requestedAmountMinor = requestedAmountMinor;
        this.requestedProcessingMinor = requestedProcessingMinor;
        this.displayCurrency = displayCurrency;
        this.processingCurrency = processingCurrency;
        this.exchangeRate = exchangeRate;
        this.reasonCode = reasonCode;
        this.status = status;
        this.note = note;
        this.allocations = allocations;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getOrderId() {
        return orderId;
    }

    public long getRequestedAmountMinor() {
        return requestedAmountMinor;
    }

    public long getRequestedProcessingMinor() {
        return requestedProcessingMinor;
    }

    public String getDisplayCurrency() {
        return displayCurrency;
    }

    public String getProcessingCurrency() {
        return processingCurrency;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public ReasonCode getReasonCode() {
        return reasonCode;
    }

    public RefundStatus getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<RefundAllocation> getAllocations() {
        return allocations;
    }
}
