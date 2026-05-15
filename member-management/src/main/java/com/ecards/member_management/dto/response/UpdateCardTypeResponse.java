package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 会员卡种修改响应DTO
 * 
 * @author Ecards Team
 * @since 2025-11-02
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCardTypeResponse {

    /**
     * 卡种ID
     */
    private Long cardTypeId;

    /**
     * 修改时间
     */
    private String updateTime;

    /**
     * 已更新的字段列表
     */
    private List<String> updatedFields;
}

