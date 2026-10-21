package com.team.orderserver.module.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单主表。
 */
@Entity
@Table(name = "t_order")
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String orderNo;

    @Column(nullable = false)
    private Long userId;

    /** 收货人姓名。 */
    @Column(nullable = false, length = 50)
    private String receiverName;

    /** 收货人电话。 */
    @Column(nullable = false, length = 20)
    private String receiverPhone;

    /** 收货地址。 */
    @Column(nullable = false, length = 255)
    private String receiverAddress;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.CREATED;

    /** 退款申请状态，与订单状态解耦。 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RefundStatus refundStatus = RefundStatus.NONE;

    /** 用户填写的退款理由。 */
    @Column(length = 255)
    private String refundReason;

    /** 管理员处理备注（拒绝时的说明）。 */
    @Column(length = 255)
    private String refundRemark;

    private LocalDateTime refundRequestedAt;

    /** 退款完成时间，用于当日退款金额统计。 */
    private LocalDateTime refundedAt;

    @Column(length = 255)
    private String remark;

    /** 支付截止时间，超时后由定时任务关闭并归还库存。 */
    @Column(nullable = false)
    private LocalDateTime expireAt;

    private LocalDateTime paidAt;

    private LocalDateTime cancelledAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
