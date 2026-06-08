package com.refund.service;

import com.refund.exception.ApiException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Converts between the major units (e.g. dollars) exposed by the API and the integer
 * minor units (cents) stored and computed internally.
 *
 * <p>Assumption: every supported currency uses 2 decimal places. This keeps the prototype
 * simple; a production system would consult a per-currency exponent table (e.g. JPY = 0).
 */
public final class Money {

    private Money() {
    }

    public static long toMinor(BigDecimal majorUnits) {
        if (majorUnits == null) {
            throw ApiException.unprocessable("INVALID_AMOUNT", "Amount is required.");
        }
        return majorUnits.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    public static BigDecimal toMajor(long minor) {
        return BigDecimal.valueOf(minor, 2);
    }
}
