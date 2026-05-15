package com.ecards.member_management.service;

import com.ecards.member_management.common.ErrorCode;
import com.ecards.member_management.dto.request.*;
import com.ecards.member_management.dto.response.*;
import com.ecards.member_management.dto.response.StoreListResponse.StoreItem;
import com.ecards.member_management.entity.Store;
import com.ecards.member_management.entity.User;
import com.ecards.member_management.entity.WorkRelation;
import com.ecards.member_management.exception.BusinessException;
import com.ecards.member_management.repository.StoreRepository;
import com.ecards.member_management.repository.UserRepository;
import com.ecards.member_management.repository.WorkRelationRepository;
import com.ecards.member_management.utils.EncryptUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 员工管理服务
 * 
 * 核心功能：
 * 1. 员工添加（绑定）
 * 2. 员工信息查询（单个/列表）
 * 
 * 权限规则：
 * - 商家：可操作所属店铺的所有员工
 * - 店长：可操作本店铺的所有员工（包括其他店长）
 * - 店员：只能查询自己
 * 
 * @author Ecards Team
 * @since 2025-10-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StaffService {

    private final WorkRelationRepository workRelationRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final EncryptUtils encryptUtils;
    private final RedisTemplate<String, Object> redisTemplate;
    private final TokenRedisService tokenRedisService;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 员工添加（绑定）
     * 
     * @param request    添加请求
     * @param storeId    店铺ID（从令牌或请求参数提取）
     * @param merchantId 商家ID（从令牌提取）
     * @param operatorId 操作人ID（从令牌提取）
     * @return 添加结果
     */
    @Transactional
    public StaffAddResponse addStaff(StaffAddRequest request, String storeId, String merchantId, String operatorId) {
        // 1. 查询待添加用户（通过手机号和邀请码）
        User targetUser = userRepository.findByPhone(request.getStaffPhone())
                .orElseThrow(() -> new BusinessException(20003, "用户不存在"));

        // 2. 校验邀请码
        if (!request.getStaffInviteCode().equals(targetUser.getInviteCode())) {
            throw new BusinessException(20004, "邀请码错误");
        }

        // 3. 校验用户类型：商家不能被雇佣
        if (targetUser.getUserType() == 2) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED.getCode(), 
                    "商家用户不能被雇佣为员工");
        }

        // 4. 查询店铺信息
        byte[] storeIdBytes = encryptUtils.uuidToBytes(storeId);
        Store store = storeRepository.findById(storeIdBytes)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_EXIST));

        // 5. 校验店铺归属（防止跨商家操作）
        String storeMerchantId = encryptUtils.bytesToUuid(store.getMerchantId());
        if (!merchantId.equals(storeMerchantId)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED.getCode(), 
                    "店铺不属于当前商家");
        }

        // 6. 检查是否已绑定该店铺
        byte[] userIdBytes = encryptUtils.uuidToBytes(encryptUtils.bytesToUuid(targetUser.getUserId()));
        if (workRelationRepository.existsByStoreIdAndUserIdAndStatus(storeIdBytes, userIdBytes, 1)) {
            throw new BusinessException(50001, "员工已关联该店铺");
        }

        // 7. 创建工作关系
        WorkRelation workRelation = new WorkRelation();
        workRelation.setStoreId(storeIdBytes);
        workRelation.setMerchantId(encryptUtils.uuidToBytes(merchantId));
        workRelation.setUserId(userIdBytes);
        
        // 角色转换：STAFF -> employee, STORE_MANAGER -> manager
        String dbRole = "STAFF".equals(request.getStaffRole()) ? "employee" : "manager";
        workRelation.setRole(dbRole);
        
        workRelation.setName(request.getStaffName());
        
        // 权限处理：如果为空，则根据角色设置默认空权限
        String permission = request.getStaffPermission();
        if (permission == null || permission.trim().isEmpty()) {
            // 未传递权限时，设置默认空权限JSON
            permission = String.format("{\"%s\": []}", dbRole);
            log.info("未传递员工权限，设置默认空权限: role={}, permission={}", dbRole, permission);
        }
        workRelation.setPermission(permission);
        
        workRelation.setTokenVersion(1);  // 初始版本号
        workRelation.setRemark(request.getRemark() != null ? request.getRemark() : "");
        workRelation.setStatus(1);  // 在职
        workRelation.setEntryTime(LocalDateTime.now());
        workRelation.setUpdateTime(LocalDateTime.now());

        WorkRelation savedRelation = workRelationRepository.save(workRelation);

        // 8. 更新用户类型为员工
        if (targetUser.getUserType() != 3) {
            targetUser.setUserType(3);
            userRepository.save(targetUser);
            log.info("用户类型更新为员工: userId={}", encryptUtils.bytesToUuid(targetUser.getUserId()));
        }

        log.info("员工添加成功: staffId={}, storeId={}, relationId={}, role={}", 
                encryptUtils.bytesToUuid(targetUser.getUserId()), storeId, savedRelation.getId(), dbRole);

        return StaffAddResponse.builder()
                .staffId(encryptUtils.bytesToUuid(targetUser.getUserId()))
                .storeId(storeId)
                .relationId(savedRelation.getId())
                .tokenVersion(1)
                .createTime(savedRelation.getEntryTime().format(DATETIME_FORMATTER))
                .build();
    }

    /**
     * 员工信息查询（单个）
     * 
     * @param staffId       员工ID
     * @param storeId       店铺ID
     * @param currentUserId 当前用户ID
     * @param currentRole   当前用户角色（从令牌提取）
     * @return 员工详情
     */
    public StaffDetailResponse getStaffDetail(String staffId, String storeId, String currentUserId, String currentRole) {
        byte[] staffIdBytes = encryptUtils.uuidToBytes(staffId);
        byte[] storeIdBytes = encryptUtils.uuidToBytes(storeId);

        // 1. 查询工作关系
        WorkRelation workRelation = workRelationRepository
                .findByStoreIdAndUserIdAndStatus(storeIdBytes, staffIdBytes, 1)
                .orElseThrow(() -> new BusinessException(50002, "员工未关联该店铺"));

        // 2. 查询用户信息
        User user = userRepository.findById(staffIdBytes)
                .orElseThrow(() -> new BusinessException(20003, "用户不存在"));

        // 3. 权限校验
        boolean isSelf = staffId.equals(currentUserId);
        boolean canViewPermission = false;

        if ("MERCHANT".equals(currentRole)) {
            // 商家可以查看所有员工的权限
            canViewPermission = true;
        } else if ("manager".equals(currentRole)) {
            // 店长可以查看所有员工的权限（包括其他店长）
            canViewPermission = true;
        } else if ("employee".equals(currentRole)) {
            // 店员只能查看自己，且不能查看权限
            if (!isSelf) {
                throw new BusinessException(ErrorCode.PERMISSION_DENIED.getCode(), 
                        "店员只能查询自己的信息");
            }
            canViewPermission = false;
        }

        // 4. 构建响应
        StaffDetailResponse.StaffDetailResponseBuilder builder = StaffDetailResponse.builder()
                .staffId(staffId)
                .staffName(workRelation.getName())
                .staffPhone(user.getPhone())
                .staffRole("employee".equals(workRelation.getRole()) ? "STAFF" : "STORE_MANAGER")
                .storeId(storeId)
                .remark(workRelation.getRemark())
                .entryTime(workRelation.getEntryTime().format(DATETIME_FORMATTER))
                .lastLoginTime(user.getLastLoginTime() != null ? 
                        user.getLastLoginTime().format(DATETIME_FORMATTER) : null);

        // 5. 根据权限决定是否返回权限配置
        if (canViewPermission) {
            builder.staffPermission(workRelation.getPermission());
        }

        // 6. 员工自查时返回店铺名称
        if (isSelf && !canViewPermission) {
            Store store = storeRepository.findById(storeIdBytes)
                    .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_EXIST));
            builder.storeName(store.getStoreName());
        }

        log.info("员工详情查询成功: staffId={}, storeId={}, currentUserId={}", 
                staffId, storeId, currentUserId);

        return builder.build();
    }

    /**
     * 员工信息查询（列表）
     * 
     * @param storeId    店铺ID
     * @param staffRole  角色筛选（可选）
     * @param staffName  姓名筛选（可选）
     * @param pageNum    页码
     * @param pageSize   每页条数
     * @return 员工列表
     */
    public StaffListResponse getStaffList(String storeId, String staffRole, String staffName, 
                                          Integer pageNum, Integer pageSize) {
        byte[] storeIdBytes = encryptUtils.uuidToBytes(storeId);

        // 1. 构建分页和排序
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, 
                Sort.by(Sort.Direction.DESC, "entryTime"));

        // 2. 查询工作关系
        Page<WorkRelation> page;
        
        if (staffRole != null && !staffRole.isEmpty()) {
            // 角色转换：STAFF -> employee, STORE_MANAGER -> manager
            String dbRole = "STAFF".equals(staffRole) ? "employee" : "manager";
            page = workRelationRepository.findByStoreIdAndRoleAndStatus(storeIdBytes, dbRole, 1, pageable);
        } else {
            page = workRelationRepository.findByStoreIdAndStatus(storeIdBytes, 1, pageable);
        }

        // 3. 构建响应列表
        List<StaffListItem> items = page.getContent().stream()
                .filter(wr -> {
                    // 姓名筛选
                    if (staffName != null && !staffName.isEmpty()) {
                        return wr.getName() != null && wr.getName().contains(staffName);
                    }
                    return true;
                })
                .map(wr -> {
                    User user = userRepository.findById(wr.getUserId()).orElse(null);
                    return StaffListItem.builder()
                            .staffId(encryptUtils.bytesToUuid(wr.getUserId()))
                            .staffName(wr.getName())
                            .staffPhone(user != null ? user.getPhone() : "")
                            .staffRole("employee".equals(wr.getRole()) ? "STAFF" : "STORE_MANAGER")
                            .entryTime(wr.getEntryTime().format(DATETIME_FORMATTER))
                            .build();
                })
                .collect(Collectors.toList());

        log.info("员工列表查询成功: storeId={}, total={}, pageNum={}", storeId, page.getTotalElements(), pageNum);

        return StaffListResponse.builder()
                .total(page.getTotalElements())
                .pageNum(pageNum)
                .pageSize(pageSize)
                .list(items)
                .build();
    }

    /**
     * 查询用户的所有工作关系
     * 
     * @param userId 用户ID
     * @return 工作关系列表
     */
    public UserWorkRelationsResponse getUserWorkRelations(String userId) {
        byte[] userIdBytes = encryptUtils.uuidToBytes(userId);

        // 查询所有在职的工作关系
        List<WorkRelation> relations = workRelationRepository.findByUserIdAndStatus(userIdBytes, 1);

        List<WorkRelationItem> items = relations.stream()
                .map(wr -> {
                    Store store = storeRepository.findById(wr.getStoreId()).orElse(null);
                    return WorkRelationItem.builder()
                            .relationId(wr.getId())
                            .storeId(encryptUtils.bytesToUuid(wr.getStoreId()))
                            .storeName(store != null ? store.getStoreName() : "未知店铺")
                            .merchantId(encryptUtils.bytesToUuid(wr.getMerchantId()))
                            .role("employee".equals(wr.getRole()) ? "STAFF" : "STORE_MANAGER")
                            .entryTime(wr.getEntryTime().format(DATETIME_FORMATTER))
                            .status(wr.getStatus())
                            .build();
                })
                .collect(Collectors.toList());

        log.info("查询用户工作关系成功: userId={}, count={}", userId, items.size());

        return UserWorkRelationsResponse.builder()
                .workRelations(items)
                .build();
    }

    /**
     * 员工信息修改
     * 
     * @param staffId      员工ID
     * @param request      修改请求
     * @param storeId      店铺ID（从令牌或请求参数提取）
     * @param operatorId   操作者ID
     * @param operatorRole 操作者角色（MERCHANT/manager/employee）
     * @return 修改响应
     */
    @Transactional
    public StaffUpdateResponse updateStaff(String staffId, StaffUpdateRequest request, 
                                           String storeId, String operatorId, String operatorRole) {
        log.info("员工信息修改开始: staffId={}, storeId={}, operatorId={}, operatorRole={}", 
                staffId, storeId, operatorId, operatorRole);

        // 1. 校验修改内容：至少修改一项
        if ((request.getStaffName() == null || request.getStaffName().trim().isEmpty()) &&
            (request.getRemark() == null || request.getRemark().trim().isEmpty())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "至少需要修改一项信息");
        }

        byte[] storeIdBytes = encryptUtils.uuidToBytes(storeId);
        byte[] staffIdBytes = encryptUtils.uuidToBytes(staffId);

        // 2. 查询目标员工的工作关系
        WorkRelation workRelation = workRelationRepository
                .findByStoreIdAndUserIdAndStatus(storeIdBytes, staffIdBytes, 1)
                .orElseThrow(() -> new BusinessException(50002, "员工不存在或已离职"));

        // 3. 权限校验：店长只能修改店员
        if ("manager".equals(operatorRole) && "manager".equals(workRelation.getRole())) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED.getCode(), 
                    "店长不能修改其他店长的信息");
        }

        // 4. 执行修改
        boolean modified = false;
        if (request.getStaffName() != null && !request.getStaffName().trim().isEmpty()) {
            workRelation.setName(request.getStaffName().trim());
            modified = true;
        }
        if (request.getRemark() != null && !request.getRemark().trim().isEmpty()) {
            workRelation.setRemark(request.getRemark().trim());
            modified = true;
        }

        if (modified) {
            workRelation.setUpdateTime(LocalDateTime.now());
            workRelationRepository.save(workRelation);
        }

        log.info("员工信息修改成功: staffId={}, storeId={}", staffId, storeId);

        return StaffUpdateResponse.builder()
                .updateTime(workRelation.getUpdateTime().format(DATETIME_FORMATTER))
                .build();
    }

    /**
     * 员工角色调整
     * 
     * @param staffId      员工ID
     * @param request      调整请求
     * @param operatorId   操作者ID（商家）
     * @return 调整响应
     */
    @Transactional
    public StaffRoleAdjustResponse adjustStaffRole(String staffId, StaffRoleAdjustRequest request, 
                                                   String operatorId) {
        log.info("员工角色调整开始: staffId={}, storeId={}, targetRole={}", 
                staffId, request.getStoreId(), request.getTargetRole());

        byte[] storeIdBytes = encryptUtils.uuidToBytes(request.getStoreId());
        byte[] staffIdBytes = encryptUtils.uuidToBytes(staffId);

        // 1. 查询目标员工的工作关系
        WorkRelation workRelation = workRelationRepository
                .findByStoreIdAndUserIdAndStatus(storeIdBytes, staffIdBytes, 1)
                .orElseThrow(() -> new BusinessException(50002, "员工不存在或已离职"));

        // 2. 校验商家ID
        String currentMerchantId = encryptUtils.bytesToUuid(workRelation.getMerchantId());
        if (!request.getMerchantId().equals(currentMerchantId)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED.getCode(), 
                    "无权操作该员工");
        }

        // 3. 角色转换：STAFF -> employee, STORE_MANAGER -> manager
        String targetDbRole = "STAFF".equals(request.getTargetRole()) ? "employee" : "manager";
        String oldRole = workRelation.getRole();

        // 4. 检查目标角色是否与当前角色相同
        if (targetDbRole.equals(oldRole)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), 
                    "目标角色与当前角色相同");
        }

        // 5. 执行角色调整
        workRelation.setRole(targetDbRole);
        workRelation.setTokenVersion(workRelation.getTokenVersion() + 1);  // 工作令牌版本递增
        workRelation.setUpdateTime(LocalDateTime.now());
        workRelationRepository.save(workRelation);

        // 6. 清除Redis中的工作令牌版本缓存
        tokenRedisService.clearWorkTokenVersionCache(staffId, request.getStoreId());

        // 注释：角色调整只影响工作令牌，不影响普通令牌
        // 普通令牌是用户身份认证令牌，与工作关系变动无关
        // 只有当用户身份发生变化（如修改密码、账号安全问题等）时才需要使普通令牌失效

        log.info("员工角色调整成功: staffId={}, oldRole={}, newRole={}, workTokenVersion={}", 
                staffId, oldRole, targetDbRole, workRelation.getTokenVersion());

        return StaffRoleAdjustResponse.builder()
                .staffId(staffId)
                .oldRole("employee".equals(oldRole) ? "STAFF" : "STORE_MANAGER")
                .newRole(request.getTargetRole())
                .adjustTime(workRelation.getUpdateTime().format(DATETIME_FORMATTER))
                .build();
    }

    /**
     * 员工权限修改
     * 
     * @param staffId      员工ID
     * @param request      修改请求
     * @param storeId      店铺ID（从令牌或请求参数提取）
     * @param operatorId   操作者ID
     * @param operatorRole 操作者角色（MERCHANT/manager）
     * @return 修改响应
     */
    @Transactional
    public StaffPermissionUpdateResponse updateStaffPermission(String staffId, StaffPermissionUpdateRequest request,
                                                               String storeId, String operatorId, String operatorRole) {
        log.info("员工权限修改开始: staffId={}, storeId={}, operatorRole={}", 
                staffId, storeId, operatorRole);

        byte[] storeIdBytes = encryptUtils.uuidToBytes(storeId);
        byte[] staffIdBytes = encryptUtils.uuidToBytes(staffId);

        // 1. 查询目标员工的工作关系
        WorkRelation workRelation = workRelationRepository
                .findByStoreIdAndUserIdAndStatus(storeIdBytes, staffIdBytes, 1)
                .orElseThrow(() -> new BusinessException(50002, "员工不存在或已离职"));

        // 2. 权限校验：店长只能修改店员权限
        if ("manager".equals(operatorRole) && "manager".equals(workRelation.getRole())) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED.getCode(), 
                    "店长不能修改其他店长的权限");
        }

        // 3. 验证权限JSON格式
        String newPermission = request.getNewPermission();
        try {
            // 简单验证JSON格式
            if (!newPermission.trim().startsWith("{") || !newPermission.trim().endsWith("}")) {
                throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "权限格式错误");
            }
            
            // 验证权限JSON中的角色键是否与员工角色匹配
            String expectedKey = "\"" + workRelation.getRole() + "\"";
            if (!newPermission.contains(expectedKey)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), 
                        "权限配置角色不匹配，应为: " + workRelation.getRole());
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "权限格式错误");
        }

        // 4. 执行权限修改
        workRelation.setPermission(newPermission);
        workRelation.setTokenVersion(workRelation.getTokenVersion() + 1);  // 令牌版本递增
        workRelation.setUpdateTime(LocalDateTime.now());
        workRelationRepository.save(workRelation);

        // 5. 清除Redis中的工作令牌版本缓存
        tokenRedisService.clearWorkTokenVersionCache(staffId, storeId);

        log.info("员工权限修改成功: staffId={}, storeId={}, tokenVersion={}", 
                staffId, storeId, workRelation.getTokenVersion());

        return StaffPermissionUpdateResponse.builder()
                .updateTime(workRelation.getUpdateTime().format(DATETIME_FORMATTER))
                .build();
    }

    /**
     * 员工解绑
     * 
     * @param staffId      员工ID
     * @param request      解绑请求
     * @param operatorId   操作者ID（商家）
     * @return 解绑响应
     */
    @Transactional
    public StaffUnbindResponse unbindStaff(String staffId, StaffUnbindRequest request, String operatorId) {
        log.info("员工解绑开始: staffId={}, storeId={}", staffId, request.getStoreId());

        // 1. 校验确认标志
        if (!Boolean.TRUE.equals(request.getConfirm())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "请确认解绑操作");
        }

        byte[] storeIdBytes = encryptUtils.uuidToBytes(request.getStoreId());
        byte[] staffIdBytes = encryptUtils.uuidToBytes(staffId);

        // 2. 查询目标员工的工作关系
        WorkRelation workRelation = workRelationRepository
                .findByStoreIdAndUserIdAndStatus(storeIdBytes, staffIdBytes, 1)
                .orElseThrow(() -> new BusinessException(50002, "员工不存在或已离职"));

        // 3. 校验商家ID
        String currentMerchantId = encryptUtils.bytesToUuid(workRelation.getMerchantId());
        if (!request.getMerchantId().equals(currentMerchantId)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED.getCode(), "无权操作该员工");
        }

        // 4. 删除工作关系记录
        workRelationRepository.delete(workRelation);
        LocalDateTime unbindTime = LocalDateTime.now();

        // 4.5. 清除Redis中的工作令牌版本缓存
        tokenRedisService.clearWorkTokenVersionCache(staffId, request.getStoreId());

        // 5. 查询该员工是否还有其他工作关系
        List<WorkRelation> remainingRelations = workRelationRepository
                .findByUserIdAndStatus(staffIdBytes, 1);

        // 6. 如果没有其他工作关系，user_type改为1，并递增token_version
        if (remainingRelations.isEmpty()) {
            User user = userRepository.findById(staffIdBytes)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_EXIST));
            
            user.setUserType(1);  // 恢复为普通用户
            Integer newVersion = user.getTokenVersion() + 1;
            user.setTokenVersion(newVersion);  // 普通令牌失效
            userRepository.save(user);
            
            // ⭐ 同步更新Redis缓存，防止缓存不一致导致用户无法登录
            String versionCacheKey = "user:token:version:" + staffId;
            try {
                redisTemplate.opsForValue().set(versionCacheKey, newVersion, 5, TimeUnit.MINUTES);
                log.info("员工最后一个工作关系已解绑，user_type改为1，token_version已更新并同步缓存: staffId={}, newVersion={}", 
                        staffId, newVersion);
            } catch (Exception e) {
                log.warn("更新Redis缓存失败，但数据库已更新: staffId={}, newVersion={}", staffId, newVersion, e);
            }
        } else {
            // 仍有其他工作关系，不需要递增普通令牌版本
            // 工作关系解绑只影响工作令牌，不影响普通令牌
            log.info("员工解绑，仍有{}个其他工作关系，普通令牌保持有效: staffId={}", remainingRelations.size(), staffId);
        }

        log.info("员工解绑成功: staffId={}, storeId={}, remainingRelations={}", 
                staffId, request.getStoreId(), remainingRelations.size());

        return StaffUnbindResponse.builder()
                .staffId(staffId)
                .unbindTime(unbindTime.format(DATETIME_FORMATTER))
                .build();
    }

    /**
     * 员工查询自己的工作店铺列表
     * 
     * @param userId 用户ID（员工）
     * @return 店铺列表响应
     */
    public StoreListResponse getWorkStores(String userId) {
        byte[] userIdBytes = encryptUtils.uuidToBytes(userId);

        // 1. 查询所有在职的工作关系
        List<WorkRelation> relations = workRelationRepository.findByUserIdAndStatus(userIdBytes, 1);

        // 2. 根据工作关系查询店铺信息，构建店铺列表
        List<StoreItem> storeItems = relations.stream()
                .map(wr -> {
                    Store store = storeRepository.findById(wr.getStoreId()).orElse(null);
                    if (store == null) {
                        return null;
                    }
                    return StoreItem.builder()
                            .storeId(encryptUtils.bytesToUuid(store.getStoreId()))
                            .storeName(store.getStoreName())
                            .storeType(store.getStoreType())
                            .storeStatus(store.getStatus())
                            .createTime(store.getCreateTime().format(DATETIME_FORMATTER))
                            .build();
                })
                .filter(item -> item != null)
                .collect(Collectors.toList());

        log.info("员工查询工作店铺列表成功: userId={}, count={}", userId, storeItems.size());

        return StoreListResponse.builder()
                .total(storeItems.size())
                .list(storeItems)
                .build();
    }
}

