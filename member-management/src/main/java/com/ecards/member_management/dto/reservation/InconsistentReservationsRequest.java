package com.ecards.member_management.dto.reservation;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口18：查询不一致预约记录 - 请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InconsistentReservationsRequest {

    @NotBlank(message = "店铺ID不能为空")
    private String storeId;

    @NotBlank(message = "开始日期不能为空")
    private String startDate;

    @NotBlank(message = "结束日期不能为空")
    private String endDate;
}
