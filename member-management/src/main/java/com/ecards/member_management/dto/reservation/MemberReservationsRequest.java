package com.ecards.member_management.dto.reservation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 接口14：查询某会员预约情况请求
 */
@Data
public class MemberReservationsRequest {

    @NotBlank(message = "会员手机号不能为空")
    private String userPhone; // 会员手机号（AES加密传输）

    private Integer status; // 预约状态（可选）

    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "开始日期格式必须为YYYY-MM-DD")
    private String startDate; // 开始日期（可选）

    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "结束日期格式必须为YYYY-MM-DD")
    private String endDate; // 结束日期（可选）

    @Min(value = 1, message = "页码必须大于0")
    private Integer page = 1; // 页码，默认1

    @Min(value = 1, message = "每页数量必须大于0")
    private Integer pageSize = 20; // 每页数量，默认20
}
