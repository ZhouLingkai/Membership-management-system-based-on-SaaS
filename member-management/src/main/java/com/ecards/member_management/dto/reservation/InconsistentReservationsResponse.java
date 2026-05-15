package com.ecards.member_management.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 接口18：查询不一致预约记录 - 响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InconsistentReservationsResponse {

    private List<InconsistentReservationItem> reservations;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InconsistentReservationItem {
        private Long reservationId;
        private String userId;
        private String userPhone;
        private String reservationDate;
        private String startTime;
        private String endTime;
        private String resourceId;
        private String resourceName;
    }
}
