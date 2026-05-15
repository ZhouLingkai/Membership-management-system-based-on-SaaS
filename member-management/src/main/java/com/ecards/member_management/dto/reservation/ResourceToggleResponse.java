package com.ecards.member_management.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口9：启停用资源响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceToggleResponse {
    
    private Long id;
    private Integer isReservable;
    private String downTime;
    private String updateTime;
}
