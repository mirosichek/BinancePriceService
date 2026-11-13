package com.example.BinancePriceService.Repository;

import com.example.BinancePriceService.doming.TradingPair;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface TradingPairRepository extends JpaRepository<TradingPair, Long> {
    Optional<TradingPair> findTopByOrderByFetchedAtDesc();

    Optional<TradingPair> findFirstBySymbolOrderByFetchedAtDescIdDesc(String symbol);

    Page<TradingPair> findBySymbolAndFetchedAtBetween(
            String symbol,
            OffsetDateTime from,
            OffsetDateTime to,
            Pageable pageable
    );

    List<TradingPair> findBySymbolAndFetchedAtBetweenOrderByFetchedAtDescIdDesc(
            String symbol,
            OffsetDateTime from,
            OffsetDateTime to,
            Pageable pageable
    );

    @Query(value = """
            select avg(price)
            from trading_pair
            where symbol = :symbol
              and fetched_at >= :from
              and fetched_at <= :to
            """, nativeQuery = true)
    Optional<BigDecimal> findAveragePrice(
            @Param("symbol") String symbol,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );
}
