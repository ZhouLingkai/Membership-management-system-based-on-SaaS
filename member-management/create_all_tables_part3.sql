-- ============================================
-- 会员管理系统 - 完整数据库建表脚本（第3部分）
-- 版本: v2.0
-- 创建日期: 2025-11-20
-- 说明: 包含预约系统相关表（表14-16）
-- 前置条件: 需先执行 create_all_tables_part1.sql 和 create_all_tables_part2.sql
-- ============================================

USE ecards_db;

-- ============================================
-- 14. 预约模板表（t_reservation_template）
-- ============================================
CREATE TABLE IF NOT EXISTS t_reservation_template (
    reserve_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '模板ID，主键（自增）',
    store_id BINARY(16) NOT NULL COMMENT '店铺ID',
    reservation_time_list VARCHAR(300) NOT NULL COMMENT '可预约时间段，格式：["08:00-09:00","09:00-10:00"]',
    cancel_rule VARCHAR(50) NOT NULL COMMENT '取消规则，格式：["60:0.1","180:5"]',
    advance_days TINYINT NOT NULL COMMENT '可提前预约天数',
    forbidden_days VARCHAR(300) DEFAULT NULL COMMENT '不支持预约的日子，格式：["周六","周日","2025-11-16"]',
    customize_forbidden TINYINT NOT NULL DEFAULT 0 COMMENT '是否支持资源自定义禁止日期：0-不支持，1-支持',
    effective_start_time DATE NOT NULL COMMENT '模板生效开始日期',
    effective_end_time DATE DEFAULT NULL COMMENT '模板生效结束日期，NULL表示长期有效',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    ext_json JSON DEFAULT NULL COMMENT '扩展字段',
    PRIMARY KEY (reserve_id),
    INDEX idx_store_id (store_id),
    INDEX idx_effective_start (effective_start_time),
    INDEX idx_effective_end (effective_end_time),
    INDEX idx_store_effective (store_id, effective_start_time, effective_end_time),
    FOREIGN KEY (store_id) REFERENCES t_store(store_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预约模板表';

-- ============================================
-- 15. 预约资源表（t_reservation_resource）
-- ============================================
CREATE TABLE IF NOT EXISTS t_reservation_resource (
    resource_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '资源ID，主键（自增）',
    store_id BINARY(16) NOT NULL COMMENT '店铺ID',
    resource_name VARCHAR(20) NOT NULL COMMENT '资源名称',
    is_reservable TINYINT NOT NULL DEFAULT 1 COMMENT '是否开启预约：0-未开启，1-已开启',
    support_card_types TINYINT NOT NULL COMMENT '支持的卡种：1-余额卡，2-次数卡',
    min_continuous_time INT DEFAULT NULL COMMENT '最少连续预约时间（分钟）',
    max_continuous_time INT DEFAULT NULL COMMENT '最大连续预约时间（分钟）',
    unit_price DECIMAL(10,2) NOT NULL COMMENT '单价',
    resource_img VARCHAR(100) DEFAULT NULL COMMENT '资源图片URL',
    resource_desc VARCHAR(150) DEFAULT NULL COMMENT '资源描述',
    down_time DATETIME DEFAULT NULL COMMENT '停用时间',
    promotion_strategy JSON DEFAULT NULL COMMENT '优惠策略，格式：{"non_effective_dates":[],"week":[]}',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    ext_json JSON DEFAULT NULL COMMENT '扩展字段',
    PRIMARY KEY (resource_id),
    INDEX idx_store_id (store_id),
    INDEX idx_resource_name (resource_name),
    INDEX idx_is_reservable (is_reservable),
    INDEX idx_support_card_types (support_card_types),
    INDEX idx_store_reservable (store_id, is_reservable),
    INDEX idx_store_name (store_id, resource_name),
    FOREIGN KEY (store_id) REFERENCES t_store(store_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预约资源表';

-- ============================================
-- 16. 预约记录表（t_reservation_record）
-- ============================================
CREATE TABLE IF NOT EXISTS t_reservation_record (
    reservation_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '预约ID，主键（自增）',
    resource_id BIGINT NOT NULL COMMENT '资源ID',
    template_id BIGINT NOT NULL COMMENT '模板ID',
    reservation_date DATE NOT NULL COMMENT '预约日期',
    start_time TIME NOT NULL COMMENT '开始时间',
    end_time TIME NOT NULL COMMENT '结束时间',
    operate_type TINYINT NOT NULL COMMENT '预约方式：1-用户预约，2-线下占用，3-资源停用',
    user_id BINARY(16) NOT NULL COMMENT '预约人ID',
    user_phone VARCHAR(15) NOT NULL COMMENT '预约人手机号',
    transaction_id BIGINT NOT NULL COMMENT '交易ID',
    transaction_amount DECIMAL(10,2) NOT NULL COMMENT '交易值（次数/金额）',
    reservation_status TINYINT NOT NULL COMMENT '预约状态：0-待使用，1-已使用，2-主动取消，3-员工取消，4-未使用且过期',
    remark VARCHAR(50) DEFAULT NULL COMMENT '预约备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (reservation_id),
    INDEX idx_resource_id (resource_id),
    INDEX idx_template_id (template_id),
    INDEX idx_reservation_date (reservation_date),
    INDEX idx_operate_type (operate_type),
    INDEX idx_user_id (user_id),
    INDEX idx_user_phone (user_phone),
    INDEX idx_transaction_id (transaction_id),
    INDEX idx_reservation_status (reservation_status),
    INDEX idx_create_time (create_time),
    INDEX idx_resource_date_status (resource_id, reservation_date, reservation_status),
    INDEX idx_user_status_date (user_id, reservation_status, reservation_date),
    INDEX idx_date_resource (reservation_date, resource_id),
    FOREIGN KEY (resource_id) REFERENCES t_reservation_resource(resource_id) ON DELETE CASCADE,
    FOREIGN KEY (template_id) REFERENCES t_reservation_template(reserve_id) ON DELETE RESTRICT,
    FOREIGN KEY (user_id) REFERENCES t_user(user_id) ON DELETE CASCADE,
    FOREIGN KEY (transaction_id) REFERENCES t_transaction_record(transaction_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预约记录表';

-- ============================================
-- 验证所有表创建完成
-- ============================================
SELECT 'Part 3 tables (14-16) created successfully!' AS status;
SELECT 'All 16 tables have been created!' AS final_status;

-- ============================================
-- 表结构总结
-- ============================================
-- 1. t_user - 用户表
-- 2. t_merchant_extend - 商家信息扩展表
-- 3. t_merchant_audit_record - 商户审核记录表
-- 4. t_store - 店铺表
-- 5. t_work_relation - 工作关系表
-- 6. t_member_card_type - 会员卡种表
-- 7. t_member_card - 会员卡表
-- 8. t_mcard_status_log - 会员卡状态变更记录表
-- 9. t_transaction_record - 交易记录表
-- 10. t_points_record - 积分记录表
-- 11. t_registration_card_record - 办卡记录表
-- 12. t_admin - 管理员表
-- 13. t_admin_operation_log - 管理员操作日志表
-- 14. t_reservation_template - 预约模板表
-- 15. t_reservation_resource - 预约资源表
-- 16. t_reservation_record - 预约记录表
-- ============================================
