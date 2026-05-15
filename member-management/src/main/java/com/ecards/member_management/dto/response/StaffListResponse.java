package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 员工列表响应DTO
 * 
 * @author Ecards Team
 * @since 2025-10-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffListResponse {

    /**
     * 总员工数量
     */
    private Long total;

    /**
     * 当前页码
     */
    private Integer pageNum;

    /**
     * 每页条数
     */
    private Integer pageSize;

    /**
     * 员工列表
     */
    private List<StaffListItem> list;
}

