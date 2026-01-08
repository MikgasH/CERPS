package com.example.cerpshashkin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "supported_currencies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SupportedCurrencyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "supported_currencies_seq_gen")
    @SequenceGenerator(
            name = "supported_currencies_seq_gen",
            sequenceName = "supported_currencies_seq",
            allocationSize = 1
    )
    private Long id;

    @Column(name = "currency_code", nullable = false, unique = true, length = 3)
    private String currencyCode;
}
