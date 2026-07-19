package com.smartlearning.common.mapper;

public interface CreateMapper<E, C> {
    E toEntity(C request);
}
