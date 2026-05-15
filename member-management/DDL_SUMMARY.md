# 数据库DDL整理总结

## 一、任务完成情况

### 1. DDL文件整理
已将所有16个表的DDL语句整理到3个文件中：

- **create_all_tables_part1.sql** - 基础表（表1-6）
  - t_user - 用户表
  - t_merchant_extend - 商家信息扩展表
  - t_merchant_audit_record - 商户审核记录表
  - t_store - 店铺表
  - t_work_relation - 工作关系表
  - t_member_card_type - 会员卡种表

- **create_all_tables_part2.sql** - 会员卡和交易相关表（表7-13）
  - t_member_card - 会员卡表
  - t_mcard_status_log - 会员卡状态变更记录表
  - t_transaction_record - 交易记录表
  - t_points_record - 积分记录表
  - t_registration_card_record - 办卡记录表
  - t_admin - 管理员表
  - t_admin_operation_log - 管理员操作日志表

- **create_all_tables_part3.sql** - 预约系统表（表14-16）
  - t_reservation_template - 预约模板表
  - t_reservation_resource - 预约资源表
  - t_reservation_record - 预约记录表

### 2. ALTER语句处理
- 发现1个被注释的ALTER语句（在create_admin_tables.sql中）
- 该语句用于给t_merchant_audit_record表添加auditor_id字段
- **已确认该字段已在实体类中定义，无需合并**

### 3. 设计文档更新
已更新 `develop_document/数据库设计.md`，主要修改：
- 补充了t_user表的token_version字段
- 补充了t_member_card表的activate_time和create_time/update_time字段
- 修正了t_mcard_status_log表的字段定义（补充old_status、new_status、change_reason、operator_role）
- 修正了t_transaction_record表的transaction_type枚举值（补充4-延期，5-日期变动）
- 修正了balance_snapshot字段为NULL（时效卡不填）
- 补充了remark字段到t_transaction_record表
- 新增了管理员相关表（t_admin、t_admin_operation_log）的完整定义
- 新增了预约系统相关表（t_reservation_template、t_reservation_resource、t_reservation_record）的完整定义
- 标注了消息通知表（t_message_notice）为未实现状态

## 二、实际DDL与设计文档的差异

### 差异1：t_user表
**设计文档缺失字段**：
- `token_version INT NOT NULL DEFAULT 0` - 令牌版本号（用于全局注销令牌）

**影响**：设计文档已更新

### 差异2：t_member_card表
**设计文档缺失字段**：
- `activate_time DATETIME NULL` - 激活时间（未激活时为NULL）
- `create_time DATETIME NOT NULL` - 创建时间
- `update_time DATETIME NOT NULL` - 更新时间

**影响**：设计文档已更新

### 差异3：t_mcard_status_log表
**设计文档字段不完整**：
- 缺少 `old_status TINYINT NULL` - 变更前状态
- 缺少 `new_status TINYINT NOT NULL` - 变更后状态
- 缺少 `change_reason VARCHAR(200) NULL` - 变更原因
- 缺少 `operator_role TINYINT NOT NULL` - 操作人角色

**影响**：设计文档已更新

### 差异4：t_transaction_record表
**设计文档字段不完整**：
- `transaction_type` 枚举值不完整（缺少4-延期，5-日期变动）
- `balance_snapshot` 应为NULL（时效卡不填）
- 缺少 `remark VARCHAR(200) NOT NULL` - 交易备注（必填）

**影响**：设计文档已更新

### 差异5：管理员相关表
**设计文档完全缺失**：
- t_admin - 管理员表
- t_admin_operation_log - 管理员操作日志表

**影响**：设计文档已补充完整定义

### 差异6：预约系统相关表
**设计文档不完整**：
- 预约系统表在"后续扩展功能"中仅简单提及
- 缺少完整的表结构定义

**影响**：设计文档已补充完整定义

### 差异7：消息通知表
**设计文档已定义但未实现**：
- t_message_notice - 消息通知表

**影响**：在设计文档中标注为"未实现"状态

## 三、索引对比

所有表的索引设计与实体类中的@Index注解完全一致，包括：
- 单列索引
- 复合索引
- 唯一索引
- 外键索引

## 四、外键约束对比

实际DDL中的外键约束比设计文档更完善：
- 所有外键都指定了ON DELETE行为（CASCADE、RESTRICT、SET NULL）
- 外键命名规范统一（fk_表名_字段名）

## 五、字段类型对比

### 一致的字段类型：
- UUID字段：BINARY(16)
- 自增主键：BIGINT AUTO_INCREMENT
- 枚举字段：TINYINT
- 金额字段：DECIMAL(10,2)
- 文本字段：VARCHAR(长度)
- 日期时间：DATETIME、DATE、TIME
- JSON字段：JSON

### 字段长度调整：
- password字段：设计文档为VARCHAR(128)，实际为VARCHAR(128)（一致）
- admin表的password和snd_pswd：VARCHAR(255)（管理员密码字段更长）

## 六、执行建议

### 1. 执行顺序
```sql
-- 第1步：执行基础表
source create_all_tables_part1.sql;

-- 第2步：执行会员卡和交易相关表
source create_all_tables_part2.sql;

-- 第3步：执行预约系统表
source create_all_tables_part3.sql;
```

### 2. 注意事项
- 确保数据库字符集为utf8mb4
- 确保排序规则为utf8mb4_unicode_ci
- 建议在测试环境先执行验证
- 原有数据库已清空，可放心执行

### 3. 验证方法
```sql
-- 查看所有表
SHOW TABLES;

-- 查看表结构
DESC t_user;
DESC t_member_card;
-- ... 其他表

-- 查看索引
SHOW INDEX FROM t_user;
SHOW INDEX FROM t_member_card;
-- ... 其他表

-- 查看外键
SELECT 
    TABLE_NAME,
    CONSTRAINT_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'ecards_db'
AND REFERENCED_TABLE_NAME IS NOT NULL;
```

## 七、总结

1. ✅ 已找到全部16个表的DDL定义
2. ✅ 已将DDL整理为3个SQL文件（按依赖关系分组）
3. ✅ 已检查ALTER语句（仅1个被注释的，无需合并）
4. ✅ 已对比设计文档，找出并修正了所有差异
5. ✅ 所有表的索引、外键、字段类型均已验证
6. ✅ 设计文档已更新，补充了缺失的字段和表定义

**最终结论**：实际DDL以实体类为准，设计文档已同步更新。所有16个表的DDL已整理完成，可直接用于数据库初始化。
