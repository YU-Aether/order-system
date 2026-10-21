CREATE TABLE IF NOT EXISTS t_user
(
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username   VARCHAR(50)  NOT NULL COMMENT '用户名',
    password   VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密密码',
    nickname   VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    role       VARCHAR(20)  NOT NULL COMMENT '角色 USER/ADMIN',
    status     VARCHAR(20)  NOT NULL COMMENT '状态 ACTIVE/DISABLED',
    created_at DATETIME(6)  NOT NULL COMMENT '创建时间',
    updated_at DATETIME(6)  NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表';

CREATE TABLE IF NOT EXISTS t_product
(
    id         BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    sku        VARCHAR(64)    NOT NULL COMMENT '商品编码',
    name       VARCHAR(128)   NOT NULL COMMENT '商品名称',
    price      DECIMAL(12, 2) NOT NULL COMMENT '单价',
    stock      INT            NOT NULL COMMENT '库存',
    status     VARCHAR(20)    NOT NULL COMMENT '状态 ON_SALE/OFF_SALE',
    version    BIGINT         NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(6)    NOT NULL COMMENT '创建时间',
    updated_at DATETIME(6)    NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_sku (sku),
    KEY idx_product_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '商品表';

CREATE TABLE IF NOT EXISTS t_order
(
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_no     VARCHAR(40)    NOT NULL COMMENT '订单号',
    user_id      BIGINT         NOT NULL COMMENT '下单用户',
    total_amount DECIMAL(12, 2) NOT NULL COMMENT '订单总金额',
    status       VARCHAR(20)    NOT NULL COMMENT '订单状态',
    remark       VARCHAR(255)   DEFAULT NULL COMMENT '备注',
    expire_at    DATETIME(6)    NOT NULL COMMENT '支付截止时间',
    paid_at      DATETIME(6)    DEFAULT NULL COMMENT '支付时间',
    cancelled_at DATETIME(6)    DEFAULT NULL COMMENT '取消时间',
    version      BIGINT         NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at   DATETIME(6)    NOT NULL COMMENT '创建时间',
    updated_at   DATETIME(6)    NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_order_user (user_id, id),
    KEY idx_order_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单主表';

CREATE TABLE IF NOT EXISTS t_order_item
(
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_id      BIGINT         NOT NULL COMMENT '订单ID',
    product_id    BIGINT         NOT NULL COMMENT '商品ID',
    product_name  VARCHAR(128)   NOT NULL COMMENT '商品名称快照',
    product_price DECIMAL(12, 2) NOT NULL COMMENT '下单时单价快照',
    quantity      INT            NOT NULL COMMENT '购买数量',
    subtotal      DECIMAL(12, 2) NOT NULL COMMENT '小计',
    PRIMARY KEY (id),
    KEY idx_order_item_order (order_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单明细表';

CREATE TABLE IF NOT EXISTS t_payment_record
(
    id         BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    pay_no     VARCHAR(40)    NOT NULL COMMENT '支付单号',
    order_no   VARCHAR(40)    NOT NULL COMMENT '订单号',
    amount     DECIMAL(12, 2) NOT NULL COMMENT '支付金额',
    channel    VARCHAR(20)    NOT NULL COMMENT '支付渠道',
    status     VARCHAR(20)    NOT NULL COMMENT '支付状态',
    paid_at    DATETIME(6)    NOT NULL COMMENT '支付时间',
    created_at DATETIME(6)    NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_pay_no (pay_no),
    UNIQUE KEY uk_payment_order_no (order_no)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '支付流水表';
