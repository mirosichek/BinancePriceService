package com.example.BinancePriceService.exception;

import java.time.OffsetDateTime;

public class TrendNotFoundException extends RuntimeException {

    public TrendNotFoundException(String symbol, OffsetDateTime from, OffsetDateTime to) {
        super("No data for symbol " + symbol + " between " + from + " and " + to);
    }
}
