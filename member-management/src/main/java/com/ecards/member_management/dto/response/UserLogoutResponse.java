package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户主动退出响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLogoutResponse {
    /**
     * 已注销令牌的JTI列表
     */
    private List<String> revokedJtis;
}

