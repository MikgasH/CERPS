package com.example.cerpshashkin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

@Entity
@Table(name = "exchange_rates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ExchangeRateEntity {

    @Id
    private UUID id;

    @Column(name = "base_currency", nullable = false, length = 3)
    private Currency baseCurrency;

    @Column(name = "target_currency", nullable = false, length = 3)
    private Currency targetCurrency;

    @Column(nullable = false, precision = 20, scale = 10)
    private BigDecimal rate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ExchangeRateSource source;

    @Column(nullable = false)
    private Instant timestamp;
}
