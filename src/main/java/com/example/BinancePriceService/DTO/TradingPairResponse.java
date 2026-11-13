package com.example.BinancePriceService.DTO;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class TradingPairResponse {

    private final String symbol;
    private final BigDecimal price;
    private final OffsetDateTime fetchedAt;

    public TradingPairResponse(String symbol, BigDecimal price, OffsetDateTime fetchedAt) {
        this.symbol = symbol;
        this.price = price;
        this.fetchedAt = fetchedAt;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public OffsetDateTime getFetchedAt() {
        return fetchedAt;
    }
}
