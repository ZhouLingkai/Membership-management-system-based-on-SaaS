package com.ecards.member_management.service;

import com.ecards.member_management.common.ErrorCode;
import com.ecards.member_management.dto.request.PointsAdjustRequest;
import com.ecards.member_management.dto.request.PointsRecordsQueryRequest;
import com.ecards.member_management.dto.response.PointsAdjustResponse;
import com.ecards.member_management.dto.response.PointsRecordListResponse;
import com.ecards.member_management.dto.response.PointsRecordVO;
import com.ecards.member_management.entity.*;
import com.ecards.member_management.exception.BusinessException;
import com.ecards.member_management.repository.*;
import com.ecards.member_management.utils.EncryptUtils;
import com.ecards.member_management.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 积分管理服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointsServiceImpl implements PointsService {

    private final JwtUtils jwtUtils;
    private final EncryptUtils encryptUtils;
    private final MemberCardRepository memberCardRepository;
    private final PointsRecordRepository pointsRecordRepository;
    private final StoreRepository storeRepository;
    private final MerchantExtendRepository merchantExtendRepository;
    private final MemberCardTypeRepository memberCardTypeRepository;

    /**
     * 接口1：积分变动
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PointsAdjustResponse adjustPoints(PointsAdjustRequest request, String token) {
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
                throw new BusinessException(ErrorCode.PERMISSION_DENIED, "普通用户无权操作积分");
            }
        } else {
            throw new BusinessException(ErrorCode.TOKEN_PERMISSION_INSUFFICIENT);
        }

        // 3. 验证店铺存在
        Store store = storeRepository.findById(storeIdBytes)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        // 4. 验证会员卡存在
        MemberCard memberCard = memberCardRepository.findByMemberCardId(memberCardIdBytes)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_CARD_NOT_FOUND));

        // 5. 验证会员卡状态（正常或未激活）
        if (memberCard.getStatus() != 0 && memberCard.getStatus() != 1) {
            throw new BusinessException(ErrorCode.MEMBER_CARD_STATUS_INVALID, "会员卡状态异常，无法操作");
        }

        // 6. 验证商家认证状态
        MerchantExtend merchant = merchantExtendRepository.findById(store.getMerchantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_NOT_FOUND));
        if (merchant.getCertification() != 1 && merchant.getCertification() != 2) {
            throw new BusinessException(ErrorCode.MERCHANT_NOT_AUTHENTICATED, "商家未认证，无法操作");
        }

        // 7. 验证店铺归属（本店卡或跨店卡）
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

        // 8. 积分变动值校验
        if (request.getPointsChange() == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "积分变动值不能为0");
        }

        // 9. 积分余额校验
        int newPoints = memberCard.getPoints() + request.getPointsChange();
        if (newPoints < 0) {
            throw new BusinessException(ErrorCode.POINTS_INSUFFICIENT, "积分不足，无法扣减");
        }
        if (newPoints > Integer.MAX_VALUE) {
            throw new BusinessException(ErrorCode.POINTS_OVERFLOW, "积分变动超过限制");
        }

        // 10. 更新会员卡积分和累积积分
        memberCard.setPoints(newPoints);
        
        // 如果是增加积分，累积到cumulative_points
        if (request.getPointsChange() > 0) {
            int newCumulativePoints = memberCard.getCumulativePoints() + request.getPointsChange();
            memberCard.setCumulativePoints(newCumulativePoints);
        }
        
        memberCardRepository.save(memberCard);

        // 11. 创建积分记录
        PointsRecord record = new PointsRecord();
        record.setMemberCardId(memberCardIdBytes);
        record.setUserId(memberCard.getUserId());  // 可能为NULL
        record.setMerchantId(memberCard.getMerchantId());
        record.setTransStoreId(storeIdBytes);
        record.setPointsChange(request.getPointsChange());
        record.setPointsSnapshot(newPoints);
        record.setOperatorId(operatorId);
        record.setRemark(request.getRemark());
        record.setCreateTime(LocalDateTime.now());
        pointsRecordRepository.save(record);

        // 12. 构建响应
        return PointsAdjustResponse.builder()
                .memberCardId(request.getMemberCardId())
                .pointsChange(request.getPointsChange())
                .pointsSnapshot(newPoints)
                .operateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .build();
    }

    /**
     * 接口2：积分记录查询
     */
    @Override
    public PointsRecordListResponse queryPointsRecords(PointsRecordsQueryRequest request, String token) {
        // 1. 解析令牌
        Claims claims = jwtUtils.parseToken(token);
        Integer tokenType = claims.get("token_type", Integer.class);
        String userIdStr = claims.get("user_id", String.class);
        byte[] tokenUserId = encryptUtils.uuidToBytes(userIdStr);

        byte[] memberCardIdBytes = null;
        MemberCard memberCard = null;

        // 2. 令牌验证与查询方式
        if (tokenType == 3) {
            // 工作令牌：必须提供storeId，通过memberCardId或memberPhone查询
            if (request.getStoreId() == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "工作令牌必须提供店铺ID");
            }
            byte[] storeIdBytes = encryptUtils.uuidToBytes(request.getStoreId());
            
            // 验证工作店铺
            String tokenStoreId = claims.get("store_id", String.class);
            if (!request.getStoreId().equals(tokenStoreId)) {
                throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权查询该店铺");
            }

            Store store = storeRepository.findById(storeIdBytes)
                    .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

            // 根据memberCardId或memberPhone查询
            if (request.getMemberCardId() != null) {
                memberCardIdBytes = encryptUtils.uuidToBytes(request.getMemberCardId());
                memberCard = memberCardRepository.findByMemberCardId(memberCardIdBytes)
                        .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_CARD_NOT_FOUND));
            } else if (request.getMemberPhone() != null) {
                // 解密手机号
                String decryptedPhone;
                try {
                    decryptedPhone = encryptUtils.decryptAES(request.getMemberPhone());
                } catch (Exception e) {
                    // 如果解密失败，说明可能是明文（用于测试）
                    decryptedPhone = request.getMemberPhone();
                }

                // 查询该手机号在该店铺的会员卡（本店卡+跨店卡）
                List<MemberCard> localCards = memberCardRepository.findByStoreIdAndMemberPhone(storeIdBytes, decryptedPhone);
                List<MemberCard> crossCards = memberCardRepository.findCrossStoreCardsByPhone(
                        decryptedPhone, store.getMerchantId(), storeIdBytes, PageRequest.of(0, 500));
                
                List<MemberCard> allCards = new java.util.ArrayList<>(localCards);
                allCards.addAll(crossCards);

                if (allCards.isEmpty()) {
                    throw new BusinessException(ErrorCode.MEMBER_CARD_NOT_FOUND, "该手机号无会员卡");
                }
                if (allCards.size() > 1) {
                    throw new BusinessException(ErrorCode.MULTIPLE_CARDS_FOUND, "该手机号有多张会员卡，请使用会员卡ID查询");
                }
                memberCard = allCards.get(0);
                memberCardIdBytes = memberCard.getMemberCardId();
            } else {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "必须提供会员卡ID或手机号");
            }

            // 验证会员卡归属（本店卡或跨店卡）
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
                throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权查询该会员卡");
            }

        } else if (tokenType == 1) {
            // 普通令牌
            Integer userType = claims.get("user_type", Integer.class);
            
            if (userType != null && userType == 2) {
                // 商家：可查询名下所有会员卡
                if (request.getMemberCardId() == null) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "必须提供会员卡ID");
                }
                memberCardIdBytes = encryptUtils.uuidToBytes(request.getMemberCardId());
                memberCard = memberCardRepository.findByMemberCardId(memberCardIdBytes)
                        .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_CARD_NOT_FOUND));
                
                String merchantIdStr = claims.get("merchant_id", String.class);
                byte[] tokenMerchantId = encryptUtils.uuidToBytes(merchantIdStr);
                if (!Arrays.equals(memberCard.getMerchantId(), tokenMerchantId)) {
                    throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权查询该会员卡");
                }
            } else {
                // 普通用户：仅能查询自己的积分记录
                if (request.getMemberCardId() != null) {
                    memberCardIdBytes = encryptUtils.uuidToBytes(request.getMemberCardId());
                    memberCard = memberCardRepository.findByMemberCardId(memberCardIdBytes)
                            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_CARD_NOT_FOUND));
                    
                    // 验证是本人的卡
                    if (!Arrays.equals(memberCard.getUserId(), tokenUserId)) {
                        throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权查询该会员卡");
                    }
                } else {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "必须提供会员卡ID");
                }
            }
        } else {
            throw new BusinessException(ErrorCode.TOKEN_PERMISSION_INSUFFICIENT);
        }

        // 3. 解析日期范围
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;
        if (request.getStartDate() != null) {
            startDate = LocalDate.parse(request.getStartDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    .atStartOfDay();
        }
        if (request.getEndDate() != null) {
            endDate = LocalDate.parse(request.getEndDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    .atTime(LocalTime.MAX);
        }

        // 4. 查询积分记录
        Pageable pageable = PageRequest.of(request.getPageNum() - 1, request.getPageSize(), 
                Sort.by(Sort.Direction.DESC, "createTime"));
        Page<PointsRecord> recordPage = pointsRecordRepository.findByMemberCardIdAndDateRange(
                memberCardIdBytes, startDate, endDate, pageable);

        // 5. 构建响应
        List<PointsRecordVO> voList = recordPage.getContent().stream().map(record -> {
            // 填充冗余字段
            record.fillRedundantFields();
            
            return PointsRecordVO.builder()
                    .pointsRecordId(record.getPointsRecordId())
                    .pointsChange(record.getPointsChange())
                    .pointsSnapshot(record.getPointsSnapshot())
                    .operatorName(record.getOperatorName())
                    .storeName(record.getStoreName())
                    .remark(record.getRemark())
                    .createTime(record.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .build();
        }).collect(Collectors.toList());

        return PointsRecordListResponse.builder()
                .currentPoints(memberCard.getPoints())
                .total(recordPage.getTotalElements())
                .pageNum(request.getPageNum())
                .pageSize(request.getPageSize())
                .list(voList)
                .build();
    }
}

