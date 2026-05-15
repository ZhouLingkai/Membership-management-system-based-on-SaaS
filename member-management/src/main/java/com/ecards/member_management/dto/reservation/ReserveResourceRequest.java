package com.ecards.member_management.dto.reservation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 接口12：预约资源请求
 */
@Data
public class ReserveResourceRequest {
    
    @NotBlank(message = "店铺ID不能为空")
    private String storeId;
    
    @NotNull(message = "资源ID不能为空")
    private Long resourceId;
    
    @NotBlank(message = "会员卡ID不能为空")
    private String memberCardId;
    
    @NotBlank(message = "预约日期不能为空")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "日期格式必须为YYYY-MM-DD")
    private String reservationDate;
    
    @NotEmpty(message = "预约时间段不能为空")
    @Size(min = 1, message = "至少需要一个时间段")
    private List<String> timeSlots;
    
    @Size(max = 50, message = "备注不能超过50字符")
    private String remark; // 备注（可选，0-50字符）
    
    // ========== 以下字段由后端自动设置，不在请求中 ==========
    
    // 由timeSlots推导，用于数据库存储
    private String startTime;
    private String endTime;
}
