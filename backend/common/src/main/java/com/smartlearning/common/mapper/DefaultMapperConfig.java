package com.smartlearning.common.mapper;

import org.mapstruct.*;

@MapperConfig(
        componentModel = MappingConstants.ComponentModel.SPRING,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        nullValueIterableMappingStrategy =
        NullValueMappingStrategy.RETURN_DEFAULT
)
public class DefaultMapperConfig {
}
