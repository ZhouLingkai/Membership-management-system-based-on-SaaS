package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户信息修改响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoUpdateResponse {
    /**
     * 信息更新时间
     */
    private String updateTime;
}

