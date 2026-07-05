package org.example.analyticsservice.entity;

import com.example.cerps.common.converter.CurrencyAttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;

/**
 * One EUR-based daily rate point sourced from Frankfurter.
 * Rows with rate_date in the past are immutable; today's row is provisional
 * until the ECB fixing and may be refreshed.
 */
@Entity
@Table(name = "historical_rates", uniqueConstraints = {
        @UniqueConstraint(name = "uq_hist_rate",
                columnNames = {"base_currency", "target_currency", "rate_date"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class HistoricalRate {

    // Sequence (not IDENTITY) so Hibernate can batch the cold-backfill
    // inserts — identity keys force one round-trip per row. The allocation
    // size must match the sequence's INCREMENT BY in v1.1.
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "historical_rates_seq_gen")
    @SequenceGenerator(name = "historical_rates_seq_gen",
            sequenceName = "historical_rates_seq", allocationSize = 50)
    private Long id;

    @Column(name = "base_currency", nullable = false, length = 3)
    @Convert(converter = CurrencyAttributeConverter.class)
    private Currency baseCurrency;

    @Column(name = "target_currency", nullable = false, length = 3)
    @Convert(converter = CurrencyAttributeConverter.class)
    private Currency targetCurrency;

    @Column(name = "rate_date", nullable = false)
    private LocalDate rateDate;

    @Column(nullable = false, precision = 20, scale = 10)
    private BigDecimal rate;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;
}
