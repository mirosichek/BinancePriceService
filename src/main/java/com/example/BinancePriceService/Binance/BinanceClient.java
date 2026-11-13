package com.example.BinancePriceService.Binance;

import com.example.BinancePriceService.exception.BinanceClientException;
import com.example.BinancePriceService.exception.BinanceRateLimitException;
import com.example.BinancePriceService.DTO.BinanceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class BinanceClient {
    private static final String  AVG_PRICE_PATH = "/api/v3/avgPrice";
    private static final Logger log = LoggerFactory.getLogger(BinanceClient.class);
    private static final String RETRY_AFTER_HEADER = "Retry-After";

    private final RestClient restClient;
    private final BinanceProperties properties;

    public BinanceClient(RestClient.Builder builder, BinanceProperties properties) {
        this.restClient = builder.build();
        this.properties = properties;
    }

    public BinanceResponse getPrice(String symbol) {
        BinanceResponse response;

        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(AVG_PRICE_PATH)
                            .queryParam("symbol", symbol)
                            .build())
                    .retrieve()
                    .body(BinanceResponse.class);
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();

            if (status == 429 || status == 418) {
                Duration retryAfter = parseRetryAfter(ex.getResponseHeaders());
                log.warn("Binance rate limit hit (status={}, retryAfter={})", status, retryAfter);
                throw new BinanceRateLimitException(status, retryAfter, ex);
            }

            log.warn("Binance request failed (status={})", status);
            throw new BinanceClientException(
                    "Binance request failed with status " + status,
                    status,
                    ex
            );
        } catch (ResourceAccessException ex) {
            log.warn("Binance is unavailable: {}", ex.getMessage());
            throw new BinanceClientException("Binance is unavailable: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            log.warn("Binance request failed: {}", ex.getMessage());
            throw new BinanceClientException("Binance request failed: " + ex.getMessage(), ex);
        }

        validate(response, symbol);
        response.setSymbol(symbol);
        log.debug("Fetched price for {}: {}", symbol, response.getPrice());
        return response;
    }
    private void validate(BinanceResponse response, String symbol) {
        if (response == null) {
            throw new BinanceClientException(
                    "Binance returned an empty response for symbol " + symbol
            );
        }
        if (response.getPrice() == null || response.getPrice().signum() <= 0) {
            throw new BinanceClientException(
                    "Binance returned an invalid price for symbol " + symbol
            );
        }
    }

    private Duration parseRetryAfter(HttpHeaders headers) {
        if (headers == null) {
            return properties.getRateLimitDefaultRetryAfter();
        }

        String value = headers.getFirst(RETRY_AFTER_HEADER);
        if (value == null || value.isBlank()) {
            return properties.getRateLimitDefaultRetryAfter();
        }

        try {
            long seconds = Long.parseLong(value.trim());
            return seconds >= 0
                    ? Duration.ofSeconds(seconds)
                    : properties.getRateLimitDefaultRetryAfter();
        } catch (NumberFormatException ex) {
            try {
                ZonedDateTime retryAt = ZonedDateTime.parse(
                        value,
                        DateTimeFormatter.RFC_1123_DATE_TIME
                );
                Duration duration = Duration.between(Instant.now(), retryAt.toInstant());
                return duration.isNegative() ? Duration.ZERO : duration;
            } catch (DateTimeParseException parseException) {
                log.warn(
                        "Invalid Retry-After header '{}', using default {}",
                        value,
                        properties.getRateLimitDefaultRetryAfter()
                );
                return properties.getRateLimitDefaultRetryAfter();
            }
        }
    }
}
