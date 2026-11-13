package com.example.BinancePriceService.config;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Component
@Validated
@ConfigurationProperties(prefix = "trend")
public class TrendProperties {

    @Min(2)
    private int windowSize = 20;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal flatThresholdPct = new BigDecimal("0.1");

    public int getWindowSize() {
        return windowSize;
    }

    public void setWindowSize(int windowSize) {
        this.windowSize = windowSize;
    }

    public BigDecimal getFlatThresholdPct() {
        return flatThresholdPct;
    }

    public void setFlatThresholdPct(BigDecimal flatThresholdPct) {
        this.flatThresholdPct = flatThresholdPct;
    }
}

