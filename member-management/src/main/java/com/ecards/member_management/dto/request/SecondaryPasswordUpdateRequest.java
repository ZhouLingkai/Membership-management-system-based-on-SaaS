package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商户二级密码修改请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecondaryPasswordUpdateRequest {

    /**
     * 商户ID（与令牌一致）
     */
    @NotBlank(message = "商户ID不能为空")
    private String merchantId;

    /**
     * 原二级密码（明文）
     */
    @NotBlank(message = "原二级密码不能为空")
    private String oldSndPswd;

    /**
     * 新二级密码（明文，复杂度需符合要求）
     */
    @NotBlank(message = "新二级密码不能为空")
    @Size(min = 8, message = "新二级密码长度至少为8位")
    private String newSndPswd;

    /**
     * 确认新二级密码（需与newSndPswd一致）
     */
    @NotBlank(message = "确认密码不能为空")
    private String confirmSndPswd;
}

