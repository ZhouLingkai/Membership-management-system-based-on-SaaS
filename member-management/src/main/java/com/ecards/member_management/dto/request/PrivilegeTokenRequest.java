package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 获取特权令牌请求DTO
 */
@Data
public class PrivilegeTokenRequest {

    /**
     * 目标操作列表
     * CARD_CREATE - 办卡
     * RECHARGE - 充值
     * CONSUME - 消费
     * STAFF_BIND - 绑定员工
     * APPOINTMENT - 预约结果展示
     * QUERY_MCARD - 查询会员卡
     */
    @NotEmpty(message = "目标操作列表不能为空")
    private List<String> targetOperate;
}

