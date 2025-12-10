package org.example.analyticsservice.repository;

import org.example.analyticsservice.entity.ExchangeRateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRateEntity, Long> {

    @Query("SELECT e FROM ExchangeRateEntity e WHERE " +
            "e.baseCurrency = :fromCode AND e.targetCurrency = :toCode AND " +
            "e.timestamp BETWEEN :startDate AND :endDate " +
            "ORDER BY e.timestamp ASC")
    List<ExchangeRateEntity> findRatesForPeriod(
            @Param("fromCode") String fromCode,
            @Param("toCode") String toCode,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );
}
