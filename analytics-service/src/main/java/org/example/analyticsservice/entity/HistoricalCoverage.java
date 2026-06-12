package org.example.analyticsservice.entity;

import com.example.cerps.common.converter.CurrencyAttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;

/**
 * Per-currency fetched-interval bookkeeping: distinguishes "never fetched"
 * from "fetched, genuinely absent upstream" without refetch loops.
 */
@Entity
@Table(name = "historical_coverage")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class HistoricalCoverage {

    @Id
    @Column(name = "target_currency", nullable = false, length = 3)
    @Convert(converter = CurrencyAttributeConverter.class)
    private Currency targetCurrency;

    @Column(name = "covered_from", nullable = false)
    private LocalDate coveredFrom;

    @Column(name = "covered_to", nullable = false)
    private LocalDate coveredTo;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
