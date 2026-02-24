package com.example.cerpshashkin.repository;

import com.example.cerpshashkin.entity.ExchangeRateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRateEntity, UUID> {

    String FIND_BEST_RATE_QUERY = """
            WITH rate_options AS (
                SELECT * FROM (
                    SELECT 
                        1 as priority,
                        'DIRECT' as rate_type,
                        rate,
                        timestamp
                    FROM exchange_rates
                    WHERE base_currency = :fromCurrency
                      AND target_currency = :toCurrency
                      AND timestamp >= :maxAge
                    ORDER BY timestamp DESC
                    LIMIT 1
                ) direct_rate
                
                UNION ALL
                
                SELECT * FROM (
                    SELECT 
                        2 as priority,
                        'INVERSE' as rate_type,
                        1.0 / rate as rate,
                        timestamp
                    FROM exchange_rates
                    WHERE base_currency = :toCurrency
                      AND target_currency = :fromCurrency
                      AND timestamp >= :maxAge
                    ORDER BY timestamp DESC
                    LIMIT 1
                ) inverse_rate
                
                UNION ALL
                
                SELECT * FROM (
                    SELECT 
                        3 as priority,
                        'CROSS' as rate_type,
                        e2.rate / e1.rate as rate,
                        LEAST(e1.timestamp, e2.timestamp) as timestamp
                    FROM exchange_rates e1
                    INNER JOIN exchange_rates e2 
                        ON e1.base_currency = e2.base_currency
                    WHERE e1.base_currency = :baseCurrency
                      AND e1.target_currency = :fromCurrency
                      AND e2.target_currency = :toCurrency
                      AND e1.timestamp >= :maxAge
                      AND e2.timestamp >= :maxAge
                    ORDER BY LEAST(e1.timestamp, e2.timestamp) DESC
                    LIMIT 1
                ) cross_rate
            )
            SELECT rate, rate_type as rateType, timestamp
            FROM rate_options
            WHERE rate IS NOT NULL
            ORDER BY priority
            LIMIT 1
            """;

    @Query(value = FIND_BEST_RATE_QUERY, nativeQuery = true)
    Optional<RateQueryResult> findBestRate(
            @Param("fromCurrency") String fromCurrency,
            @Param("toCurrency") String toCurrency,
            @Param("baseCurrency") String baseCurrency,
            @Param("maxAge") Instant maxAge
    );

    Optional<ExchangeRateEntity> findFirstByBaseCurrencyAndTargetCurrencyOrderByTimestampDesc(
            Currency baseCurrency,
            Currency targetCurrency
    );

    @Query("""
            SELECT e FROM ExchangeRateEntity e
            WHERE e.baseCurrency = :baseCurrency
              AND e.targetCurrency = :targetCurrency
              AND e.timestamp >= :startDate
              AND e.timestamp <= :endDate
            ORDER BY e.timestamp ASC
            """)
    List<ExchangeRateEntity> findRatesForPeriod(
            @Param("baseCurrency") Currency baseCurrency,
            @Param("targetCurrency") Currency targetCurrency,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    @Modifying
    @Query("DELETE FROM ExchangeRateEntity e WHERE e.timestamp < :cutoffDate")
    int deleteByTimestampBefore(@Param("cutoffDate") Instant cutoffDate);
}
