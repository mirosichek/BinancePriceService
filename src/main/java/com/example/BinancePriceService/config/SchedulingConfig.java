package com.example.BinancePriceService.config;

import com.example.BinancePriceService.Binance.BinanceProperties;
import com.example.BinancePriceService.Schedule.BinanceSchedule;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.FixedDelayTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "binance.scheduling",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class SchedulingConfig implements SchedulingConfigurer {

    private final BinanceProperties properties;
    private final BinanceSchedule binanceSchedule;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        Duration interval = properties.getPollInterval();
        taskRegistrar.addFixedDelayTask(
                new FixedDelayTask(binanceSchedule::run, interval, Duration.ZERO)
        );
    }
}
