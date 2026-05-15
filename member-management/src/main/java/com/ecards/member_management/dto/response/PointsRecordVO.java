package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 积分记录VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsRecordVO {

    /**
     * 积分记录ID
     */
    private Long pointsRecordId;

    /**
     * 积分变动值
     */
    private Integer pointsChange;

    /**
     * 变动后积分余额
     */
    private Integer pointsSnapshot;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 操作店铺名称
     */
    private String storeName;

    /**
     * 变动原因
     */
    private String remark;

    /**
     * 操作时间
     */
    private String createTime;
}

