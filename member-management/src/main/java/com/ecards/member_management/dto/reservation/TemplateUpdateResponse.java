package com.ecards.member_management.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口3：修改高级预约模板 - 响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateUpdateResponse {
    private Long reserveId;
    private String updateTime;
}
