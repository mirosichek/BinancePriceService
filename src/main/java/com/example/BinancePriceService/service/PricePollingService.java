package com.example.BinancePriceService.service;

import com.example.BinancePriceService.Binance.BinanceClient;
import com.example.BinancePriceService.Binance.BinanceProperties;
import com.example.BinancePriceService.DTO.BinanceResponse;
import com.example.BinancePriceService.Repository.TradingPairWriter;
import com.example.BinancePriceService.Repository.WriteResult;
import com.example.BinancePriceService.doming.TradingPair;
import com.example.BinancePriceService.mapper.TradingPairMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PricePollingService {
    private static final Logger log = LoggerFactory.getLogger(PricePollingService.class);

    private final BinanceClient binanceClient;
    private final BinanceProperties binanceProperties;
    private final TradingPairMapper tradingPairMapper;
    private final TradingPairWriter tradingPairWriter;
    private final MeterRegistry meterRegistry;

    public WriteResult poll() {
        Timer.Sample sample = Timer.start(meterRegistry);
        String symbol = binanceProperties.getSymbol();

        try {
            BinanceResponse response = binanceClient.getPrice(symbol);
            TradingPair tradingPair = tradingPairMapper.responseToTradingPair(response);
            WriteResult result = tradingPairWriter.saveToDB(tradingPair);

            meterRegistry.counter(
                    "binance.poll.result",
                    "result",
                    result.name().toLowerCase(Locale.ROOT)
            ).increment();
            log.debug("Polled {}: {} ({})", symbol, response.getPrice(), result);
            return result;
        } catch (RuntimeException ex) {
            meterRegistry.counter("binance.poll.result", "result", "failure").increment();
            throw ex;
        } finally {
            sample.stop(meterRegistry.timer("binance.poll.duration"));
        }
    }
}
