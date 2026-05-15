package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * STS临时凭证响应DTO
 * 包含前端直传OSS所需的所有凭证信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StsCredentialsResponse {

    /**
     * 临时访问密钥ID
     */
    private String accessKeyId;

    /**
     * 临时访问密钥Secret
     */
    private String accessKeySecret;

    /**
     * 安全令牌
     */
    private String securityToken;

    /**
     * 凭证过期时间（ISO 8601格式）
     */
    private String expiration;

    /**
     * OSS Region（如：cn-hangzhou）
     */
    private String region;

    /**
     * OSS Endpoint（如：oss-cn-hangzhou.aliyuncs.com）
     */
    private String endpoint;

    /**
     * OSS Bucket名称
     */
    private String bucket;

    /**
     * 推荐的上传路径前缀（如：merchant/userId/）
     */
    private String pathPrefix;
}

