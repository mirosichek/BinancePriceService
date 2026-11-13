package com.example.BinancePriceService.Binance;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Component
@Validated
@ConfigurationProperties(prefix = "binance")
public class BinanceProperties {

    @NotBlank
    private String baseUrl = "https://api.binance.com";

    @Pattern(
            regexp = "[A-Z0-9]{5,20}",
            message = "binance.symbol must contain 5-20 uppercase letters or digits"
    )
    private String symbol = "BNBUSDT";

    @NotNull
    private Duration connectTimeout = Duration.ofSeconds(5);

    @NotNull
    private Duration readTimeout = Duration.ofSeconds(10);

    @NotNull
    private Duration pollInterval = Duration.ofSeconds(30);

    @NotNull
    private Duration rateLimitDefaultRetryAfter = Duration.ofMinutes(1);

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public Duration getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(Duration pollInterval) {
        this.pollInterval = pollInterval;
    }

    public Duration getRateLimitDefaultRetryAfter() {
        return rateLimitDefaultRetryAfter;
    }

    public void setRateLimitDefaultRetryAfter(Duration rateLimitDefaultRetryAfter) {
        this.rateLimitDefaultRetryAfter = rateLimitDefaultRetryAfter;
    }

    @AssertTrue(message = "binance.connect-timeout must be greater than zero")
    public boolean isConnectTimeoutPositive() {
        return isPositive(connectTimeout);
    }

    @AssertTrue(message = "binance.read-timeout must be greater than zero")
    public boolean isReadTimeoutPositive() {
        return isPositive(readTimeout);
    }

    @AssertTrue(message = "binance.poll-interval must be greater than zero")
    public boolean isPollIntervalPositive() {
        return isPositive(pollInterval);
    }

    @AssertTrue(message = "binance.rate-limit-default-retry-after must be greater than zero")
    public boolean isRateLimitDefaultRetryAfterPositive() {
        return isPositive(rateLimitDefaultRetryAfter);
    }

    private static boolean isPositive(Duration duration) {
        return duration != null && !duration.isZero() && !duration.isNegative();
    }
}
