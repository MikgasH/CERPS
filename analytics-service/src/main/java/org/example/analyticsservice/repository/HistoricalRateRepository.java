package org.example.analyticsservice.repository;

import org.example.analyticsservice.entity.HistoricalRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Currency;
import java.util.List;

/**
 * Derived queries only — no native SQL, so the repository stays portable
 * to the H2 MODE=PostgreSQL test datasource.
 */
@Repository
public interface HistoricalRateRepository extends JpaRepository<HistoricalRate, Long> {

    List<HistoricalRate> findByBaseCurrencyAndTargetCurrencyInAndRateDateBetweenOrderByRateDate(
            Currency baseCurrency,
            Collection<Currency> targetCurrencies,
            LocalDate from,
            LocalDate to);
}
