package com.example.BinancePriceService.doming;

import com.example.BinancePriceService.config.TrendProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Comparator;
import java.util.List;

@Component
public class TrendCalculator {

    private static final MathContext MATH_CONTEXT = MathContext.DECIMAL128;

    private final TrendProperties properties;

    public TrendCalculator(TrendProperties properties) {
        this.properties = properties;
    }

    public enum Trend { UPWARD, DOWNWARD, FLAT }

    public record TrendResult(
            String symbol,
            Trend trend,
            BigDecimal slope,       // наклон линии регрессии (цена за одну точку окна)
            BigDecimal changePct,   // изменение за окно по линии регрессии, %
            int points) {}

    public TrendResult calculate(String symbol, List<TradingPair> prices) {
        if (prices == null || prices.isEmpty()) {
            throw new IllegalArgumentException("Prices must not be empty");
        }

        List<TradingPair> ordered = prices.stream()
                .sorted(Comparator.comparing(TradingPair::getFetchedAt))
                .toList();

        int windowSize = properties.getWindowSize();
        int start = Math.max(0, ordered.size() - windowSize);
        List<TradingPair> window = ordered.subList(start, ordered.size());
        int points = window.size();

        BigDecimal sumX = BigDecimal.ZERO;
        BigDecimal sumY = BigDecimal.ZERO;
        BigDecimal sumXY = BigDecimal.ZERO;
        BigDecimal sumXX = BigDecimal.ZERO;

        for (int i = 0; i < points; i++) {
            BigDecimal x = BigDecimal.valueOf(i);
            BigDecimal y = window.get(i).getPrice();

            sumX = sumX.add(x);
            sumY = sumY.add(y);
            sumXY = sumXY.add(x.multiply(y));
            sumXX = sumXX.add(x.multiply(x));
        }

        BigDecimal pointsCount = BigDecimal.valueOf(points);
        BigDecimal meanY = sumY.divide(pointsCount, MATH_CONTEXT);

        BigDecimal denominator = pointsCount
                .multiply(sumXX)
                .subtract(sumX.multiply(sumX));

        BigDecimal slope = denominator.signum() == 0
                ? BigDecimal.ZERO
                : pointsCount
                .multiply(sumXY)
                .subtract(sumX.multiply(sumY))
                .divide(denominator, MATH_CONTEXT);

        BigDecimal predictedChange = slope.multiply(BigDecimal.valueOf(points - 1L));

        BigDecimal changePct = meanY.signum() == 0
                ? BigDecimal.ZERO
                : predictedChange
                .divide(meanY, MATH_CONTEXT)
                .multiply(BigDecimal.valueOf(100));

        BigDecimal flatThreshold = properties.getFlatThresholdPct();

        Trend trend;
        if (changePct.abs().compareTo(flatThreshold) < 0) {
            trend = Trend.FLAT;
        } else {
            trend = changePct.signum() > 0 ? Trend.UPWARD : Trend.DOWNWARD;
        }

        return new TrendResult(symbol, trend, slope, changePct, points);
    }
}
