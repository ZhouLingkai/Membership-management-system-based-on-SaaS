package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberCardDetailResponse {
    private String memberCardId;
    private Long cardTypeId;
    private String cardTypeName;
    private String cardBgc;
    private String cardMask;
    private Integer cardTtype;
    private String cardTtypeName;
    private String description;
    private String storeId;
    private String storeName;
    private String merchantId;
    private String merchantName;
    private String userId;
    private String memberName;
    private String memberPhone;
    private BigDecimal balance;
    private Integer times;
    private Integer points;
    private Integer status;
    private String statusName;
    private String openCardTime;
    private String activateTime;
    private String expireTime;
    private Integer autoNotify;
    private Integer crossStore;
}

