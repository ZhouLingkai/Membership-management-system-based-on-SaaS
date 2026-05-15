package com.ecards.member_management.dto.reservation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 接口17：员工取消预约请求
 */
@Data
public class StaffCancelRequest {

    @NotNull(message = "预约ID不能为空")
    private Long reservationId;

    @NotBlank(message = "取消原因不能为空")
    @Size(min = 5, max = 100, message = "取消原因必须在5-100字符之间")
    private String cancelReason;

    @NotNull(message = "是否使用取消规则不能为空")
    private Boolean useCancelRule; // true-使用取消规则，false-全额退款
}
