package com.example.BinancePriceService.mapper;

import com.example.BinancePriceService.DTO.BinanceResponse;
import com.example.BinancePriceService.DTO.TradingPairResponse;
import com.example.BinancePriceService.doming.TradingPair;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class TradingPairMapperTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-11-13T10:20:30.987Z"), ZoneOffset.UTC);

    private final TradingPairMapper mapper = new TradingPairMapper(CLOCK);

    @Test
    void mapsBinanceResponseToEntityWithSecondPrecision() {
        BinanceResponse response = new BinanceResponse();
        response.setSymbol("BNBUSDT");
        response.setPrice(new BigDecimal("654.85561546"));

        TradingPair tradingPair = mapper.responseToTradingPair(response);

        assertThat(tradingPair.getSymbol()).isEqualTo("BNBUSDT");
        assertThat(tradingPair.getPrice()).isEqualByComparingTo("654.85561546");
        assertThat(tradingPair.getFetchedAt())
                .isEqualTo(OffsetDateTime.parse("2026-11-13T10:20:30Z"));
    }

    @Test
    void mapsEntityToResponse() {
        TradingPair tradingPair = new TradingPair(
                "BNBUSDT",
                new BigDecimal("654.85561546"),
                OffsetDateTime.parse("2026-11-12T21:04:31Z")
        );

        TradingPairResponse response = mapper.tradingPairToResponse(tradingPair);

        assertThat(response.getSymbol()).isEqualTo("BNBUSDT");
        assertThat(response.getPrice()).isEqualByComparingTo("654.85561546");
        assertThat(response.getFetchedAt())
                .isEqualTo(OffsetDateTime.parse("2026-11-12T21:04:31Z"));
    }
}
