package com.team.orderserver.module.order.service;

import com.team.orderserver.common.api.PageResult;
import com.team.orderserver.module.order.dto.CreateOrderRequest;
import com.team.orderserver.module.order.dto.OrderQuery;
import com.team.orderserver.module.order.dto.OrderResponse;
import com.team.orderserver.module.order.dto.RevenueResponse;
import com.team.orderserver.module.payment.entity.PaymentChannel;

import java.time.LocalDate;
import java.util.List;

/**
 * 订单领域服务。
 */
public interface OrderService {

    /** 创建订单：幂等 + 分布式锁 + 扣减库存。 */
    OrderResponse create(Long userId, CreateOrderRequest request);

    /** 分页查询；admin=true 时查询全量。 */
    PageResult<OrderResponse> page(Long userId, boolean admin, OrderQuery query);

    /** 订单详情（带缓存），内部做归属校验。 */
    OrderResponse detail(Long userId, boolean admin, String orderNo);

    /** 支付订单。 */
    OrderResponse pay(Long userId, String orderNo, PaymentChannel channel);

    /** 用户取消订单并归还库存。 */
    OrderResponse cancel(Long userId, String orderNo);

    /** 系统超时关单（定时任务调用）。 */
    void closeTimeoutOrder(String orderNo);

    /** 管理员发货。 */
    OrderResponse ship(String orderNo);

    /** 用户确认收货。 */
    OrderResponse complete(Long userId, String orderNo);

    /** 管理员退款。 */
    OrderResponse refund(String orderNo);

    /** 用户申请退款（附理由），等待管理员处理。 */
    OrderResponse applyRefund(Long userId, String orderNo, String reason);

    /** 管理员查看待处理的退款申请。 */
    List<OrderResponse> pendingRefunds();

    /** 管理员同意退款。 */
    OrderResponse approveRefund(String orderNo, String remark);

    /** 管理员拒绝退款。 */
    OrderResponse rejectRefund(String orderNo, String remark);

    /** 按日期统计营收；date 为空时统计当天。 */
    RevenueResponse revenue(LocalDate date);
}
