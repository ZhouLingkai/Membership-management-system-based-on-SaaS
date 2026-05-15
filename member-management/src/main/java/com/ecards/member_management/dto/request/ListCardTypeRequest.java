package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会员卡种列表查询请求DTO
 * 
 * @author Ecards Team
 * @since 2025-11-02
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListCardTypeRequest {

    /**
     * 店铺ID（UUID格式）
     */
    @NotBlank(message = "店铺ID不能为空")
    private String storeId;

    /**
     * 卡种类型筛选：1-余额卡，2-次数卡，3-时效卡，4-积分卡（可选）
     */
    private Integer cardTtype;

    /**
     * 卡种名称模糊筛选（可选）
     */
    private String cardTypeName;

    /**
     * 跨店通用筛选：0-仅本店铺，1-同商家跨店通用（可选）
     */
    private Integer crossStore;

    /**
     * 页码（从1开始）
     */
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须从1开始")
    private Integer pageNum;

    /**
     * 每页条数（1-50）
     */
    @NotNull(message = "每页条数不能为空")
    @Min(value = 1, message = "每页条数必须为1-50")
    @Max(value = 50, message = "每页条数必须为1-50")
    private Integer pageSize;
}

