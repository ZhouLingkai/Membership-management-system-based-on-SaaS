package com.ecards.member_management.service;

import com.ecards.member_management.common.ErrorCode;
import com.ecards.member_management.dto.request.*;
import com.ecards.member_management.dto.response.*;
import com.ecards.member_management.entity.*;
import com.ecards.member_management.enums.TokenType;
import com.ecards.member_management.exception.BusinessException;
import com.ecards.member_management.repository.*;
import com.ecards.member_management.utils.EncryptUtils;
import com.ecards.member_management.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 交易管理服务实现类
 * 
 * @author Ecards Team
 * @since 2025-11-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final MemberCardRepository memberCardRepository;
    private final MemberCardTypeRepository memberCardTypeRepository;
    private final StoreRepository storeRepository;
    private final MerchantExtendRepository merchantExtendRepository;
    private final WorkRelationRepository workRelationRepository;
    private final TransactionRecordRepository transactionRecordRepository;
    private final PointsRecordRepository pointsRecordRepository;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final EncryptUtils encryptUtils;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("10000.00");

    /**
     * 接口1：会员卡充值（幂等性接口）
     */
    @Override
    @Transactional
    public RechargeResponse recharge(RechargeRequest request, String token) {
        log.info("会员卡充值请求：memberCardId={}, storeId={}, amount={}", 
                request.getMemberCardId(), request.getStoreId(), request.getAmount());

        // 1. 解析令牌
        Claims claims = jwtUtils.parseToken(token);
        Integer tokenType = claims.get("token_type", Integer.class);
        String operatorIdStr = claims.get("user_id", String.class);
        byte[] operatorId = encryptUtils.uuidToBytes(operatorIdStr);

        byte[] storeIdBytes = encryptUtils.uuidToBytes(request.getStoreId());
        byte[] memberCardIdBytes = encryptUtils.uuidToBytes(request.getMemberCardId());

        // 2. 令牌与权限验证
        if (tokenType == 3) {
            // 工作令牌：验证工作店铺
            String tokenStoreId = claims.get("store_id", String.class);
            if (!request.getStoreId().equals(tokenStoreId)) {
                throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权在该店铺操作");
            }
            
            // 验证店员权限：需要transaction_recharge权限
            String role = claims.get("role", String.class);
            if ("STAFF".equals(role)) {
                List<String> permissions = claims.get("permissions", List.class);
                if (permissions == null || !permissions.contains("transaction_recharge")) {
                    throw new BusinessException(ErrorCode.PERMISSION_DENIED, "店员无充值权限");
                }
            }
        } else if (tokenType == 1) {
            // 普通令牌且商家：验证店铺归属
            Integer userType = claims.get("user_type", Integer.class);
            if (userType != null && userType == 2) {
                // 商家令牌，验证店铺归属
                Store store = storeRepository.findById(storeIdBytes)
                        .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));
                String merchantIdStr = claims.get("merchant_id", String.class);
                byte[] tokenMerchantId = encryptUtils.uuidToBytes(merchantIdStr);
                if (!Arrays.equals(store.getMerchantId(), tokenMerchantId)) {
                    throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权操作该店铺");
                }
            } else {
                throw new BusinessException(ErrorCode.PERMISSION_DENIED, "普通用户无权操作充值");
            }
        } else {
            throw new BusinessException(ErrorCode.TOKEN_PERMISSION_INSUFFICIENT);
        }

        // 3. 验证店铺存在
        Store store = storeRepository.findById(storeIdBytes)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        // 4. 使用悲观锁查询会员卡（防止并发问题）
        MemberCard memberCard = memberCardRepository.findByIdForUpdate(memberCardIdBytes)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_CARD_NOT_FOUND));

        // 5. 验证会员卡状态（正常或未激活）
        if (memberCard.getStatus() != 0 && memberCard.getStatus() != 1) {
            throw new BusinessException(ErrorCode.MEMBER_CARD_STATUS_INVALID, "会员卡状态异常，无法操作");
        }

        // 6. 验证卡种类型（必须为余额卡或次数卡）
        if (memberCard.getCardTtype() != 1 && memberCard.getCardTtype() != 2) {
            throw new BusinessException(ErrorCode.CARD_TYPE_NOT_SUPPORT_OPERATION, "该卡种不支持充值操作");
        }

        // 6.5. 验证充值类型与卡种是否匹配
        if (request.getRechargeType() == 1 && memberCard.getCardTtype() != 1) {
            throw new BusinessException(ErrorCode.RECHARGE_TYPE_MISMATCH, "充值类型为金额，但该卡种不是余额卡");
        }
        if (request.getRechargeType() == 2 && memberCard.getCardTtype() != 2) {
            throw new BusinessException(ErrorCode.RECHARGE_TYPE_MISMATCH, "充值类型为次数，但该卡种不是次数卡");
        }

        // 7. 验证商家认证状态
        MerchantExtend merchant = merchantExtendRepository.findById(store.getMerchantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_NOT_FOUND));
        if (merchant.getCertification() != 1 && merchant.getCertification() != 2) {
            throw new BusinessException(ErrorCode.MERCHANT_NOT_AUTHENTICATED, "商家未认证，无法操作");
        }

        // 8. 验证店铺归属（本店卡或跨店卡）
        boolean isLocalCard = Arrays.equals(memberCard.getStoreId(), storeIdBytes);
        boolean isCrossStoreCard = false;
        if (!isLocalCard) {
            // 检查是否为跨店卡
            if (Arrays.equals(memberCard.getMerchantId(), store.getMerchantId())) {
                MemberCardType cardType = memberCardTypeRepository.findById(memberCard.getCardTypeId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.CARD_TYPE_NOT_FOUND));
                if (cardType.getCrossStore() == 1) {
                    isCrossStoreCard = true;
                }
            }
        }
        if (!isLocalCard && !isCrossStoreCard) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权操作该会员卡");
        }

        // 9. 充值金额校验
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "充值金额必须大于0");
        }
        if (request.getAmount().compareTo(MAX_AMOUNT) > 0) {
            throw new BusinessException(ErrorCode.RECHARGE_AMOUNT_EXCEEDED, "充值金额超过限制");
        }

        // 10. 获取卡种信息
        MemberCardType cardType = memberCardTypeRepository.findById(memberCard.getCardTypeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_TYPE_NOT_FOUND));

        // 11. 执行充值操作（根据充值类型）
        BigDecimal newBalance;
        if (request.getRechargeType() == 1) {
            // 充值类型为金额：增加余额
            newBalance = memberCard.getBalance().add(request.getAmount());
            memberCard.setBalance(newBalance);
        } else {
            // 充值类型为次数：增加次数（将amount转换为整数）
            int times = request.getAmount().intValue();
            int newTimes = memberCard.getTimes() + times;
            memberCard.setTimes(newTimes);
            newBalance = new BigDecimal(newTimes);
        }
        memberCardRepository.save(memberCard);

        // 12. 创建交易记录
        LocalDateTime now = LocalDateTime.now();
        TransactionRecord record = new TransactionRecord();
        record.setMemberCardId(memberCardIdBytes);
        record.setUserId(memberCard.getUserId());  // 可能为NULL
        record.setMerchantId(memberCard.getMerchantId());
        record.setTransactionType(1);  // 1-充值
        record.setAmount(request.getAmount());
        record.setBalanceSnapshot(newBalance);
        record.setOperatorId(operatorId);
        record.setTransStoreId(storeIdBytes);
        record.setRemark(request.getRemark());
        record.setTransactionTime(now);
        transactionRecordRepository.save(record);

        log.info("会员卡充值成功：transactionId={}, memberCardId={}, amount={}, newBalance={}",
                record.getTransactionId(), request.getMemberCardId(), request.getAmount(), newBalance);

        // 13. 构建响应
        return RechargeResponse.builder()
                .transactionId(record.getTransactionId())
                .memberCardId(request.getMemberCardId())
                .cardTypeName(cardType.getCardTypeName())
                .cardTtype(memberCard.getCardTtype())
                .rechargeAmount(request.getAmount())
                .balanceSnapshot(newBalance)
                .transactionTime(now.format(DATETIME_FORMATTER))
                .build();
    }

    /**
     * 接口2：时效调整（幂等性接口）
     */
    @Override
    @Transactional
    public ExpireAdjustResponse expireAdjust(ExpireAdjustRequest request, String token) {
        log.info("时效调整请求：memberCardId={}, storeId={}, adjustType={}", 
                request.getMemberCardId(), request.getStoreId(), request.getAdjustType());

        // 1. 验证参数
        if (request.getAdjustType() == 1 && (request.getDays() == null || request.getDays() <= 0)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "相对延期时，天数必须大于0");
        }
        if (request.getAdjustType() == 2 && (request.getExpireTime() == null || request.getExpireTime().isBlank())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "绝对设置时，到期时间不能为空");
        }

        // 2. 解析令牌
        Claims claims = jwtUtils.parseToken(token);
        Integer tokenType = claims.get("token_type", Integer.class);
        String operatorIdStr = claims.get("user_id", String.class);
        byte[] operatorId = encryptUtils.uuidToBytes(operatorIdStr);

        byte[] storeIdBytes = encryptUtils.uuidToBytes(request.getStoreId());
        byte[] memberCardIdBytes = encryptUtils.uuidToBytes(request.getMemberCardId());

        // 3. 令牌与权限验证（与充值相同）
        if (tokenType == 3) {
            // 工作令牌：验证工作店铺
            String tokenStoreId = claims.get("store_id", String.class);
            if (!request.getStoreId().equals(tokenStoreId)) {
                throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权在该店铺操作");
            }
            
            // 验证店员权限：需要transaction_recharge权限
            String role = claims.get("role", String.class);
            if ("STAFF".equals(role)) {
                List<String> permissions = claims.get("permissions", List.class);
                if (permissions == null || !permissions.contains("transaction_recharge")) {
                    throw new BusinessException(ErrorCode.PERMISSION_DENIED, "店员无时效调整权限");
                }
            }
        } else if (tokenType == 1) {
            // 普通令牌且商家：验证店铺归属
            Integer userType = claims.get("user_type", Integer.class);
            if (userType != null && userType == 2) {
                Store store = storeRepository.findById(storeIdBytes)
                        .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));
                String merchantIdStr = claims.get("merchant_id", String.class);
                byte[] tokenMerchantId = encryptUtils.uuidToBytes(merchantIdStr);
                if (!Arrays.equals(store.getMerchantId(), tokenMerchantId)) {
                    throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权操作该店铺");
                }
            } else {
                throw new BusinessException(ErrorCode.PERMISSION_DENIED, "普通用户无权操作时效调整");
            }
        } else {
            throw new BusinessException(ErrorCode.TOKEN_PERMISSION_INSUFFICIENT);
        }

        // 4. 验证店铺存在
        Store store = storeRepository.findById(storeIdBytes)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        // 5. 使用悲观锁查询会员卡
        MemberCard memberCard = memberCardRepository.findByIdForUpdate(memberCardIdBytes)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_CARD_NOT_FOUND));

        // 6. 验证会员卡状态
        if (memberCard.getStatus() != 0 && memberCard.getStatus() != 1) {
            throw new BusinessException(ErrorCode.MEMBER_CARD_STATUS_INVALID, "会员卡状态异常，无法操作");
        }

        // 7. 验证卡种类型（必须为时效卡）
        if (memberCard.getCardTtype() != 3) {
            throw new BusinessException(ErrorCode.NOT_TIME_CARD, "该会员卡非时效卡，不支持延期操作");
        }

        // 8. 验证店铺归属（本店卡或跨店卡）
        boolean isLocalCard = Arrays.equals(memberCard.getStoreId(), storeIdBytes);
        boolean isCrossStoreCard = false;
        if (!isLocalCard) {
            if (Arrays.equals(memberCard.getMerchantId(), store.getMerchantId())) {
                MemberCardType cardType = memberCardTypeRepository.findById(memberCard.getCardTypeId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.CARD_TYPE_NOT_FOUND));
                if (cardType.getCrossStore() == 1) {
                    isCrossStoreCard = true;
                }
            }
        }
        if (!isLocalCard && !isCrossStoreCard) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权操作该会员卡");
        }

        // 9. 获取卡种信息
        MemberCardType cardType = memberCardTypeRepository.findById(memberCard.getCardTypeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_TYPE_NOT_FOUND));

        // 10. 记录旧到期时间
        LocalDateTime oldExpireTime = memberCard.getExpireTime();
        LocalDateTime now = LocalDateTime.now(); // 提前定义now变量
        LocalDateTime newExpireTime;
        int transactionType;
        BigDecimal amount;
        String adjustTypeName;

        // 11. 执行时效调整
        if (request.getAdjustType() == 1) {
            // 相对延期：如果已过期，从今天开始延期；如果未过期，在原到期时间基础上延期
            LocalDateTime baseTime = oldExpireTime.isBefore(now) ? now : oldExpireTime;
            newExpireTime = baseTime.plusDays(request.getDays());
            transactionType = 4;  // 4-延期
            amount = new BigDecimal(request.getDays());
            adjustTypeName = "相对延期";
        } else {
            // 绝对设置
            try {
                newExpireTime = LocalDateTime.parse(request.getExpireTime(), DATETIME_FORMATTER);
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "到期时间格式错误，应为：yyyy-MM-dd HH:mm:ss");
            }
            transactionType = 5;  // 5-日期变动
            amount = BigDecimal.ZERO;
            adjustTypeName = "绝对设置";
        }

        memberCard.setExpireTime(newExpireTime);
        memberCardRepository.save(memberCard);

        // 12. 创建交易记录
        TransactionRecord record = new TransactionRecord();
        record.setMemberCardId(memberCardIdBytes);
        record.setUserId(memberCard.getUserId());
        record.setMerchantId(memberCard.getMerchantId());
        record.setTransactionType(transactionType);
        record.setAmount(amount);
        record.setBalanceSnapshot(null);  // 时效卡不填此字段
        record.setOperatorId(operatorId);
        record.setTransStoreId(storeIdBytes);
        record.setRemark(request.getRemark() + 
                (request.getAdjustType() == 2 ? 
                    String.format("（原：%s，新：%s）", 
                        oldExpireTime.format(DATETIME_FORMATTER), 
                        newExpireTime.format(DATETIME_FORMATTER)) : ""));
        record.setTransactionTime(now);
        transactionRecordRepository.save(record);

        log.info("时效调整成功：transactionId={}, memberCardId={}, adjustType={}, oldExpireTime={}, newExpireTime={}",
                record.getTransactionId(), request.getMemberCardId(), adjustTypeName, oldExpireTime, newExpireTime);

        // 13. 构建响应
        return ExpireAdjustResponse.builder()
                .transactionId(record.getTransactionId())
                .memberCardId(request.getMemberCardId())
                .cardTypeName(cardType.getCardTypeName())
                .oldExpireTime(oldExpireTime.format(DATETIME_FORMATTER))
                .newExpireTime(newExpireTime.format(DATETIME_FORMATTER))
                .adjustType(request.getAdjustType())
                .adjustTypeName(adjustTypeName)
                .operateTime(now.format(DATETIME_FORMATTER))
                .build();
    }

    /**
     * 接口3：会员卡消费（幂等性接口）
     */
    @Override
    @Transactional
    public ConsumeResponse consume(ConsumeRequest request, String token) {
        log.info("会员卡消费请求：memberCardId={}, storeId={}, amount={}", 
                request.getMemberCardId(), request.getStoreId(), request.getAmount());

        // 1. 解析令牌
        Claims claims = jwtUtils.parseToken(token);
        Integer tokenType = claims.get("token_type", Integer.class);
        String operatorIdStr = claims.get("user_id", String.class);
        byte[] operatorId = encryptUtils.uuidToBytes(operatorIdStr);

        byte[] storeIdBytes = encryptUtils.uuidToBytes(request.getStoreId());
        byte[] memberCardIdBytes = encryptUtils.uuidToBytes(request.getMemberCardId());

        // 2. 令牌与权限验证（消费无需额外权限检查）
        if (tokenType == 3) {
            // 工作令牌：验证工作店铺
            String tokenStoreId = claims.get("store_id", String.class);
            if (!request.getStoreId().equals(tokenStoreId)) {
                throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权在该店铺操作");
            }
        } else if (tokenType == 1) {
            // 普通令牌且商家：验证店铺归属
            Integer userType = claims.get("user_type", Integer.class);
            if (userType != null && userType == 2) {
                Store store = storeRepository.findById(storeIdBytes)
                        .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));
                String merchantIdStr = claims.get("merchant_id", String.class);
                byte[] tokenMerchantId = encryptUtils.uuidToBytes(merchantIdStr);
                if (!Arrays.equals(store.getMerchantId(), tokenMerchantId)) {
                    throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权操作该店铺");
                }
            } else {
                throw new BusinessException(ErrorCode.PERMISSION_DENIED, "普通用户无权操作消费");
            }
        } else {
            throw new BusinessException(ErrorCode.TOKEN_PERMISSION_INSUFFICIENT);
        }

        // 3. 验证店铺存在
        Store store = storeRepository.findById(storeIdBytes)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        // 4. 使用悲观锁查询会员卡
        MemberCard memberCard = memberCardRepository.findByIdForUpdate(memberCardIdBytes)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_CARD_NOT_FOUND));

        // 5. 验证会员卡状态（仅正常状态可消费）
        if (memberCard.getStatus() != 1) {
            throw new BusinessException(ErrorCode.MEMBER_CARD_STATUS_INVALID, "会员卡状态异常，无法消费");
        }

        // 6. 验证卡种类型（必须为余额卡或次数卡）
        if (memberCard.getCardTtype() != 1 && memberCard.getCardTtype() != 2) {
            throw new BusinessException(ErrorCode.CARD_TYPE_NOT_SUPPORT_OPERATION, "该卡种不支持消费操作");
        }

        // 6.5. 验证消费类型与卡种是否匹配
        if (request.getConsumeType() == 1 && memberCard.getCardTtype() != 1) {
            throw new BusinessException(ErrorCode.CONSUME_TYPE_MISMATCH, "消费类型为金额，但该卡种不是余额卡");
        }
        if (request.getConsumeType() == 2 && memberCard.getCardTtype() != 2) {
            throw new BusinessException(ErrorCode.CONSUME_TYPE_MISMATCH, "消费类型为次数，但该卡种不是次数卡");
        }

        // 7. 验证店铺归属（本店卡或跨店卡）
        boolean isLocalCard = Arrays.equals(memberCard.getStoreId(), storeIdBytes);
        boolean isCrossStoreCard = false;
        if (!isLocalCard) {
            if (Arrays.equals(memberCard.getMerchantId(), store.getMerchantId())) {
                MemberCardType cardType = memberCardTypeRepository.findById(memberCard.getCardTypeId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.CARD_TYPE_NOT_FOUND));
                if (cardType.getCrossStore() == 1) {
                    isCrossStoreCard = true;
                }
            }
        }
        if (!isLocalCard && !isCrossStoreCard) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权操作该会员卡");
        }

        // 8. 消费金额校验
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "消费金额必须大于0");
        }
        if (request.getAmount().compareTo(MAX_AMOUNT) > 0) {
            throw new BusinessException(ErrorCode.CONSUME_AMOUNT_EXCEEDED, "消费金额超过限制");
        }

        // 9. 余额/次数校验（不允许透支，根据消费类型）
        BigDecimal newBalance;
        if (request.getConsumeType() == 1) {
            // 消费类型为金额：检查余额是否充足
            if (memberCard.getBalance().compareTo(request.getAmount()) < 0) {
                throw new BusinessException(ErrorCode.BALANCE_INSUFFICIENT, "余额不足，无法消费");
            }
            newBalance = memberCard.getBalance().subtract(request.getAmount());
            memberCard.setBalance(newBalance);
        } else {
            // 消费类型为次数：检查次数是否充足
            int times = request.getAmount().intValue();
            if (memberCard.getTimes() < times) {
                throw new BusinessException(ErrorCode.TIMES_INSUFFICIENT, "次数不足，无法消费");
            }
            int newTimes = memberCard.getTimes() - times;
            memberCard.setTimes(newTimes);
            newBalance = new BigDecimal(newTimes);
        }

        // 10. 获取卡种信息
        MemberCardType cardType = memberCardTypeRepository.findById(memberCard.getCardTypeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_TYPE_NOT_FOUND));

        // 11. 处理积分增加逻辑
        Integer pointsToAdd = calculatePointsToAdd(request, memberCard);
        Integer earnedPoints = 0;
        
        if (pointsToAdd != null && pointsToAdd > 0) {
            // 更新会员卡积分
            int newPoints = memberCard.getPoints() + pointsToAdd;
            
            // 积分上限检查
            if (newPoints > 10000) {
                throw new BusinessException(ErrorCode.POINTS_OVERFLOW, "积分超过上限（10000）");
            }
            
            memberCard.setPoints(newPoints);
            earnedPoints = pointsToAdd;
            
            // 创建积分记录
            PointsRecord pointsRecord = new PointsRecord();
            pointsRecord.setMemberCardId(memberCardIdBytes);
            pointsRecord.setUserId(memberCard.getUserId());
            pointsRecord.setMerchantId(memberCard.getMerchantId());
            pointsRecord.setTransStoreId(storeIdBytes);
            pointsRecord.setPointsChange(pointsToAdd);
            pointsRecord.setPointsSnapshot(newPoints);
            pointsRecord.setOperatorId(operatorId);
            pointsRecord.setRemark("消费获得积分");
            pointsRecord.setCreateTime(LocalDateTime.now());
            pointsRecordRepository.save(pointsRecord);
            
            log.info("消费获得积分：memberCardId={}, earnedPoints={}, totalPoints={}", 
                    request.getMemberCardId(), pointsToAdd, newPoints);
        }

        // 12. 更新会员卡
        memberCardRepository.save(memberCard);

        // 13. 创建交易记录
        LocalDateTime now = LocalDateTime.now();
        TransactionRecord record = new TransactionRecord();
        record.setMemberCardId(memberCardIdBytes);
        record.setUserId(memberCard.getUserId());
        record.setMerchantId(memberCard.getMerchantId());
        record.setTransactionType(2);  // 2-消费
        record.setAmount(request.getAmount());
        record.setBalanceSnapshot(newBalance);
        record.setOperatorId(operatorId);
        record.setTransStoreId(storeIdBytes);
        record.setRemark(request.getRemark());
        record.setTransactionTime(now);
        transactionRecordRepository.save(record);

        log.info("会员卡消费成功：transactionId={}, memberCardId={}, amount={}, newBalance={}, earnedPoints={}",
                record.getTransactionId(), request.getMemberCardId(), request.getAmount(), newBalance, earnedPoints);

        // 14. 构建响应
        return ConsumeResponse.builder()
                .transactionId(record.getTransactionId())
                .memberCardId(request.getMemberCardId())
                .cardTypeName(cardType.getCardTypeName())
                .cardTtype(memberCard.getCardTtype())
                .consumeAmount(request.getAmount())
                .balanceSnapshot(newBalance)
                .transactionTime(now.format(DATETIME_FORMATTER))
                .earnedPoints(earnedPoints)
                .build();
    }

    /**
     * 接口4：单卡交易记录查询
     */
    @Override
    public CardRecordsQueryResponse queryCardRecords(CardRecordsQueryRequest request, String token) {
        log.info("单卡交易记录查询：memberCardId={}, pageNum={}, pageSize={}",
                request.getMemberCardId(), request.getPageNum(), request.getPageSize());

        // 1. 解析令牌
        Claims claims = jwtUtils.parseToken(token);
        Integer tokenType = claims.get("token_type", Integer.class);
        String userIdStr = claims.get("user_id", String.class);
        Integer userType = claims.get("user_type", Integer.class);

        byte[] memberCardIdBytes = encryptUtils.uuidToBytes(request.getMemberCardId());

        // 2. 查询会员卡信息
        MemberCard memberCard = memberCardRepository.findById(memberCardIdBytes)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_CARD_NOT_FOUND));

        // 3. 权限验证
        if (tokenType == 3) {
            // 工作令牌：验证会员卡归属于工作店铺或为跨店卡
            String tokenStoreId = claims.get("store_id", String.class);
            String cardStoreId = encryptUtils.bytesToUuid(memberCard.getStoreId());
            if (!tokenStoreId.equals(cardStoreId)) {
                throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权查询该会员卡");
            }
        } else if (tokenType == 1) {
            // 普通令牌
            if (userType != null && userType == 2) {
                // 商家：验证会员卡归属于该商家
                byte[] userIdBytes = encryptUtils.uuidToBytes(userIdStr);
                if (!Arrays.equals(memberCard.getMerchantId(), userIdBytes)) {
                    throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权查询该会员卡");
                }
            } else {
                // 普通用户：验证card.user_id = token.user_id
                byte[] userIdBytes = encryptUtils.uuidToBytes(userIdStr);
                if (!Arrays.equals(memberCard.getUserId(), userIdBytes)) {
                    throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权查询该会员卡");
                }
            }
        } else {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "令牌类型错误");
        }

        // 4. 构建查询条件
        Specification<TransactionRecord> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 会员卡ID
            predicates.add(cb.equal(root.get("memberCardId"), memberCardIdBytes));
            
            // 交易类型筛选
            if (request.getTransactionType() != null) {
                predicates.add(cb.equal(root.get("transactionType"), request.getTransactionType()));
            }
            
            // 日期范围筛选
            if (request.getStartDate() != null && !request.getStartDate().isEmpty()) {
                LocalDateTime startDateTime = LocalDate.parse(request.getStartDate()).atStartOfDay();
                predicates.add(cb.greaterThanOrEqualTo(root.get("transactionTime"), startDateTime));
            }
            if (request.getEndDate() != null && !request.getEndDate().isEmpty()) {
                LocalDateTime endDateTime = LocalDate.parse(request.getEndDate()).atTime(LocalTime.MAX);
                predicates.add(cb.lessThanOrEqualTo(root.get("transactionTime"), endDateTime));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 5. 分页查询（按交易时间降序）
        PageRequest pageRequest = PageRequest.of(
                request.getPageNum() - 1,
                request.getPageSize(),
                Sort.by(Sort.Direction.DESC, "transactionTime")
        );
        Page<TransactionRecord> page = transactionRecordRepository.findAll(spec, pageRequest);

        // 6. 查询卡种信息
        MemberCardType cardType = memberCardTypeRepository.findById(memberCard.getCardTypeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_TYPE_NOT_FOUND));

        // 7. 构建会员卡信息
        CardRecordsQueryResponse.CardInfo cardInfo = CardRecordsQueryResponse.CardInfo.builder()
                .memberCardId(request.getMemberCardId())
                .cardTypeName(cardType.getCardTypeName())
                .cardTtype(memberCard.getCardTtype())
                .currentBalance(memberCard.getCardTtype() == 1 ? memberCard.getBalance() : new BigDecimal(memberCard.getTimes()))
                .build();

        // 8. 转换交易记录列表
        List<TransactionRecordVO> voList = page.getContent().stream().map(record -> {
            TransactionRecordVO vo = new TransactionRecordVO();
            vo.setTransactionId(record.getTransactionId());
            vo.setTransactionType(record.getTransactionType());
            vo.setTransactionTypeName(getTransactionTypeName(record.getTransactionType()));
            vo.setAmount(record.getAmount());
            vo.setBalanceSnapshot(record.getBalanceSnapshot());
            vo.setRemark(record.getRemark());
            vo.setTransactionTime(record.getTransactionTime().format(DATETIME_FORMATTER));
            
            // 查询操作人姓名
            User operator = userRepository.findById(record.getOperatorId()).orElse(null);
            vo.setOperatorName(operator != null ? operator.getNickname() : "未知");
            
            // 查询店铺名称
            Store store = storeRepository.findById(record.getTransStoreId()).orElse(null);
            vo.setStoreName(store != null ? store.getStoreName() : "未知");
            
            return vo;
        }).collect(Collectors.toList());

        // 9. 构建响应
        return CardRecordsQueryResponse.builder()
                .cardInfo(cardInfo)
                .total(page.getTotalElements())
                .pageNum(request.getPageNum())
                .pageSize(request.getPageSize())
                .list(voList)
                .build();
    }

    /**
     * 接口5：个人交易记录查询
     */
    @Override
    public MyRecordsQueryResponse queryMyRecords(MyRecordsQueryRequest request, String token) {
        log.info("个人交易记录查询：pageNum={}, pageSize={}", request.getPageNum(), request.getPageSize());

        // 1. 验证普通令牌
        Claims claims = jwtUtils.parseToken(token);
        Integer tokenType = claims.get("token_type", Integer.class);
        if (tokenType != 1) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "需要普通令牌");
        }

        String userIdStr = claims.get("user_id", String.class);
        byte[] userIdBytes = encryptUtils.uuidToBytes(userIdStr);

        // 2. 构建查询条件
        Specification<TransactionRecord> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 用户ID
            predicates.add(cb.equal(root.get("userId"), userIdBytes));
            
            // 交易类型：仅充值和消费
            if (request.getTransactionType() != null) {
                predicates.add(cb.equal(root.get("transactionType"), request.getTransactionType()));
            } else {
                predicates.add(cb.in(root.get("transactionType")).value(Arrays.asList(1, 2)));
            }
            
            // 日期范围筛选
            if (request.getStartDate() != null && !request.getStartDate().isEmpty()) {
                LocalDateTime startDateTime = LocalDate.parse(request.getStartDate()).atStartOfDay();
                predicates.add(cb.greaterThanOrEqualTo(root.get("transactionTime"), startDateTime));
            }
            if (request.getEndDate() != null && !request.getEndDate().isEmpty()) {
                LocalDateTime endDateTime = LocalDate.parse(request.getEndDate()).atTime(LocalTime.MAX);
                predicates.add(cb.lessThanOrEqualTo(root.get("transactionTime"), endDateTime));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 3. 分页查询（按交易时间降序）
        PageRequest pageRequest = PageRequest.of(
                request.getPageNum() - 1,
                request.getPageSize(),
                Sort.by(Sort.Direction.DESC, "transactionTime")
        );
        Page<TransactionRecord> page = transactionRecordRepository.findAll(spec, pageRequest);

        // 4. 转换交易记录列表
        List<TransactionRecordVO> voList = page.getContent().stream().map(record -> {
            TransactionRecordVO vo = new TransactionRecordVO();
            vo.setTransactionId(record.getTransactionId());
            vo.setMemberCardId(encryptUtils.bytesToUuid(record.getMemberCardId()));
            vo.setTransactionType(record.getTransactionType());
            vo.setTransactionTypeName(getTransactionTypeName(record.getTransactionType()));
            vo.setAmount(record.getAmount());
            vo.setTransactionTime(record.getTransactionTime().format(DATETIME_FORMATTER));
            
            // 查询卡种名称
            MemberCard memberCard = memberCardRepository.findById(record.getMemberCardId()).orElse(null);
            if (memberCard != null) {
                MemberCardType cardType = memberCardTypeRepository.findById(memberCard.getCardTypeId()).orElse(null);
                vo.setCardTypeName(cardType != null ? cardType.getCardTypeName() : "未知");
            } else {
                vo.setCardTypeName("未知");
            }
            
            // 查询店铺名称
            Store store = storeRepository.findById(record.getTransStoreId()).orElse(null);
            vo.setStoreName(store != null ? store.getStoreName() : "未知");
            
            return vo;
        }).collect(Collectors.toList());

        // 5. 构建响应
        return MyRecordsQueryResponse.builder()
                .total(page.getTotalElements())
                .pageNum(request.getPageNum())
                .pageSize(request.getPageSize())
                .list(voList)
                .build();
    }

    /**
     * 接口6：店铺交易统计
     */
    @Override
    public StoreStatisticsResponse queryStoreStatistics(StoreStatisticsRequest request, String token) {
        log.info("店铺交易统计：storeId={}, startDate={}, endDate={}",
                request.getStoreId(), request.getStartDate(), request.getEndDate());

        // 1. 解析令牌
        Claims claims = jwtUtils.parseToken(token);
        Integer tokenType = claims.get("token_type", Integer.class);
        String userIdStr = claims.get("user_id", String.class);

        byte[] storeIdBytes = encryptUtils.uuidToBytes(request.getStoreId());

        // 2. 查询店铺信息
        Store store = storeRepository.findById(storeIdBytes)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_EXIST));

        // 3. 权限验证
        if (tokenType == 3) {
            // 工作令牌：验证token.store_id = storeId
            String tokenStoreId = claims.get("store_id", String.class);
            if (!request.getStoreId().equals(tokenStoreId)) {
                throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权查询该店铺");
            }
            
            // 验证角色：店员无权查询统计
            String role = claims.get("role", String.class);
            if ("STAFF".equals(role)) {
                throw new BusinessException(ErrorCode.PERMISSION_DENIED, "店员无权查询统计数据");
            }
        } else if (tokenType == 1) {
            // 普通令牌（商家）：验证store.merchant_id = token.merchant_id
            byte[] merchantIdBytes = encryptUtils.uuidToBytes(userIdStr);
            if (!Arrays.equals(store.getMerchantId(), merchantIdBytes)) {
                throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权查询该店铺");
            }
        } else {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "令牌类型错误");
        }

        // 4. 日期范围校验（最大90天）
        LocalDate startDate = LocalDate.parse(request.getStartDate());
        LocalDate endDate = LocalDate.parse(request.getEndDate());
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween > 90) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "日期范围不能超过90天");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // 5. 构建查询条件（仅统计余额卡）
        List<TransactionRecord> records = transactionRecordRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 店铺ID
            predicates.add(cb.equal(root.get("transStoreId"), storeIdBytes));
            
            // 时间范围
            predicates.add(cb.between(root.get("transactionTime"), startDateTime, endDateTime));
            
            // 交易类型筛选（可选）
            if (request.getTransactionType() != null) {
                predicates.add(cb.equal(root.get("transactionType"), request.getTransactionType()));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        });

        // 6. 过滤：仅统计余额卡
        List<TransactionRecord> balanceCardRecords = records.stream()
                .filter(record -> {
                    MemberCard card = memberCardRepository.findById(record.getMemberCardId()).orElse(null);
                    return card != null && card.getCardTtype() == 1;
                })
                .collect(Collectors.toList());

        // 7. 统计充值
        BigDecimal rechargeTotal = balanceCardRecords.stream()
                .filter(r -> r.getTransactionType() == 1)
                .map(TransactionRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int rechargeCount = (int) balanceCardRecords.stream()
                .filter(r -> r.getTransactionType() == 1)
                .count();

        // 8. 统计消费
        BigDecimal consumeTotal = balanceCardRecords.stream()
                .filter(r -> r.getTransactionType() == 2)
                .map(TransactionRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int consumeCount = (int) balanceCardRecords.stream()
                .filter(r -> r.getTransactionType() == 2)
                .count();

        // 9. 统计退款
        BigDecimal refundTotal = balanceCardRecords.stream()
                .filter(r -> r.getTransactionType() == 3)
                .map(r -> r.getAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int refundCount = (int) balanceCardRecords.stream()
                .filter(r -> r.getTransactionType() == 3)
                .count();

        // 10. 构建响应
        return StoreStatisticsResponse.builder()
                .storeInfo(StoreStatisticsResponse.StoreInfo.builder()
                        .storeId(request.getStoreId())
                        .storeName(store.getStoreName())
                        .build())
                .dateRange(StoreStatisticsResponse.DateRange.builder()
                        .startDate(request.getStartDate())
                        .endDate(request.getEndDate())
                        .build())
                .rechargeStats(StoreStatisticsResponse.RechargeStats.builder()
                        .totalAmount(rechargeTotal)
                        .totalCount(rechargeCount)
                        .build())
                .consumeStats(StoreStatisticsResponse.ConsumeStats.builder()
                        .totalAmount(consumeTotal)
                        .totalCount(consumeCount)
                        .build())
                .refundStats(StoreStatisticsResponse.RefundStats.builder()
                        .totalAmount(refundTotal)
                        .totalCount(refundCount)
                        .build())
                .build();
    }

    /**
     * 接口7：商家交易统计
     */
    @Override
    public MerchantStatisticsResponse queryMerchantStatistics(MerchantStatisticsRequest request, String token) {
        log.info("商家交易统计：startDate={}, endDate={}", request.getStartDate(), request.getEndDate());

        // 1. 解析并验证令牌（工作令牌或普通令牌）
        Integer tokenType = jwtUtils.extractTokenType(token);
        
        // 2. 验证令牌类型（支持工作令牌和普通令牌）
        if (tokenType != TokenType.WORK.getCode() && tokenType != TokenType.NORMAL.getCode()) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "需要工作令牌或普通令牌");
        }
        
        // 3. 根据令牌类型进行权限验证
        String merchantIdStr = null;
        
        if (tokenType == TokenType.WORK.getCode()) {
            // 工作令牌：验证角色（仅商家和店长可查询）
            String role = jwtUtils.extractRole(token);
            if (!"MERCHANT".equals(role) && !"MANAGER".equals(role)) {
                throw new BusinessException(ErrorCode.PERMISSION_DENIED, "仅商家和店长可查询商家交易统计");
            }
            merchantIdStr = jwtUtils.extractMerchantId(token);
        } else {
            // 普通令牌：验证是否为商家
            String role = jwtUtils.extractRole(token);
            if (!"MERCHANT".equals(role)) {
                throw new BusinessException(ErrorCode.PERMISSION_DENIED, "非商家用户，无权限操作");
            }
            merchantIdStr = jwtUtils.extractMerchantId(token);
        }
        
        if (merchantIdStr == null) {
            throw new BusinessException(ErrorCode.MERCHANT_NOT_EXIST, "商家信息不存在");
        }
        byte[] merchantIdBytes = encryptUtils.uuidToBytes(merchantIdStr);

        // 2. 查询商家信息
        MerchantExtend merchant = merchantExtendRepository.findById(merchantIdBytes)
                .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_NOT_EXIST));

        // 3. 日期范围校验（最大90天）
        LocalDate startDate = LocalDate.parse(request.getStartDate());
        LocalDate endDate = LocalDate.parse(request.getEndDate());
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween > 90) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "日期范围不能超过90天");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // 4. 构建查询条件
        List<TransactionRecord> records = transactionRecordRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 商家ID
            predicates.add(cb.equal(root.get("merchantId"), merchantIdBytes));
            
            // 时间范围
            predicates.add(cb.between(root.get("transactionTime"), startDateTime, endDateTime));
            
            // 交易类型筛选（可选）
            if (request.getTransactionType() != null) {
                predicates.add(cb.equal(root.get("transactionType"), request.getTransactionType()));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        });

        // 5. 过滤：仅统计余额卡
        List<TransactionRecord> balanceCardRecords = records.stream()
                .filter(record -> {
                    MemberCard card = memberCardRepository.findById(record.getMemberCardId()).orElse(null);
                    return card != null && card.getCardTtype() == 1;
                })
                .collect(Collectors.toList());

        // 6. 统计充值
        BigDecimal rechargeTotal = balanceCardRecords.stream()
                .filter(r -> r.getTransactionType() == 1)
                .map(TransactionRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int rechargeCount = (int) balanceCardRecords.stream()
                .filter(r -> r.getTransactionType() == 1)
                .count();

        // 7. 统计消费
        BigDecimal consumeTotal = balanceCardRecords.stream()
                .filter(r -> r.getTransactionType() == 2)
                .map(TransactionRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int consumeCount = (int) balanceCardRecords.stream()
                .filter(r -> r.getTransactionType() == 2)
                .count();

        // 8. 统计退款
        BigDecimal refundTotal = balanceCardRecords.stream()
                .filter(r -> r.getTransactionType() == 3)
                .map(r -> r.getAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int refundCount = (int) balanceCardRecords.stream()
                .filter(r -> r.getTransactionType() == 3)
                .count();

        // 9. 获取商家名称
        byte[] userIdBytes = merchant.getUserId();
        User merchantUser = userRepository.findById(userIdBytes)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_EXIST));

        // 10. 构建响应
        return MerchantStatisticsResponse.builder()
                .merchantInfo(MerchantStatisticsResponse.MerchantInfo.builder()
                        .merchantId(merchantIdStr)
                        .merchantName(merchantUser.getNickname())
                        .build())
                .dateRange(MerchantStatisticsResponse.DateRange.builder()
                        .startDate(request.getStartDate())
                        .endDate(request.getEndDate())
                        .build())
                .rechargeStats(MerchantStatisticsResponse.RechargeStats.builder()
                        .totalAmount(rechargeTotal)
                        .totalCount(rechargeCount)
                        .build())
                .consumeStats(MerchantStatisticsResponse.ConsumeStats.builder()
                        .totalAmount(consumeTotal)
                        .totalCount(consumeCount)
                        .build())
                .refundStats(MerchantStatisticsResponse.RefundStats.builder()
                        .totalAmount(refundTotal)
                        .totalCount(refundCount)
                        .build())
                .build();
    }

    /**
     * 获取交易类型名称
     */
    private String getTransactionTypeName(Integer type) {
        return switch (type) {
            case 1 -> "充值";
            case 2 -> "消费";
            case 3 -> "退款";
            case 4 -> "延期";
            case 5 -> "日期变动";
            default -> "未知";
        };
    }

    /**
     * 计算本次消费应获得的积分
     * 
     * @param request 消费请求
     * @param memberCard 会员卡信息
     * @return 应增加的积分数（null或0表示不增加积分）
     */
    private Integer calculatePointsToAdd(ConsumeRequest request, MemberCard memberCard) {
        Integer points = request.getPoints();
        
        if (memberCard.getCardTtype() == 1) {
            // 余额卡
            if (points == null) {
                // 不填：按消费金额向下取整
                return request.getAmount().intValue();
            } else if (points == 0) {
                // 填0：不变动
                return null;
            } else {
                // 填其他值：按指定值
                return points;
            }
        } else {
            // 次数卡（cardTtype == 2）
            if (points == null) {
                // 不填：默认为0，不变动
                return null;
            } else if (points == 0) {
                // 填0：不变动
                return null;
            } else {
                // 填其他值：按指定值
                return points;
            }
        }
    }

    /**
     * 接口8：流水数据统计
     */
    @Override
    public TransStatisticsResponse queryTransStatistics(TransStatisticsRequest request, String token) {
        log.info("流水数据统计：storeId={}, merchantId={}, dateRange={}", 
                request.getStoreId(), request.getMerchantId(), request.getDateRange());

        // 1. 参数验证：storeId和merchantId至少填一个
        if (request.getStoreId() == null && request.getMerchantId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "storeId和merchantId至少填写一个");
        }

        // 2. 解析令牌
        Claims claims = jwtUtils.parseToken(token);
        Integer tokenType = claims.get("token_type", Integer.class);
        
        // 3. 验证令牌类型（仅支持工作令牌）
        if (tokenType != 3) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "需要工作令牌");
        }

        // 4. 计算日期范围
        LocalDateTime[] dateRange = calculateDateRange(request.getDateRange());
        LocalDateTime startDateTime = dateRange[0];
        LocalDateTime endDateTime = dateRange[1];

        // 5. 权限验证和数据查询
        TransStatisticsResponse.TransStatisticsResponseBuilder responseBuilder = TransStatisticsResponse.builder();
        
        // 5.1 店铺统计
        if (request.getStoreId() != null) {
            byte[] storeIdBytes = encryptUtils.uuidToBytes(request.getStoreId());
            
            // 验证权限
            validateStorePermission(claims, request.getStoreId(), storeIdBytes);
            
            // 查询店铺信息
            Store store = storeRepository.findById(storeIdBytes)
                    .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));
            
            // 统计店铺数据
            List<TransStatisticsResponse.DailyStatistics> storeStats = 
                    queryStatisticsByStore(storeIdBytes, startDateTime, endDateTime, request.getCardTypeId());
            
            responseBuilder
                    .storeId(request.getStoreId())
                    .storeName(store.getStoreName())
                    .haveStoreStatistics(true)
                    .storeStatistics(storeStats);
        } else {
            responseBuilder.haveStoreStatistics(false);
        }
        
        // 5.2 商家统计
        if (request.getMerchantId() != null) {
            byte[] merchantIdBytes = encryptUtils.uuidToBytes(request.getMerchantId());
            
            // 验证权限
            validateMerchantPermission(claims, request.getMerchantId(), merchantIdBytes);
            
            // 查询商家信息
            MerchantExtend merchant = merchantExtendRepository.findById(merchantIdBytes)
                    .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_NOT_EXIST));
            
            // 统计商家数据
            List<TransStatisticsResponse.DailyStatistics> merchantStats = 
                    queryStatisticsByMerchant(merchantIdBytes, startDateTime, endDateTime, request.getCardTypeId());
            
            responseBuilder
                    .merchantId(request.getMerchantId())
                    .merchantName(merchant.getMerchantName())
                    .haveMerchantStatistics(true)
                    .merchantStatistics(merchantStats);
        } else {
            responseBuilder.haveMerchantStatistics(false);
        }

        return responseBuilder.build();
    }

    /**
     * 计算日期范围
     */
    private LocalDateTime[] calculateDateRange(Integer dateRangeCode) {
        LocalDate today = LocalDate.now();
        LocalDate startDate;
        LocalDate endDate = today;
        
        switch (dateRangeCode) {
            case 0: // 昨日
                startDate = today.minusDays(1);
                endDate = today.minusDays(1);
                break;
            case 1: // 今日
                startDate = today;
                break;
            case 7: // 近7日
                startDate = today.minusDays(6);
                break;
            case 17: // 本周（周一到今天）
                startDate = today.with(java.time.DayOfWeek.MONDAY);
                break;
            case 30: // 近30日
                startDate = today.minusDays(29);
                break;
            case 32: // 本月（1号到今天）
                startDate = today.withDayOfMonth(1);
                break;
            default:
                throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的日期范围代号");
        }
        
        return new LocalDateTime[]{
                startDate.atStartOfDay(),
                endDate.atTime(LocalTime.MAX)
        };
    }

    /**
     * 验证店铺权限
     */
    private void validateStorePermission(Claims claims, String storeId, byte[] storeIdBytes) {
        String tokenStoreId = claims.get("store_id", String.class);
        
        // 如果工作令牌的店铺ID与请求的店铺ID一致，直接通过
        if (storeId.equals(tokenStoreId)) {
            return;
        }
        
        // 否则验证是否为同商家的店铺
        String merchantIdStr = claims.get("merchant_id", String.class);
        if (merchantIdStr != null) {
            byte[] tokenMerchantId = encryptUtils.uuidToBytes(merchantIdStr);
            Store store = storeRepository.findById(storeIdBytes)
                    .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));
            
            if (!Arrays.equals(store.getMerchantId(), tokenMerchantId)) {
                throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权查询该店铺数据");
            }
        } else {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权查询该店铺数据");
        }
    }

    /**
     * 验证商家权限
     */
    private void validateMerchantPermission(Claims claims, String merchantId, byte[] merchantIdBytes) {
        String tokenMerchantId = claims.get("merchant_id", String.class);
        
        if (tokenMerchantId == null || !merchantId.equals(tokenMerchantId)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权查询该商家数据");
        }
        
        // 验证是否为商家角色
        String userIdStr = claims.get("user_id", String.class);
        byte[] userIdBytes = encryptUtils.uuidToBytes(userIdStr);
        User user = userRepository.findById(userIdBytes)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_EXIST));
        
        if (user.getUserType() != 2) {
            throw new BusinessException(ErrorCode.MERCHANT_PERMISSION_DENIED, "非商家用户，无权查询商家统计数据");
        }
    }

    /**
     * 按店铺统计数据
     */
    private List<TransStatisticsResponse.DailyStatistics> queryStatisticsByStore(
            byte[] storeIdBytes, LocalDateTime startDateTime, LocalDateTime endDateTime, Long cardTypeId) {
        
        // 查询交易记录
        Map<LocalDate, List<TransactionRecord>> transRecordsByDate = queryTransactionRecords(
                storeIdBytes, null, startDateTime, endDateTime, cardTypeId, true);
        
        // 查询会员数据
        Map<LocalDate, TransStatisticsResponse.MemberData> memberDataByDate = queryMemberData(
                storeIdBytes, null, startDateTime, endDateTime, true);
        
        // 合并数据
        return buildDailyStatistics(transRecordsByDate, memberDataByDate);
    }

    /**
     * 按商家统计数据
     */
    private List<TransStatisticsResponse.DailyStatistics> queryStatisticsByMerchant(
            byte[] merchantIdBytes, LocalDateTime startDateTime, LocalDateTime endDateTime, Long cardTypeId) {
        
        // 查询交易记录
        Map<LocalDate, List<TransactionRecord>> transRecordsByDate = queryTransactionRecords(
                null, merchantIdBytes, startDateTime, endDateTime, cardTypeId, false);
        
        // 查询会员数据
        Map<LocalDate, TransStatisticsResponse.MemberData> memberDataByDate = queryMemberData(
                null, merchantIdBytes, startDateTime, endDateTime, false);
        
        // 合并数据
        return buildDailyStatistics(transRecordsByDate, memberDataByDate);
    }

    /**
     * 查询交易记录（按日期分组）
     */
    private Map<LocalDate, List<TransactionRecord>> queryTransactionRecords(
            byte[] storeIdBytes, byte[] merchantIdBytes, 
            LocalDateTime startDateTime, LocalDateTime endDateTime, 
            Long cardTypeId, boolean isStoreQuery) {
        
        List<TransactionRecord> records = transactionRecordRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 店铺或商家条件
            if (isStoreQuery) {
                predicates.add(cb.equal(root.get("transStoreId"), storeIdBytes));
            } else {
                predicates.add(cb.equal(root.get("merchantId"), merchantIdBytes));
            }
            
            // 时间范围
            predicates.add(cb.between(root.get("transactionTime"), startDateTime, endDateTime));
            
            // 交易类型：仅统计充值(1)、消费(2)、退款(3)
            predicates.add(root.get("transactionType").in(Arrays.asList(1, 2, 3)));
            
            return cb.and(predicates.toArray(new Predicate[0]));
        });
        
        // 如果有卡种筛选，需要过滤
        if (cardTypeId != null) {
            records = records.stream()
                    .filter(record -> {
                        MemberCard card = memberCardRepository.findById(record.getMemberCardId()).orElse(null);
                        return card != null && card.getCardTypeId().equals(cardTypeId);
                    })
                    .collect(Collectors.toList());
        }
        
        // 按日期分组
        return records.stream()
                .collect(Collectors.groupingBy(
                        record -> record.getTransactionTime().toLocalDate()
                ));
    }

    /**
     * 查询会员数据（按日期分组）
     */
    private Map<LocalDate, TransStatisticsResponse.MemberData> queryMemberData(
            byte[] storeIdBytes, byte[] merchantIdBytes,
            LocalDateTime startDateTime, LocalDateTime endDateTime,
            boolean isStoreQuery) {
        
        // 查询会员卡（使用create_time）
        List<MemberCard> cards = memberCardRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 店铺或商家条件
            if (isStoreQuery) {
                predicates.add(cb.equal(root.get("storeId"), storeIdBytes));
            } else {
                predicates.add(cb.equal(root.get("merchantId"), merchantIdBytes));
            }
            
            // 时间范围（使用create_time）
            predicates.add(cb.between(root.get("createTime"), startDateTime, endDateTime));
            
            return cb.and(predicates.toArray(new Predicate[0]));
        });
        
        // 按日期分组统计
        Map<LocalDate, TransStatisticsResponse.MemberData> result = new HashMap<>();
        
        cards.stream()
                .collect(Collectors.groupingBy(card -> card.getCreateTime().toLocalDate()))
                .forEach((date, cardList) -> {
                    // 新增会员数量：不同的user_id数量（排除NULL）
                    long newMembers = cardList.stream()
                            .filter(card -> card.getUserId() != null)
                            .map(MemberCard::getUserId)
                            .distinct()
                            .count();
                    
                    // 新办会员卡数量
                    int newMemberCards = cardList.size();
                    
                    result.put(date, TransStatisticsResponse.MemberData.builder()
                            .newMembers((int) newMembers)
                            .newMemberCards(newMemberCards)
                            .build());
                });
        
        return result;
    }

    /**
     * 构建每日统计数据
     */
    private List<TransStatisticsResponse.DailyStatistics> buildDailyStatistics(
            Map<LocalDate, List<TransactionRecord>> transRecordsByDate,
            Map<LocalDate, TransStatisticsResponse.MemberData> memberDataByDate) {
        
        // 合并所有日期
        Set<LocalDate> allDates = new HashSet<>();
        allDates.addAll(transRecordsByDate.keySet());
        allDates.addAll(memberDataByDate.keySet());
        
        // 强制添加今天和昨天
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        allDates.add(today);
        allDates.add(yesterday);
        
        // 检查是否有余额卡和次数卡数据（全局标记）
        boolean hasAnyBalanceCard = false;
        boolean hasAnyCountCard = false;
        
        for (List<TransactionRecord> records : transRecordsByDate.values()) {
            for (TransactionRecord record : records) {
                MemberCard card = memberCardRepository.findById(record.getMemberCardId()).orElse(null);
                if (card != null) {
                    if (card.getCardTtype() == 1) hasAnyBalanceCard = true;
                    if (card.getCardTtype() == 2) hasAnyCountCard = true;
                }
                if (hasAnyBalanceCard && hasAnyCountCard) break;
            }
            if (hasAnyBalanceCard && hasAnyCountCard) break;
        }
        
        // 创建零数据对象
        final TransStatisticsResponse.BalanceCardData zeroBalanceData = hasAnyBalanceCard ? 
                TransStatisticsResponse.BalanceCardData.builder()
                        .consumeAmount(BigDecimal.ZERO)
                        .consumeCount(0)
                        .rechargeAmount(BigDecimal.ZERO)
                        .rechargeCount(0)
                        .refundAmount(BigDecimal.ZERO)
                        .refundCount(0)
                        .build() : null;
        
        final TransStatisticsResponse.CountCardData zeroCountData = hasAnyCountCard ?
                TransStatisticsResponse.CountCardData.builder()
                        .consumeTimes(0)
                        .rechargeTimes(0)
                        .build() : null;
        
        final boolean finalHasAnyBalanceCard = hasAnyBalanceCard;
        final boolean finalHasAnyCountCard = hasAnyCountCard;
        
        // 构建每日统计
        return allDates.stream()
                .sorted(Comparator.reverseOrder()) // 降序排列（今天在前）
                .map(date -> {
                    List<TransactionRecord> records = transRecordsByDate.getOrDefault(date, Collections.emptyList());
                    TransStatisticsResponse.MemberData memberData = memberDataByDate.get(date);
                    
                    // 分离余额卡和次数卡记录
                    List<TransactionRecord> balanceCardRecords = records.stream()
                            .filter(r -> {
                                MemberCard card = memberCardRepository.findById(r.getMemberCardId()).orElse(null);
                                return card != null && card.getCardTtype() == 1;
                            })
                            .collect(Collectors.toList());
                    
                    List<TransactionRecord> countCardRecords = records.stream()
                            .filter(r -> {
                                MemberCard card = memberCardRepository.findById(r.getMemberCardId()).orElse(null);
                                return card != null && card.getCardTtype() == 2;
                            })
                            .collect(Collectors.toList());
                    
                    // 构建余额卡数据（如果全局有余额卡，则必须返回数据，没有记录则返回零数据）
                    TransStatisticsResponse.BalanceCardData balanceCardData = null;
                    if (finalHasAnyBalanceCard) {
                        balanceCardData = !balanceCardRecords.isEmpty() ? 
                                buildBalanceCardData(balanceCardRecords) : zeroBalanceData;
                    }
                    
                    // 构建次数卡数据（如果全局有次数卡，则必须返回数据，没有记录则返回零数据）
                    TransStatisticsResponse.CountCardData countCardData = null;
                    if (finalHasAnyCountCard) {
                        countCardData = !countCardRecords.isEmpty() ? 
                                buildCountCardData(countCardRecords) : zeroCountData;
                    }
                    
                    // 如果没有会员数据，设置为0
                    if (memberData == null) {
                        memberData = TransStatisticsResponse.MemberData.builder()
                                .newMembers(0)
                                .newMemberCards(0)
                                .build();
                    }
                    
                    return TransStatisticsResponse.DailyStatistics.builder()
                            .date(date.toString())
                            .haveBalanceCard(finalHasAnyBalanceCard)
                            .haveCountCard(finalHasAnyCountCard)
                            .balanceCardData(balanceCardData)
                            .countCardData(countCardData)
                            .memberData(memberData)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 构建余额卡数据
     */
    private TransStatisticsResponse.BalanceCardData buildBalanceCardData(List<TransactionRecord> records) {
        // 消费统计
        BigDecimal consumeAmount = records.stream()
                .filter(r -> r.getTransactionType() == 2)
                .map(TransactionRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int consumeCount = (int) records.stream()
                .filter(r -> r.getTransactionType() == 2)
                .count();
        
        // 充值统计
        BigDecimal rechargeAmount = records.stream()
                .filter(r -> r.getTransactionType() == 1)
                .map(TransactionRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int rechargeCount = (int) records.stream()
                .filter(r -> r.getTransactionType() == 1)
                .count();
        
        // 退款统计
        BigDecimal refundAmount = records.stream()
                .filter(r -> r.getTransactionType() == 3)
                .map(r -> r.getAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int refundCount = (int) records.stream()
                .filter(r -> r.getTransactionType() == 3)
                .count();
        
        return TransStatisticsResponse.BalanceCardData.builder()
                .consumeAmount(consumeAmount)
                .consumeCount(consumeCount)
                .rechargeAmount(rechargeAmount)
                .rechargeCount(rechargeCount)
                .refundAmount(refundAmount)
                .refundCount(refundCount)
                .build();
    }

    /**
     * 构建次数卡数据
     */
    private TransStatisticsResponse.CountCardData buildCountCardData(List<TransactionRecord> records) {
        // 消费统计
        int consumeTimes = records.stream()
                .filter(r -> r.getTransactionType() == 2)
                .mapToInt(r -> r.getAmount().intValue())
                .sum();
        
        // 充值统计
        int rechargeTimes = records.stream()
                .filter(r -> r.getTransactionType() == 1)
                .mapToInt(r -> r.getAmount().intValue())
                .sum();
        
        return TransStatisticsResponse.CountCardData.builder()
                .consumeTimes(consumeTimes)
                .rechargeTimes(rechargeTimes)
                .build();
    }
}

