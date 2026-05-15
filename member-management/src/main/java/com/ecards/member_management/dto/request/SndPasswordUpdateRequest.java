package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员修改二级密码请求DTO
 * 
 * @author Ecards Team
 * @since 2025-10-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SndPasswordUpdateRequest {

    /**
     * 旧二级密码
     */
    @NotBlank(message = "旧二级密码不能为空")
    private String oldSndPassword;

    /**
     * 新二级密码
     */
    @NotBlank(message = "新二级密码不能为空")
    @Size(min = 6, max = 20, message = "新二级密码长度必须在6到20个字符之间")
    private String newSndPassword;
}

