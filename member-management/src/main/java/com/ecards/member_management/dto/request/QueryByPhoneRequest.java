package com.ecards.member_management.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class QueryByPhoneRequest {
    @NotBlank(message = "店铺ID不能为空")
    private String storeId;
    
    @NotBlank(message = "手机号不能为空")
    private String memberPhone;
    
    @NotBlank(message = "查询范围不能为空")
    private String cardScope; // local 或 cross_store
}

