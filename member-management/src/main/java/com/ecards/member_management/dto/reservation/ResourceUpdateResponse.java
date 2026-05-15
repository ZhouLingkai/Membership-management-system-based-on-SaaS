package com.ecards.member_management.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口7：修改资源 - 响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceUpdateResponse {
    private Long id;
    private String updateTime;
}
