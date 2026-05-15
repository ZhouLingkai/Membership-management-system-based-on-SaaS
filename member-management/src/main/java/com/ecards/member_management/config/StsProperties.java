package com.ecards.member_management.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云 STS 配置属性
 * 用于生成临时访问凭证
 */
@Data
@Component
@ConfigurationProperties(prefix = "aliyun.sts")
public class StsProperties {

    /**
     * RAM角色ARN
     * 格式：acs:ram::账号ID:role/角色名称
     */
    private String roleArn;

    /**
     * 角色会话名称
     * 用于标识此次会话的用途
     */
    private String roleSessionName;

    /**
     * 临时凭证有效期（单位：秒）
     * 建议值：900-3600（15分钟到1小时）
     */
    private Long durationSeconds;

    /**
     * STS访问控制策略
     * JSON格式的策略文档，用于限制临时凭证的权限
     */
    private String policy;

    /**
     * 子账号AccessKey ID（用于调用AssumeRole，避免主账号问题）
     */
    private String subAccessKeyId;

    /**
     * 子账号AccessKey Secret
     */
    private String subAccessKeySecret;
}

