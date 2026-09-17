-- 二期啤酒币：动态价格、购买订单、钱包流水和赛事结算
-- 仅新增表，不修改一期报名收款表。

CREATE TABLE IF NOT EXISTS beer_coin_product (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '全局价格设置主键',
    product_code VARCHAR(64) NOT NULL COMMENT '全局价格设置编码',
    name VARCHAR(128) NOT NULL COMMENT '内部兼容名称',
    status VARCHAR(32) NOT NULL DEFAULT 'INACTIVE' COMMENT '内部状态：ACTIVE/INACTIVE',
    active_guard TINYINT GENERATED ALWAYS AS (IF(status = 'ACTIVE', 1, NULL)) STORED,
    effective_time DATETIME NULL COMMENT '生效时间',
    version_no INT NOT NULL DEFAULT 1 COMMENT '方案版本号',
    created_by_admin_id BIGINT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_beer_coin_product_code (product_code),
    UNIQUE KEY uk_beer_coin_product_active_guard (active_guard),
    KEY idx_beer_coin_product_status (status, effective_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='啤酒币全局价格设置';

CREATE TABLE IF NOT EXISTS beer_coin_product_tier (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '阶梯主键',
    product_id BIGINT NOT NULL COMMENT '全局价格设置ID',
    start_quantity BIGINT NOT NULL COMMENT '起始数量（含）',
    end_quantity BIGINT NULL COMMENT '结束数量（含），为空表示以上',
    unit_price DECIMAL(10,2) NOT NULL COMMENT '单枚价格',
    sort_order INT NOT NULL COMMENT '阶梯顺序',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_beer_coin_product_tier_order (product_id, sort_order),
    KEY idx_beer_coin_product_tier_range (product_id, start_quantity, end_quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='啤酒币价格阶梯';

CREATE TABLE IF NOT EXISTS beer_coin_purchase_order (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '购买订单主键',
    order_no VARCHAR(64) NOT NULL COMMENT '业务订单号',
    enterprise_account_id BIGINT NOT NULL COMMENT '企业账户ID',
    product_id BIGINT NOT NULL COMMENT '下单时全局价格设置ID',
    product_snapshot_json JSON NOT NULL COMMENT '下单时价格和阶梯快照',
    quantity BIGINT NOT NULL COMMENT '购买数量',
    amount DECIMAL(12,2) NOT NULL COMMENT '订单金额',
    status VARCHAR(32) NOT NULL COMMENT '订单状态',
    out_trade_no VARCHAR(64) NULL COMMENT '微信商户订单号',
    wechat_transaction_id VARCHAR(64) NULL COMMENT '微信交易号',
    code_url VARCHAR(512) NULL COMMENT 'Native 支付二维码链接',
    expire_time DATETIME NULL COMMENT '支付过期时间',
    wechat_trade_state VARCHAR(32) NULL COMMENT '微信交易状态',
    wechat_trade_state_desc VARCHAR(128) NULL COMMENT '微信交易状态描述',
    notify_raw_json JSON NULL COMMENT '支付通知原始JSON',
    paid_time DATETIME NULL COMMENT '支付完成时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_beer_coin_purchase_order_no (order_no),
    UNIQUE KEY uk_beer_coin_purchase_out_trade_no (out_trade_no),
    UNIQUE KEY uk_beer_coin_purchase_wechat_transaction_id (wechat_transaction_id),
    KEY idx_beer_coin_purchase_account_status (enterprise_account_id, status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='啤酒币购买订单';

CREATE TABLE IF NOT EXISTS beer_coin_lot (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '到账批次主键',
    lot_no VARCHAR(64) NOT NULL COMMENT '到账批次号',
    enterprise_account_id BIGINT NOT NULL COMMENT '企业账户ID',
    purchase_order_id BIGINT NULL COMMENT '购买订单ID',
    total_quantity BIGINT NOT NULL COMMENT '到账总数',
    remaining_quantity BIGINT NOT NULL COMMENT '剩余数量',
    available_from DATETIME NOT NULL COMMENT '可用时间',
    expires_at DATETIME NOT NULL COMMENT '失效时间',
    source_type VARCHAR(32) NOT NULL COMMENT '来源类型',
    source_order_id VARCHAR(64) NULL COMMENT '来源业务单号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_beer_coin_lot_no (lot_no),
    UNIQUE KEY uk_beer_coin_lot_purchase_order (purchase_order_id),
    KEY idx_beer_coin_lot_account_expire (enterprise_account_id, expires_at, available_from, remaining_quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='啤酒币到账批次';

CREATE TABLE IF NOT EXISTS beer_coin_ledger (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '流水主键',
    ledger_no VARCHAR(64) NOT NULL COMMENT '流水号',
    enterprise_account_id BIGINT NOT NULL COMMENT '企业账户ID',
    direction VARCHAR(32) NOT NULL COMMENT '方向：CREDIT/DEBIT/REVERSAL',
    quantity BIGINT NOT NULL COMMENT '流水数量',
    business_type VARCHAR(64) NOT NULL COMMENT '业务类型',
    business_id VARCHAR(64) NULL COMMENT '业务ID',
    idempotency_key VARCHAR(128) NOT NULL COMMENT '幂等键',
    operator_admin_id BIGINT NULL COMMENT '操作管理员',
    reason VARCHAR(500) NULL COMMENT '操作原因',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_beer_coin_ledger_no (ledger_no),
    UNIQUE KEY uk_beer_coin_ledger_idempotency (enterprise_account_id, idempotency_key),
    KEY idx_beer_coin_ledger_account_time (enterprise_account_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='啤酒币账户流水';

CREATE TABLE IF NOT EXISTS beer_coin_consumption_allocation (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '分配明细主键',
    ledger_id BIGINT NOT NULL COMMENT '扣减流水ID',
    lot_id BIGINT NOT NULL COMMENT '到账批次ID',
    quantity BIGINT NOT NULL COMMENT '本批次扣减数量',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_beer_coin_allocation_ledger_lot (ledger_id, lot_id),
    KEY idx_beer_coin_allocation_lot (lot_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='啤酒币消费批次分配';

CREATE TABLE IF NOT EXISTS competition_coin_settlement (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '赛事结算主键',
    competition_id BIGINT NOT NULL COMMENT '比赛ID',
    enterprise_account_id BIGINT NOT NULL COMMENT '企业账户ID',
    settlement_type VARCHAR(32) NOT NULL COMMENT '结算类型',
    effective_entry_count INT NOT NULL DEFAULT 0 COMMENT '有效酒款数',
    billing_tier VARCHAR(128) NULL COMMENT '计费说明',
    required_quantity BIGINT NOT NULL DEFAULT 0 COMMENT '应消耗数量',
    charged_quantity BIGINT NOT NULL DEFAULT 0 COMMENT '本次扣除数量',
    status VARCHAR(32) NOT NULL COMMENT '结算状态',
    ledger_id BIGINT NULL COMMENT '扣减流水ID',
    idempotency_key VARCHAR(128) NOT NULL COMMENT '幂等键',
    settled_by_admin_id BIGINT NULL COMMENT '操作管理员',
    settled_time DATETIME NULL COMMENT '结算时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_competition_coin_settlement_type (competition_id, settlement_type),
    UNIQUE KEY uk_competition_coin_settlement_idempotency (enterprise_account_id, idempotency_key),
    KEY idx_competition_coin_settlement_account (enterprise_account_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='赛事啤酒币结算';

CREATE TABLE IF NOT EXISTS competition_coin_settlement_entry (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '结算酒款快照主键',
    settlement_id BIGINT NOT NULL COMMENT '结算ID',
    beer_entry_id BIGINT NOT NULL COMMENT '酒款ID',
    entry_status_snapshot VARCHAR(32) NOT NULL COMMENT '结算时酒款状态',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_competition_coin_settlement_entry (settlement_id, beer_entry_id),
    KEY idx_competition_coin_settlement_entry_entry (beer_entry_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='赛事啤酒币结算酒款快照';

ALTER TABLE wechat_pay_notify
    MODIFY COLUMN business_type VARCHAR(32) NOT NULL COMMENT '回调业务类型：PAYMENT/REFUND/BEER_COIN_PURCHASE';

-- 已执行过建表脚本的环境补充数据库级单活动价格约束。
SET @beer_coin_active_guard_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'beer_coin_product'
      AND column_name = 'active_guard'
);
SET @beer_coin_active_guard_column_sql = IF(
    @beer_coin_active_guard_column_exists = 0,
    'ALTER TABLE beer_coin_product ADD COLUMN active_guard TINYINT GENERATED ALWAYS AS (IF(status = ''ACTIVE'', 1, NULL)) STORED',
    'SELECT 1'
);
PREPARE beer_coin_active_guard_column_stmt FROM @beer_coin_active_guard_column_sql;
EXECUTE beer_coin_active_guard_column_stmt;
DEALLOCATE PREPARE beer_coin_active_guard_column_stmt;

SET @beer_coin_active_guard_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'beer_coin_product'
      AND index_name = 'uk_beer_coin_product_active_guard'
);
SET @beer_coin_active_guard_index_sql = IF(
    @beer_coin_active_guard_index_exists = 0,
    'ALTER TABLE beer_coin_product ADD UNIQUE KEY uk_beer_coin_product_active_guard (active_guard)',
    'SELECT 1'
);
PREPARE beer_coin_active_guard_stmt FROM @beer_coin_active_guard_index_sql;
EXECUTE beer_coin_active_guard_stmt;
DEALLOCATE PREPARE beer_coin_active_guard_stmt;

SET @beer_coin_transaction_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'beer_coin_purchase_order'
      AND index_name = 'uk_beer_coin_purchase_wechat_transaction_id'
);
SET @beer_coin_transaction_index_sql = IF(
    @beer_coin_transaction_index_exists = 0,
    'ALTER TABLE beer_coin_purchase_order ADD UNIQUE KEY uk_beer_coin_purchase_wechat_transaction_id (wechat_transaction_id)',
    'SELECT 1'
);
PREPARE beer_coin_transaction_index_stmt FROM @beer_coin_transaction_index_sql;
EXECUTE beer_coin_transaction_index_stmt;
DEALLOCATE PREPARE beer_coin_transaction_index_stmt;
