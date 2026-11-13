package com.example.BinancePriceService.Repository;

import com.example.BinancePriceService.doming.TradingPair;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TradingPairWriter {
    private static final Logger log = LoggerFactory.getLogger(TradingPairWriter.class);

    private final TradingPairRepository tradingPairRepository;

    public WriteResult saveToDB(TradingPair tradingPair) {
        try {
            tradingPairRepository.save(tradingPair);
            return WriteResult.SAVED;
        } catch (DataIntegrityViolationException ex) {
            log.warn(
                    "Skipping duplicate trading pair {} at {}",
                    tradingPair.getSymbol(),
                    tradingPair.getFetchedAt()
            );
            log.debug("Duplicate write details", ex);
            return WriteResult.DUPLICATE;
        }
    }
}
