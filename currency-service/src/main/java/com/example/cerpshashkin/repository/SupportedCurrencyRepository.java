package com.example.cerpshashkin.repository;

import com.example.cerpshashkin.entity.SupportedCurrencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportedCurrencyRepository extends JpaRepository<SupportedCurrencyEntity, Long> {

    boolean existsByCurrencyCode(String currencyCode);
}
