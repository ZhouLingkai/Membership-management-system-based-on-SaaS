-- ============================================
-- 会员管理系统 - 完整数据库建表脚本（第2部分）
-- 版本: v2.0
-- 创建日期: 2025-11-20
-- 说明: 包含会员卡、交易记录、管理员表（表7-13）
-- 前置条件: 需先执行 create_all_tables_part1.sql
-- ============================================

USE ecards_db;

-- ============================================
-- 7. 会员卡表（t_member_card）
-- ============================================
CREATE TABLE IF NOT EXISTS t_member_card (
    member_card_id BINARY(16) NOT NULL COMMENT '会员卡ID，主键（UUID）',
    card_type_id BIGINT NOT NULL COMMENT '卡种ID（外键，关联t_member_card_type.card_type_id）',
    store_id BINARY(16) NOT NULL COMMENT '店铺ID（冗余字段提高查询性能）',
    merchant_id BINARY(16) NOT NULL COMMENT '商家ID（冗余字段提高查询性能）',
    user_id BINARY(16) DEFAULT NULL COMMENT '用户ID（外键，可先办卡后认主）',
    member_name VARCHAR(30) DEFAULT NULL COMMENT '会员预留姓名',
    member_phone VARCHAR(15) DEFAULT NULL COMMENT '会员预留手机号（明文存储，传输加密）',
    card_ttype TINYINT NOT NULL COMMENT '卡种类型（冗余字段）：1-余额卡，2-次数卡，3-时效卡，4-积分卡',
    balance DECIMAL(10,2) DEFAULT 0.00 COMMENT '会员卡余额（余额卡主要使用）',
    times SMALLINT DEFAULT 0 COMMENT '会员卡剩余次数（次数卡主要使用）',
    points INT DEFAULT 0 COMMENT '积分值（所有卡种都可使用）',
    cumulative_points INT NOT NULL DEFAULT 0 COMMENT '累积总积分（只增不减）',
    status TINYINT NOT NULL COMMENT '会员卡状态：0-未激活，1-正常，2-已过期，3-已冻结，4-已注销',
    open_card_time DATETIME NOT NULL COMMENT '开卡时间',
    activate_time DATETIME DEFAULT NULL COMMENT '激活时间（未激活时为NULL）',
    expire_time DATETIME NOT NULL COMMENT '到期时间（时效卡主要参考）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    ext_json JSON DEFAULT NULL COMMENT '扩展字段',
    PRIMARY KEY (member_card_id),
    INDEX idx_card_type_id (card_type_id),
    INDEX idx_store_id (store_id),
    INDEX idx_merchant_id (merchant_id),
    INDEX idx_user_id (user_id),
    INDEX idx_member_phone (member_phone),
    INDEX idx_status (status),
    INDEX idx_card_ttype (card_ttype),
    INDEX idx_open_card_time (open_card_time),
    INDEX idx_activate_time (activate_time),
    INDEX idx_expire_time (expire_time),
    INDEX idx_store_status (store_id, status),
    INDEX idx_merchant_status (merchant_id, status),
    INDEX idx_user_status (user_id, status),
    INDEX idx_merchant_user (merchant_id, user_id),
    INDEX idx_phone_store (member_phone, store_id),
    FOREIGN KEY (card_type_id) REFERENCES t_member_card_type(card_type_id) ON DELETE RESTRICT,
    FOREIGN KEY (store_id) REFERENCES t_store(store_id) ON DELETE CASCADE,
    FOREIGN KEY (merchant_id) REFERENCES t_merchant_extend(merchant_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES t_user(user_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员卡表';

-- ============================================
-- 8. 会员卡状态变更记录表（t_mcard_status_log）
-- ============================================
CREATE TABLE IF NOT EXISTS t_mcard_status_log (
    mcardlog_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '会员卡变更记录ID，主键（自增）',
    member_card_id BINARY(16) NOT NULL COMMENT '关联会员卡ID（外键）',
    change_type TINYINT NOT NULL COMMENT '变更类型：0-冻结，1-解冻，2-激活，3-到期，4-注销',
    old_status TINYINT DEFAULT NULL COMMENT '变更前状态',
    new_status TINYINT NOT NULL COMMENT '变更后状态',
    change_reason VARCHAR(60) DEFAULT NULL COMMENT '变更原因',
    operator_id BINARY(16) NOT NULL COMMENT '操作人ID（外键）',
    operator_role TINYINT NOT NULL COMMENT '操作人角色：0-商家，1-店长，2-店员，3-用户本人',
    operator_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (mcardlog_id),
    INDEX idx_member_card_id (member_card_id),
    INDEX idx_change_type (change_type),
    INDEX idx_operator_id (operator_id),
    INDEX idx_operator_time (operator_time),
    FOREIGN KEY (member_card_id) REFERENCES t_member_card(member_card_id) ON DELETE CASCADE,
    FOREIGN KEY (operator_id) REFERENCES t_user(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员卡状态变更记录表';

-- ============================================
-- 9. 交易记录表（t_transaction_record）
-- ============================================
CREATE TABLE IF NOT EXISTS t_transaction_record (
    transaction_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '交易ID，主键（自增）',
    member_card_id BINARY(16) NOT NULL COMMENT '会员卡ID（外键）',
    user_id BINARY(16) DEFAULT NULL COMMENT '用户ID（冗余字段）',
    merchant_id BINARY(16) NOT NULL COMMENT '商家ID（冗余字段）',
    transaction_type TINYINT NOT NULL COMMENT '交易类型：1-充值，2-消费，3-退款，4-延期，5-日期变动',
    amount DECIMAL(10,2) NOT NULL COMMENT '交易值（金额/次数/天数，正数为入，负数为出）',
    balance_snapshot DECIMAL(10,2) DEFAULT NULL COMMENT '余额/次数快照（交易后的值，时效卡不填）',
    operator_id BINARY(16) NOT NULL COMMENT '操作员ID（外键）',
    trans_store_id BINARY(16) NOT NULL COMMENT '交易店铺ID（跨店卡关键字段）',
    remark VARCHAR(60) NOT NULL COMMENT '交易备注（必填）',
    transaction_time DATETIME NOT NULL COMMENT '交易时间',
    PRIMARY KEY (transaction_id),
    INDEX idx_member_card_id (member_card_id),
    INDEX idx_user_id (user_id),
    INDEX idx_trans_store_id (trans_store_id),
    INDEX idx_merchant_id (merchant_id),
    INDEX idx_transaction_type (transaction_type),
    INDEX idx_transaction_time (transaction_time),
    INDEX idx_operator_id (operator_id),
    INDEX idx_store_time (trans_store_id, transaction_time),
    INDEX idx_merchant_time (merchant_id, transaction_time),
    INDEX idx_user_time (user_id, transaction_time),
    INDEX idx_card_time (member_card_id, transaction_time),
    INDEX idx_store_type_time (trans_store_id, transaction_type, transaction_time),
    FOREIGN KEY (member_card_id) REFERENCES t_member_card(member_card_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES t_user(user_id) ON DELETE SET NULL,
    FOREIGN KEY (merchant_id) REFERENCES t_merchant_extend(merchant_id) ON DELETE CASCADE,
    FOREIGN KEY (operator_id) REFERENCES t_user(user_id) ON DELETE CASCADE,
    FOREIGN KEY (trans_store_id) REFERENCES t_store(store_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易记录表';

-- ============================================
-- 10. 积分记录表（t_points_record）
-- ============================================
CREATE TABLE IF NOT EXISTS t_points_record (
    points_record_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '积分记录ID，主键（自增）',
    member_card_id BINARY(16) NOT NULL COMMENT '会员卡ID（外键）',
    user_id BINARY(16) DEFAULT NULL COMMENT '用户ID（冗余字段）',
    merchant_id BINARY(16) NOT NULL COMMENT '商家ID（冗余字段）',
    trans_store_id BINARY(16) NOT NULL COMMENT '操作店铺ID',
    points_change INT NOT NULL COMMENT '积分变动值（正数为增加，负数为扣减）',
    points_snapshot INT NOT NULL COMMENT '变动后积分余额',
    operator_id BINARY(16) NOT NULL COMMENT '操作人ID',
    remark VARCHAR(60) NOT NULL COMMENT '变动原因（必填）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (points_record_id),
    INDEX idx_member_card_id (member_card_id),
    INDEX idx_user_id (user_id),
    INDEX idx_merchant_id (merchant_id),
    INDEX idx_trans_store_id (trans_store_id),
    INDEX idx_create_time (create_time),
    INDEX idx_operator_id (operator_id),
    INDEX idx_card_time (member_card_id, create_time),
    INDEX idx_user_time (user_id, create_time),
    FOREIGN KEY (member_card_id) REFERENCES t_member_card(member_card_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES t_user(user_id) ON DELETE SET NULL,
    FOREIGN KEY (merchant_id) REFERENCES t_merchant_extend(merchant_id) ON DELETE CASCADE,
    FOREIGN KEY (operator_id) REFERENCES t_user(user_id) ON DELETE CASCADE,
    FOREIGN KEY (trans_store_id) REFERENCES t_store(store_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分记录表';

-- ============================================
-- 11. 办卡记录表（t_registration_card_record）
-- ============================================
CREATE TABLE IF NOT EXISTS t_registration_card_record (
    registration_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '办卡记录ID，主键（自增）',
    member_card_id BINARY(16) NOT NULL COMMENT '会员卡ID（外键）',
    operator_id BINARY(16) NOT NULL COMMENT '操作员ID（办卡操作人）',
    registration_channel TINYINT NOT NULL COMMENT '办卡渠道：0-线下二维码，1-先办后激活，2-线上领卡，3-批量办卡',
    operator_role TINYINT NOT NULL COMMENT '操作员角色：0-商家，1-店长，2-店员',
    trans_store_id BINARY(16) NOT NULL COMMENT '办卡店铺ID',
    registration_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '办卡时间',
    PRIMARY KEY (registration_id),
    INDEX idx_member_card_id (member_card_id),
    INDEX idx_operator_id (operator_id),
    INDEX idx_trans_store_id (trans_store_id),
    INDEX idx_registration_time (registration_time),
    INDEX idx_registration_channel (registration_channel),
    INDEX idx_operator_role (operator_role),
    FOREIGN KEY (member_card_id) REFERENCES t_member_card(member_card_id) ON DELETE CASCADE,
    FOREIGN KEY (operator_id) REFERENCES t_user(user_id) ON DELETE CASCADE,
    FOREIGN KEY (trans_store_id) REFERENCES t_store(store_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='办卡记录表';

-- ============================================
-- 12. 管理员表（t_admin）
-- ============================================
CREATE TABLE IF NOT EXISTS t_admin (
    admin_id BINARY(16) NOT NULL COMMENT '管理员ID，主键（UUID二进制存储）',
    phone VARCHAR(15) NOT NULL UNIQUE COMMENT '手机号（明文）',
    account VARCHAR(50) NOT NULL UNIQUE COMMENT '登录账号',
    password VARCHAR(128) NOT NULL COMMENT '密码（Argon2加密）',
    snd_pswd VARCHAR(128) NOT NULL COMMENT '二级密码（Argon2加密，用于危险操作确认）',
    admin_role TINYINT NOT NULL DEFAULT 2 COMMENT '角色：1-超级管理员，2-审核员',
    token_version INT NOT NULL DEFAULT 1 COMMENT '令牌版本号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    allowed_ips VARCHAR(225) DEFAULT NULL COMMENT 'IP白名单',
    last_login_time DATETIME DEFAULT NULL COMMENT '最后登录时间',
    last_login_ip VARCHAR(45) DEFAULT NULL COMMENT '最后登录IP',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator_id BINARY(16) DEFAULT NULL COMMENT '创建者ID',
    remark VARCHAR(100) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (admin_id),
    UNIQUE KEY uk_phone (phone),
    UNIQUE KEY uk_account (account),
    INDEX idx_phone (phone),
    INDEX idx_account (account),
    INDEX idx_admin_role (admin_role),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员表';

-- ============================================
-- 13. 管理员操作日志表（t_admin_operation_log）
-- ============================================
CREATE TABLE IF NOT EXISTS t_admin_operation_log (
    log_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID，主键（自增）',
    admin_id BINARY(16) NOT NULL COMMENT '操作管理员ID',
    admin_account VARCHAR(50) NOT NULL COMMENT '管理员账号',
    operation_type VARCHAR(50) NOT NULL COMMENT '操作类型',
    target_type VARCHAR(20) NOT NULL COMMENT '操作对象类型',
    target_id BINARY(16) DEFAULT NULL COMMENT '操作对象ID',
    operation_desc VARCHAR(150) NOT NULL COMMENT '操作描述',
    request_params TEXT DEFAULT NULL COMMENT '请求参数（JSON）',
    operation_ip VARCHAR(45) NOT NULL COMMENT '操作IP',
    device_id VARCHAR(60) DEFAULT NULL COMMENT '设备ID',
    operation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    result TINYINT NOT NULL DEFAULT 1 COMMENT '操作结果：1-成功，0-失败',
    error_msg VARCHAR(150) DEFAULT NULL COMMENT '错误信息',
    PRIMARY KEY (log_id),
    INDEX idx_admin_id (admin_id),
    INDEX idx_operation_type (operation_type),
    INDEX idx_target_id (target_id),
    INDEX idx_operation_time (operation_time),
    INDEX idx_result (result)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员操作日志表';

-- 验证表创建
SELECT 'Part 2 tables (7-13) created successfully!' AS status;
