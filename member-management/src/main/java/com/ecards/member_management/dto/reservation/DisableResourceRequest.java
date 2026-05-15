package com.ecards.member_management.dto.reservation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 接口15：停用预约资源请求
 */
@Data
public class DisableResourceRequest {

    @NotNull(message = "资源ID不能为空")
    private Long resourceId;

    @NotBlank(message = "停用日期不能为空")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "日期格式必须为YYYY-MM-DD")
    private String reservationDate;

    @NotEmpty(message = "停用时间段不能为空")
    @Size(min = 1, message = "至少需要一个时间段")
    private List<String> timeSlots;

    @NotBlank(message = "停用原因不能为空")
    @Size(min = 5, max = 100, message = "停用原因必须在5-100字符之间")
    private String reason;

    // 由timeSlots推导，用于数据库存储
    private String startTime;
    private String endTime;
}
