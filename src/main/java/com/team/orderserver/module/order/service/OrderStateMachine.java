package com.team.orderserver.module.order.service;

import com.team.orderserver.common.api.ResultCode;
import com.team.orderserver.common.exception.BusinessException;
import com.team.orderserver.module.order.entity.Order;
import com.team.orderserver.module.order.entity.OrderStatus;
import org.springframework.stereotype.Component;

/**
 * 订单状态流转校验，集中管理业务规则，避免状态被随意修改。
 */
@Component
public class OrderStateMachine {

    public void transit(Order order, OrderStatus target) {
        OrderStatus current = order.getStatus();
        if (!current.canTransitionTo(target)) {
            throw new BusinessException(ResultCode.ORDER_STATUS_INVALID,
                    "订单当前状态[" + current.getDescription() + "]不允许变更为[" + target.getDescription() + "]");
        }
        order.setStatus(target);
    }
}
