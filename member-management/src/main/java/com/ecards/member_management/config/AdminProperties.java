package com.ecards.member_management.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 管理员系统配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "admin")
public class AdminProperties {

    /**
     * 初始化配置
     */
    private Init init = new Init();

    /**
     * JWT配置
     */
    private Jwt jwt = new Jwt();

    /**
     * 日志配置
     */
    private Log log = new Log();

    /**
     * 初始化配置
     */
    @Data
    public static class Init {
        /**
         * 初始超管手机号
         */
        private String phone;

        /**
         * 初始超管账号
         */
        private String account;

        /**
         * 初始超管密码
         */
        private String password;

        /**
         * 初始超管二级密码
         */
        private String sndPassword;
    }

    /**
     * JWT配置
     */
    @Data
    public static class Jwt {
        /**
         * JWT密钥
         */
        private String secret;

        /**
         * 过期时间（毫秒）
         */
        private Long expiration;
    }

    /**
     * 日志配置
     */
    @Data
    public static class Log {
        /**
         * 日志保留天数
         */
        private Retention retention = new Retention();

        @Data
        public static class Retention {
            /**
             * 保留天数
             */
            private Integer days = 90;
        }
    }
}

