package com.team.orderserver.module.order.job;

import com.team.orderserver.module.order.service.OrderService;
import com.team.orderserver.module.order.service.OrderTimeoutScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 超时关单定时任务：扫描 Redis 延迟队列，关闭超时未支付订单并归还库存。
 * <p>多实例部署时依靠关单分布式锁 + 订单状态判断保证幂等。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutJob {

    private static final int BATCH_SIZE = 100;

    private final OrderTimeoutScheduler timeoutScheduler;
    private final OrderService orderService;

    @Scheduled(fixedDelayString = "${app.order.timeout-scan-interval-ms:10000}", initialDelay = 5000)
    public void closeExpiredOrders() {
        List<String> expiredOrders = timeoutScheduler.pollExpired(BATCH_SIZE);
        if (expiredOrders.isEmpty()) {
            return;
        }
        log.info("扫描到 {} 个超时订单待处理", expiredOrders.size());
        for (String orderNo : expiredOrders) {
            try {
                orderService.closeTimeoutOrder(orderNo);
            } catch (Exception ex) {
                log.error("超时关单失败，等待下次重试 | orderNo={}", orderNo, ex);
            }
        }
    }
}
