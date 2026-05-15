package com.ecards.member_management.service;

import com.ecards.member_management.common.ErrorCode;
import com.ecards.member_management.dto.request.MerchantInfoUpdateRequest;
import com.ecards.member_management.dto.request.MerchantRegistrationRequest;
import com.ecards.member_management.dto.request.QualificationSubmitRequest;
import com.ecards.member_management.dto.request.SecondaryPasswordUpdateRequest;
import com.ecards.member_management.dto.response.MerchantInfoResponse;
import com.ecards.member_management.dto.response.MerchantInfoUpdateResponse;
import com.ecards.member_management.dto.response.MerchantRegistrationResponse;
import com.ecards.member_management.dto.response.QualificationSubmitResponse;
import com.ecards.member_management.dto.response.SecondaryPasswordUpdateResponse;
import com.ecards.member_management.entity.MerchantAuditRecord;
import com.ecards.member_management.entity.MerchantExtend;
import com.ecards.member_management.entity.Store;
import com.ecards.member_management.entity.User;
import com.ecards.member_management.exception.BusinessException;
import com.ecards.member_management.repository.MerchantAuditRecordRepository;
import com.ecards.member_management.repository.MerchantExtendRepository;
import com.ecards.member_management.repository.StoreRepository;
import com.ecards.member_management.repository.UserRepository;
import com.ecards.member_management.utils.EncryptUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * 商户服务
 * 提供商户注册、信息查询、信息修改、二级密码修改等功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantService {

    private final UserRepository userRepository;
    private final MerchantExtendRepository merchantExtendRepository;
    private final MerchantAuditRecordRepository merchantAuditRecordRepository;
    private final StoreRepository storeRepository;
    private final EncryptUtils encryptUtils;
    private final VerifyCodeService verifyCodeService;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int TEST_PERIOD_DAYS = 7;

    /**
     * 商户注册（升级）
     *
     * @param request 商户注册请求
     * @return 商户注册响应
     */
    @Transactional
    public MerchantRegistrationResponse registerMerchant(MerchantRegistrationRequest request) {
        log.info("商户注册：用户ID={}, 申请方式={}", request.getUserId(), request.getApplicationType());

        // 1. 查询用户信息
        byte[] userIdBytes = encryptUtils.uuidToBytes(request.getUserId());
        User user = userRepository.findById(userIdBytes)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_EXIST.getCode(), "用户不存在"));

        // 2. 校验用户类型（只有普通用户可以申请）
        if (user.getUserType() == 2) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED.getCode(), "该用户已是商户，无需重复申请");
        }

        // 3. 检查是否已存在商户记录（已是商户的用户不能再提交任何申请）
        if (merchantExtendRepository.existsByUserId(userIdBytes)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED.getCode(), "该用户已申请过商户，无法再次申请");
        }

        // 4. 检查是否有待审核或已通过的审核记录（防止直接认证通道重复提交）
        List<MerchantAuditRecord> existingAudits = merchantAuditRecordRepository.findByUserIdOrderByCreateTimeDesc(userIdBytes);
        if (existingAudits != null && !existingAudits.isEmpty()) {
            for (MerchantAuditRecord audit : existingAudits) {
                // 如果存在待审核(0)或已通过(1)的记录，拒绝提交
                if (audit.getAuditStatus() == 0) {
                    throw new BusinessException(ErrorCode.PERMISSION_DENIED.getCode(),
                            "您已提交过商户申请且正在审核中，请勿重复申请");
                }
                if (audit.getAuditStatus() == 1) {
                    throw new BusinessException(ErrorCode.PERMISSION_DENIED.getCode(),
                            "您的商户申请已通过，请勿重复申请");
                }
            }
        }

        // 5. 根据申请方式处理
        Integer applicationType = request.getApplicationType();
        if (applicationType == 1) {
            // 免认证通道
            return registerByFreeChannel(user, request);
        } else if (applicationType == 2) {
            // 直接认证通道
            return registerByDirectChannel(user, request);
        } else {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "申请方式只能是1或2");
        }
    }

    /**
     * 免认证通道注册
     *
     * @param user    用户信息
     * @param request 注册请求
     * @return 注册响应
     */
    private MerchantRegistrationResponse registerByFreeChannel(User user, MerchantRegistrationRequest request) {
        // 1. 校验必填参数
        if (request.getMerchantName() == null || request.getMerchantName().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "免认证通道必须填写商户名称");
        }
        if (request.getSndPswd() == null || request.getSndPswd().length() < 8) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "免认证通道必须填写二级密码，且长度至少为8位");
        }

        LocalDateTime now = LocalDateTime.now();
        byte[] merchantId = encryptUtils.uuidToBytes(UUID.randomUUID());

        // 2. 创建商户扩展信息
        MerchantExtend merchantExtend = new MerchantExtend();
        merchantExtend.setMerchantId(merchantId);
        merchantExtend.setUserId(user.getUserId());
        merchantExtend.setMerchantName(request.getMerchantName());
        merchantExtend.setCertification(2); // 未认证测试中
        merchantExtend.setMerchantLevel(1); // 普通等级
        merchantExtend.setSndPswd(encryptUtils.encryptPassword(request.getSndPswd())); // argon2加密
        merchantExtend.setPrivilegeExpireTime(now); // 普通等级，特权过期时间设为当前时间
        merchantExtend.setRemainingNoticeCount(10); // 默认10次
        merchantExtend.setCreateTime(now);
        merchantExtend.setUpdateTime(now);
        merchantExtendRepository.save(merchantExtend);

        // 3. 更新用户类型为商户
        user.setUserType(2);
        user.setUpdateTime(now);
        userRepository.save(user);

        // 4. 计算测试期过期时间
        LocalDateTime testExpireTime = now.plusDays(TEST_PERIOD_DAYS);

        // 5. 构造响应
        return MerchantRegistrationResponse.builder()
                .merchantId(encryptUtils.bytesToUuid(merchantId))
                .merchantName(request.getMerchantName())
                .certification(2)
                .applyTime(now.format(DATETIME_FORMATTER))
                .testExpireTime(testExpireTime.format(DATETIME_FORMATTER))
                .build();
    }

    /**
     * 直接认证通道注册
     *
     * @param user    用户信息
     * @param request 注册请求
     * @return 注册响应
     */
    private MerchantRegistrationResponse registerByDirectChannel(User user, MerchantRegistrationRequest request) {
        // 1. 校验必填参数
        if (request.getNumStores() == null || request.getNumStores() <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "直接认证通道必须填写店铺规模");
        }
        if (request.getNumMembers() == null || request.getNumMembers().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "直接认证通道必须填写会员规模");
        }
        if (request.getStoreName() == null || request.getStoreName().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "直接认证通道必须填写第一家店铺名称");
        }
        if (request.getStorePhotos() == null || request.getStorePhotos().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "直接认证通道必须上传门头店照");
        }
        if (request.getBusinessLicense() == null || request.getBusinessLicense().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "直接认证通道必须上传营业执照");
        }

        LocalDateTime now = LocalDateTime.now();

        // 2. 只创建审核记录（不创建商户信息，不修改用户类型）
        MerchantAuditRecord auditRecord = new MerchantAuditRecord();
        auditRecord.setUserId(user.getUserId());
        auditRecord.setNumStores(request.getNumStores());
        auditRecord.setNumMembers(request.getNumMembers());
        auditRecord.setStoreName(request.getStoreName());
        auditRecord.setStorePhotos(request.getStorePhotos()); // Object路径
        auditRecord.setBusinessLicense(request.getBusinessLicense()); // Object路径
        auditRecord.setApplicationMethod(1); // 1-直接认证
        auditRecord.setAuditStatus(0); // 0-待审核
        auditRecord.setCreateTime(now);
        merchantAuditRecordRepository.save(auditRecord);

        log.info("直接认证申请已提交: userId={}, auditId={}", 
                encryptUtils.bytesToUuid(user.getUserId()), auditRecord.getAuditId());

        // 3. 构造响应（返回审核记录ID）
        return MerchantRegistrationResponse.builder()
                .auditId(auditRecord.getAuditId())
                .applyTime(now.format(DATETIME_FORMATTER))
                .message("申请已提交，请等待审核")
                .build();
    }

    /**
     * 查询商户基础信息
     *
     * @param merchantId 商户ID
     * @return 商户信息响应
     */
    public MerchantInfoResponse getMerchantInfo(String merchantId) {
        log.info("查询商户基础信息：merchantId={}", merchantId);

        // 1. 查询商户信息
        byte[] merchantIdBytes = encryptUtils.uuidToBytes(merchantId);
        MerchantExtend merchant = merchantExtendRepository.findById(merchantIdBytes)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "商户不存在"));

        // 2. 查询用户信息（获取手机号）
        User user = userRepository.findById(merchant.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_EXIST.getCode(), "用户不存在"));

        // 3. 查询关联店铺数量
        Long storeCount = storeRepository.countByMerchantIdAndStatus(merchantIdBytes, 1);

        // 4. 构造响应
        MerchantInfoResponse.MerchantInfoResponseBuilder builder = MerchantInfoResponse.builder()
                .merchantId(merchantId)
                .merchantName(merchant.getMerchantName())
                .certification(merchant.getCertification())
                .merchantLevel(merchant.getMerchantLevel())
                .contactPhone(user.getPhone())
                .contactEmail(merchant.getContactEmail())
                .merchantIntro(merchant.getMerchantIntro())
                .remainingNoticeCount(merchant.getRemainingNoticeCount())
                .createTime(merchant.getCreateTime().format(DATETIME_FORMATTER))
                .storeCount(storeCount.intValue());

        // 5. 如果是测试期商户，计算剩余天数和过期时间
        if (merchant.getCertification() == 2) {
            LocalDateTime createTime = merchant.getCreateTime();
            LocalDateTime testExpireTime = createTime.plusDays(TEST_PERIOD_DAYS);
            long daysPassed = ChronoUnit.DAYS.between(createTime, LocalDateTime.now());
            int remainingDays = Math.max(0, TEST_PERIOD_DAYS - (int) daysPassed);

            builder.testExpireTime(testExpireTime.format(DATETIME_FORMATTER))
                    .remainingDays(remainingDays);
        }

        return builder.build();
    }

    /**
     * 修改商户基础信息
     *
     * @param request 商户信息修改请求
     * @return 商户信息修改响应
     */
    @Transactional
    public MerchantInfoUpdateResponse updateMerchantInfo(MerchantInfoUpdateRequest request) {
        log.info("修改商户基础信息：merchantId={}", request.getMerchantId());

        // 1. 查询商户信息
        byte[] merchantIdBytes = encryptUtils.uuidToBytes(request.getMerchantId());
        MerchantExtend merchant = merchantExtendRepository.findById(merchantIdBytes)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "商户不存在"));

        // 2. 校验商户状态（只有已认证或测试中的商户可以修改）
        Integer certification = merchant.getCertification();
        if (certification != 1 && certification != 2) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED.getCode(), 
                    "当前商户状态不允许修改信息，请等待审核完成或联系客服");
        }

        // 3. 更新字段（只更新非空字段）
        boolean updated = false;
        if (request.getMerchantName() != null && !request.getMerchantName().trim().isEmpty()) {
            merchant.setMerchantName(request.getMerchantName().trim());
            updated = true;
        }
        if (request.getContactEmail() != null) {
            merchant.setContactEmail(request.getContactEmail());
            updated = true;
        }
        if (request.getMerchantIntro() != null) {
            merchant.setMerchantIntro(request.getMerchantIntro());
            updated = true;
        }

        if (!updated) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "没有需要更新的字段");
        }

        // 4. 保存更新
        LocalDateTime now = LocalDateTime.now();
        merchant.setUpdateTime(now);
        merchantExtendRepository.save(merchant);

        // 5. 构造响应
        return MerchantInfoUpdateResponse.builder()
                .updateTime(now.format(DATETIME_FORMATTER))
                .build();
    }

    /**
     * 修改商户二级密码
     *
     * @param request 二级密码修改请求
     * @return 二级密码修改响应
     */
    @Transactional
    public SecondaryPasswordUpdateResponse updateSecondaryPassword(SecondaryPasswordUpdateRequest request) {
        log.info("修改商户二级密码：merchantId={}", request.getMerchantId());

        // 1. 查询商户信息
        byte[] merchantIdBytes = encryptUtils.uuidToBytes(request.getMerchantId());
        MerchantExtend merchant = merchantExtendRepository.findById(merchantIdBytes)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "商户不存在"));

        // 2. 校验商户状态（只有已认证或测试中的商户可以修改）
        Integer certification = merchant.getCertification();
        if (certification != 1 && certification != 2) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED.getCode(), 
                    "当前商户状态不允许修改二级密码，请等待审核完成或联系客服");
        }

        // 3. 校验两次新密码是否一致
        if (!request.getNewSndPswd().equals(request.getConfirmSndPswd())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "两次输入的新密码不一致");
        }

        // 4. 校验原密码是否正确
        boolean oldPasswordMatch = encryptUtils.verifyPassword(request.getOldSndPswd(), merchant.getSndPswd());
        if (!oldPasswordMatch) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "原二级密码错误");
        }

        // 5. 更新二级密码
        String newHashedPassword = encryptUtils.encryptPassword(request.getNewSndPswd());
        merchant.setSndPswd(newHashedPassword);
        LocalDateTime now = LocalDateTime.now();
        merchant.setUpdateTime(now);
        merchantExtendRepository.save(merchant);

        // 6. 构造响应
        return SecondaryPasswordUpdateResponse.builder()
                .updateTime(now.format(DATETIME_FORMATTER))
                .build();
    }

    /**
     * 通过验证码重置商户二级密码
     *
     * @param merchantId   商户ID
     * @param encryptedPhone 手机号（AES加密）
     * @param verifyCode   验证码
     * @param newSndPswd   新二级密码
     * @param deviceId     设备ID
     * @return 重置时间
     */
    @Transactional
    public String resetSecondaryPassword(String merchantId, String encryptedPhone, 
                                         String verifyCode, String newSndPswd, String deviceId) {
        log.info("商户二级密码重置请求: merchantId={}, deviceId={}", merchantId, deviceId);

        // 1. 解密手机号
        String plainPhone = encryptUtils.decryptAES(encryptedPhone);
        if (plainPhone == null || plainPhone.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "手机号格式错误");
        }

        // 2. 验证验证码
        if (!verifyCodeService.verifyCode(encryptedPhone, verifyCode, deviceId)) {
            throw new BusinessException(ErrorCode.VERIFY_CODE_INVALID.getCode(), "验证码错误或已失效");
        }

        // 3. 查询商户信息
        byte[] merchantIdBytes = encryptUtils.uuidToBytes(merchantId);
        MerchantExtend merchant = merchantExtendRepository.findById(merchantIdBytes)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "商户不存在"));

        // 4. 查询商户关联的用户信息
        User user = userRepository.findById(merchant.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_EXIST.getCode(), "关联用户不存在"));

        // 5. 验证手机号是否匹配
        if (!plainPhone.equals(user.getPhone())) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED.getCode(), 
                    "手机号与商户注册手机号不一致");
        }

        // 6. 校验商户状态（只有已认证或测试中的商户可以重置）
        Integer certification = merchant.getCertification();
        if (certification != 1 && certification != 2) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED.getCode(), 
                    "当前商户状态不允许重置二级密码，请等待审核完成或联系客服");
        }

        // 7. 加密新二级密码
        String hashedPassword = encryptUtils.encryptPassword(newSndPswd);

        // 8. 更新二级密码
        merchant.setSndPswd(hashedPassword);
        LocalDateTime now = LocalDateTime.now();
        merchant.setUpdateTime(now);
        merchantExtendRepository.save(merchant);

        log.info("商户二级密码重置成功: merchantId={}, phone={}", merchantId, plainPhone);

        return now.format(DATETIME_FORMATTER);
    }

    /**
     * 查询商户资质审核结果
     *
     * @param userId 用户ID
     * @return 审核结果数据
     */
    public MerchantAuditRecord getQualificationAuditStatus(String userId) {
        log.info("查询商户资质审核结果: userId={}", userId);

        // 1. 转换userId为字节数组
        byte[] userIdBytes = encryptUtils.uuidToBytes(userId);

        // 2. 查询用户信息获取用户类型
        User user = userRepository.findById(userIdBytes)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_EXIST));
        
        Integer userType = user.getUserType();
        log.info("用户类型: userType={}", userType);

        // 3. 校验用户类型
        if (userType == 3) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED.getCode(),
                    "员工账号不能申请成为商家，您需要先解除所有工作关系！");
        }

        // 4. 查询审核记录（user_id唯一，最多返回1条）
        List<MerchantAuditRecord> records = merchantAuditRecordRepository.findByUserIdOrderByCreateTimeDesc(userIdBytes);

        if (records.isEmpty()) {
            // 无审核记录
            throw new BusinessException(404, "您还没有提交申请成为商家");
        }

        MerchantAuditRecord record = records.get(0);
        log.info("查询到审核记录: auditId={}, auditStatus={}", record.getAuditId(), record.getAuditStatus());

        return record;
    }

    /**
     * 商户资质补充提交
     *
     * @param request 资质补充请求
     * @return 提交时间
     */
    @Transactional
    public QualificationSubmitResponse submitQualification(QualificationSubmitRequest request) {
        log.info("商户资质补充提交: merchantId={}", request.getMerchantId());

        // 1. 查询商户信息
        byte[] merchantIdBytes = encryptUtils.uuidToBytes(request.getMerchantId());
        MerchantExtend merchant = merchantExtendRepository.findById(merchantIdBytes)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "商户不存在"));

        // 2. 校验商户状态（只允许 certification=2,4,5,14 提交）
        Integer currentCertification = merchant.getCertification();
        if (currentCertification != 2 && currentCertification != 4 && 
            currentCertification != 5 && currentCertification != 14) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED.getCode(),
                    "当前商户状态不允许提交资质，certification=" + currentCertification);
        }

        // 3. 确定新的认证状态
        Integer newCertification;
        if (currentCertification == 5 || currentCertification == 14) {
            // 测试期过 或 过期后审核拒绝 → 过期后审核中
            newCertification = 13;
        } else {
            // 测试中 或 审核拒绝 → 审核中
            newCertification = 3;
        }

        // 3.5 如果是测试中商户且已创建店铺，更新店铺信息（门头店照、营业执照、店铺名称）
        if (currentCertification == 2) {
            List<Store> existingStores = storeRepository.findByMerchantId(merchantIdBytes);
            if (!existingStores.isEmpty()) {
                // 已有店铺，更新第一个店铺的信息（以申请表信息为准）
                Store store = existingStores.get(0);
                store.setStorePhotos(request.getStorePhotos());
                store.setBusinessLicense(request.getBusinessLicense());
                // 更新店铺名称
                if (request.getStoreName() != null && !request.getStoreName().trim().isEmpty()) {
                    store.setStoreName(request.getStoreName());
                }
                storeRepository.save(store);
                log.info("更新测试中商户的已有店铺信息: storeId={}, storeName={}", 
                        encryptUtils.bytesToUuid(store.getStoreId()), store.getStoreName());
            }
        }

        // 4. 查询是否已有审核记录
        List<MerchantAuditRecord> existingRecords = merchantAuditRecordRepository.findByUserIdOrderByCreateTimeDesc(merchant.getUserId());

        LocalDateTime now = LocalDateTime.now();
        MerchantAuditRecord auditRecord;

        if (!existingRecords.isEmpty()) {
            // 4.1 有记录：更新（覆盖）
            auditRecord = existingRecords.get(0);
            auditRecord.setNumStores(request.getNumStores());
            auditRecord.setNumMembers(request.getNumMembers());
            auditRecord.setStoreName(request.getStoreName());
            auditRecord.setStorePhotos(request.getStorePhotos());
            auditRecord.setBusinessLicense(request.getBusinessLicense());
            // application_method 保持不变
            auditRecord.setAuditStatus(0); // 重置为待审核
            auditRecord.setAuditorId(null); // 清空审核人
            auditRecord.setRejectReason(null); // 清空拒绝原因
            auditRecord.setAuditTime(null); // 清空审核时间
            auditRecord.setCreateTime(now); // 更新创建时间为当前时间
            
            log.info("更新已有审核记录: auditId={}", auditRecord.getAuditId());
        } else {
            // 4.2 无记录：创建新记录（免认证商户首次补充资质）
            auditRecord = new MerchantAuditRecord();
            auditRecord.setUserId(merchant.getUserId());
            auditRecord.setNumStores(request.getNumStores());
            auditRecord.setNumMembers(request.getNumMembers());
            auditRecord.setStoreName(request.getStoreName());
            auditRecord.setStorePhotos(request.getStorePhotos());
            auditRecord.setBusinessLicense(request.getBusinessLicense());
            auditRecord.setApplicationMethod(2); // 免认证后续补充
            auditRecord.setAuditStatus(0); // 待审核
            auditRecord.setCreateTime(now);
            
            log.info("创建新审核记录: userId={}", request.getMerchantId());
        }

        auditRecord = merchantAuditRecordRepository.save(auditRecord);

        // 5. 更新商户认证状态
        merchant.setCertification(newCertification);
        merchant.setUpdateTime(now);
        merchantExtendRepository.save(merchant);

        log.info("商户资质补充成功: merchantId={}, certification: {} -> {}", 
                request.getMerchantId(), currentCertification, newCertification);

        // 6. 构建响应
        return QualificationSubmitResponse.builder()
                .auditStatus("WAIT")
                .submitTime(now.format(DATETIME_FORMATTER))
                .certification(newCertification)
                .build();
    }
}

