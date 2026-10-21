package com.team.orderserver.module.order.repository;

import com.team.orderserver.module.order.entity.Order;
import com.team.orderserver.module.order.entity.RefundStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByOrderNo(String orderNo);

    /**
     * 查询指定退款状态的订单，按申请时间升序（管理员处理队列）。
     */
    List<Order> findByRefundStatusOrderByRefundRequestedAtAsc(RefundStatus refundStatus);

    /**
     * 悲观锁读取，用于支付/取消等状态变更场景，避免并发重复操作。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.orderNo = :orderNo")
    Optional<Order> findByOrderNoForUpdate(@Param("orderNo") String orderNo);

    // ---------------- 营收统计 ----------------

    long countByPaidAtGreaterThanEqualAndPaidAtLessThan(LocalDateTime start, LocalDateTime end);

    @Query("""
            select coalesce(sum(o.totalAmount), 0)
              from Order o
             where o.paidAt >= :start and o.paidAt < :end
            """)
    BigDecimal sumPaidAmount(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    long countByRefundedAtGreaterThanEqualAndRefundedAtLessThan(LocalDateTime start, LocalDateTime end);

    @Query("""
            select coalesce(sum(o.totalAmount), 0)
              from Order o
             where o.refundedAt >= :start and o.refundedAt < :end
            """)
    BigDecimal sumRefundedAmount(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime start, LocalDateTime end);
}
