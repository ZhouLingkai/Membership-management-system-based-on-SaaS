package com.ecards.member_management.controller;

import com.ecards.member_management.common.Result;
import com.ecards.member_management.constants.TokenConstants;
import com.ecards.member_management.dto.request.VerifyCodeRequest;
import com.ecards.member_management.dto.response.VerifyCodeResponse;
import com.ecards.member_management.service.VerifyCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 验证码控制器
 * 提供验证码相关的API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class VerifyCodeController {

    private final VerifyCodeService verifyCodeService;

    /**
     * 获取短信验证码
     * POST /api/v1/users/verify-code
     *
     * @param request  验证码请求
     * @param deviceId 设备ID
     * @return 验证码发送结果
     */
    @PostMapping("/verify-code")
    public Result<VerifyCodeResponse> getVerifyCode(
            @Valid @RequestBody VerifyCodeRequest request,
            @RequestHeader(TokenConstants.DEVICE_ID_HEADER) String deviceId) {

        try {
            log.info("收到验证码请求: phone={}, platform={}, deviceId={}", 
                    request.getPhone().substring(0, Math.min(10, request.getPhone().length())) + "...", 
                    request.getPlatform(), deviceId);

            // 生成并发送验证码
            Map<String, Object> result = verifyCodeService.generateAndSendCode(
                    request.getPhone(),
                    deviceId,
                    request.getPlatform()
            );

            // 构建响应
            VerifyCodeResponse response = VerifyCodeResponse.builder()
                    .sendTime((String) result.get("sendTime"))
                    .expireSeconds((Integer) result.get("expireSeconds"))
                    .remainingRetries((Integer) result.get("remainingRetries"))
                    .build();

            return Result.success("验证码已发送", response);
        } catch (Exception e) {
            log.error("获取验证码失败", e);
            return Result.fail(e.getMessage());
        }
    }
}

