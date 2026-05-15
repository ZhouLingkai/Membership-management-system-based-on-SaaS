package com.ecards.member_management.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 接口14：查询某会员预约情况响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberReservationsResponse {

    private UserInfo userInfo; // 会员信息
    private Integer total; // 总记录数
    private Integer page; // 当前页码
    private Integer pageSize; // 每页数量
    private List<ReservationItem> list; // 预约列表

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private String userId;
        private String nickname;
        private String phone; // AES加密
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservationItem {
        private Long reservationId;
        private Long resourceId;
        private String resourceName;
        private String reservationDate;
        private String startTime;
        private String endTime;
        private Integer operateType;
        private Long transactionId;
        private BigDecimal transactionAmount;
        private Integer reservationStatus;
        private String remark;
        private String createTime;
    }
}
