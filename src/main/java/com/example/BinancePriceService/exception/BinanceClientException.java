package com.example.BinancePriceService.exception;

public class BinanceClientException extends BinanceException {
    private final int statusCode;

    public BinanceClientException(String message) {
        super(message);
        this.statusCode = 0;
    }

    public BinanceClientException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
    }

    public BinanceClientException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
