package com.ecards.member_management.controller;

import com.ecards.member_management.common.Result;
import com.ecards.member_management.constants.TokenConstants;
import com.ecards.member_management.context.TokenContext;
import com.ecards.member_management.dto.request.MerchantInfoUpdateRequest;
import com.ecards.member_management.dto.request.MerchantRegistrationRequest;
import com.ecards.member_management.dto.request.QualificationSubmitRequest;
import com.ecards.member_management.dto.request.SecondaryPasswordResetRequest;
import com.ecards.member_management.dto.request.SecondaryPasswordUpdateRequest;
import com.ecards.member_management.dto.response.MerchantInfoResponse;
import com.ecards.member_management.dto.response.MerchantInfoUpdateResponse;
import com.ecards.member_management.dto.response.MerchantRegistrationResponse;
import com.ecards.member_management.dto.response.QualificationAuditQueryResponse;
import com.ecards.member_management.dto.response.QualificationSubmitResponse;
import com.ecards.member_management.dto.response.SecondaryPasswordResetResponse;
import com.ecards.member_management.dto.response.SecondaryPasswordUpdateResponse;
import com.ecards.member_management.entity.MerchantAuditRecord;
import com.ecards.member_management.exception.BusinessException;
import com.ecards.member_management.service.MerchantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 商户控制器
 * 提供商户注册、信息查询、信息修改、二级密码修改等API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    /**
     * 商户注册（升级）
     * POST /api/v1/merchants/registration
     *
     * @param request  商户注册请求
     * @param deviceId 设备ID
     * @return 商户注册响应
     */
    @PostMapping("/registration")
    public Result<MerchantRegistrationResponse> registerMerchant(
            @Valid @RequestBody MerchantRegistrationRequest request,
            @RequestHeader(TokenConstants.DEVICE_ID_HEADER) String deviceId) {

        try {
            log.info("收到商户注册请求: userId={}, applicationType={}", 
                    request.getUserId(), request.getApplicationType());

            // 从TokenContext获取当前用户ID进行校验
            String tokenUserId = TokenContext.getCurrentUserId();
            if (!request.getUserId().equals(tokenUserId)) {
                return Result.fail(403, "用户ID与令牌不匹配");
            }

            // 调用Service处理商户注册
            MerchantRegistrationResponse response = merchantService.registerMerchant(request);

            // 根据申请方式返回不同的消息
            String message;
            if (request.getApplicationType() == 1) {
                message = "商户申请成功，已开通7天测试期，请及时在测试期内补充资质认证";
            } else {
                message = "商户资质提交成功，待审核（预计1-2个工作日）";
            }

            log.info("商户注册成功: merchantId={}, certification={}", 
                    response.getMerchantId(), response.getCertification());

            return Result.success(message, response);
        } catch (Exception e) {
            log.error("商户注册失败: userId={}, error={}", request.getUserId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 商户基础信息查询
     * GET /api/v1/merchants/info
     *
     * @param merchantId 商户ID
     * @return 商户信息响应
     */
    @GetMapping("/info")
    public Result<MerchantInfoResponse> getMerchantInfo(
            @RequestParam String merchantId) {

        try {
            log.info("查询商户基础信息: merchantId={}", merchantId);

            // 从TokenContext获取当前用户信息进行校验（可选，取决于业务需求）
            // 这里暂不强制校验，允许商户查询自己的信息

            // 调用Service查询商户信息
            MerchantInfoResponse response = merchantService.getMerchantInfo(merchantId);

            // 根据认证状态返回不同的消息
            String message = "信息查询成功";
            if (response.getCertification() == 2 && response.getRemainingDays() != null) {
                message = String.format("信息查询成功（测试期剩余%d天，请尽快补充资质认证）", 
                        response.getRemainingDays());
            }

            log.info("商户信息查询成功: merchantId={}, merchantName={}", 
                    merchantId, response.getMerchantName());

            return Result.success(message, response);
        } catch (Exception e) {
            log.error("商户信息查询失败: merchantId={}, error={}", merchantId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 商户基础信息修改
     * PUT /api/v1/merchants/info
     *
     * @param request  商户信息修改请求
     * @param deviceId 设备ID
     * @return 商户信息修改响应
     */
    @PutMapping("/info")
    public Result<MerchantInfoUpdateResponse> updateMerchantInfo(
            @Valid @RequestBody MerchantInfoUpdateRequest request,
            @RequestHeader(TokenConstants.DEVICE_ID_HEADER) String deviceId) {

        try {
            log.info("修改商户基础信息: merchantId={}", request.getMerchantId());

            // 从TokenContext获取当前用户信息进行校验（可选）
            // 这里暂不强制校验，Service层会进行权限和状态校验

            // 调用Service处理信息修改
            MerchantInfoUpdateResponse response = merchantService.updateMerchantInfo(request);

            log.info("商户信息修改成功: merchantId={}, updateTime={}", 
                    request.getMerchantId(), response.getUpdateTime());

            return Result.success("商户信息修改成功", response);
        } catch (Exception e) {
            log.error("商户信息修改失败: merchantId={}, error={}", 
                    request.getMerchantId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 商户二级密码修改
     * PUT /api/v1/merchants/snd-pwd
     *
     * @param request  二级密码修改请求
     * @param deviceId 设备ID
     * @return 二级密码修改响应
     */
    @PutMapping("/snd-pwd")
    public Result<SecondaryPasswordUpdateResponse> updateSecondaryPassword(
            @Valid @RequestBody SecondaryPasswordUpdateRequest request,
            @RequestHeader(TokenConstants.DEVICE_ID_HEADER) String deviceId) {

        try {
            log.info("修改商户二级密码: merchantId={}", request.getMerchantId());

            // 从TokenContext获取当前用户信息进行校验（可选）
            // 这里暂不强制校验，Service层会进行权限和状态校验

            // 调用Service处理二级密码修改
            SecondaryPasswordUpdateResponse response = merchantService.updateSecondaryPassword(request);

            log.info("商户二级密码修改成功: merchantId={}, updateTime={}", 
                    request.getMerchantId(), response.getUpdateTime());

            return Result.success("二级密码修改成功，请重新获取管理令牌", response);
        } catch (Exception e) {
            log.error("商户二级密码修改失败: merchantId={}, error={}", 
                    request.getMerchantId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 商户二级密码重置（通过验证码）
     * POST /api/v1/merchants/snd-pwd/reset
     *
     * @param request  二级密码重置请求
     * @param deviceId 设备ID
     * @return 二级密码重置响应
     */
    @PostMapping("/snd-pwd/reset")
    public Result<SecondaryPasswordResetResponse> resetSecondaryPassword(
            @Valid @RequestBody SecondaryPasswordResetRequest request,
            @RequestHeader(TokenConstants.DEVICE_ID_HEADER) String deviceId) {

        try {
            log.info("收到商户二级密码重置请求: merchantId={}, platform={}", 
                    request.getMerchantId(), request.getPlatform());

            // 调用Service处理二级密码重置
            String resetTime = merchantService.resetSecondaryPassword(
                    request.getMerchantId(),
                    request.getPhone(),
                    request.getVerifyCode(),
                    request.getNewSndPswd(),
                    deviceId
            );

            SecondaryPasswordResetResponse response = SecondaryPasswordResetResponse.builder()
                    .resetTime(resetTime)
                    .build();

            log.info("商户二级密码重置成功: merchantId={}, resetTime={}", 
                    request.getMerchantId(), resetTime);

            return Result.success("二级密码重置成功", response);
        } catch (Exception e) {
            log.error("商户二级密码重置失败: merchantId={}, error={}", 
                    request.getMerchantId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 商户资质审核结果查询
     * GET /api/v1/merchants/qual-audit
     *
     * @param userId 用户ID
     * @return 审核结果响应
     */
    @GetMapping("/qual-audit")
    public Result<QualificationAuditQueryResponse> getQualificationAuditStatus(
            @RequestParam String userId) {

        try {
            log.info("查询商户资质审核结果: userId={}", userId);

            // 从TokenContext获取当前用户ID进行校验
            String tokenUserId = TokenContext.getCurrentUserId();
            if (!userId.equals(tokenUserId)) {
                return Result.fail(403, "用户ID与令牌不匹配");
            }
            
            // 调用Service查询审核结果（Service内部会查询用户类型）
            MerchantAuditRecord record = merchantService.getQualificationAuditStatus(userId);

            // 构建响应
            QualificationAuditQueryResponse.QualificationAuditQueryResponseBuilder builder = 
                    QualificationAuditQueryResponse.builder()
                            .auditId(record.getAuditId())
                            .submitTime(record.getCreateTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            // 根据审核状态设置不同的字段
            Integer auditStatus = record.getAuditStatus();
            if (auditStatus == 0) {
                builder.auditStatus("WAIT");
            } else if (auditStatus == 1) {
                builder.auditStatus("PASSED")
                       .auditTime(record.getAuditTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            } else if (auditStatus == 2) {
                builder.auditStatus("REJECTED")
                       .auditTime(record.getAuditTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                       .rejectReason(record.getRejectReason());
            }

            QualificationAuditQueryResponse response = builder.build();

            log.info("审核结果查询成功: userId={}, auditStatus={}", userId, response.getAuditStatus());

            return Result.success("审核结果查询成功", response);
        } catch (BusinessException e) {
            // 特殊处理404（无审核记录）
            if (e.getCode() == 404) {
                log.info("用户无审核记录: userId={}", userId);
                return Result.fail(404, e.getMessage());
            }
            log.error("审核结果查询失败: userId={}, error={}", userId, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("审核结果查询失败: userId={}, error={}", userId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 商户资质补充提交
     * POST /api/v1/merchants/qualification/submit
     *
     * @param request  资质补充请求
     * @param deviceId 设备ID
     * @return 资质补充响应
     */
    @PostMapping("/qualification/submit")
    public Result<QualificationSubmitResponse> submitQualification(
            @Valid @RequestBody QualificationSubmitRequest request,
            @RequestHeader(TokenConstants.DEVICE_ID_HEADER) String deviceId) {

        try {
            log.info("收到商户资质补充提交请求: merchantId={}", request.getMerchantId());

            // 从TokenContext获取当前用户信息进行校验
            String tokenUserId = TokenContext.getCurrentUserId();
            
            // 调用Service提交资质
            QualificationSubmitResponse response = merchantService.submitQualification(request);

            log.info("商户资质补充成功: merchantId={}, certification={}", 
                    request.getMerchantId(), response.getCertification());

            return Result.success("资质补充成功，已转入审核流程（预计1-2个工作日）", response);
        } catch (Exception e) {
            log.error("商户资质补充失败: merchantId={}, error={}", 
                    request.getMerchantId(), e.getMessage(), e);
            throw e;
        }
    }
}

