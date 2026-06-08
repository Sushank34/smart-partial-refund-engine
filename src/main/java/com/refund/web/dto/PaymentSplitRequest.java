package com.refund.web.dto;

import com.refund.domain.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** One payment method and the amount paid through it, in display-currency major units. */
public record PaymentSplitRequest(
        @NotNull(message = "method is required") PaymentMethod method,
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be positive") BigDecimal amount) {
}
