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
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A placed order and the payment splits that funded it.
 *
 * <p>All money is stored in <b>minor units</b> (cents) of the {@code displayCurrency}
 * as a {@code long} — never floating point — so refund arithmetic is exact.
 * {@code exchangeRate} converts display → processing currency (e.g. 1 USD = 3.75 PEN),
 * captured at payment time and reused for every later refund.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    private String id;

    /** Optimistic-lock guard: blocks concurrent refunds from racing on the balance and over-refunding. */
    @Version
    private long version;

    private String displayCurrency;

    private String processingCurrency;

    /** Display → processing rate captured at payment time. 1 when both currencies match. */
    private BigDecimal exchangeRate;

    /** Order total in display-currency minor units (sum of the payment splits). */
    private long totalAmountMinor;

    /** Running sum of all refunds issued, in display-currency minor units. */
    private long totalRefundedMinor;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private Instant createdAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id")
    private List<PaymentSplit> splits = new ArrayList<>();

    protected Order() {
    }

    public Order(String id, String displayCurrency, String processingCurrency, BigDecimal exchangeRate,
                 List<PaymentSplit> splits, Instant createdAt) {
        this.id = id;
        this.displayCurrency = displayCurrency;
        this.processingCurrency = processingCurrency;
        this.exchangeRate = exchangeRate;
        this.splits = splits;
        this.totalAmountMinor = splits.stream().mapToLong(PaymentSplit::getAmountMinor).sum();
        this.totalRefundedMinor = 0L;
        this.status = OrderStatus.ACTIVE;
        this.createdAt = createdAt;
    }

    /** Amount still available to refund, in display-currency minor units. */
    public long refundableRemainingMinor() {
        return totalAmountMinor - totalRefundedMinor;
    }

    /** Record an issued refund and flip status to FULLY_REFUNDED once nothing remains. */
    public void applyRefund(long refundMinor) {
        this.totalRefundedMinor += refundMinor;
        if (refundableRemainingMinor() <= 0) {
            this.status = OrderStatus.FULLY_REFUNDED;
        }
    }

    public String getId() {
        return id;
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

    public long getTotalAmountMinor() {
        return totalAmountMinor;
    }

    public long getTotalRefundedMinor() {
        return totalRefundedMinor;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<PaymentSplit> getSplits() {
        return splits;
    }
}
