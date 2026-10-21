package com.team.orderserver.module.order.dto;

import com.team.orderserver.common.api.ResultCode;
import com.team.orderserver.common.exception.BusinessException;
import com.team.orderserver.module.order.entity.OrderStatus;
import org.springframework.util.StringUtils;

import java.io.Serializable;

/**
 * 订单列表查询条件。
 */
public record OrderQuery(String status, String keyword, Integer page, Integer size) implements Serializable {

    public int pageNo() {
        return page == null || page < 1 ? 1 : page;
    }

    public int pageSize() {
        int value = size == null || size < 1 ? 10 : size;
        return Math.min(value, 100);
    }

    public OrderStatus statusEnum() {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return OrderStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单状态不合法: " + status);
        }
    }
}
