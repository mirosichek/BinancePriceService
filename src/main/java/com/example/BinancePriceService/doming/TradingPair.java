package com.example.BinancePriceService.doming;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "trading_pair")
public class TradingPair {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String symbol;
    private BigDecimal price;
    private OffsetDateTime fetchedAt;

    public TradingPair() {
    }

    public TradingPair(String symbol, BigDecimal price, OffsetDateTime fetchedAt) {
        this.symbol = symbol;
        this.price = price;
        this.fetchedAt = fetchedAt;
    }

    public Long getId() {
        return id;
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
