package com.ecards.member_management.dto.reservation;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 接口11：获取预约列表请求
 */
@Data
public class MyReservationsRequest {
    
    private Integer status; // 预约状态（可选）
    
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "开始日期格式必须为YYYY-MM-DD")
    private String startDate; // 开始日期（可选）
    
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "结束日期格式必须为YYYY-MM-DD")
    private String endDate; // 结束日期（可选）
    
    private Integer page; // 页码，默认1
    private Integer pageSize; // 每页数量，默认20
}
