package com.ecards.member_management.service;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.auth.sts.AssumeRoleRequest;
import com.aliyuncs.auth.sts.AssumeRoleResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.ecards.member_management.common.ErrorCode;
import com.ecards.member_management.config.OssProperties;
import com.ecards.member_management.config.StsProperties;
import com.ecards.member_management.dto.response.StsCredentialsResponse;
import com.ecards.member_management.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * STS服务类
 * 用于生成阿里云OSS临时访问凭证
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StsService {

    private final StsProperties stsProperties;
    private final OssProperties ossProperties;

    /**
     * 允许的路径类型白名单
     * merchant - 商户相关（店铺照片、营业执照等）
     * user - 普通用户（头像等）
     * member - 会员相关
     * card - 会员卡相关
     * employee - 员工相关
     * resource - 预约资源相关
     */
    private static final Set<String> ALLOWED_PATH_TYPES = Set.of(
            "merchant", "user", "member", "card", "employee", "resource"
    );

    /**
     * 默认路径类型
     */
    private static final String DEFAULT_PATH_TYPE = "merchant";

    /**
     * 生成OSS临时访问凭证（兼容旧接口，默认使用merchant路径）
     *
     * @param userId 用户ID（用于生成个性化的上传路径）
     * @return STS临时凭证响应
     */
    public StsCredentialsResponse generateCredentials(byte[] userId) {
        return generateCredentials(userId, null);
    }

    /**
     * 生成OSS临时访问凭证（支持自定义路径类型）
     *
     * @param userId 用户ID（用于生成个性化的上传路径）
     * @param pathType 路径类型（merchant/user/member/card/employee/resource），为null时默认merchant
     * @return STS临时凭证响应
     */
    public StsCredentialsResponse generateCredentials(byte[] userId, String pathType) {
        // 校验并设置默认值
        if (pathType == null || pathType.trim().isEmpty()) {
            pathType = DEFAULT_PATH_TYPE;
        }
        pathType = pathType.toLowerCase().trim();

        // 校验路径类型合法性
        if (!ALLOWED_PATH_TYPES.contains(pathType)) {
            log.warn("不支持的路径类型: {}, 允许的类型: {}", pathType, ALLOWED_PATH_TYPES);
            throw new BusinessException(400, "不支持的路径类型: " + pathType + ", 允许: " + ALLOWED_PATH_TYPES);
        }

        try {
            // 1. 优先使用子账号AccessKey（如果配置了）
            String accessKeyId = (stsProperties.getSubAccessKeyId() != null && !stsProperties.getSubAccessKeyId().isEmpty())
                    ? stsProperties.getSubAccessKeyId()
                    : ossProperties.getAccessKeyId();
            
            String accessKeySecret = (stsProperties.getSubAccessKeySecret() != null && !stsProperties.getSubAccessKeySecret().isEmpty())
                    ? stsProperties.getSubAccessKeySecret()
                    : ossProperties.getAccessKeySecret();
            
            log.info("使用AccessKey调用STS: accessKeyId={}, roleArn={}", 
                    accessKeyId.substring(0, Math.min(10, accessKeyId.length())) + "...", 
                    stsProperties.getRoleArn());
            
            // 2. 创建STS客户端
            DefaultProfile profile = DefaultProfile.getProfile(
                    extractRegion(ossProperties.getEndpoint()),  // region，如：cn-hangzhou
                    accessKeyId,
                    accessKeySecret
            );
            IAcsClient client = new DefaultAcsClient(profile);

            // 3. 构建AssumeRole请求
            AssumeRoleRequest request = new AssumeRoleRequest();
            request.setSysMethod(MethodType.POST);
            request.setRoleArn(stsProperties.getRoleArn());
            request.setRoleSessionName(stsProperties.getRoleSessionName());
            request.setDurationSeconds(stsProperties.getDurationSeconds());
            
            // 设置权限策略
            if (stsProperties.getPolicy() != null && !stsProperties.getPolicy().isEmpty()) {
                request.setPolicy(stsProperties.getPolicy());
            }

            // 4. 调用STS服务获取临时凭证
            AssumeRoleResponse response = client.getAcsResponse(request);
            AssumeRoleResponse.Credentials credentials = response.getCredentials();

            // 5. 生成推荐的上传路径前缀（根据pathType动态生成）
            String userIdHex = bytesToHex(userId);
            String pathPrefix = pathType + "/" + userIdHex + "/";

            // 5. 构建响应
            StsCredentialsResponse result = StsCredentialsResponse.builder()
                    .accessKeyId(credentials.getAccessKeyId())
                    .accessKeySecret(credentials.getAccessKeySecret())
                    .securityToken(credentials.getSecurityToken())
                    .expiration(credentials.getExpiration())
                    .region(extractRegion(ossProperties.getEndpoint()))
                    .endpoint(ossProperties.getEndpoint())
                    .bucket(ossProperties.getBucketName())
                    .pathPrefix(pathPrefix)
                    .build();

            log.info("STS临时凭证生成成功: userId={}, pathType={}, pathPrefix={}, expiration={}", 
                    userIdHex, pathType, pathPrefix, credentials.getExpiration());

            return result;

        } catch (ClientException e) {
            log.error("STS临时凭证生成失败: userId={}, error={}", 
                    bytesToHex(userId), e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR.getCode(), 
                    "获取上传凭证失败: " + e.getErrMsg());
        }
    }

    /**
     * 从endpoint中提取region
     * 例如：oss-cn-hangzhou.aliyuncs.com -> cn-hangzhou
     *
     * @param endpoint OSS endpoint
     * @return region
     */
    private String extractRegion(String endpoint) {
        if (endpoint == null || endpoint.isEmpty()) {
            return "cn-hangzhou"; // 默认region
        }
        
        // endpoint格式: oss-cn-hangzhou.aliyuncs.com
        // 提取: cn-hangzhou
        if (endpoint.startsWith("oss-")) {
            int endIndex = endpoint.indexOf(".aliyuncs.com");
            if (endIndex > 0) {
                return endpoint.substring(4, endIndex); // 跳过"oss-"前缀
            }
        }
        
        return "cn-hangzhou"; // 默认值
    }

    /**
     * 将字节数组转换为16进制字符串
     *
     * @param bytes 字节数组
     * @return 16进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

