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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceQueryServiceTest {

    private static final String CONFIGURED_SYMBOL = "BNBUSDT";
    private static final OffsetDateTime FROM = OffsetDateTime.parse("2026-11-12T00:00:00Z");
    private static final OffsetDateTime TO = OffsetDateTime.parse("2026-11-13T00:00:00Z");
    private static final OffsetDateTime FETCHED_AT = OffsetDateTime.parse("2026-11-12T21:04:31Z");
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-11-13T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private TradingPairRepository repository;

    @Mock
    private TrendCalculator trendCalculator;

    private TrendProperties trendProperties;
    private PriceQueryService service;

    @BeforeEach
    void setUp() {
        BinanceProperties binanceProperties = new BinanceProperties();
        binanceProperties.setSymbol(CONFIGURED_SYMBOL);

        trendProperties = new TrendProperties();
        trendProperties.setWindowSize(20);

        service = new PriceQueryService(
                repository,
                new TradingPairMapper(CLOCK),
                binanceProperties,
                trendProperties,
                trendCalculator
        );
    }

    @Test
    void returnsLatestPriceForConfiguredSymbol() {
        when(repository.findFirstBySymbolOrderByFetchedAtDescIdDesc(CONFIGURED_SYMBOL))
                .thenReturn(Optional.of(tradingPair("654.85561546")));

        TradingPairResponse response = service.getLatestPrice();

        assertThat(response.getSymbol()).isEqualTo(CONFIGURED_SYMBOL);
        assertThat(response.getPrice()).isEqualByComparingTo("654.85561546");
        assertThat(response.getFetchedAt()).isEqualTo(FETCHED_AT);
    }

    @Test
    void normalizesRequestedSymbolToUpperCase() {
        when(repository.findFirstBySymbolOrderByFetchedAtDescIdDesc("BTCUSDT"))
                .thenReturn(Optional.of(new TradingPair("BTCUSDT", new BigDecimal("75786.9"), FETCHED_AT)));

        TradingPairResponse response = service.getLatestPrice(" btcusdt ");

        assertThat(response.getSymbol()).isEqualTo("BTCUSDT");
    }

    @Test
    void throwsWhenLatestPriceIsMissing() {
        when(repository.findFirstBySymbolOrderByFetchedAtDescIdDesc(CONFIGURED_SYMBOL))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLatestPrice())
                .isInstanceOf(PriceNotFoundException.class)
                .hasMessageContaining(CONFIGURED_SYMBOL);
    }

    @Test
    void calculatesAverageAndRoundsToEightDigits() {
        when(repository.findAveragePrice(CONFIGURED_SYMBOL, FROM, TO))
                .thenReturn(Optional.of(new BigDecimal("654.4947302212")));

        BigDecimal average = service.getAveragePrice(null, FROM, TO);

        assertThat(average.toPlainString()).isEqualTo("654.49473022");
    }

    @Test
    void stripsTrailingZerosFromAverage() {
        when(repository.findAveragePrice(any(), any(), any()))
                .thenReturn(Optional.of(new BigDecimal("10.2000000000")));

        assertThat(service.getAveragePrice(null, FROM, TO).toPlainString()).isEqualTo("10.2");
    }

    @Test
    void convertsPeriodToUtcBeforeAverageQuery() {
        OffsetDateTime fromWithOffset = OffsetDateTime.parse("2026-11-12T03:00:00+03:00");
        when(repository.findAveragePrice(any(), any(), any()))
                .thenReturn(Optional.of(new BigDecimal("100")));

        service.getAveragePrice(null, fromWithOffset, fromWithOffset.plusDays(1));

        verify(repository).findAveragePrice(
                eq(CONFIGURED_SYMBOL),
                eq(FROM),
                eq(TO)
        );
    }

    @Test
    void throwsWhenAveragePeriodHasNoData() {
        when(repository.findAveragePrice(any(), any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAveragePrice(null, FROM, TO))
                .isInstanceOf(AveragePriceNotFoundException.class);
    }

    @Test
    void rejectsInvertedPeriodForAverage() {
        assertThatThrownBy(() -> service.getAveragePrice(null, TO, FROM))
                .isInstanceOf(InvalidPeriodException.class)
                .hasMessageContaining("'from' must be before or equal to 'to'");

        verifyNoInteractions(repository);
    }

    @Test
    void mapsPageOfPricesNewestFirst() {
        List<TradingPair> content = List.of(
                new TradingPair(CONFIGURED_SYMBOL, new BigDecimal("654.85561546"), FETCHED_AT),
                new TradingPair(CONFIGURED_SYMBOL, new BigDecimal("654.65072070"), FETCHED_AT.minusMinutes(2))
        );
        Page<TradingPair> page = new PageImpl<>(content, PageRequest.of(1, 5), 25);
        when(repository.findBySymbolAndFetchedAtBetween(eq(CONFIGURED_SYMBOL), eq(FROM), eq(TO), any()))
                .thenReturn(page);

        PricePageResponse response = service.getPrices(FROM, TO, 1, 5);

        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(5);
        assertThat(response.getTotalElements()).isEqualTo(25);
        assertThat(response.isHasNext()).isTrue();
        assertThat(response.getItems())
                .extracting(TradingPairResponse::getPrice)
                .containsExactly(new BigDecimal("654.85561546"), new BigDecimal("654.65072070"));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findBySymbolAndFetchedAtBetween(
                eq(CONFIGURED_SYMBOL),
                eq(FROM),
                eq(TO),
                pageable.capture()
        );
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
        assertThat(pageable.getValue().getSort())
                .containsExactly(Sort.Order.desc("fetchedAt"), Sort.Order.desc("id"));
    }

    @Test
    void returnsEmptyPageWhenPeriodHasNoData() {
        when(repository.findBySymbolAndFetchedAtBetween(any(), any(), any(), any()))
                .thenReturn(Page.empty(PageRequest.of(0, 20)));

        PricePageResponse response = service.getPrices(FROM, TO, 0, 20);

        assertThat(response.getItems()).isEmpty();
        assertThat(response.getTotalElements()).isZero();
        assertThat(response.isHasNext()).isFalse();
    }

    @Test
    void rejectsInvertedPeriodForPriceList() {
        assertThatThrownBy(() -> service.getPrices(TO, FROM, 0, 20))
                .isInstanceOf(InvalidPeriodException.class);

        verifyNoInteractions(repository);
    }

    @Test
    void returnsTrendForConfiguredSymbolAndRequestedDates() {
        List<TradingPair> prices = List.of(
                tradingPair("100"),
                tradingPair("101"),
                tradingPair("102")
        );
        when(repository.findBySymbolAndFetchedAtBetweenOrderByFetchedAtDescIdDesc(
                eq(CONFIGURED_SYMBOL), eq(FROM), eq(TO), any()))
                .thenReturn(prices);
        when(trendCalculator.calculate(CONFIGURED_SYMBOL, prices))
                .thenReturn(new TrendCalculator.TrendResult(
                        CONFIGURED_SYMBOL,
                        TrendCalculator.Trend.UPWARD,
                        BigDecimal.ONE,
                        BigDecimal.ONE,
                        prices.size()
                ));

        TrendResponse response = service.getTrend(FROM, TO);

        assertThat(response.getSymbol()).isEqualTo(CONFIGURED_SYMBOL);
        assertThat(response.getStartDate()).isEqualTo(FROM.toLocalDate());
        assertThat(response.getEndDate()).isEqualTo(TO.toLocalDate());
        assertThat(response.getTrend()).isEqualTo(TrendCalculator.Trend.UPWARD);
    }

    @Test
    void requestsOnlyLastWindowPointsForTrend() {
        when(repository.findBySymbolAndFetchedAtBetweenOrderByFetchedAtDescIdDesc(
                any(), any(), any(), any())).thenReturn(List.of(tradingPair("100")));
        when(trendCalculator.calculate(any(), any())).thenReturn(new TrendCalculator.TrendResult(
                CONFIGURED_SYMBOL, TrendCalculator.Trend.FLAT, BigDecimal.ZERO, BigDecimal.ZERO, 1
        ));

        service.getTrend(FROM, TO);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findBySymbolAndFetchedAtBetweenOrderByFetchedAtDescIdDesc(
                eq(CONFIGURED_SYMBOL),
                eq(FROM),
                eq(TO),
                pageable.capture()
        );
        assertThat(pageable.getValue().getPageSize()).isEqualTo(trendProperties.getWindowSize());
        assertThat(pageable.getValue().getOffset()).isZero();
    }

    @Test
    void throwsWhenTrendPeriodHasNoData() {
        when(repository.findBySymbolAndFetchedAtBetweenOrderByFetchedAtDescIdDesc(any(), any(), any(), any()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.getTrend(FROM, TO))
                .isInstanceOf(TrendNotFoundException.class)
                .hasMessageContaining(CONFIGURED_SYMBOL);

        verify(trendCalculator, never()).calculate(any(), any());
    }

    private static TradingPair tradingPair(String price) {
        return new TradingPair(CONFIGURED_SYMBOL, new BigDecimal(price), FETCHED_AT);
    }
}
