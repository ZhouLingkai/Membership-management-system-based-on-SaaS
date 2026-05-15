package com.ecards.member_management.service;

import com.ecards.member_management.common.ErrorCode;
import com.ecards.member_management.dto.request.CreateCardTypeRequest;
import com.ecards.member_management.dto.request.DetailCardTypeRequest;
import com.ecards.member_management.dto.request.ListCardTypeRequest;
import com.ecards.member_management.dto.request.UpdateCardTypeRequest;
import com.ecards.member_management.dto.response.*;
import com.ecards.member_management.entity.MemberCardType;
import com.ecards.member_management.entity.MerchantExtend;
import com.ecards.member_management.entity.Store;
import com.ecards.member_management.enums.CardTtypeEnum;
import com.ecards.member_management.exception.BusinessException;
import com.ecards.member_management.repository.MemberCardTypeRepository;
import com.ecards.member_management.repository.MerchantExtendRepository;
import com.ecards.member_management.repository.StoreRepository;
import com.ecards.member_management.utils.EncryptUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 会员卡种服务
 * 提供会员卡种创建、查询、修改等功能
 * 
 * @author Ecards Team
 * @since 2025-11-02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberCardTypeService {

    private final MemberCardTypeRepository memberCardTypeRepository;
    private final StoreRepository storeRepository;
    private final MerchantExtendRepository merchantExtendRepository;
    private final EncryptUtils encryptUtils;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String EMPTY_JSON_ARRAY = "[]";

    /**
     * 创建会员卡种
     *
     * @param request 创建请求
     * @param merchantId 商家ID（从令牌提取）
     * @return 创建响应
     */
    @Transactional
    public CreateCardTypeResponse createCardType(CreateCardTypeRequest request, String merchantId) {
        log.info("创建会员卡种：商家ID={}, 店铺ID={}, 卡种名称={}", merchantId, request.getStoreId(), request.getCardTypeName());

        // 1. 查询店铺信息
        byte[] storeIdBytes = encryptUtils.uuidToBytes(request.getStoreId());
        Store store = storeRepository.findById(storeIdBytes)
                .orElseThrow(() -> new BusinessException(40002, "店铺不存在"));

        // 2. 验证店铺归属
        String storeMerchantId = encryptUtils.bytesToUuid(store.getMerchantId());
        if (!storeMerchantId.equals(merchantId)) {
            throw new BusinessException(40001, "无权在该店铺创建卡种");
        }

        // 3. 查询商家信息并验证认证状态
        byte[] merchantIdBytes = encryptUtils.uuidToBytes(merchantId);
        MerchantExtend merchant = merchantExtendRepository.findById(merchantIdBytes)
                .orElseThrow(() -> new BusinessException(30002, "商家信息不存在"));

        Integer certification = merchant.getCertification();
        if (certification != 1 && certification != 2) {
            throw new BusinessException(40001, "只有已认证或测试中的商家才能创建卡种");
        }

        // 4. 验证商家卡种数量限制（普通商家最多3个）
        Integer merchantLevel = merchant.getMerchantLevel();
        if (merchantLevel == 1) { // 普通商家
            long cardTypeCount = memberCardTypeRepository.countByMerchantId(merchantIdBytes);
            if (cardTypeCount >= 3) {
                throw new BusinessException(60006, "普通商家最多创建3个卡种，当前已有" + cardTypeCount + "个，请升级VIP");
            }
        }

        // 5. 验证卡种名称唯一性
        Optional<MemberCardType> existing = memberCardTypeRepository
                .findByStoreIdAndCardTypeName(storeIdBytes, request.getCardTypeName());
        if (existing.isPresent()) {
            throw new BusinessException(60001, "该店铺下已存在同名卡种");
        }

        // 6. 验证卡种类型有效性
        if (!CardTtypeEnum.isValid(request.getCardTtype())) {
            throw new BusinessException(90001, "卡种类型必须为1-余额卡、2-次数卡、3-时效卡、4-积分卡");
        }

        // 7. 处理预设项目（积分卡强制为空数组）
        String presetRecharge = request.getPresetRecharge();
        String presetCost = request.getPresetCost();
        
        if (request.getCardTtype() == 4) { // 积分卡
            presetRecharge = EMPTY_JSON_ARRAY;
            presetCost = EMPTY_JSON_ARRAY;
        } else {
            // 非积分卡需要验证JSON格式
            validateJsonArray(presetRecharge, "预设充值项目");
            validateJsonArray(presetCost, "预设消费项目");
        }

        // 8. 创建卡种实体
        LocalDateTime now = LocalDateTime.now();
        MemberCardType cardType = new MemberCardType();
        cardType.setStoreId(storeIdBytes);
        cardType.setMerchantId(merchantIdBytes);
        cardType.setCardTypeName(request.getCardTypeName());
        cardType.setDescription(request.getDescription());
        cardType.setCardMask(request.getCardMask());
        cardType.setCardBgc(request.getCardBgc());
        cardType.setCardTtype(request.getCardTtype());
        cardType.setPresetRecharge(presetRecharge);
        cardType.setPresetCost(presetCost);
        cardType.setAutoNotify(request.getAutoNotify());
        cardType.setCrossStore(request.getCrossStore());
        cardType.setCreateTime(now);

        MemberCardType saved = memberCardTypeRepository.save(cardType);

        log.info("会员卡种创建成功：cardTypeId={}, storeId={}, merchantId={}", 
                saved.getCardTypeId(), request.getStoreId(), merchantId);

        // 9. 构建响应
        return CreateCardTypeResponse.builder()
                .cardTypeId(saved.getCardTypeId())
                .storeId(request.getStoreId())
                .merchantId(merchantId)
                .cardTypeName(saved.getCardTypeName())
                .cardTtype(saved.getCardTtype())
                .createTime(now.format(DATETIME_FORMATTER))
                .build();
    }

    /**
     * 查询会员卡种列表
     *
     * @param request 列表查询请求
     * @return 列表响应
     */
    public CardTypeListResponse listCardTypes(ListCardTypeRequest request) {
        log.info("查询会员卡种列表：店铺ID={}, 页码={}, 每页条数={}", 
                request.getStoreId(), request.getPageNum(), request.getPageSize());

        // 1. 验证店铺存在
        byte[] storeIdBytes = encryptUtils.uuidToBytes(request.getStoreId());
        if (!storeRepository.existsById(storeIdBytes)) {
            throw new BusinessException(40002, "店铺不存在");
        }

        // 2. 构建分页参数（按card_type_id降序）
        Pageable pageable = PageRequest.of(
                request.getPageNum() - 1, 
                request.getPageSize(),
                Sort.by(Sort.Direction.DESC, "cardTypeId")
        );

        // 3. 执行多条件查询
        Page<MemberCardType> page = memberCardTypeRepository.findByConditions(
                storeIdBytes,
                request.getCardTtype(),
                request.getCrossStore(),
                request.getCardTypeName(),
                pageable
        );

        // 4. 转换为响应DTO
        List<CardTypeItemResponse> items = page.getContent().stream()
                .map(this::convertToItemResponse)
                .collect(Collectors.toList());

        log.info("会员卡种列表查询成功：店铺ID={}, 总数量={}, 当前页数量={}", 
                request.getStoreId(), page.getTotalElements(), items.size());

        return CardTypeListResponse.builder()
                .total(page.getTotalElements())
                .pageNum(request.getPageNum())
                .pageSize(request.getPageSize())
                .list(items)
                .build();
    }

    /**
     * 查询会员卡种详情
     *
     * @param request 详情查询请求
     * @return 详情响应
     */
    public CardTypeDetailResponse getCardTypeDetail(DetailCardTypeRequest request) {
        log.info("查询会员卡种详情：卡种ID={}, 店铺ID={}", request.getCardTypeId(), request.getStoreId());

        // 1. 验证店铺存在
        byte[] storeIdBytes = encryptUtils.uuidToBytes(request.getStoreId());
        if (!storeRepository.existsById(storeIdBytes)) {
            throw new BusinessException(40002, "店铺不存在");
        }

        // 2. 查询卡种（双重校验：卡种ID + 店铺ID）
        MemberCardType cardType = memberCardTypeRepository
                .findByCardTypeIdAndStoreId(request.getCardTypeId(), storeIdBytes)
                .orElseThrow(() -> new BusinessException(60003, "卡种与店铺不匹配或卡种不存在"));

        // 3. 查询店铺名称
        Store store = storeRepository.findById(storeIdBytes).orElseThrow();

        // 4. 转换为响应DTO
        CardTypeDetailResponse response = CardTypeDetailResponse.builder()
                .cardTypeId(cardType.getCardTypeId())
                .storeId(request.getStoreId())
                .merchantId(encryptUtils.bytesToUuid(cardType.getMerchantId()))
                .storeName(store.getStoreName())
                .cardTypeName(cardType.getCardTypeName())
                .cardTtype(cardType.getCardTtype())
                .cardTtypeName(CardTtypeEnum.getNameByCode(cardType.getCardTtype()))
                .description(cardType.getDescription())
                .presetRecharge(cardType.getPresetRecharge())
                .presetCost(cardType.getPresetCost())
                .autoNotify(cardType.getAutoNotify())
                .autoNotifyName(getAutoNotifyName(cardType.getAutoNotify()))
                .crossStore(cardType.getCrossStore())
                .crossStoreName(getCrossStoreName(cardType.getCrossStore()))
                .cardMask(cardType.getCardMask())
                .cardBgc(cardType.getCardBgc())
                .build();

        log.info("会员卡种详情查询成功：卡种ID={}, 卡种名称={}", request.getCardTypeId(), cardType.getCardTypeName());

        return response;
    }

    /**
     * 修改会员卡种设置
     *
     * @param request 修改请求
     * @param merchantId 商家ID（从令牌提取，商家操作时使用）
     * @param storeIdFromToken 店铺ID（从工作令牌提取，店长操作时使用）
     * @return 修改响应
     */
    @Transactional
    public UpdateCardTypeResponse updateCardType(UpdateCardTypeRequest request, 
                                                  String merchantId, String storeIdFromToken) {
        log.info("修改会员卡种：卡种ID={}, 店铺ID={}", request.getCardTypeId(), request.getStoreId());

        // 1. 验证店铺存在
        byte[] storeIdBytes = encryptUtils.uuidToBytes(request.getStoreId());
        Store store = storeRepository.findById(storeIdBytes)
                .orElseThrow(() -> new BusinessException(40002, "店铺不存在"));

        // 2. 验证权限（商家或店长）
        if (merchantId != null) {
            // 商家使用普通令牌
            String storeMerchantId = encryptUtils.bytesToUuid(store.getMerchantId());
            if (!storeMerchantId.equals(merchantId)) {
                throw new BusinessException(40001, "无权修改该店铺的卡种");
            }
        } else if (storeIdFromToken != null) {
            // 店长使用工作令牌
            if (!storeIdFromToken.equals(request.getStoreId())) {
                throw new BusinessException(60003, "只能修改本店铺的卡种");
            }
        } else {
            throw new BusinessException(10004, "令牌权限不足");
        }

        // 3. 验证商家认证状态
        byte[] merchantIdBytes = store.getMerchantId();
        MerchantExtend merchant = merchantExtendRepository.findById(merchantIdBytes)
                .orElseThrow(() -> new BusinessException(30002, "商家信息不存在"));

        Integer certification = merchant.getCertification();
        if (certification != 1 && certification != 2) {
            throw new BusinessException(40001, "只有已认证或测试中的商家才能修改卡种");
        }

        // 4. 查询卡种
        MemberCardType cardType = memberCardTypeRepository
                .findByCardTypeIdAndStoreId(request.getCardTypeId(), storeIdBytes)
                .orElseThrow(() -> new BusinessException(60003, "卡种与店铺不匹配或卡种不存在"));

        // 5. 记录已更新的字段
        List<String> updatedFields = new ArrayList<>();

        // 6. 更新卡种名称（如果提供且不同）
        if (request.getCardTypeName() != null && !request.getCardTypeName().equals(cardType.getCardTypeName())) {
            // 验证新名称唯一性
            Optional<MemberCardType> existing = memberCardTypeRepository
                    .findByStoreIdAndCardTypeNameAndCardTypeIdNot(
                            storeIdBytes, request.getCardTypeName(), request.getCardTypeId());
            if (existing.isPresent()) {
                throw new BusinessException(60001, "该店铺下已存在同名卡种");
            }
            cardType.setCardTypeName(request.getCardTypeName());
            updatedFields.add("cardTypeName");
        }

        // 7. 更新卡种描述（如果提供）
        if (request.getDescription() != null) {
            cardType.setDescription(request.getDescription());
            updatedFields.add("description");
        }

        // 8. 更新预设充值项目（积分卡不允许修改）
        if (request.getPresetRecharge() != null) {
            if (cardType.getCardTtype() == 4) {
                throw new BusinessException(90001, "积分卡不支持修改预设充值项目");
            }
            validateJsonArray(request.getPresetRecharge(), "预设充值项目");
            cardType.setPresetRecharge(request.getPresetRecharge());
            updatedFields.add("presetRecharge");
        }

        // 9. 更新预设消费项目（积分卡不允许修改）
        if (request.getPresetCost() != null) {
            if (cardType.getCardTtype() == 4) {
                throw new BusinessException(90001, "积分卡不支持修改预设消费项目");
            }
            validateJsonArray(request.getPresetCost(), "预设消费项目");
            cardType.setPresetCost(request.getPresetCost());
            updatedFields.add("presetCost");
        }

        // 10. 更新自动消息通知类型（如果提供）
        if (request.getAutoNotify() != null) {
            cardType.setAutoNotify(request.getAutoNotify());
            updatedFields.add("autoNotify");
        }

        // 11. 更新跨店通用设置（如果提供）
        if (request.getCrossStore() != null) {
            cardType.setCrossStore(request.getCrossStore());
            updatedFields.add("crossStore");
        }

        // 12. 更新卡面蒙版图片（如果提供）
        if (request.getCardMask() != null) {
            cardType.setCardMask(request.getCardMask());
            updatedFields.add("cardMask");
        }

        // 13. 更新卡种背景图URL（如果提供）
        if (request.getCardBgc() != null) {
            cardType.setCardBgc(request.getCardBgc());
            updatedFields.add("cardBgc");
        }

        // 14. 保存修改
        if (!updatedFields.isEmpty()) {
            memberCardTypeRepository.save(cardType);
            log.info("会员卡种修改成功：卡种ID={}, 更新字段={}", request.getCardTypeId(), updatedFields);
        } else {
            log.info("会员卡种无修改：卡种ID={}", request.getCardTypeId());
        }

        LocalDateTime now = LocalDateTime.now();
        return UpdateCardTypeResponse.builder()
                .cardTypeId(request.getCardTypeId())
                .updateTime(now.format(DATETIME_FORMATTER))
                .updatedFields(updatedFields)
                .build();
    }

    /**
     * 转换为列表项响应DTO
     */
    private CardTypeItemResponse convertToItemResponse(MemberCardType cardType) {
        return CardTypeItemResponse.builder()
                .cardTypeId(cardType.getCardTypeId())
                .cardTypeName(cardType.getCardTypeName())
                .cardTtype(cardType.getCardTtype())
                .cardTtypeName(CardTtypeEnum.getNameByCode(cardType.getCardTtype()))
                .description(cardType.getDescription())
                .autoNotify(cardType.getAutoNotify())
                .crossStore(cardType.getCrossStore())
                .cardMask(cardType.getCardMask())
                .cardBgc(cardType.getCardBgc())
                .presetRechargeCount(countJsonArrayItems(cardType.getPresetRecharge()))
                .presetCostCount(countJsonArrayItems(cardType.getPresetCost()))
                .build();
    }

    /**
     * 验证JSON数组格式
     * 允许空数组 "[]"
     */
    private void validateJsonArray(String jsonStr, String fieldName) {
        try {
            JSONArray array = new JSONArray(jsonStr);

            // 允许空数组，直接返回
            if (array.length() == 0) {
                return;
            }

            // 验证每个元素包含必需字段
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);

                if (!item.has("itemName") || !item.has("amount")) {
                    throw new BusinessException(90001,
                            fieldName + "格式错误：每个项目必须包含itemName和amount");
                }

                // 验证金额为正数
                double amount = item.getDouble("amount");
                if (amount <= 0) {
                    throw new BusinessException(90001,
                            fieldName + "格式错误：amount必须为正数");
                }
            }
        } catch (JSONException e) {
            throw new BusinessException(90001, fieldName + "格式错误：" + e.getMessage());
        }
    }

    /**
     * 统计JSON数组项目数量
     */
    private Integer countJsonArrayItems(String jsonStr) {
        try {
            JSONArray array = new JSONArray(jsonStr);
            return array.length();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 获取自动消息通知类型名称
     */
    private String getAutoNotifyName(Integer autoNotify) {
        return switch (autoNotify) {
            case 0 -> "关闭";
            case 1 -> "短信通知";
            case 2 -> "订阅通知";
            case 3 -> "程序内推送";
            default -> "未知";
        };
    }

    /**
     * 获取跨店通用设置名称
     */
    private String getCrossStoreName(Integer crossStore) {
        return crossStore == 0 ? "仅本店铺" : "同商家跨店通用";
    }
}

