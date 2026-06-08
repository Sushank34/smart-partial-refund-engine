package com.refund.web.dto;

import com.refund.domain.ReasonCode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Request to issue a partial (or full) refund against an order, in display-currency major units. */
public record CreateRefundRequest(
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be positive") BigDecimal amount,
        @NotNull(message = "reasonCode is required") ReasonCode reasonCode) {
}
