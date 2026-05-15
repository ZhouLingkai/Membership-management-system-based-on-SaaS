package com.ecards.member_management.service.impl;

import com.ecards.member_management.common.ErrorCode;
import com.ecards.member_management.dto.request.*;
import com.ecards.member_management.dto.response.*;
import com.ecards.member_management.entity.*;
import com.ecards.member_management.enums.CardTtypeEnum;
import com.ecards.member_management.enums.TokenType;
import com.ecards.member_management.exception.BusinessException;
import com.ecards.member_management.repository.*;
import com.ecards.member_management.service.MemberCardService;
import com.ecards.member_management.service.TokenRedisService;
import com.ecards.member_management.utils.EncryptUtils;
import com.ecards.member_management.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 会员卡服务实现类
 * 
 * @author Ecards Team
 * @since 2025-11-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberCardServiceImpl implements MemberCardService {

    private final MemberCardRepository memberCardRepository;
    private final MemberCardTypeRepository memberCardTypeRepository;
    private final StoreRepository storeRepository;
    private final MerchantExtendRepository merchantExtendRepository;
    private final UserRepository userRepository;
    private final WorkRelationRepository workRelationRepository;
    private final RegistrationCardRecordRepository registrationCardRecordRepository;
    private final McardStatusLogRepository mcardStatusLogRepository;
    private final TransactionRecordRepository transactionRecordRepository;
    private final JwtUtils jwtUtils;
    private final EncryptUtils encryptUtils;
    private final TokenRedisService tokenRedisService;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final long DEFAULT_EXPIRE_YEARS = 60; // 默认60年过期

    /**
     * 接口1：会员卡办理（手机号快速办理）
     */
    @Override
    @Transactional
    public CreateMemberCardResponse createByPhone(CreateMemberCardByPhoneRequest request, String token) {
        log.info("手机号办卡请求：storeId={}, cardTypeId={}", request.getStoreId(), request.getCardTypeId());

        // 1. 解析并验证令牌（工作令牌或普通令牌）
        Integer tokenType = jwtUtils.extractTokenType(token);

        // 2. 验证令牌类型（必须是工作令牌或普通令牌）
        if (tokenType != TokenType.WORK.getCode() && tokenType != TokenType.NORMAL.getCode()) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID.getCode(), "接口1需要工作令牌或普通令牌");
        }

        // 3. 如果是工作令牌，且角色是店员，需要验证member_card_create权限
        if (tokenType == TokenType.WORK.getCode()) {
            String userId = jwtUtils.extractUserId(token);
            String role = jwtUtils.extractRole(token);
            String storeId = jwtUtils.extractStoreId(token);

            // 只有店员需要校验权限，店长和商家拥有最高权限无需校验
            if ("STAFF".equals(role)) {
                validateStaffPermission(userId, storeId, "member_card_create");
            }
        }

        // 4. 转换店铺ID和卡种ID
        byte[] storeIdBytes = encryptUtils.uuidToBytes(request.getStoreId());

        // 4. 查询卡种信息
        MemberCardType cardType = memberCardTypeRepository.findById(request.getCardTypeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_TYPE_NOT_EXIST));

        // 5. 验证卡种归属店铺
        String cardTypeStoreId = encryptUtils.bytesToUuid(cardType.getStoreId());
        if (!cardTypeStoreId.equals(request.getStoreId())) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED.getCode(), "该卡种不属于指定店铺");
        }

        // 6. 查询店铺信息
        Store store = storeRepository.findById(storeIdBytes)
                .orElseThrow(() -> new BusinessException(40002, "店铺不存在"));

        // 7. 查询商家信息
        byte[] merchantIdBytes = store.getMerchantId();
        MerchantExtend merchant = merchantExtendRepository.findById(merchantIdBytes)
                .orElseThrow(() -> new BusinessException(30002, "商家信息不存在"));

        // 8. 验证商家认证状态
        Integer certification = merchant.getCertification();
        if (certification != 1 && certification != 2) {
            throw new BusinessException(40001, "只有已认证或测试中的商家才能办理会员卡");
        }

        // 9. 验证会员数量限制（普通商家最多200个会员卡，不去重）
        Integer merchantLevel = merchant.getMerchantLevel();
        if (merchantLevel == 1) { // 普通商家
            long memberCardCount = memberCardRepository.countByMerchantId(merchantIdBytes);
            if (memberCardCount >= 200) {
                throw new BusinessException(ErrorCode.MEMBER_CARD_LIMIT_EXCEEDED);
            }
        }

        // 10. 强制解密手机号
        String memberPhone;
        try {
            memberPhone = encryptUtils.decryptAES(request.getMemberPhone());
        } catch (Exception e) {
            log.error("手机号解密失败", e);
            throw new BusinessException(40001, "手机号解密失败");
        }

        // 11. 验证手机号格式
        if (!memberPhone.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException(40001, "手机号格式不正确");
        }

        // 12. 检查重复办卡（同一卡种+同一手机号）
        if (memberCardRepository.existsByCardTypeIdAndMemberPhone(request.getCardTypeId(), memberPhone)) {
            throw new BusinessException(ErrorCode.MEMBER_CARD_PHONE_DUPLICATE);
        }

        // 13. 根据手机号查询用户是否注册
        User user = userRepository.findByPhone(memberPhone).orElse(null);
        byte[] userIdBytes = null;
        int cardStatus;
        if (user != null) {
            userIdBytes = user.getUserId();
            cardStatus = 1; // 已注册，状态设为正常
            
            // 检查该用户是否已办理该卡种
            if (memberCardRepository.existsByCardTypeIdAndUserId(request.getCardTypeId(), userIdBytes)) {
                throw new BusinessException(ErrorCode.MEMBER_CARD_USER_DUPLICATE);
            }
        } else {
            cardStatus = 0; // 未注册，状态设为未激活
        }

        // 14. 创建会员卡
        MemberCard memberCard = new MemberCard();
        byte[] memberCardId = encryptUtils.uuidToBytes(UUID.randomUUID().toString());
        memberCard.setMemberCardId(memberCardId);
        memberCard.setCardTypeId(request.getCardTypeId());
        memberCard.setStoreId(storeIdBytes);
        memberCard.setMerchantId(merchantIdBytes);
        memberCard.setUserId(userIdBytes);
        memberCard.setMemberPhone(memberPhone);
        memberCard.setMemberName(request.getMemberName());
        memberCard.setCardTtype(cardType.getCardTtype()); // 冗余字段

        // 15. 设置初始余额、次数、积分
        memberCard.setBalance(request.getInitialBalance() != null ? request.getInitialBalance() : BigDecimal.ZERO);
        memberCard.setTimes(request.getInitialTimes() != null ? request.getInitialTimes() : 0);
        memberCard.setPoints(request.getInitialPoints() != null ? request.getInitialPoints() : 0);

        // 16. 设置状态
        memberCard.setStatus(cardStatus);

        // 17. 设置时间
        LocalDateTime now = LocalDateTime.now();
        memberCard.setOpenCardTime(now);
        if (cardStatus == 1) {
            memberCard.setActivateTime(now); // 已注册用户，立即激活
        }

        // 18. 设置到期时间（默认60年后）
        LocalDateTime expireTime;
        if (request.getExpireTime() != null && !request.getExpireTime().isEmpty()) {
            try {
                expireTime = LocalDateTime.parse(request.getExpireTime(), DATETIME_FORMATTER);
            } catch (Exception e) {
                expireTime = now.plusYears(DEFAULT_EXPIRE_YEARS);
            }
        } else {
            expireTime = now.plusYears(DEFAULT_EXPIRE_YEARS);
        }
        memberCard.setExpireTime(expireTime);

        // 19. 保存会员卡
        memberCardRepository.save(memberCard);

        // 19.5 创建交易记录（如果初始充值不为0）
        String operatorUserId = jwtUtils.extractUserId(token);
        byte[] operatorIdBytes = encryptUtils.uuidToBytes(operatorUserId);
        createInitialTransactionRecord(memberCard, cardType, operatorIdBytes, storeIdBytes, now);

        // 20. 记录办卡信息
        String role = jwtUtils.extractRole(token);
        saveRegistrationRecord(
            memberCardId,
            operatorUserId,
            1,  // registration_channel = 1（先办后激活）
            role,
            storeIdBytes,
            now
        );

        log.info("会员卡办理成功：memberCardId={}, cardTypeId={}, memberPhone={}, status={}",
                encryptUtils.bytesToUuid(memberCardId), request.getCardTypeId(), memberPhone, cardStatus);

        // 21. 构建响应
        CreateMemberCardResponse response = CreateMemberCardResponse.builder()
                .memberCardId(encryptUtils.bytesToUuid(memberCardId))
                .cardTypeId(cardType.getCardTypeId())
                .cardTypeName(cardType.getCardTypeName())
                .cardTtype(cardType.getCardTtype())
                .cardTtypeName(CardTtypeEnum.getNameByCode(cardType.getCardTtype()))
                .memberPhone(memberPhone)
                .memberName(request.getMemberName())
                .balance(memberCard.getBalance())
                .times(memberCard.getTimes())
                .points(memberCard.getPoints())
                .status(cardStatus)
                .statusName(getStatusName(cardStatus))
                .openCardTime(now.format(DATETIME_FORMATTER))
                .expireTime(expireTime.format(DATETIME_FORMATTER))
                .message(cardStatus == 0 ? "该手机号尚未注册，会员卡将处于未激活状态，用户注册后可激活" : "办卡成功")
                .build();

        return response;
    }

    /**
     * 接口2：会员卡办理（线下扫码办理）
     */
    @Override
    @Transactional
    public CreateMemberCardResponse createByScan(CreateMemberCardByScanRequest request, String token) {
        log.info("扫码办卡请求：storeId={}, cardTypeId={}, privilegeToken={}", 
                request.getStoreId(), request.getCardTypeId(), request.getPrivilegeToken());

        // 1. 解析并验证工作令牌
        Integer tokenType = jwtUtils.extractTokenType(token);

        // 2. 验证令牌类型（必须是工作令牌或普通令牌）
        if (tokenType != TokenType.WORK.getCode() && tokenType != TokenType.NORMAL.getCode()) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID.getCode(), "接口2需要工作令牌或普通令牌");
        }

        // 3. 如果是工作令牌，且角色是店员，需要验证member_card_create权限
        if (tokenType == TokenType.WORK.getCode()) {
            String userId = jwtUtils.extractUserId(token);
            String role = jwtUtils.extractRole(token);
            String storeId = jwtUtils.extractStoreId(token);

            // 只有店员需要校验权限，店长和商家拥有最高权限无需校验
            if ("STAFF".equals(role)) {
                validateStaffPermission(userId, storeId, "member_card_create");
            }
        }

        // 4. 解析并验证特权令牌
        Claims privilegeClaims;
        try {
            privilegeClaims = jwtUtils.parseToken(request.getPrivilegeToken());
        } catch (Exception e) {
            log.error("特权令牌解析失败", e);
            throw new BusinessException(ErrorCode.PRIVILEGE_TOKEN_INVALID);
        }

        // 4. 提取特权令牌信息
        String jti = privilegeClaims.getId();
        String userId = privilegeClaims.get("user_id", String.class);
        Boolean singleUse = privilegeClaims.get("single_use", Boolean.class);
        @SuppressWarnings("unchecked")
        List<String> permissions = (List<String>) privilegeClaims.get("permissions", List.class);

        // 5. 检查特权令牌是否在黑名单（已使用）
        if (tokenRedisService.isInBlacklist(jti)) {
            throw new BusinessException(ErrorCode.PRIVILEGE_TOKEN_USED);
        }

        // 6. 验证特权令牌类型和权限
        if (!Boolean.TRUE.equals(singleUse)) {
            throw new BusinessException(ErrorCode.PRIVILEGE_TOKEN_PERMISSION_DENIED);
        }
        if (permissions == null || !permissions.contains("CARD_CREATE")) {
            throw new BusinessException(ErrorCode.PRIVILEGE_TOKEN_PERMISSION_DENIED);
        }

        // 7. 验证特权令牌未过期
        if (privilegeClaims.getExpiration().before(new java.util.Date())) {
            throw new BusinessException(ErrorCode.PRIVILEGE_TOKEN_INVALID);
        }

        // 8. 转换店铺ID和用户ID
        byte[] storeIdBytes = encryptUtils.uuidToBytes(request.getStoreId());
        byte[] userIdBytes = encryptUtils.uuidToBytes(userId);

        // 9. 查询用户信息
        User user = userRepository.findById(userIdBytes)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_EXIST));

        // 10. 查询卡种信息
        MemberCardType cardType = memberCardTypeRepository.findById(request.getCardTypeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_TYPE_NOT_EXIST));

        // 11. 验证卡种归属店铺
        String cardTypeStoreId = encryptUtils.bytesToUuid(cardType.getStoreId());
        if (!cardTypeStoreId.equals(request.getStoreId())) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED.getCode(), "该卡种不属于指定店铺");
        }

        // 12. 查询店铺信息
        Store store = storeRepository.findById(storeIdBytes)
                .orElseThrow(() -> new BusinessException(40002, "店铺不存在"));

        // 13. 查询商家信息
        byte[] merchantIdBytes = store.getMerchantId();
        MerchantExtend merchant = merchantExtendRepository.findById(merchantIdBytes)
                .orElseThrow(() -> new BusinessException(30002, "商家信息不存在"));

        // 14. 验证商家认证状态
        Integer certification = merchant.getCertification();
        if (certification != 1 && certification != 2) {
            throw new BusinessException(40001, "只有已认证或测试中的商家才能办理会员卡");
        }

        // 15. 验证会员数量限制（普通商家最多200个会员卡）
        Integer merchantLevel = merchant.getMerchantLevel();
        if (merchantLevel == 1) { // 普通商家
            long memberCardCount = memberCardRepository.countByMerchantId(merchantIdBytes);
            if (memberCardCount >= 200) {
                throw new BusinessException(ErrorCode.MEMBER_CARD_LIMIT_EXCEEDED);
            }
        }

        // 16. 检查重复办卡（同一卡种+同一用户）
        if (memberCardRepository.existsByCardTypeIdAndUserId(request.getCardTypeId(), userIdBytes)) {
            throw new BusinessException(ErrorCode.MEMBER_CARD_USER_DUPLICATE);
        }

        // 17. 创建会员卡
        MemberCard memberCard = new MemberCard();
        byte[] memberCardId = encryptUtils.uuidToBytes(UUID.randomUUID().toString());
        memberCard.setMemberCardId(memberCardId);
        memberCard.setCardTypeId(request.getCardTypeId());
        memberCard.setStoreId(storeIdBytes);
        memberCard.setMerchantId(merchantIdBytes);
        memberCard.setUserId(userIdBytes);
        memberCard.setMemberPhone(user.getPhone());
        memberCard.setMemberName(request.getMemberName() != null ? request.getMemberName() : user.getNickname());
        memberCard.setCardTtype(cardType.getCardTtype()); // 冗余字段

        // 18. 设置初始余额、次数、积分
        memberCard.setBalance(request.getInitialBalance() != null ? request.getInitialBalance() : BigDecimal.ZERO);
        memberCard.setTimes(request.getInitialTimes() != null ? request.getInitialTimes() : 0);
        memberCard.setPoints(request.getInitialPoints() != null ? request.getInitialPoints() : 0);

        // 19. 设置状态（扫码办卡，用户已注册，状态为正常）
        memberCard.setStatus(1);

        // 20. 设置时间
        LocalDateTime now = LocalDateTime.now();
        memberCard.setOpenCardTime(now);
        memberCard.setActivateTime(now);

        // 21. 设置到期时间（默认60年后）
        LocalDateTime expireTime;
        if (request.getExpireTime() != null && !request.getExpireTime().isEmpty()) {
            try {
                expireTime = LocalDateTime.parse(request.getExpireTime(), DATETIME_FORMATTER);
            } catch (Exception e) {
                expireTime = now.plusYears(DEFAULT_EXPIRE_YEARS);
            }
        } else {
            expireTime = now.plusYears(DEFAULT_EXPIRE_YEARS);
        }
        memberCard.setExpireTime(expireTime);

        // 22. 保存会员卡
        memberCardRepository.save(memberCard);

        // 22.5 创建交易记录（如果初始充值不为0）
        String operatorUserId = jwtUtils.extractUserId(token);
        byte[] operatorIdBytes = encryptUtils.uuidToBytes(operatorUserId);
        createInitialTransactionRecord(memberCard, cardType, operatorIdBytes, storeIdBytes, now);

        // 23. 记录办卡信息
        String operatorRole = jwtUtils.extractRole(token);
        saveRegistrationRecord(
            memberCardId,
            operatorUserId,
            0,  // registration_channel = 0（线下二维码）
            operatorRole,
            storeIdBytes,
            now
        );

        // 24. 将特权令牌加入黑名单（一次性使用）
        long remainingTime = privilegeClaims.getExpiration().getTime() - System.currentTimeMillis();
        if (remainingTime > 0) {
            tokenRedisService.addToBlacklist(jti, remainingTime / 1000);
        }

        log.info("扫码办卡成功：memberCardId={}, userId={}, cardTypeId={}",
                encryptUtils.bytesToUuid(memberCardId), userId, request.getCardTypeId());

        // 24. 构建响应
        CreateMemberCardResponse response = CreateMemberCardResponse.builder()
                .memberCardId(encryptUtils.bytesToUuid(memberCardId))
                .cardTypeId(cardType.getCardTypeId())
                .cardTypeName(cardType.getCardTypeName())
                .cardTtype(cardType.getCardTtype())
                .cardTtypeName(CardTtypeEnum.getNameByCode(cardType.getCardTtype()))
                .memberPhone(user.getPhone())
                .memberName(memberCard.getMemberName())
                .balance(memberCard.getBalance())
                .times(memberCard.getTimes())
                .points(memberCard.getPoints())
                .status(1)
                .statusName(getStatusName(1))
                .openCardTime(now.format(DATETIME_FORMATTER))
                .expireTime(expireTime.format(DATETIME_FORMATTER))
                .message("办卡成功")
                .build();

        return response;
    }

    /**
     * 接口3：查询商家会员卡列表（分页）
     */
    @Override
    public MemberCardListResponse queryMerchantCardList(QueryMerchantCardListRequest request, String token) {
        log.info("查询商家会员卡列表：request={}", request);

        // 1. 解析令牌
        Integer tokenType = jwtUtils.extractTokenType(token);
        String merchantId = jwtUtils.extractMerchantId(token);
        String role = jwtUtils.extractRole(token);

        // 2. 验证令牌类型（必须是普通令牌）
        if (tokenType != TokenType.NORMAL.getCode()) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID.getCode(), "接口3需要普通令牌");
        }

        // 3. 验证角色（必须是商家）
        if (!"MERCHANT".equals(role)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED.getCode(), "只有商家可以查询");
        }

        // 4. 转换ID
        byte[] merchantIdBytes = encryptUtils.uuidToBytes(merchantId);
        byte[] storeIdBytes = request.getStoreId() != null ? encryptUtils.uuidToBytes(request.getStoreId()) : null;

        // 5. 解析时间范围
        LocalDateTime startTime = parseDateTime(request.getStartTime());
        LocalDateTime endTime = parseDateTime(request.getEndTime());

        // 6. 创建分页对象
        Pageable pageable = PageRequest.of(
                request.getPageNum() - 1,
                request.getPageSize(),
                Sort.by(Sort.Direction.DESC, "openCardTime"));

        // 7. 查询会员卡列表（支持多条件筛选）
        Page<MemberCard> page = memberCardRepository.findByMerchantConditions(
                merchantIdBytes,
                storeIdBytes,
                request.getCardTypeId(),
                request.getCardTtype(),
                request.getStatus(),
                request.getMemberPhone(),
                request.getMemberName(),
                startTime,
                endTime,
                pageable);

        // 8. 转换为VO
        List<MemberCardVO> voList = page.getContent().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        // 9. 构建响应
        return MemberCardListResponse.builder()
                .total(page.getTotalElements())
                .pageNum(request.getPageNum())
                .pageSize(request.getPageSize())
                .list(voList)
                .build();
    }

    /**
     * 接口4：查询店铺会员卡列表（分页，支持本店卡/跨店卡切换）
     */
    @Override
    public MemberCardListResponse queryStoreCardList(QueryStoreCardListRequest request, String token) {
        log.info("查询店铺会员卡列表：request={}", request);

        // 1. 解析令牌
        Integer tokenType = jwtUtils.extractTokenType(token);

        // 2. 验证令牌类型（工作令牌或普通令牌）
        if (tokenType != TokenType.WORK.getCode() && tokenType != TokenType.NORMAL.getCode()) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID.getCode(), "接口4需要工作令牌或普通令牌");
        }

        // 3. 转换店铺ID
        byte[] storeIdBytes = encryptUtils.uuidToBytes(request.getStoreId());

        // 4. 查询店铺信息
        Store store = storeRepository.findById(storeIdBytes)
                .orElseThrow(() -> new BusinessException(40002, "店铺不存在"));

        // 5. 获取商家ID
        byte[] merchantIdBytes = store.getMerchantId();

        // 6. 解析时间范围
        LocalDateTime startTime = parseDateTime(request.getStartTime());
        LocalDateTime endTime = parseDateTime(request.getEndTime());

        // 7. 创建分页对象
        Pageable pageable = PageRequest.of(
                request.getPageNum() - 1,
                request.getPageSize(),
                Sort.by(Sort.Direction.DESC, "openCardTime"));

        // 8. 根据cardScope查询不同的会员卡列表
        Page<MemberCard> page;
        boolean isLocalCard;

        if ("local".equals(request.getCardScope())) {
            // 查询本店卡
            page = memberCardRepository.findByStoreConditions(
                    storeIdBytes,
                    request.getCardTypeId(),
                    request.getCardTtype(),
                    request.getStatus(),
                    request.getMemberPhone(),
                    request.getMemberName(),
                    startTime,
                    endTime,
                    pageable);
            isLocalCard = true;

        } else if ("cross_store".equals(request.getCardScope())) {
            // 查询跨店卡
            page = memberCardRepository.findByCrossStoreConditions(
                    merchantIdBytes,
                    storeIdBytes,
                    request.getCardTypeId(),
                    request.getCardTtype(),
                    request.getStatus(),
                    request.getMemberPhone(),
                    request.getMemberName(),
                    startTime,
                    endTime,
                    pageable);
            isLocalCard = false;

        } else {
            throw new BusinessException(40001, "cardScope参数错误");
        }

        // 9. 转换为VO
        List<MemberCardVO> voList = page.getContent().stream()
                .map(card -> {
                    MemberCardVO vo = convertToVO(card);
                    vo.setIsLocalCard(isLocalCard);
                    
                    // 如果是跨店卡，设置原始店铺信息
                    if (!isLocalCard) {
                        String originalStoreId = encryptUtils.bytesToUuid(card.getStoreId());
                        vo.setOriginalStoreId(originalStoreId);
                        
                        // 查询原始店铺名称
                        Store originalStore = storeRepository.findById(card.getStoreId()).orElse(null);
                        if (originalStore != null) {
                            vo.setOriginalStoreName(originalStore.getStoreName());
                        }
                    }
                    
                    return vo;
                })
                .collect(Collectors.toList());

        // 10. 构建响应
        return MemberCardListResponse.builder()
                .total(page.getTotalElements())
                .pageNum(request.getPageNum())
                .pageSize(request.getPageSize())
                .list(voList)
                .build();
    }

    // ==================== 辅助方法 ====================

    /**
     * 验证店员权限
     * 
     * @param userId 用户ID
     * @param storeId 店铺ID
     * @param requiredPermission 需要的权限（如：member_card_create）
     */
    private void validateStaffPermission(String userId, String storeId, String requiredPermission) {
        try {
            byte[] userIdBytes = encryptUtils.uuidToBytes(userId);
            byte[] storeIdBytes = encryptUtils.uuidToBytes(storeId);

            // 查询工作关系
            WorkRelation workRelation = workRelationRepository
                    .findByStoreIdAndUserIdAndStatus(storeIdBytes, userIdBytes, 1)
                    .orElseThrow(() -> new BusinessException(ErrorCode.PERMISSION_DENIED.getCode(), 
                            "未找到有效的工作关系"));

            // 解析权限JSON
            String permissionJson = workRelation.getPermission();
            if (permissionJson == null || permissionJson.isEmpty()) {
                throw new BusinessException(ErrorCode.PERMISSION_DENIED.getCode(), 
                        "店员权限配置为空");
            }

            try {
                JSONObject permissionObj = new JSONObject(permissionJson);
                JSONArray employeePermissions = permissionObj.optJSONArray("employee");

                if (employeePermissions == null) {
                    throw new BusinessException(ErrorCode.PERMISSION_DENIED.getCode(), 
                            "店员权限配置错误：缺少employee字段");
                }

                // 检查是否有所需权限
                boolean hasPermission = false;
                for (int i = 0; i < employeePermissions.length(); i++) {
                    if (requiredPermission.equals(employeePermissions.getString(i))) {
                        hasPermission = true;
                        break;
                    }
                }

                if (!hasPermission) {
                    throw new BusinessException(ErrorCode.PERMISSION_DENIED.getCode(), 
                            String.format("店员没有【%s】权限，无法执行此操作", requiredPermission));
                }

                log.info("店员权限验证通过：userId={}, storeId={}, permission={}", userId, storeId, requiredPermission);

            } catch (JSONException e) {
                log.error("权限JSON解析失败：permissionJson={}", permissionJson, e);
                throw new BusinessException(ErrorCode.PERMISSION_DENIED.getCode(), 
                        "权限配置格式错误");
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("店员权限验证失败", e);
            throw new BusinessException(ErrorCode.PERMISSION_DENIED.getCode(), 
                    "权限验证失败");
        }
    }

    /**
     * 将MemberCard实体转换为VO
     */
    private MemberCardVO convertToVO(MemberCard card) {
        MemberCardVO vo = new MemberCardVO();
        vo.setMemberCardId(encryptUtils.bytesToUuid(card.getMemberCardId()));
        vo.setCardTypeId(card.getCardTypeId());
        vo.setStoreId(encryptUtils.bytesToUuid(card.getStoreId()));
        vo.setMerchantId(encryptUtils.bytesToUuid(card.getMerchantId()));
        
        if (card.getUserId() != null) {
            vo.setUserId(encryptUtils.bytesToUuid(card.getUserId()));
        }
        
        vo.setMemberPhone(card.getMemberPhone());
        vo.setMemberName(card.getMemberName());
        vo.setCardTtype(card.getCardTtype());
        vo.setCardTtypeName(CardTtypeEnum.getNameByCode(card.getCardTtype()));
        vo.setBalance(card.getBalance());
        vo.setTimes(card.getTimes());
        vo.setPoints(card.getPoints());
        vo.setStatus(card.getStatus());
        vo.setStatusName(getStatusName(card.getStatus()));
        vo.setOpenCardTime(card.getOpenCardTime().format(DATETIME_FORMATTER));
        
        if (card.getActivateTime() != null) {
            vo.setActivateTime(card.getActivateTime().format(DATETIME_FORMATTER));
        }
        
        vo.setExpireTime(card.getExpireTime().format(DATETIME_FORMATTER));

        // 关联查询卡种信息（名称、背景图、样式）
        if (card.getCardType() != null) {
            vo.setCardTypeName(card.getCardType().getCardTypeName());
            vo.setCardBgc(card.getCardType().getCardBgc());
            vo.setCardMask(card.getCardType().getCardMask());
        } else {
            memberCardTypeRepository.findById(card.getCardTypeId()).ifPresent(cardType -> {
                vo.setCardTypeName(cardType.getCardTypeName());
                vo.setCardBgc(cardType.getCardBgc());
                vo.setCardMask(cardType.getCardMask());
            });
        }

        // 关联查询店铺名称
        if (card.getStore() != null) {
            vo.setStoreName(card.getStore().getStoreName());
        } else {
            storeRepository.findById(card.getStoreId()).ifPresent(store -> {
                vo.setStoreName(store.getStoreName());
            });
        }

        return vo;
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(int status) {
        switch (status) {
            case 0:
                return "未激活";
            case 1:
                return "正常";
            case 2:
                return "已过期";
            case 3:
                return "已冻结";
            case 4:
                return "已注销";
            default:
                return "未知";
        }
    }

    /**
     * 解析日期时间字符串
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr, DATETIME_FORMATTER);
        } catch (Exception e) {
            log.warn("日期时间解析失败：{}", dateTimeStr);
            return null;
        }
    }

    /**
     * 保存办卡记录
     *
     * @param memberCardId 会员卡ID
     * @param operatorUserId 操作员用户ID（UUID字符串）
     * @param registrationChannel 办卡渠道
     * @param operatorRoleStr 操作员角色（字符串：MERCHANT/manager/employee）
     * @param transStoreId 办卡店铺ID
     * @param registrationTime 办卡时间
     */
    private void saveRegistrationRecord(byte[] memberCardId, String operatorUserId, 
                                       int registrationChannel, String operatorRoleStr,
                                       byte[] transStoreId, LocalDateTime registrationTime) {
        try {
            // 转换操作员ID
            byte[] operatorIdBytes = encryptUtils.uuidToBytes(operatorUserId);
            
            // 转换操作员角色
            int operatorRole;
            if ("MERCHANT".equals(operatorRoleStr) || "merchant".equals(operatorRoleStr)) {
                operatorRole = 0; // 商家
            } else if ("manager".equals(operatorRoleStr)) {
                operatorRole = 1; // 店长
            } else if ("employee".equals(operatorRoleStr) || "STAFF".equals(operatorRoleStr)) {
                operatorRole = 2; // 店员
            } else {
                log.warn("未知操作员角色: {}, 默认设置为店员", operatorRoleStr);
                operatorRole = 2; // 默认店员
            }
            
            // 创建办卡记录
            RegistrationCardRecord record = RegistrationCardRecord.builder()
                    .memberCardId(memberCardId)
                    .operatorId(operatorIdBytes)
                    .registrationChannel(registrationChannel)
                    .operatorRole(operatorRole)
                    .transStoreId(transStoreId)
                    .registrationTime(registrationTime)
                    .build();
            
            registrationCardRecordRepository.save(record);
            
            log.info("办卡记录保存成功：memberCardId={}, operatorId={}, channel={}, role={}",
                    encryptUtils.bytesToUuid(memberCardId), operatorUserId, registrationChannel, operatorRole);
                    
        } catch (Exception e) {
            // 记录失败不影响办卡主流程
            log.error("办卡记录保存失败", e);
        }
    }

    // ========== 接口5：查询商家会员统计数据 ==========
    // 注意：此方法与交易管理模块的MerchantStatisticsResponse命名冲突，临时注释
    // TODO: 需要创建专门的会员统计Response类（如MemberStatisticsResponse）后重新启用
    /*
    @Override
    public MerchantStatisticsResponse getMerchantStatistics(String merchantId) {
        try {
            byte[] merchantIdBytes = encryptUtils.uuidToBytes(merchantId);

            // 统计会员总数（去重）
            Integer totalMembers = memberCardRepository.countDistinctMembersByMerchantId(merchantIdBytes);

            // 统计会员卡总数
            Integer totalCards = memberCardRepository.countTotalCardsByMerchantId(merchantIdBytes);

            // 统计已激活会员卡数量（status != 0）
            Integer activatedCards = memberCardRepository.countActivatedCardsByMerchantId(merchantIdBytes);

            // 统计未激活会员卡数量（status = 0）
            Integer unactivatedCards = memberCardRepository.countUnactivatedCardsByMerchantId(merchantIdBytes);

            // 按状态分类统计
            Integer normalCards = memberCardRepository.countCardsByMerchantIdAndStatus(merchantIdBytes, 1);
            Integer frozenCards = memberCardRepository.countCardsByMerchantIdAndStatus(merchantIdBytes, 3);
            Integer expiredCards = memberCardRepository.countCardsByMerchantIdAndStatus(merchantIdBytes, 2);
            Integer cancelledCards = memberCardRepository.countCardsByMerchantIdAndStatus(merchantIdBytes, 4);

            return MerchantStatisticsResponse.builder()
                    .totalMembers(totalMembers != null ? totalMembers : 0)
                    .totalMemberCards(totalCards != null ? totalCards : 0)
                    .activatedCards(activatedCards != null ? activatedCards : 0)
                    .unactivatedCards(unactivatedCards != null ? unactivatedCards : 0)
                    .normalCards(normalCards != null ? normalCards : 0)
                    .frozenCards(frozenCards != null ? frozenCards : 0)
                    .expiredCards(expiredCards != null ? expiredCards : 0)
                    .cancelledCards(cancelledCards != null ? cancelledCards : 0)
                    .build();
        } catch (Exception e) {
            log.error("查询商家会员统计数据失败: merchantId={}", merchantId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }
    */

    // ========== 接口6：查询店铺会员统计数据 ==========
    // 注意：此方法与交易管理模块的StoreStatisticsResponse命名冲突，临时注释
    // TODO: 需要创建专门的会员统计Response类（如StoreMemberStatisticsResponse）后重新启用
    /*
    @Override
    public StoreStatisticsResponse getStoreStatistics(String storeId, String merchantId) {
        try {
            byte[] storeIdBytes = encryptUtils.uuidToBytes(storeId);
            byte[] merchantIdBytes = encryptUtils.uuidToBytes(merchantId);

            // 本店卡统计
            Integer localTotalMembers = memberCardRepository.countDistinctMembersByStoreId(storeIdBytes);
            Integer localTotalCards = memberCardRepository.countTotalCardsByStoreId(storeIdBytes);

            // 跨店卡统计（只统计卡总数）
            Integer crossStoreTotalCards = memberCardRepository.countCrossStoreCards(merchantIdBytes, storeIdBytes);

            return StoreStatisticsResponse.builder()
                    .localCardStats(StoreStatisticsResponse.LocalCardStats.builder()
                            .totalMembers(localTotalMembers != null ? localTotalMembers : 0)
                            .totalCards(localTotalCards != null ? localTotalCards : 0)
                            .build())
                    .crossStoreCardStats(StoreStatisticsResponse.CrossStoreCardStats.builder()
                            .totalCards(crossStoreTotalCards != null ? crossStoreTotalCards : 0)
                            .build())
                    .build();
        } catch (Exception e) {
            log.error("查询店铺会员统计数据失败: storeId={}, merchantId={}", storeId, merchantId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }
    */

    // ========== 接口7：用户查询自己的会员卡列表 ==========

    @Override
    public MemberCardListResponse getMyCardList(String userId, QueryMyCardListRequest request) {
        try {
            byte[] userIdBytes = encryptUtils.uuidToBytes(userId);
            byte[] storeIdBytes = request.getStoreId() != null ? encryptUtils.uuidToBytes(request.getStoreId()) : null;
            byte[] merchantIdBytes = request.getMerchantId() != null ? encryptUtils.uuidToBytes(request.getMerchantId()) : null;

            Pageable pageable = PageRequest.of(request.getPageNum() - 1, request.getPageSize());

            Page<MemberCard> page = memberCardRepository.findMyCards(
                    userIdBytes,
                    request.getStatus(),
                    storeIdBytes,
                    merchantIdBytes,
                    request.getCardTtype(),
                    pageable
            );

            List<MemberCardVO> list = page.getContent().stream()
                    .map(this::convertToVO)
                    .collect(Collectors.toList());

            return MemberCardListResponse.builder()
                    .total(page.getTotalElements())
                    .pageNum(request.getPageNum())
                    .pageSize(request.getPageSize())
                    .list(list)
                    .build();
        } catch (Exception e) {
            log.error("查询用户会员卡列表失败: userId={}", userId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }

    // ========== 接口8：通过手机号查询会员卡 ==========

    @Override
    public QueryByPhoneResponse queryByPhone(String storeId, String encryptedPhone, String merchantId) {
        try {
            // 1. 解密手机号
            String decryptedPhone;
            try {
                decryptedPhone = encryptUtils.decryptAES(encryptedPhone);
            } catch (Exception e) {
                log.error("手机号解密失败: encryptedPhone={}", encryptedPhone, e);
                throw new BusinessException(ErrorCode.PARAM_ERROR, "手机号格式错误或解密失败");
            }

            // 2. 验证手机号格式
            if (!decryptedPhone.matches("^1[3-9]\\d{9}$")) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "手机号格式错误");
            }

            byte[] storeIdBytes = encryptUtils.uuidToBytes(storeId);
            byte[] merchantIdBytes = encryptUtils.uuidToBytes(merchantId);

            // 3. 查询本店卡
            List<MemberCard> localCards = memberCardRepository.findLocalCardsByPhone(decryptedPhone, storeIdBytes);
            // 限制最多500条
            if (localCards.size() > 500) {
                localCards = localCards.subList(0, 500);
            }

            // 4. 查询跨店卡
            Pageable pageable = PageRequest.of(0, 500);
            List<MemberCard> crossStoreCards = memberCardRepository.findCrossStoreCardsByPhone(
                    decryptedPhone,
                    merchantIdBytes,
                    storeIdBytes,
                    pageable
            );

            // 5. 查询手机号对应的用户ID（可能为null）
            User user = userRepository.findByPhone(decryptedPhone).orElse(null);
            String userId = (user != null) ? encryptUtils.bytesToUuid(user.getUserId()) : null;

            // 6. 转换为VO
            List<MemberCardVO> localCardVOs = localCards.stream()
                    .map(card -> {
                        MemberCardVO vo = convertToVO(card);
                        vo.setIsLocalCard(true);
                        return vo;
                    })
                    .collect(Collectors.toList());

            List<MemberCardVO> crossStoreCardVOs = crossStoreCards.stream()
                    .map(card -> {
                        MemberCardVO vo = convertToVO(card);
                        vo.setIsLocalCard(false);
                        // 设置原办卡店铺信息
                        vo.setOriginalStoreId(encryptUtils.bytesToUuid(card.getStoreId()));
                        vo.setOriginalStoreName(card.getStoreName());
                        return vo;
                    })
                    .collect(Collectors.toList());

            return QueryByPhoneResponse.builder()
                    .memberPhone(decryptedPhone)
                    .userId(userId)
                    .localCards(localCardVOs)
                    .crossStoreCards(crossStoreCardVOs)
                    .build();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("通过手机号查询会员卡失败: storeId={}, merchantId={}", storeId, merchantId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }

    // ========== 接口9：会员卡详情查询 ==========

    @Override
    public MemberCardDetailResponse getCardDetail(String memberCardId) {
        try {
            byte[] cardIdBytes = encryptUtils.uuidToBytes(memberCardId);

            // 查询会员卡
            MemberCard card = memberCardRepository.findById(cardIdBytes)
                    .orElseThrow(() -> new BusinessException(70004, "会员卡不存在"));

            // 获取状态名称
            String statusName = getStatusName(card.getStatus());

            // 获取卡种类型名称
            String cardTtypeName = getCardTypeName(card.getCardTtype());

            // 填充冗余字段（包括cardBgc和cardMask）
            card.fillRedundantFields();

            return MemberCardDetailResponse.builder()
                    .memberCardId(memberCardId)
                    .cardTypeId(card.getCardTypeId())
                    .cardTypeName(card.getCardTypeName())
                    .cardBgc(card.getCardBgc())
                    .cardMask(card.getCardMask())
                    .cardTtype(card.getCardTtype())
                    .cardTtypeName(cardTtypeName)
                    .description(card.getCardTypeDescription())
                    .storeId(card.getStoreId() != null ? encryptUtils.bytesToUuid(card.getStoreId()) : null)
                    .storeName(card.getStoreName())
                    .merchantId(card.getMerchantId() != null ? encryptUtils.bytesToUuid(card.getMerchantId()) : null)
                    .merchantName(card.getMerchantName())
                    .userId(card.getUserId() != null ? encryptUtils.bytesToUuid(card.getUserId()) : null)
                    .memberName(card.getMemberName())
                    .memberPhone(card.getMemberPhone())
                    .balance(card.getBalance())
                    .times(card.getTimes())
                    .points(card.getPoints())
                    .status(card.getStatus())
                    .statusName(statusName)
                    .openCardTime(card.getOpenCardTime() != null ? card.getOpenCardTime().toString() : null)
                    .activateTime(card.getActivateTime() != null ? card.getActivateTime().toString() : null)
                    .expireTime(card.getExpireTime() != null ? card.getExpireTime().toString() : null)
                    .autoNotify(card.getAutoNotify())
                    .crossStore(card.getCrossStore())
                    .build();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询会员卡详情失败: memberCardId={}", memberCardId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }

    // ========== 接口11：批量激活会员卡（手机号批量激活） ==========

    @Override
    @Transactional
    public ActivateBatchResponse activateBatch(String userId, String phone) {
        try {
            log.info("批量激活会员卡请求：userId={}", userId);

            byte[] userIdBytes = encryptUtils.uuidToBytes(userId);

            // 从用户表查询手机号
            if (phone == null) {
                User user = userRepository.findById(userIdBytes)
                        .orElseThrow(() -> new BusinessException(20003, "用户信息不存在"));
                phone = user.getPhone();
            }

            // 查询所有满足条件的未激活会员卡：member_phone = 用户手机号 AND status = 0
            List<MemberCard> unactivatedCards = memberCardRepository.findByMemberPhoneAndStatus(phone, 0);

            if (unactivatedCards.isEmpty()) {
                return ActivateBatchResponse.builder()
                        .activatedCount(0)
                        .activatedCards(List.of())
                        .build();
            }

            // 批量激活
            LocalDateTime now = LocalDateTime.now();
            List<ActivateBatchResponse.ActivatedCardInfo> activatedCardInfos = unactivatedCards.stream()
                    .map(card -> {
                        // 更新会员卡状态
                        Integer oldStatus = card.getStatus();
                        card.setUserId(userIdBytes);
                        card.setStatus(1); // 正常状态
                        card.setActivateTime(now);
                        card.setUpdateTime(now);
                        memberCardRepository.save(card);

                        // 记录状态变更日志（change_type=2-激活，operator_role=3-用户本人）
                        McardStatusLog statusLog = new McardStatusLog();
                        statusLog.setMemberCardId(card.getMemberCardId());
                        statusLog.setChangeType(2); // 激活
                        statusLog.setOldStatus(oldStatus);
                        statusLog.setNewStatus(1);
                        statusLog.setOperatorId(userIdBytes);
                        statusLog.setOperatorRole(3); // 用户本人
                        statusLog.setChangeReason("用户批量激活");
                        statusLog.setOperatorTime(now);
                        mcardStatusLogRepository.save(statusLog);

                        // 填充冗余字段
                        card.fillRedundantFields();

                        // 构造返回信息
                        return ActivateBatchResponse.ActivatedCardInfo.builder()
                                .memberCardId(encryptUtils.bytesToUuid(card.getMemberCardId()))
                                .cardTypeName(card.getCardTypeName())
                                .storeName(card.getStoreName())
                                .build();
                    })
                    .collect(Collectors.toList());

            return ActivateBatchResponse.builder()
                    .activatedCount(activatedCardInfos.size())
                    .activatedCards(activatedCardInfos)
                    .build();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("批量激活会员卡失败: userId={}", userId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }

    // ========== 接口12：冻结会员卡 ==========

    @Override
    @Transactional
    public FreezeCardResponse freezeCard(FreezeCardRequest request, String token) {
        try {
            log.info("冻结会员卡请求：memberCardId={}", request.getMemberCardId());

            byte[] cardIdBytes = encryptUtils.uuidToBytes(request.getMemberCardId());

            // 查询会员卡
            MemberCard card = memberCardRepository.findById(cardIdBytes)
                    .orElseThrow(() -> new BusinessException(70004, "会员卡不存在"));

            // 验证会员卡状态为1（正常）或2（已过期），才可冻结
            if (card.getStatus() != 1 && card.getStatus() != 2) {
                throw new BusinessException(70008, "该会员卡状态无法冻结");
            }

            // 解析令牌
            Integer tokenType = jwtUtils.extractTokenType(token);
            String userId = jwtUtils.extractUserId(token);
            byte[] userIdBytes = encryptUtils.uuidToBytes(userId);
            Integer operatorRole = null;

            // 令牌验证和权限校验
            if (tokenType == TokenType.WORK.getCode()) {
                // 工作令牌：验证工作店铺
                String tokenStoreId = jwtUtils.extractStoreId(token);
                String cardStoreId = encryptUtils.bytesToUuid(card.getStoreId());
                if (!tokenStoreId.equals(cardStoreId)) {
                    throw new BusinessException(70005, "无权操作该会员卡");
                }

                // 确定操作人角色
                String role = jwtUtils.extractRole(token);
                if ("merchant".equalsIgnoreCase(role) || "MERCHANT".equals(role)) {
                    operatorRole = 0; // 商家
                } else if ("manager".equalsIgnoreCase(role) || "MANAGER".equals(role)) {
                    operatorRole = 1; // 店长
                } else if ("staff".equalsIgnoreCase(role) || "STAFF".equals(role)) {
                    operatorRole = 2; // 店员
                }
            } else if (tokenType == TokenType.NORMAL.getCode()) {
                // 普通令牌
                String normalRole = jwtUtils.extractRole(token);
                String merchantId = jwtUtils.extractMerchantId(token);
                
                if ("MERCHANT".equals(normalRole) && merchantId != null) {
                    // 商家：验证会员卡归属
                    String cardMerchantId = encryptUtils.bytesToUuid(card.getMerchantId());
                    if (!merchantId.equals(cardMerchantId)) {
                        throw new BusinessException(70005, "无权操作该会员卡");
                    }
                    operatorRole = 0; // 商家
                } else {
                    // 普通用户：验证是本人的卡
                    String cardUserId = card.getUserId() != null ? encryptUtils.bytesToUuid(card.getUserId()) : null;
                    if (!userId.equals(cardUserId)) {
                        throw new BusinessException(70005, "无权操作该会员卡");
                    }
                    operatorRole = 3; // 用户本人
                }
            } else {
                throw new BusinessException(ErrorCode.TOKEN_INVALID.getCode(), "令牌类型不支持此操作");
            }

            // 更新会员卡状态
            LocalDateTime now = LocalDateTime.now();
            Integer oldStatus = card.getStatus();
            card.setStatus(3); // 已冻结
            card.setUpdateTime(now);
            memberCardRepository.save(card);

            // 记录状态变更日志（change_type=0-冻结）
            McardStatusLog statusLog = new McardStatusLog();
            statusLog.setMemberCardId(card.getMemberCardId());
            statusLog.setChangeType(0); // 冻结
            statusLog.setOldStatus(oldStatus);
            statusLog.setNewStatus(3);
            statusLog.setOperatorId(userIdBytes);
            statusLog.setOperatorRole(operatorRole);
            statusLog.setChangeReason(request.getFreezeReason());
            statusLog.setOperatorTime(now);
            mcardStatusLogRepository.save(statusLog);

            return FreezeCardResponse.builder()
                    .memberCardId(request.getMemberCardId())
                    .status(3)
                    .freezeTime(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .build();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("冻结会员卡失败: memberCardId={}", request.getMemberCardId(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }

    // ========== 接口13：解冻会员卡 ==========

    @Override
    @Transactional
    public UnfreezeCardResponse unfreezeCard(UnfreezeCardRequest request, String token) {
        try {
            log.info("解冻会员卡请求：memberCardId={}", request.getMemberCardId());

            byte[] cardIdBytes = encryptUtils.uuidToBytes(request.getMemberCardId());

            // 查询会员卡
            MemberCard card = memberCardRepository.findById(cardIdBytes)
                    .orElseThrow(() -> new BusinessException(70004, "会员卡不存在"));

            // 验证会员卡状态为3（已冻结）
            if (card.getStatus() != 3) {
                throw new BusinessException(70009, "该会员卡不是冻结状态，无法解冻");
            }

            // 解析令牌
            Integer tokenType = jwtUtils.extractTokenType(token);
            String userId = jwtUtils.extractUserId(token);
            byte[] userIdBytes = encryptUtils.uuidToBytes(userId);
            Integer operatorRole = null;

            // 令牌验证和权限校验（同冻结逻辑）
            if (tokenType == TokenType.WORK.getCode()) {
                // 工作令牌：验证工作店铺
                String tokenStoreId = jwtUtils.extractStoreId(token);
                String cardStoreId = encryptUtils.bytesToUuid(card.getStoreId());
                if (!tokenStoreId.equals(cardStoreId)) {
                    throw new BusinessException(70005, "无权操作该会员卡");
                }

                // 确定操作人角色
                String role = jwtUtils.extractRole(token);
                if ("merchant".equalsIgnoreCase(role) || "MERCHANT".equals(role)) {
                    operatorRole = 0; // 商家
                } else if ("manager".equalsIgnoreCase(role) || "MANAGER".equals(role)) {
                    operatorRole = 1; // 店长
                } else if ("staff".equalsIgnoreCase(role) || "STAFF".equals(role)) {
                    operatorRole = 2; // 店员
                }
            } else if (tokenType == TokenType.NORMAL.getCode()) {
                // 普通令牌
                String normalRole = jwtUtils.extractRole(token);
                String merchantId = jwtUtils.extractMerchantId(token);
                
                if ("MERCHANT".equals(normalRole) && merchantId != null) {
                    // 商家：验证会员卡归属
                    String cardMerchantId = encryptUtils.bytesToUuid(card.getMerchantId());
                    if (!merchantId.equals(cardMerchantId)) {
                        throw new BusinessException(70005, "无权操作该会员卡");
                    }
                    operatorRole = 0; // 商家
                } else {
                    // 普通用户：验证是本人的卡
                    String cardUserId = card.getUserId() != null ? encryptUtils.bytesToUuid(card.getUserId()) : null;
                    if (!userId.equals(cardUserId)) {
                        throw new BusinessException(70005, "无权操作该会员卡");
                    }
                    operatorRole = 3; // 用户本人
                }
            } else {
                throw new BusinessException(ErrorCode.TOKEN_INVALID.getCode(), "令牌类型不支持此操作");
            }

            // 检查会员卡是否已过期
            LocalDateTime now = LocalDateTime.now();
            Integer oldStatus = card.getStatus();
            Integer newStatus;

            if (card.getExpireTime() != null && card.getExpireTime().isBefore(now)) {
                // 已过期，更新为过期状态
                newStatus = 2;
            } else {
                // 未过期，更新为正常状态
                newStatus = 1;
            }

            card.setStatus(newStatus);
            card.setUpdateTime(now);
            memberCardRepository.save(card);

            // 记录状态变更日志（change_type=1-解冻）
            McardStatusLog statusLog = new McardStatusLog();
            statusLog.setMemberCardId(card.getMemberCardId());
            statusLog.setChangeType(1); // 解冻
            statusLog.setOldStatus(oldStatus);
            statusLog.setNewStatus(newStatus);
            statusLog.setOperatorId(userIdBytes);
            statusLog.setOperatorRole(operatorRole);
            statusLog.setChangeReason(request.getUnfreezeReason() != null ? request.getUnfreezeReason() : "解冻会员卡");
            statusLog.setOperatorTime(now);
            mcardStatusLogRepository.save(statusLog);

            return UnfreezeCardResponse.builder()
                    .memberCardId(request.getMemberCardId())
                    .status(newStatus)
                    .unfreezeTime(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .build();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("解冻会员卡失败: memberCardId={}", request.getMemberCardId(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 获取卡种类型名称
     */
    private String getCardTypeName(Integer cardTtype) {
        if (cardTtype == null) return "未知";
        switch (cardTtype) {
            case 1: return "余额卡";
            case 2: return "次数卡";
            case 3: return "时效卡";
            case 4: return "积分卡";
            default: return "未知";
        }
    }

    // ========== 接口15：线下扫码查询个人会员卡 ==========

    /**
     * 场景A：会员卡详情查询（有memberCardId）
     */
    @Override
    public MemberCardDetailResponse queryByScanDetail(String storeId, String privilegeToken, String memberCardId, String merchantId) {
        try {
            // 1. 验证并解析特权令牌
            String userId = verifyPrivilegeToken(privilegeToken);

            // 2. 验证会员卡存在
            byte[] cardIdBytes = encryptUtils.uuidToBytes(memberCardId);
            MemberCard card = memberCardRepository.findById(cardIdBytes)
                    .orElseThrow(() -> new BusinessException(70004, "会员卡不存在"));

            // 3. 验证会员卡归属（防止memberCardId造假）
            String cardUserId = encryptUtils.bytesToUuid(card.getUserId());
            if (!userId.equals(cardUserId)) {
                throw new BusinessException(70005, "无权查询该会员卡");
            }

            // 4. 验证会员卡归属于当前商家
            String cardMerchantId = encryptUtils.bytesToUuid(card.getMerchantId());
            if (!merchantId.equals(cardMerchantId)) {
                throw new BusinessException(70005, "该会员卡不属于当前商家");
            }

            // 5. 填充冗余字段
            card.fillRedundantFields();

            // 6. 转换为响应DTO
            MemberCardDetailResponse response = MemberCardDetailResponse.builder()
                    .memberCardId(memberCardId)
                    .cardTypeId(card.getCardTypeId())
                    .cardTypeName(card.getCardTypeName())
                    .cardBgc(card.getCardBgc())
                    .cardMask(card.getCardMask())
                    .cardTtype(card.getCardTtype())
                    .cardTtypeName(getCardTypeName(card.getCardTtype()))
                    .description(card.getCardTypeDescription())
                    .storeId(encryptUtils.bytesToUuid(card.getStoreId()))
                    .storeName(card.getStoreName())
                    .merchantId(merchantId)
                    .merchantName(card.getMerchantName())
                    .userId(userId)
                    .memberName(card.getMemberName())
                    .memberPhone(card.getMemberPhone())
                    .balance(card.getBalance())
                    .times(card.getTimes())
                    .points(card.getPoints())
                    .status(card.getStatus())
                    .statusName(getStatusName(card.getStatus()))
                    .openCardTime(card.getOpenCardTime() != null ? card.getOpenCardTime().toString() : null)
                    .activateTime(card.getActivateTime() != null ? card.getActivateTime().toString() : null)
                    .expireTime(card.getExpireTime() != null ? card.getExpireTime().toString() : null)
                    .autoNotify(card.getAutoNotify())
                    .crossStore(card.getCrossStore())
                    .build();

            // 7. 特权令牌使用后加入黑名单
            markPrivilegeTokenAsUsed(privilegeToken);

            return response;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("线下扫码查询会员卡详情失败: memberCardId={}", memberCardId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }

    /**
     * 场景B：会员卡列表查询（无memberCardId）
     */
    @Override
    public QueryByPhoneResponse queryByScanList(String storeId, String privilegeToken, String merchantId) {
        try {
            // 1. 验证并解析特权令牌
            String userId = verifyPrivilegeToken(privilegeToken);

            // 2. 查询该用户在该店铺可用的会员卡
            byte[] storeIdBytes = encryptUtils.uuidToBytes(storeId);
            byte[] merchantIdBytes = encryptUtils.uuidToBytes(merchantId);
            byte[] userIdBytes = encryptUtils.uuidToBytes(userId);

            // 3. 查询本店卡
            List<MemberCard> localCards = memberCardRepository.findByUserIdAndStoreId(userIdBytes, storeIdBytes);
            // 限制最多500条
            if (localCards.size() > 500) {
                localCards = localCards.subList(0, 500);
            }

            // 4. 查询跨店卡
            Pageable pageable = PageRequest.of(0, 500);
            List<MemberCard> crossStoreCards = memberCardRepository.findCrossStoreCardsByUserId(
                    userIdBytes,
                    merchantIdBytes,
                    storeIdBytes,
                    pageable
            );

            // 5. 查询用户手机号
            String memberPhone = userRepository.findPhoneByUserId(userIdBytes).orElse(null);

            // 6. 转换为VO
            List<MemberCardVO> localCardVOs = localCards.stream()
                    .map(card -> {
                        MemberCardVO vo = convertToVO(card);
                        vo.setIsLocalCard(true);
                        return vo;
                    })
                    .collect(Collectors.toList());

            List<MemberCardVO> crossStoreCardVOs = crossStoreCards.stream()
                    .map(card -> {
                        MemberCardVO vo = convertToVO(card);
                        vo.setIsLocalCard(false);
                        // 设置原办卡店铺信息
                        vo.setOriginalStoreId(encryptUtils.bytesToUuid(card.getStoreId()));
                        vo.setOriginalStoreName(card.getStoreName());
                        return vo;
                    })
                    .collect(Collectors.toList());

            // 7. 特权令牌使用后加入黑名单
            markPrivilegeTokenAsUsed(privilegeToken);

            return QueryByPhoneResponse.builder()
                    .memberPhone(memberPhone)
                    .userId(userId)
                    .localCards(localCardVOs)
                    .crossStoreCards(crossStoreCardVOs)
                    .build();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("线下扫码查询会员卡列表失败: storeId={}", storeId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }

    /**
     * 验证特权令牌并返回用户ID
     * 
     * @param privilegeToken 特权令牌
     * @return 用户ID
     * @throws BusinessException 验证失败时抛出
     */
    private String verifyPrivilegeToken(String privilegeToken) {
        try {
            // 1. 解析特权令牌
            Claims claims = jwtUtils.parseToken(privilegeToken);
            String jti = claims.getId();
            String userId = claims.get("user_id", String.class);
            Boolean singleUse = claims.get("single_use", Boolean.class);
            @SuppressWarnings("unchecked")
            List<String> permissions = claims.get("permissions", List.class);

            // 2. 检查黑名单（防止重复使用）
            if (tokenRedisService.isInBlacklist(jti)) {
                throw new BusinessException(70011, "该二维码已使用，请重新生成");
            }

            // 3. 验证令牌类型
            if (!Boolean.TRUE.equals(singleUse)) {
                throw new BusinessException(10012, "特权令牌无效");
            }

            // 4. 验证权限
            if (permissions == null || !permissions.contains("QUERY_MCARD")) {
                throw new BusinessException(10013, "特权令牌权限不足");
            }

            // 5. 验证令牌未过期（parseToken已经验证，这里只是确保）
            if (claims.getExpiration().before(new Date())) {
                throw new BusinessException(10012, "特权令牌已过期");
            }

            return userId;

        } catch (ExpiredJwtException e) {
            throw new BusinessException(10012, "特权令牌已过期");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("特权令牌验证失败", e);
            throw new BusinessException(10012, "特权令牌无效或已过期");
        }
    }

    /**
     * 将特权令牌标记为已使用（加入黑名单）
     * 
     * @param privilegeToken 特权令牌
     */
    private void markPrivilegeTokenAsUsed(String privilegeToken) {
        try {
            Claims claims = jwtUtils.parseToken(privilegeToken);
            String jti = claims.getId();
            
            // 计算令牌剩余有效期
            long remainingTime = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (remainingTime > 0) {
                // 加入黑名单，TTL设置为剩余有效期（转换为秒）
                tokenRedisService.addToBlacklist(jti, remainingTime / 1000);
                log.info("特权令牌已加入黑名单: jti={}, ttl={}秒", jti, remainingTime / 1000);
            }
        } catch (Exception e) {
            log.error("特权令牌加入黑名单失败", e);
            // 不抛出异常，避免影响主流程
        }
    }

    /**
     * 创建办卡初始充值交易记录
     * 
     * @param memberCard 会员卡实体
     * @param cardType 卡种实体
     * @param operatorId 操作员ID
     * @param storeId 店铺ID
     * @param transactionTime 交易时间
     */
    private void createInitialTransactionRecord(MemberCard memberCard, MemberCardType cardType, 
                                               byte[] operatorId, byte[] storeId, LocalDateTime transactionTime) {
        try {
            boolean needRecord = false;
            BigDecimal amount = BigDecimal.ZERO;
            BigDecimal balanceSnapshot = null;
            int transactionType = 1;  // 默认充值
            String remark = "办卡初始充值";
            
            // 判断是否需要创建交易记录
            if (cardType.getCardTtype() == 1 && memberCard.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                // 余额卡且初始余额大于0
                needRecord = true;
                amount = memberCard.getBalance();
                balanceSnapshot = memberCard.getBalance();
                remark = "办卡初始充值";
            } else if (cardType.getCardTtype() == 2 && memberCard.getTimes() > 0) {
                // 次数卡且初始次数大于0
                needRecord = true;
                amount = new BigDecimal(memberCard.getTimes());
                balanceSnapshot = new BigDecimal(memberCard.getTimes());
                remark = "办卡初始充值";
            } else if (cardType.getCardTtype() == 3) {
                // 时效卡：计算办卡时间到到期时间的天数
                long days = java.time.temporal.ChronoUnit.DAYS.between(
                    transactionTime.toLocalDate(), 
                    memberCard.getExpireTime().toLocalDate()
                );
                
                // 如果不是默认的60年（约21900天），则记录
                // 这表示用户自定义了有效期
                if (days < 21900) {
                    needRecord = true;
                    amount = new BigDecimal(days);
                    balanceSnapshot = null;  // 时效卡不填余额快照
                    transactionType = 4;  // 4-延期
                    remark = "办卡初始设置有效期" + days + "天";
                }
            }
            
            if (!needRecord) {
                return;
            }
            
            // 创建交易记录
            TransactionRecord record = new TransactionRecord();
            record.setMemberCardId(memberCard.getMemberCardId());
            record.setUserId(memberCard.getUserId());  // 可能为NULL
            record.setMerchantId(memberCard.getMerchantId());
            record.setTransactionType(transactionType);
            record.setAmount(amount);
            record.setBalanceSnapshot(balanceSnapshot);
            record.setOperatorId(operatorId);
            record.setTransStoreId(storeId);
            record.setRemark(remark);
            record.setTransactionTime(transactionTime);
            
            transactionRecordRepository.save(record);
            
            log.info("办卡初始交易记录创建成功：memberCardId={}, type={}, amount={}", 
                    encryptUtils.bytesToUuid(memberCard.getMemberCardId()), transactionType, amount);
                    
        } catch (Exception e) {
            // 记录失败不影响办卡主流程
            log.error("办卡初始交易记录创建失败", e);
        }
    }
}

