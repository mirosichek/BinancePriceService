package com.example.BinancePriceService.service;

import com.example.BinancePriceService.Binance.BinanceClient;
import com.example.BinancePriceService.Binance.BinanceProperties;
import com.example.BinancePriceService.DTO.BinanceResponse;
import com.example.BinancePriceService.Repository.TradingPairWriter;
import com.example.BinancePriceService.Repository.WriteResult;
import com.example.BinancePriceService.doming.TradingPair;
import com.example.BinancePriceService.exception.AveragePriceNotFoundException;
import com.example.BinancePriceService.exception.BinanceClientException;
import com.example.BinancePriceService.mapper.TradingPairMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricePollingServiceTest {

    private static final String SYMBOL = "BNBUSDT";
    private static final OffsetDateTime FETCHED_AT = OffsetDateTime.parse("2026-11-12T21:04:31Z");

    @Mock
    private BinanceClient binanceClient;

    @Mock
    private TradingPairMapper tradingPairMapper;

    @Mock
    private TradingPairWriter tradingPairWriter;

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private PricePollingService service;
    private BinanceResponse binanceResponse;
    private TradingPair tradingPair;

    @BeforeEach
    void setUp() {
        BinanceProperties properties = new BinanceProperties();
        properties.setSymbol(SYMBOL);

        service = new PricePollingService(
                binanceClient,
                properties,
                tradingPairMapper,
                tradingPairWriter,
                meterRegistry
        );

        binanceResponse = new BinanceResponse();
        binanceResponse.setSymbol(SYMBOL);
        binanceResponse.setPrice(new BigDecimal("654.85561546"));
        tradingPair = new TradingPair(SYMBOL, new BigDecimal("654.85561546"), FETCHED_AT);
    }

    @Test
    void savesPriceAndRecordsSavedMetric() {
        when(binanceClient.getPrice(SYMBOL)).thenReturn(binanceResponse);
        when(tradingPairMapper.responseToTradingPair(binanceResponse)).thenReturn(tradingPair);
        when(tradingPairWriter.saveToDB(tradingPair)).thenReturn(WriteResult.SAVED);

        WriteResult result = service.poll();

        assertThat(result).isEqualTo(WriteResult.SAVED);
        assertThat(meterRegistry.counter("binance.poll.result", "result", "saved").count())
                .isEqualTo(1.0);
        assertThat(meterRegistry.timer("binance.poll.duration").count()).isEqualTo(1L);
    }

    @Test
    void countsDuplicateWrites() {
        when(binanceClient.getPrice(SYMBOL)).thenReturn(binanceResponse);
        when(tradingPairMapper.responseToTradingPair(binanceResponse)).thenReturn(tradingPair);
        when(tradingPairWriter.saveToDB(tradingPair)).thenReturn(WriteResult.DUPLICATE);

        assertThat(service.poll()).isEqualTo(WriteResult.DUPLICATE);
        assertThat(meterRegistry.counter("binance.poll.result", "result", "duplicate").count())
                .isEqualTo(1.0);
    }

    @Test
    void recordsFailureMetricAndRethrows() {
        when(binanceClient.getPrice(SYMBOL)).thenThrow(new BinanceClientException("Binance is unavailable"));

        assertThatThrownBy(() -> service.poll())
                .isInstanceOf(BinanceClientException.class);

        assertThat(meterRegistry.counter("binance.poll.result", "result", "failure").count())
                .isEqualTo(1.0);
        assertThat(meterRegistry.timer("binance.poll.duration").count()).isEqualTo(1L);
        verify(tradingPairWriter, never()).saveToDB(any());
    }
}
