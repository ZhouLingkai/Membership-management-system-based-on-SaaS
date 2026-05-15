package com.ecards.member_management.dto.reservation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 接口10：查询某日预约情况请求
 */
@Data
public class ReservationQueryRequest {
    
    @NotBlank(message = "店铺ID不能为空")
    private String storeId;
    
    @NotBlank(message = "查询日期不能为空")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "日期格式必须为YYYY-MM-DD")
    private String requestDate;
    
    private String keyword; // 资源名关键词（可选）
}
