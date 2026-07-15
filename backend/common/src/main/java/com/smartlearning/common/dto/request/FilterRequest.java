package com.smartlearning.common.dto.request;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.Specification;

@Getter
@Setter
public abstract class FilterRequest<T> extends PagingRequest {

    public abstract Specification<T> specification();
}
