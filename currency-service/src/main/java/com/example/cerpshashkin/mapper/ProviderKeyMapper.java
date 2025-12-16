package com.example.cerpshashkin.mapper;

import com.example.cerpshashkin.dto.ProviderKeyResponse;
import com.example.cerpshashkin.entity.ApiProviderKeyEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProviderKeyMapper {

    ProviderKeyResponse toResponse(ApiProviderKeyEntity entity);

    List<ProviderKeyResponse> toResponseList(List<ApiProviderKeyEntity> entities);
}
