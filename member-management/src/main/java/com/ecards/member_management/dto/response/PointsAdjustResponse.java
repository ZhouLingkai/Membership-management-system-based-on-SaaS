package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 积分变动响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsAdjustResponse {

    /**
     * 会员卡ID
     */
    private String memberCardId;

    /**
     * 本次积分变动
     */
    private Integer pointsChange;

    /**
     * 变动后积分余额
     */
    private Integer pointsSnapshot;

    /**
     * 操作时间
     */
    private String operateTime;
}

