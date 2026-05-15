package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 检查手机号注册状态响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckPhoneResponse {

    /**
     * 是否已注册
     * true - 已注册（yes）
     * false - 未注册（no）
     */
    private Boolean registered;
}
