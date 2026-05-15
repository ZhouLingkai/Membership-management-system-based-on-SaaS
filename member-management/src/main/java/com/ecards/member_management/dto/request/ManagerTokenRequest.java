package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 获取管理令牌请求DTO
 */
@Data
public class ManagerTokenRequest {

    /**
     * 商家ID（UUID字符串）
     */
    @NotBlank(message = "商家ID不能为空")
    private String merchantId;

    /**
     * 二级密码（明文）
     */
    @NotBlank(message = "二级密码不能为空")
    private String sndPswd;

    /**
     * 目标操作店铺ID（可选，仅操作店铺时必填）
     */
    private String storeId;
}

