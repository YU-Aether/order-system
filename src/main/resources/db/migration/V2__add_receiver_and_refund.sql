-- 订单表：增加收货信息与退款流程字段
ALTER TABLE t_order
    ADD COLUMN receiver_name       VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '收货人' AFTER user_id,
    ADD COLUMN receiver_phone      VARCHAR(20)  NOT NULL DEFAULT '' COMMENT '联系电话' AFTER receiver_name,
    ADD COLUMN receiver_address    VARCHAR(255) NOT NULL DEFAULT '' COMMENT '收货地址' AFTER receiver_phone,
    ADD COLUMN refund_status       VARCHAR(20)  NOT NULL DEFAULT 'NONE' COMMENT '退款状态 NONE/PENDING/APPROVED/REJECTED' AFTER status,
    ADD COLUMN refund_reason       VARCHAR(255) DEFAULT NULL COMMENT '用户退款理由' AFTER refund_status,
    ADD COLUMN refund_remark       VARCHAR(255) DEFAULT NULL COMMENT '管理员处理备注' AFTER refund_reason,
    ADD COLUMN refund_requested_at DATETIME(6)  DEFAULT NULL COMMENT '申请退款时间' AFTER refund_remark,
    ADD COLUMN refunded_at         DATETIME(6)  DEFAULT NULL COMMENT '退款完成时间' AFTER refund_requested_at;

-- 营收统计与退款查询索引
CREATE INDEX idx_order_paid_at ON t_order (paid_at);
CREATE INDEX idx_order_refund_status ON t_order (refund_status);
CREATE INDEX idx_order_refunded_at ON t_order (refunded_at);
