package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 冻结会员卡请求DTO
 */
@Data
public class FreezeCardRequest {
    
    /**
     * 会员卡ID（UUID格式）
     */
    @NotBlank(message = "会员卡ID不能为空")
    private String memberCardId;
    
    /**
     * 冻结原因（1-50个字符）
     */
    @NotBlank(message = "冻结原因不能为空")
    @Size(min = 1, max = 50, message = "冻结原因长度必须在1-50个字符之间")
    private String freezeReason;
}
