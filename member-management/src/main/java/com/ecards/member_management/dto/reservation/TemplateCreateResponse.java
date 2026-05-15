package com.ecards.member_management.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口2：创建高级预约模板 - 响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateCreateResponse {
    private Long reserveId;
    private String createTime;
}
