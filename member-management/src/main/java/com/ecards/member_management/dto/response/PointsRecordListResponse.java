package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 积分记录列表响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsRecordListResponse {

    /**
     * 当前积分余额
     */
    private Integer currentPoints;

    /**
     * 总记录数
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
     * 积分记录列表
     */
    private List<PointsRecordVO> list;
}

