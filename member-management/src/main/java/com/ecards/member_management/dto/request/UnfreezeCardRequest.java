package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 解冻会员卡请求DTO
 */
@Data
public class UnfreezeCardRequest {
    
    /**
     * 会员卡ID（UUID格式）
     */
    @NotBlank(message = "会员卡ID不能为空")
    private String memberCardId;
    
    /**
     * 解冻原因（0-50个字符，可选）
     */
    @Size(max = 50, message = "解冻原因长度不能超过50个字符")
    private String unfreezeReason;
}
