package com.example.BinancePriceService.Schedule;

import com.example.BinancePriceService.exception.BinanceRateLimitException;
import com.example.BinancePriceService.Repository.WriteResult;
import com.example.BinancePriceService.service.PricePollingService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class BinanceSchedule {
    private static final Logger log = LoggerFactory.getLogger(BinanceSchedule.class);

    private final PricePollingService pricePollingService;
    private final Clock clock;
    private final MeterRegistry meterRegistry;

    private volatile Instant pausedUntil = Instant.MIN;

    public void run() {
        Instant now = clock.instant();
        if (now.isBefore(pausedUntil)) {
            log.info("Binance polling paused until {}", pausedUntil);
            return;
        }

        try {
            WriteResult result = pricePollingService.poll();
            log.debug("Binance poll finished with result {}", result);
        } catch (BinanceRateLimitException ex) {
            pausedUntil = clock.instant().plus(ex.getRetryAfter());
            meterRegistry.counter("binance.poll.rate_limited").increment();
            log.warn(
                    "Binance rate limit hit (status={}), polling paused until {}",
                    ex.getStatusCode(),
                    pausedUntil
            );
        } catch (Exception ex) {
            log.error("Binance polling failed: {}", ex.getMessage(), ex);
        }
    }
}
