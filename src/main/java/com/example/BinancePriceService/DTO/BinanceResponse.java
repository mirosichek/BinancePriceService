package com.example.BinancePriceService.DTO;

import java.math.BigDecimal;

public class BinanceResponse {

    private BigDecimal price;
    private Long closeTime;
    private String symbol;

    public BinanceResponse() {
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Long getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(Long closeTime) {
        this.closeTime = closeTime;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
}
