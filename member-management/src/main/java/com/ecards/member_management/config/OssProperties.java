package com.ecards.member_management.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * 阿里云OSS配置属性
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssProperties {

    /**
     * OSS访问域名（不带https://）
     * 例如：oss-cn-hangzhou.aliyuncs.com
     */
    @NotBlank(message = "OSS endpoint不能为空")
    private String endpoint;

    /**
     * Bucket名称
     */
    @NotBlank(message = "OSS bucket-name不能为空")
    private String bucketName;

    /**
     * AccessKey ID
     */
    @NotBlank(message = "OSS access-key-id不能为空")
    private String accessKeyId;

    /**
     * AccessKey Secret
     */
    @NotBlank(message = "OSS access-key-secret不能为空")
    private String accessKeySecret;

    /**
     * URL前缀（用于拼接完整的文件访问URL）
     * 例如：https://ecards-test1.oss-cn-hangzhou.aliyuncs.com
     */
    @NotBlank(message = "OSS url-prefix不能为空")
    private String urlPrefix;
}

