package com.smartlearning.common.dto.request;

import lombok.Getter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class PagingRequest {
    private static final int PAGE_SIZE = 6;

    int page = 1;
    private Map<String, String> orders = new HashMap<>();

    public void setPage(int page) {
        this.page = Math.max(page, 1);
    }

    public Pageable pageable() {
        if (CollectionUtils.isEmpty(orders)) {
            return PageRequest.of(page - 1, PAGE_SIZE);
        }
        Sort sortable = sortable(orders);
        return PageRequest.of(page - 1, PAGE_SIZE, sortable);
    }

    public Sort sortable(Map<String, String> orders) {
        List<Sort.Order> sortableList = new ArrayList<>();
        orders.forEach((key, value) -> {
            Sort.Direction direction = Sort.Direction.DESC.name().equals(value) ? Sort.Direction.DESC : Sort.Direction.ASC;
            Sort.Order order = new Sort.Order(direction, key);
            sortableList.add(order);
        });
        return Sort.by(sortableList);

    }
}
