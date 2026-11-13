package com.example.BinancePriceService.service;

import com.example.BinancePriceService.Binance.BinanceProperties;
import com.example.BinancePriceService.DTO.PricePageResponse;
import com.example.BinancePriceService.DTO.TradingPairResponse;
import com.example.BinancePriceService.DTO.TrendResponse;
import com.example.BinancePriceService.Repository.TradingPairRepository;
import com.example.BinancePriceService.config.TrendProperties;
import com.example.BinancePriceService.doming.TradingPair;
import com.example.BinancePriceService.doming.TrendCalculator;
import com.example.BinancePriceService.exception.AveragePriceNotFoundException;
import com.example.BinancePriceService.exception.InvalidPeriodException;
import com.example.BinancePriceService.exception.PriceNotFoundException;
import com.example.BinancePriceService.exception.TrendNotFoundException;
import com.example.BinancePriceService.mapper.TradingPairMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PriceQueryService {

    private static final int AVERAGE_SCALE = 8;

    private final TradingPairRepository tradingPairRepository;
    private final TradingPairMapper tradingPairMapper;
    private final BinanceProperties binanceProperties;
    private final TrendProperties trendProperties;
    private final TrendCalculator trendCalculator;

    public TradingPairResponse getLatestPrice() {
        return getLatestPrice(binanceProperties.getSymbol());
    }

    public TradingPairResponse getLatestPrice(String requestedSymbol) {
        String symbol = resolveSymbol(requestedSymbol);

        return tradingPairRepository.findFirstBySymbolOrderByFetchedAtDescIdDesc(symbol)
                .map(tradingPairMapper::tradingPairToResponse)
                .orElseThrow(() -> new PriceNotFoundException(symbol));
    }

    public PricePageResponse getPrices(OffsetDateTime from, OffsetDateTime to, int page, int size) {
        validatePeriod(from, to);

        String symbol = binanceProperties.getSymbol();
        OffsetDateTime fromUtc = from.withOffsetSameInstant(ZoneOffset.UTC);
        OffsetDateTime toUtc = to.withOffsetSameInstant(ZoneOffset.UTC);
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("fetchedAt"), Sort.Order.desc("id"))
        );

        Page<TradingPair> prices = tradingPairRepository
                .findBySymbolAndFetchedAtBetween(symbol, fromUtc, toUtc, pageable);
        List<TradingPairResponse> items = prices.getContent().stream()
                .map(tradingPairMapper::tradingPairToResponse)
                .toList();

        return new PricePageResponse(
                items,
                prices.getNumber(),
                prices.getSize(),
                prices.getTotalElements(),
                prices.hasNext()
        );
    }

    public TrendResponse getTrend(OffsetDateTime from, OffsetDateTime to) {
        validatePeriod(from, to);

        String symbol = binanceProperties.getSymbol();
        OffsetDateTime fromUtc = from.withOffsetSameInstant(ZoneOffset.UTC);
        OffsetDateTime toUtc = to.withOffsetSameInstant(ZoneOffset.UTC);
        Pageable lastPoints = PageRequest.of(0, trendProperties.getWindowSize());

        List<TradingPair> prices = tradingPairRepository
                .findBySymbolAndFetchedAtBetweenOrderByFetchedAtDescIdDesc(
                        symbol,
                        fromUtc,
                        toUtc,
                        lastPoints
                );
        if (prices.isEmpty()) {
            throw new TrendNotFoundException(symbol, fromUtc, toUtc);
        }

        TrendCalculator.TrendResult result = trendCalculator.calculate(symbol, prices);

        return new TrendResponse(symbol, from.toLocalDate(), to.toLocalDate(), result.trend());
    }

    public BigDecimal getAveragePrice(String requestedSymbol, OffsetDateTime from, OffsetDateTime to) {
        validatePeriod(from, to);

        String symbol = resolveSymbol(requestedSymbol);
        OffsetDateTime fromUtc = from.withOffsetSameInstant(ZoneOffset.UTC);
        OffsetDateTime toUtc = to.withOffsetSameInstant(ZoneOffset.UTC);

        BigDecimal average = tradingPairRepository.findAveragePrice(symbol, fromUtc, toUtc)
                .orElseThrow(() -> new AveragePriceNotFoundException(symbol, fromUtc, toUtc));

        return normalize(average);
    }

    private void validatePeriod(OffsetDateTime from, OffsetDateTime to) {
        if (from == null || to == null) {
            throw new InvalidPeriodException("Both 'from' and 'to' are required");
        }
        if (from.isAfter(to)) {
            throw new InvalidPeriodException("'from' must be before or equal to 'to'");
        }
    }

    private String resolveSymbol(String requestedSymbol) {
        if (requestedSymbol == null || requestedSymbol.isBlank()) {
            return binanceProperties.getSymbol();
        }
        return requestedSymbol.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal normalize(BigDecimal average) {
        BigDecimal scaled = average
                .setScale(AVERAGE_SCALE, RoundingMode.HALF_UP)
                .stripTrailingZeros();
        return scaled.scale() < 0 ? scaled.setScale(0) : scaled;
    }
}
