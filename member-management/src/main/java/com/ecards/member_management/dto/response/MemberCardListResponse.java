package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 会员卡列表响应DTO
 * 
 * @author Ecards Team
 * @since 2025-11-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberCardListResponse {

    /**
     * 总会员卡数量
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
     * 卡范围（可选，接口8使用）
     * local-本店卡, cross_store-跨店卡
     */
    private String cardScope;

    /**
     * 会员卡列表
     */
    private List<MemberCardVO> list;
}

