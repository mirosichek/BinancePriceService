package com.example.BinancePriceService.mapper;

import com.example.BinancePriceService.DTO.BinanceResponse;
import com.example.BinancePriceService.DTO.TradingPairResponse;
import com.example.BinancePriceService.doming.TradingPair;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class TradingPairMapper {

    private final Clock clock;

    public TradingPair responseToTradingPair(BinanceResponse binanceResponse){
        String symbol = binanceResponse.getSymbol();
        BigDecimal price = binanceResponse.getPrice();
        OffsetDateTime fetchedAt = OffsetDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        return new TradingPair(symbol, price, fetchedAt);
    }

    public TradingPairResponse tradingPairToResponse(TradingPair tradingPair){
        return new TradingPairResponse(
                tradingPair.getSymbol(),
                tradingPair.getPrice(),
                tradingPair.getFetchedAt());
    }
}
