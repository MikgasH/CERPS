package org.example.analyticsservice.repository;

import org.example.analyticsservice.entity.ExchangeRateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRateEntity, UUID> {

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
            SELECT 
                CAST(e1.id AS VARCHAR(36)) as id, 
                :fromCode as base_currency, 
                :toCode as target_currency,
                e2.rate / e1.rate as rate,
                CONCAT(e1.source, '/', e2.source) as source,
                e1.timestamp
            FROM exchange_rates e1
            INNER JOIN exchange_rates e2
              ON e1.timestamp = e2.timestamp
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
}
