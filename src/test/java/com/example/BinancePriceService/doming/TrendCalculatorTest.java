package com.example.BinancePriceService.doming;

import com.example.BinancePriceService.config.TrendProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrendCalculatorTest {

    private static final OffsetDateTime BASE_TIME =
            OffsetDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    private final TrendProperties properties = new TrendProperties();

    private TrendCalculator calculator() {
        return new TrendCalculator(properties);
    }

    @Test
    void detectsUpwardTrend() {
        TrendCalculator.TrendResult result = calculator()
                .calculate("BNBUSDT", prices("100", "101", "102", "103", "104"));

        assertThat(result.trend()).isEqualTo(TrendCalculator.Trend.UPWARD);
        assertThat(result.symbol()).isEqualTo("BNBUSDT");
        assertThat(result.points()).isEqualTo(5);
    }

    @Test
    void detectsDownwardTrend() {
        TrendCalculator.TrendResult result = calculator()
                .calculate("BNBUSDT", prices("104", "103", "102", "101", "100"));

        assertThat(result.trend()).isEqualTo(TrendCalculator.Trend.DOWNWARD);
    }

    @Test
    void returnsFlatWhenChangeIsWithinThreshold() {
        TrendCalculator.TrendResult result = calculator()
                .calculate("BNBUSDT", prices("100.00", "100.01", "99.99", "100.02", "100.00"));

        assertThat(result.trend()).isEqualTo(TrendCalculator.Trend.FLAT);
        assertThat(result.changePct().abs()).isLessThan(properties.getFlatThresholdPct());
    }

    @Test
    void returnsFlatForSinglePoint() {
        TrendCalculator.TrendResult result = calculator()
                .calculate("BNBUSDT", prices("100"));

        assertThat(result.trend()).isEqualTo(TrendCalculator.Trend.FLAT);
        assertThat(result.points()).isEqualTo(1);
    }

    @Test
    void sortsUnorderedInputByFetchedAt() {
        List<TradingPair> unordered = new ArrayList<>(prices("100", "101", "102", "103"));
        Collections.shuffle(unordered, new java.util.Random(42));

        TrendCalculator.TrendResult result = calculator().calculate("BNBUSDT", unordered);

        assertThat(result.trend()).isEqualTo(TrendCalculator.Trend.UPWARD);
    }

    @Test
    void usesOnlyLastWindowPoints() {
        properties.setWindowSize(3);

        // Сильный рост, который заканчивается тремя почти одинаковыми значениями.
        TrendCalculator.TrendResult result = calculator().calculate(
                "BNBUSDT",
                prices("100", "110", "120", "130", "130.01", "130.02")
        );

        assertThat(result.points()).isEqualTo(3);
        assertThat(result.trend()).isEqualTo(TrendCalculator.Trend.FLAT);
    }

    @Test
    void respectsConfiguredFlatThreshold() {
        properties.setFlatThresholdPct(new BigDecimal("0.0001"));

        TrendCalculator.TrendResult result = calculator()
                .calculate("BNBUSDT", prices("100.00", "100.01", "99.99", "100.02", "100.00"));

        assertThat(result.trend()).isEqualTo(TrendCalculator.Trend.UPWARD);
    }

    @Test
    void rejectsEmptyInput() {
        assertThatThrownBy(() -> calculator().calculate("BNBUSDT", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be empty");
    }

    private static List<TradingPair> prices(String... values) {
        List<TradingPair> result = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            result.add(new TradingPair(
                    "BNBUSDT",
                    new BigDecimal(values[i]),
                    BASE_TIME.plusMinutes(i)
            ));
        }
        return result;
    }
}
