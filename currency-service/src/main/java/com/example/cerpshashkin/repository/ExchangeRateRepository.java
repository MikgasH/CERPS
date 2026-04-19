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

    String FIND_RATES_WITH_CROSS_SUPPORT_QUERY = """
            -- Direct rate: EUR -> TO (when FROM is EUR)
            SELECT
                CAST(id AS VARCHAR(36)) as id,
                base_currency,
                target_currency,
                rate,
                source,
                timestamp
            FROM exchange_rates
            WHERE base_currency = 'EUR'
              AND target_currency = :toCode
              AND 'EUR' = :fromCode
              AND (CAST(:startDate AS timestamp) IS NULL
                   OR timestamp BETWEEN CAST(:startDate AS timestamp) AND CAST(:endDate AS timestamp))

            UNION ALL

            -- Inverse rate: FROM -> EUR (when TO is EUR)
            SELECT
                CAST(id AS VARCHAR(36)) as id,
                :fromCode as base_currency,
                :toCode as target_currency,
                1.0 / rate as rate,
                source,
                timestamp
            FROM exchange_rates
            WHERE base_currency = 'EUR'
              AND target_currency = :fromCode
              AND 'EUR' = :toCode
              AND (CAST(:startDate AS timestamp) IS NULL
                   OR timestamp BETWEEN CAST(:startDate AS timestamp) AND CAST(:endDate AS timestamp))

            UNION ALL

            -- Cross rate: FROM -> TO via EUR (when both are not EUR)
            -- Join on same-hour bucket to tolerate minor clock skew between write batches
            -- while still pairing the closest snapshot per hour.
            SELECT
                CAST(e1.id AS VARCHAR(36)) as id,
                :fromCode as base_currency,
                :toCode as target_currency,
                e2.rate / e1.rate as rate,
                CONCAT(e1.source, '/', e2.source) as source,
                e1.timestamp
            FROM exchange_rates e1
            INNER JOIN exchange_rates e2
              ON date_trunc('hour', e1.timestamp) = date_trunc('hour', e2.timestamp)
              AND e1.base_currency = 'EUR'
              AND e2.base_currency = 'EUR'
            WHERE e1.target_currency = :fromCode
              AND e2.target_currency = :toCode
              AND 'EUR' != :fromCode
              AND 'EUR' != :toCode
              AND (CAST(:startDate AS timestamp) IS NULL
                   OR e1.timestamp BETWEEN CAST(:startDate AS timestamp) AND CAST(:endDate AS timestamp))

            ORDER BY timestamp ASC
            """;

    @Query(value = FIND_RATES_WITH_CROSS_SUPPORT_QUERY, nativeQuery = true)
    List<Object[]> findRatesWithCrossSupport(
            @Param("fromCode") String fromCode,
            @Param("toCode") String toCode,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    @Query(value = """
            SELECT DISTINCT ON (target_currency)
                   id, base_currency, target_currency, rate, source, timestamp
            FROM exchange_rates
            WHERE base_currency = :baseCurrency
              AND timestamp >= :maxAge
            ORDER BY target_currency, timestamp DESC
            """, nativeQuery = true)
    List<ExchangeRateEntity> findAllLatestByBaseCurrency(
            @Param("baseCurrency") String baseCurrency,
            @Param("maxAge") Instant maxAge
    );
}
