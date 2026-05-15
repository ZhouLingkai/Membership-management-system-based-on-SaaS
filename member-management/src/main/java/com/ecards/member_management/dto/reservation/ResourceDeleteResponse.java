package com.ecards.member_management.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口8：删除资源 - 响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceDeleteResponse {
    private Long id;
    private String deleteTime;
}
