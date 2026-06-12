package org.example.analyticsservice.repository;

import org.example.analyticsservice.entity.HistoricalCoverage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Currency;

@Repository
public interface HistoricalCoverageRepository extends JpaRepository<HistoricalCoverage, Currency> {
}
