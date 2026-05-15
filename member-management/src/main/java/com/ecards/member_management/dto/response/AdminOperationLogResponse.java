package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 操作日志查询响应DTO
 * 
 * @author Ecards Team
 * @since 2025-10-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOperationLogResponse {

    /**
     * 日志记录列表
     */
    private List<LogRecord> records;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页码
     */
    private Integer pageNum;

    /**
     * 每页大小
     */
    private Integer pageSize;

    /**
     * 总页数
     */
    private Integer totalPages;

    /**
     * 日志记录详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogRecord {
        /**
         * 日志ID
         */
        private Long logId;

        /**
         * 管理员账号
         */
        private String adminAccount;

        /**
         * 操作类型
         */
        private String operationType;

        /**
         * 操作描述
         */
        private String operationDesc;

        /**
         * 目标类型
         */
        private String targetType;

        /**
         * 操作IP
         */
        private String operationIp;

        /**
         * 设备ID
         */
        private String deviceId;

        /**
         * 结果（1-成功，0-失败）
         */
        private Integer result;

        /**
         * 操作时间
         */
        private String createTime;
    }
}

