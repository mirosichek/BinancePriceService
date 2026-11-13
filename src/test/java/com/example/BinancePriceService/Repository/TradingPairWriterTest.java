package com.example.BinancePriceService.Repository;

import com.example.BinancePriceService.doming.TradingPair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradingPairWriterTest {

    @Mock
    private TradingPairRepository repository;

    @InjectMocks
    private TradingPairWriter writer;

    private final TradingPair tradingPair = new TradingPair(
            "BNBUSDT",
            new BigDecimal("654.85561546"),
            OffsetDateTime.parse("2026-11-12T21:04:31Z")
    );

    @Test
    void reportsSavedWhenInsertSucceeds() {
        when(repository.save(tradingPair)).thenReturn(tradingPair);

        assertThat(writer.saveToDB(tradingPair)).isEqualTo(WriteResult.SAVED);
    }

    @Test
    void reportsDuplicateWhenUniqueConstraintIsViolated() {
        when(repository.save(tradingPair))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThat(writer.saveToDB(tradingPair)).isEqualTo(WriteResult.DUPLICATE);
    }
}
