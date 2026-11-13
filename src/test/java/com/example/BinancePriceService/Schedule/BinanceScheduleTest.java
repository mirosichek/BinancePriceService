package com.example.BinancePriceService.Schedule;

import com.example.BinancePriceService.exception.AveragePriceNotFoundException;
import com.example.BinancePriceService.exception.BinanceClientException;
import com.example.BinancePriceService.exception.BinanceRateLimitException;
import com.example.BinancePriceService.Repository.WriteResult;
import com.example.BinancePriceService.service.PricePollingService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BinanceScheduleTest {

    @Mock
    private PricePollingService pollingService;

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-13T10:00:00Z"));

    private BinanceSchedule schedule;

    @BeforeEach
    void setUp() {
        schedule = new BinanceSchedule(pollingService, clock, meterRegistry);
    }

    @Test
    void pollsWhenNotRateLimited() {
        when(pollingService.poll()).thenReturn(WriteResult.SAVED);

        schedule.run();

        verify(pollingService).poll();
        assertThat(meterRegistry.find("binance.poll.rate_limited").counter()).isNull();
    }

    @Test
    void pausesPollingAfterRateLimit() {
        when(pollingService.poll())
                .thenThrow(new BinanceRateLimitException(429, Duration.ofSeconds(60), null));

        schedule.run();
        schedule.run();

        verify(pollingService, times(1)).poll();
        assertThat(meterRegistry.counter("binance.poll.rate_limited").count()).isEqualTo(1.0);
    }

    @Test
    void resumesPollingWhenPauseExpires() {
        when(pollingService.poll())
                .thenThrow(new BinanceRateLimitException(429, Duration.ofSeconds(60), null))
                .thenReturn(WriteResult.SAVED);

        schedule.run();
        clock.advance(Duration.ofSeconds(61));
        schedule.run();

        verify(pollingService, times(2)).poll();
    }

    @Test
    void swallowsUnexpectedPollingFailures() {
        when(pollingService.poll()).thenThrow(new BinanceClientException("Binance is unavailable"));

        assertThatCode(() -> schedule.run()).doesNotThrowAnyException();

        assertThat(meterRegistry.find("binance.poll.rate_limited").counter()).isNull();
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
