package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会员卡种详情查询请求DTO
 * 
 * @author Ecards Team
 * @since 2025-11-02
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetailCardTypeRequest {

    /**
     * 卡种ID
     */
    @NotNull(message = "卡种ID不能为空")
    private Long cardTypeId;

    /**
     * 店铺ID（双重校验，避免卡种与店铺不匹配）
     */
    @NotBlank(message = "店铺ID不能为空")
    private String storeId;
}

