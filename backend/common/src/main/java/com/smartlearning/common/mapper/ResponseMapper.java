package com.smartlearning.common.mapper;

import java.util.List;

public interface ResponseMapper<E, R> {

    R toResponse(E entity);

    List<R> toResponses(List<E> entities);
}