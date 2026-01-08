package org.example.analyticsservice.repository;

import org.example.analyticsservice.entity.SupportedCurrencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportedCurrencyRepository extends JpaRepository<SupportedCurrencyEntity, Long> {

    boolean existsByCurrencyCode(String currencyCode);
}
