package com.example.cerpshashkin.repository;

import com.example.cerpshashkin.entity.ApiProviderKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiProviderKeyRepository extends JpaRepository<ApiProviderKeyEntity, Long> {

    Optional<ApiProviderKeyEntity> findByProviderNameAndActiveTrue(String providerName);

    List<ApiProviderKeyEntity> findAllByActiveTrue();
}
