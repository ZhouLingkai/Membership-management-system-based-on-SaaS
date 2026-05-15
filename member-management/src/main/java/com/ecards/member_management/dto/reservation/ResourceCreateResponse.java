package com.ecards.member_management.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 接口6：创建资源 - 响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceCreateResponse {
    private Integer successCount;
    private List<CreatedResource> createdResources;
    private String createTime;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatedResource {
        private Long id;
        private String resourceName;
    }
}
