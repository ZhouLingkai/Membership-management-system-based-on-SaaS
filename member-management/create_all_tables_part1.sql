-- ============================================
-- 会员管理系统 - 完整数据库建表脚本（第1部分）
-- 版本: v2.0
-- 创建日期: 2025-11-20
-- 说明: 包含基础表和会员卡相关表（表1-11）
-- ============================================

USE ecards_db;

-- ============================================
-- 1. 用户表（t_user）
-- ============================================
CREATE TABLE IF NOT EXISTS t_user (
    user_id BINARY(16) NOT NULL COMMENT '用户ID，主键（UUID）',
    phone VARCHAR(15) NOT NULL UNIQUE COMMENT '手机号（明文存储，传输时AES256CBC加密）',
    password VARCHAR(128) NOT NULL COMMENT '密码（Argon2加密）',
    nickname VARCHAR(30) NOT NULL COMMENT '会员昵称',
    avatar VARCHAR(100) DEFAULT NULL COMMENT '用户头像URL',
    member_avatar VARCHAR(100) DEFAULT NULL COMMENT '会员头像（用于商家验证是否本人交易）',
    user_type TINYINT NOT NULL COMMENT '用户类型：1-普通用户，2-商家用户，3-员工用户',
    invite_code VARCHAR(10) DEFAULT NULL UNIQUE COMMENT '邀请码',
    invited_code VARCHAR(10) DEFAULT NULL COMMENT '邀请者的邀请码',
    last_login_time DATETIME DEFAULT NULL COMMENT '最后登录时间',
    token_version INT NOT NULL DEFAULT 0 COMMENT '令牌版本号（用于全局注销令牌）',
    register_time DATETIME NOT NULL COMMENT '注册时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    ext_json JSON DEFAULT NULL COMMENT '扩展字段',
    PRIMARY KEY (user_id),
    INDEX idx_user_type (user_type),
    INDEX idx_register_time (register_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================
-- 2. 商家信息扩展表（t_merchant_extend）
-- ============================================
CREATE TABLE IF NOT EXISTS t_merchant_extend (
    merchant_id BINARY(16) NOT NULL COMMENT '商家ID，主键（UUID）',
    user_id BINARY(16) NOT NULL UNIQUE COMMENT '用户ID（外键，关联t_user.user_id）',
    merchant_name VARCHAR(40) DEFAULT NULL COMMENT '商家名称',
    contact_email VARCHAR(255) DEFAULT NULL COMMENT '联系邮箱（明文存储，不脱敏）',
    merchant_intro VARCHAR(300) DEFAULT NULL COMMENT '商户简介',
    certification TINYINT NOT NULL COMMENT '认证状态：1-已认证，2-未认证测试中，3-审核中，4-审核拒绝，5-未认证测试期过，6-认证存疑',
    merchant_level TINYINT NOT NULL DEFAULT 1 COMMENT '商家特权等级：1-普通，2-VIP，3-SVIP，4-至尊VIP',
    snd_pswd VARCHAR(128) NOT NULL COMMENT '二级密码（Argon2加密）',
    privilege_expire_time DATETIME NOT NULL COMMENT '商家特权过期时间',
    remaining_notice_count INT NOT NULL DEFAULT 0 COMMENT '剩余消息通知次数',
    maximum_store_limit TINYINT NOT NULL DEFAULT 2 COMMENT '最大店铺数量限制',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    ext_json JSON DEFAULT NULL COMMENT '扩展字段',
    PRIMARY KEY (merchant_id),
    INDEX idx_merchant_name (merchant_name),
    INDEX idx_merchant_level (merchant_level),
    INDEX idx_privilege_expire_time (privilege_expire_time),
    FOREIGN KEY (user_id) REFERENCES t_user(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家信息扩展表';

-- ============================================
-- 3. 商户审核记录表（t_merchant_audit_record）
-- ============================================
CREATE TABLE IF NOT EXISTS t_merchant_audit_record (
    audit_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '审核ID，主键（自增）',
    user_id BINARY(16) NOT NULL COMMENT '申请用户ID（外键，关联t_user）',
    num_stores TINYINT NOT NULL COMMENT '店铺规模',
    num_members VARCHAR(100) NOT NULL COMMENT '会员规模',
    store_name VARCHAR(40) NOT NULL COMMENT '第一家店铺名称',
    store_photos VARCHAR(300) NOT NULL COMMENT '门头店照（OSS URL）',
    business_license VARCHAR(100) NOT NULL COMMENT '营业执照（OSS URL）',
    application_method TINYINT NOT NULL COMMENT '申请方式：1-直接认证，2-免认证后续补充',
    audit_status TINYINT NOT NULL COMMENT '审核状态：0-待审核，1-已通过，2-已拒绝',
    auditor_id BINARY(16) DEFAULT NULL COMMENT '审核员ID（关联管理员用户ID）',
    reject_reason VARCHAR(60) DEFAULT NULL COMMENT '拒绝原因',
    audit_time DATETIME DEFAULT NULL COMMENT '审核时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    PRIMARY KEY (audit_id),
    INDEX idx_user_id (user_id),
    INDEX idx_application_method (application_method),
    INDEX idx_audit_status (audit_status),
    INDEX idx_auditor_id (auditor_id),
    INDEX idx_audit_time (audit_time),
    FOREIGN KEY (user_id) REFERENCES t_user(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商户审核记录表';

-- ============================================
-- 4. 店铺表（t_store）
-- ============================================
CREATE TABLE IF NOT EXISTS t_store (
    store_id BINARY(16) NOT NULL COMMENT '店铺ID，主键（UUID）',
    merchant_id BINARY(16) NOT NULL COMMENT '商家ID（外键，关联t_merchant_extend.merchant_id）',
    store_name VARCHAR(40) NOT NULL COMMENT '店铺名称',
    store_type VARCHAR(24) DEFAULT NULL COMMENT '店铺类型（CONVENIENCE-便利店、RESTAURANT-餐饮等）',
    address VARCHAR(128) DEFAULT NULL COMMENT '店铺地址',
    store_photos VARCHAR(300) NOT NULL COMMENT '门头店照',
    business_license VARCHAR(100) NOT NULL COMMENT '营业执照',
    contact_phone VARCHAR(15) NOT NULL COMMENT '联系电话（明文存储）',
    contact_wx VARCHAR(40) NOT NULL COMMENT '联系微信号（明文存储）',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '店铺状态：0-关闭，1-正常营业，2-暂停营业，3-店铺注销',
    business_time VARCHAR(50) DEFAULT NULL COMMENT '营业时间（文字描述）',
    appointment TINYINT NOT NULL DEFAULT 0 COMMENT '是否支持预约：1-开启，0-关闭',
    open_store_time DATETIME DEFAULT NULL COMMENT '建店时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    ext_json JSON DEFAULT NULL COMMENT '扩展字段',
    PRIMARY KEY (store_id),
    INDEX idx_merchant_id (merchant_id),
    INDEX idx_store_name (store_name),
    INDEX idx_status (status),
    FOREIGN KEY (merchant_id) REFERENCES t_merchant_extend(merchant_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='店铺表';

-- ============================================
-- 5. 工作关系表（t_work_relation）
-- ============================================
CREATE TABLE IF NOT EXISTS t_work_relation (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '关系ID，主键（自增）',
    store_id BINARY(16) NOT NULL COMMENT '店铺ID（外键，关联t_store.store_id）',
    merchant_id BINARY(16) NOT NULL COMMENT '所属商家ID（冗余字段，方便跨店铺查询）',
    user_id BINARY(16) NOT NULL COMMENT '用户ID（外键，关联t_user.user_id）',
    role VARCHAR(20) NOT NULL COMMENT '角色：manager-店长，employee-店员',
    name VARCHAR(30) DEFAULT NULL COMMENT '员工姓名（绑定时商家填写）',
    permission JSON NOT NULL COMMENT '权限列表（JSON格式）',
    token_version INT NOT NULL DEFAULT 1 COMMENT '工作令牌版本号',
    remark VARCHAR(100) DEFAULT '' COMMENT '备注',
    status TINYINT DEFAULT 1 COMMENT '状态：0-离职，1-在职',
    entry_time DATETIME NOT NULL COMMENT '入职时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_store_user (store_id, user_id),
    INDEX idx_store_id (store_id),
    INDEX idx_merchant_id (merchant_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role (role),
    FOREIGN KEY (store_id) REFERENCES t_store(store_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES t_user(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作关系表';

-- ============================================
-- 6. 会员卡种表（t_member_card_type）
-- ============================================
CREATE TABLE IF NOT EXISTS t_member_card_type (
    card_type_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '卡种ID，主键（自增）',
    store_id BINARY(16) NOT NULL COMMENT '店铺ID（外键，关联t_store.store_id）',
    merchant_id BINARY(16) NOT NULL COMMENT '商家ID（外键，冗余字段提高性能）',
    card_type_name VARCHAR(30) NOT NULL COMMENT '卡种名称',
    description VARCHAR(300) DEFAULT NULL COMMENT '卡种描述',
    card_mask VARCHAR(100) DEFAULT NULL COMMENT '卡面样式标识',
    card_bgc VARCHAR(100) DEFAULT NULL COMMENT '卡种背景图URL',
    card_ttype TINYINT NOT NULL COMMENT '卡种类型：1-余额卡，2-次数卡，3-时效卡，4-积分卡（创建后不可修改）',
    preset_recharge JSON NOT NULL COMMENT '预设充值项目（JSON格式）',
    preset_cost JSON NOT NULL COMMENT '预设消费项目（JSON格式）',
    auto_notify TINYINT NOT NULL DEFAULT 0 COMMENT '自动消息通知：0-关闭，1-短信通知，2-订阅通知，3-程序内推送',
    cross_store TINYINT NOT NULL DEFAULT 0 COMMENT '跨店通用：0-仅本店铺，1-同商家跨店通用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    ext_json JSON DEFAULT NULL COMMENT '扩展字段',
    PRIMARY KEY (card_type_id),
    INDEX idx_store_id (store_id),
    INDEX idx_merchant_id (merchant_id),
    INDEX idx_card_type_name (card_type_name),
    FOREIGN KEY (store_id) REFERENCES t_store(store_id) ON DELETE CASCADE,
    FOREIGN KEY (merchant_id) REFERENCES t_merchant_extend(merchant_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员卡种表';

-- 验证表创建
SELECT 'Part 1 tables (1-6) created successfully!' AS status;
