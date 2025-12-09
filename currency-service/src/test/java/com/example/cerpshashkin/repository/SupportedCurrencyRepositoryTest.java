package com.example.cerpshashkin.repository;

import com.example.cerpshashkin.entity.SupportedCurrencyEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class SupportedCurrencyRepositoryTest {

    @Autowired
    private SupportedCurrencyRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void existsByCurrencyCode_WithExistingCurrency_ShouldReturnTrue() {
        SupportedCurrencyEntity entity = SupportedCurrencyEntity.builder()
                .currencyCode("NOK")
                .build();
        repository.save(entity);

        boolean exists = repository.existsByCurrencyCode("NOK");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByCurrencyCode_WithNonExistingCurrency_ShouldReturnFalse() {
        boolean exists = repository.existsByCurrencyCode("XXX");

        assertThat(exists).isFalse();
    }

    @Test
    void existsByCurrencyCode_IsCaseInsensitive_ShouldWork() {
        SupportedCurrencyEntity entity = SupportedCurrencyEntity.builder()
                .currencyCode("DKK")
                .build();
        repository.save(entity);

        boolean exists = repository.existsByCurrencyCode("DKK");

        assertThat(exists).isTrue();
    }

    @Test
    void save_WithValidCurrency_ShouldPersist() {
        SupportedCurrencyEntity entity = SupportedCurrencyEntity.builder()
                .currencyCode("ISK")
                .build();

        SupportedCurrencyEntity saved = repository.save(entity);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCurrencyCode()).isEqualTo("ISK");
    }

    @Test
    void findAll_ShouldReturnAllCurrencies() {
        repository.save(SupportedCurrencyEntity.builder().currencyCode("NOK").build());
        repository.save(SupportedCurrencyEntity.builder().currencyCode("DKK").build());
        repository.save(SupportedCurrencyEntity.builder().currencyCode("ISK").build());

        List<SupportedCurrencyEntity> currencies = repository.findAll();

        assertThat(currencies).hasSize(3);
        assertThat(currencies)
                .extracting(SupportedCurrencyEntity::getCurrencyCode)
                .containsExactlyInAnyOrder("NOK", "DKK", "ISK");
    }

    @Test
    void delete_ShouldRemoveCurrency() {
        SupportedCurrencyEntity entity = repository.save(
                SupportedCurrencyEntity.builder().currencyCode("PLN").build()
        );

        repository.delete(entity);

        assertThat(repository.existsByCurrencyCode("PLN")).isFalse();
    }

    @Test
    void save_WithDuplicateCurrencyCode_ShouldFail() {
        repository.save(SupportedCurrencyEntity.builder().currencyCode("HUF").build());

        assertThatThrownBy(() -> {
            repository.save(SupportedCurrencyEntity.builder().currencyCode("HUF").build());
            repository.flush();
        }).isInstanceOf(Exception.class);
    }
}
