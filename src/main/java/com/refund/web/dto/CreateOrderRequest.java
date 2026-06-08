package com.refund.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * Request to record an order. The order total is derived from the sum of {@code payments},
 * so splits are always internally consistent.
 *
 * <p>{@code exchangeRate} (display → processing) is optional: it defaults to 1 when the
 * display and processing currencies are the same.
 */
public record CreateOrderRequest(
        @NotBlank(message = "displayCurrency is required") String displayCurrency,
        @NotBlank(message = "processingCurrency is required") String processingCurrency,
        BigDecimal exchangeRate,
        @NotEmpty(message = "at least one payment split is required")
        @Size(max = 4, message = "an order may have at most 4 payment splits")
        @Valid List<PaymentSplitRequest> payments) {
}
