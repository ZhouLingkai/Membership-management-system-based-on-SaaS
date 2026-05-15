package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 刷新令牌请求DTO
 */
@Data
public class RefreshTokenRequest {

    /**
     * 令牌类型
     * 1 - 普通令牌
     * 2 - 工作令牌
     */
    @NotNull(message = "令牌类型不能为空")
    @Min(value = 1, message = "令牌类型只能是1或2")
    @Max(value = 2, message = "令牌类型只能是1或2")
    private Integer tokenType;
}

