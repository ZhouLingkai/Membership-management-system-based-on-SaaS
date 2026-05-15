package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 通过手机号查询会员卡响应DTO（接口8）
 * 线下扫码查询个人会员卡列表响应DTO（接口15-场景B）
 * 
 * @author Ecards Team
 * @since 2025-11-04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryByPhoneResponse {

    /**
     * 查询时的手机号（明文）
     */
    private String memberPhone;

    /**
     * 手机号对应的用户ID（UUID格式，可能为null表示用户未注册）
     */
    private String userId;

    /**
     * 本店办理的会员卡列表
     */
    private List<MemberCardVO> localCards;

    /**
     * 可用的跨店卡列表（其他店铺办理的跨店卡）
     */
    private List<MemberCardVO> crossStoreCards;
}

