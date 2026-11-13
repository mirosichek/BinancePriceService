package com.example.BinancePriceService.Binance;

import com.example.BinancePriceService.exception.AveragePriceNotFoundException;
import com.example.BinancePriceService.exception.BinanceClientException;
import com.example.BinancePriceService.exception.BinanceRateLimitException;
import com.example.BinancePriceService.DTO.BinanceResponse;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * Интеграционный тест HTTP-слоя: настоящий {@link RestClient} против WireMock.
 */
class BinanceClientWireMockTest {

    private static final String SYMBOL = "BNBUSDT";
    private static final String AVG_PRICE_PATH = "/api/v3/avgPrice";

    private static WireMockServer server;

    private BinanceClient client;

    @BeforeAll
    static void startServer() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @BeforeEach
    void setUp() {
        server.resetAll();

        BinanceProperties properties = new BinanceProperties();
        properties.setBaseUrl(server.baseUrl());
        properties.setRateLimitDefaultRetryAfter(Duration.ofSeconds(30));

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(server.baseUrl())
                .requestFactory(new SimpleClientHttpRequestFactory());

        client = new BinanceClient(builder, properties);
    }

    @Test
    void parsesAveragePriceResponse() {
        server.stubFor(get(urlPathEqualTo(AVG_PRICE_PATH))
                .withQueryParam("symbol", equalTo(SYMBOL))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"mins":5,"price":"654.85561546","closeTime":1762980271000}
                                """)));

        BinanceResponse response = client.getPrice(SYMBOL);

        assertThat(response.getSymbol()).isEqualTo(SYMBOL);
        assertThat(response.getPrice()).isEqualByComparingTo("654.85561546");
        assertThat(response.getCloseTime()).isEqualTo(1762980271000L);
    }

    @Test
    void mapsTooManyRequestsToRateLimitExceptionWithRetryAfterHeader() {
        server.stubFor(get(urlPathEqualTo(AVG_PRICE_PATH))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Retry-After", "120")
                        .withBody("{\"code\":-1003,\"msg\":\"Too many requests\"}")));

        BinanceRateLimitException exception = catchThrowableOfType(
                () -> client.getPrice(SYMBOL),
                BinanceRateLimitException.class
        );

        assertThat(exception.getStatusCode()).isEqualTo(429);
        assertThat(exception.getRetryAfter()).isEqualTo(Duration.ofSeconds(120));
    }

    @Test
    void fallsBackToDefaultRetryAfterWhenHeaderIsMissing() {
        server.stubFor(get(urlPathEqualTo(AVG_PRICE_PATH))
                .willReturn(aResponse().withStatus(418)));

        BinanceRateLimitException exception = catchThrowableOfType(
                () -> client.getPrice(SYMBOL),
                BinanceRateLimitException.class
        );

        assertThat(exception.getStatusCode()).isEqualTo(418);
        assertThat(exception.getRetryAfter()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void mapsServerErrorToClientException() {
        server.stubFor(get(urlPathEqualTo(AVG_PRICE_PATH))
                .willReturn(aResponse().withStatus(500).withBody("oops")));

        BinanceClientException exception = catchThrowableOfType(
                () -> client.getPrice(SYMBOL),
                BinanceClientException.class
        );

        assertThat(exception.getStatusCode()).isEqualTo(500);
    }

    @Test
    void rejectsNonPositivePrice() {
        server.stubFor(get(urlPathEqualTo(AVG_PRICE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"mins\":5,\"price\":\"0\"}")));

        assertThatThrownBy(() -> client.getPrice(SYMBOL))
                .isInstanceOf(BinanceClientException.class)
                .hasMessageContaining("invalid price");
    }
}
