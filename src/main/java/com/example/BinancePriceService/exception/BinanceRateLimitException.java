package com.example.BinancePriceService.exception;

import java.time.Duration;
import java.util.Objects;

public class BinanceRateLimitException extends BinanceException {

    private final int statusCode;
    private final Duration retryAfter;

    public BinanceRateLimitException(int statusCode, Duration retryAfter, Throwable cause) {
        super("Binance rate limit exceeded (status=" + statusCode + ", retryAfter=" + retryAfter + ")", cause);
        this.statusCode = statusCode;
        this.retryAfter = Objects.requireNonNull(retryAfter, "retryAfter");
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}
