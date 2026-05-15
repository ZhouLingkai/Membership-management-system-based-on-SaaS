package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户工作关系列表响应DTO
 * 
 * @author Ecards Team
 * @since 2025-10-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserWorkRelationsResponse {

    /**
     * 工作关系列表
     */
    private List<WorkRelationItem> workRelations;
}

