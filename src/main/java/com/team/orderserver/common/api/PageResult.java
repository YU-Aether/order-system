package com.team.orderserver.common.api;

import org.springframework.data.domain.Page;

import java.io.Serializable;
import java.util.List;
import java.util.function.Function;

/**
 * 统一分页响应结构。
 */
public record PageResult<T>(long total, int page, int size, int totalPages, List<T> records) implements Serializable {

    public static <E, T> PageResult<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResult<>(
                page.getTotalElements(),
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalPages(),
                page.getContent().stream().map(mapper).toList()
        );
    }
}
