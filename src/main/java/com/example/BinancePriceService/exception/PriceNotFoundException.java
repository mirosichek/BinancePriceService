package com.example.BinancePriceService.exception;

public class PriceNotFoundException extends RuntimeException {

    public PriceNotFoundException(String symbol) {
        super("No price found for symbol " + symbol);
    }
}
