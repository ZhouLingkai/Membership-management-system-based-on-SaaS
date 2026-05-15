package com.ecards.member_management.controller;

import com.ecards.member_management.annotation.RequireAdminAuth;
import com.ecards.member_management.common.Result;
import com.ecards.member_management.context.AdminContext;
import com.ecards.member_management.dto.response.StsCredentialsResponse;
import com.ecards.member_management.service.StsService;
import com.ecards.member_management.utils.EncryptUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * STS临时凭证控制器
 * 提供获取OSS上传临时凭证的接口（支持普通用户和管理员）
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class StsController {

    private final StsService stsService;
    private final EncryptUtils encryptUtils;

    /**
     * 普通用户获取OSS上传临时凭证
     * GET /api/v1/sts/credentials?pathType=merchant
     *
     * @param authentication 认证信息（从普通令牌中获取）
     * @param pathType 路径类型（非必填，默认merchant）可选值: merchant/user/member/card/employee/resource
     * @return STS临时凭证
     */
    @GetMapping("/sts/credentials")
    public Result<StsCredentialsResponse> getCredentials(
            Authentication authentication,
            @RequestParam(required = false) String pathType) {
        try {
            // 1. 从认证信息中获取用户ID
            String userIdStr = authentication.getName();
            byte[] userId = encryptUtils.uuidToBytes(userIdStr);

            log.info("普通用户获取STS临时凭证: userId={}, pathType={}", userIdStr, pathType);

            // 2. 生成STS临时凭证（pathType为null时默认merchant）
            StsCredentialsResponse credentials = stsService.generateCredentials(userId, pathType);

            log.info("普通用户STS临时凭证生成成功: userId={}, pathPrefix={}", userIdStr, credentials.getPathPrefix());

            return Result.success("获取上传凭证成功", credentials);

        } catch (Exception e) {
            log.error("普通用户获取STS临时凭证失败: error={}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 管理员获取OSS上传临时凭证
     * GET /api/v1/admin/sts/credentials?pathType=merchant
     *
     * @param pathType 路径类型（非必填，默认merchant）可选值: merchant/user/member/card/employee/resource
     * @return STS临时凭证
     */
    @GetMapping("/admin/sts/credentials")
    @RequireAdminAuth("获取STS凭证")
    public Result<StsCredentialsResponse> getAdminCredentials(
            @RequestParam(required = false) String pathType) {
        try {
            // 1. 从AdminContext获取管理员ID
            String adminId = AdminContext.getAdminId();
            byte[] adminIdBytes = encryptUtils.uuidToBytes(adminId);

            log.info("管理员获取STS临时凭证: adminId={}, pathType={}", adminId, pathType);

            // 2. 生成STS临时凭证（pathType为null时默认merchant）
            StsCredentialsResponse credentials = stsService.generateCredentials(adminIdBytes, pathType);

            log.info("管理员STS临时凭证生成成功: adminId={}, pathPrefix={}", adminId, credentials.getPathPrefix());

            return Result.success("获取上传凭证成功", credentials);

        } catch (Exception e) {
            log.error("管理员获取STS临时凭证失败: error={}", e.getMessage(), e);
            throw e;
        }
    }
}

