package com.ecards.member_management.controller;

import com.ecards.member_management.common.ErrorCode;
import com.ecards.member_management.common.Result;
import com.ecards.member_management.constants.TokenConstants;
import com.ecards.member_management.dto.request.*;
import com.ecards.member_management.dto.response.*;
import com.ecards.member_management.dto.response.StoreListResponse;
import com.ecards.member_management.entity.MerchantExtend;
import com.ecards.member_management.entity.User;
import com.ecards.member_management.exception.BusinessException;
import com.ecards.member_management.repository.MerchantExtendRepository;
import com.ecards.member_management.repository.UserRepository;
import com.ecards.member_management.service.StaffService;
import com.ecards.member_management.utils.EncryptUtils;
import com.ecards.member_management.utils.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 员工管理Controller
 * 
 * 接口列表：
 * 1. POST /api/v1/staffs - 员工添加（绑定）
 * 2. GET /api/v1/staffs/{staffId} - 员工信息查询（单个）
 * 3. GET /api/v1/staffs - 员工信息查询（列表）
 * 
 * 权限规则：
 * - 商家：可使用普通令牌或工作令牌
 * - 店长：必须使用工作令牌
 * - 店员：只能查询自己（普通令牌）
 * 
 * @author Ecards Team
 * @since 2025-10-30
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/staffs")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;
    private final JwtUtils jwtUtils;
    private final MerchantExtendRepository merchantExtendRepository;
    private final UserRepository userRepository;
    private final EncryptUtils encryptUtils;

    /**
     * 员工添加（绑定）
     * POST /api/v1/staffs
     * 
     * 权限：商家/店长
     * - 商家：普通令牌或工作令牌
     * - 店长：工作令牌（需要 staff_add 权限）
     */
    @PostMapping
    public Result<StaffAddResponse> addStaff(
            @Valid @RequestBody StaffAddRequest request,
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String authorization) {
        
        try {
            // 1. 提取令牌
            String token = extractBearerToken(authorization);
            if (token == null) {
                return Result.fail("Authorization格式错误");
            }

            // 2. 获取当前用户信息
            String currentUserId = jwtUtils.extractUserId(token);
            Integer tokenType = jwtUtils.extractTokenType(token);

            String storeId;
            String merchantId;
            String currentRole;

            // 3. 根据令牌类型提取必要信息
            if (tokenType == 1) {
                // 普通令牌（商家）
                if (request.getStoreId() == null || request.getStoreId().isEmpty()) {
                    return Result.fail("商家使用普通令牌时，storeId 必填");
                }
                storeId = request.getStoreId();
                
                // 从令牌提取 merchantId，如果为null则通过数据库查询（保证健壮性）
                merchantId = jwtUtils.extractMerchantId(token);
                if (merchantId == null) {
                    log.warn("令牌中merchantId为null，尝试从数据库查询: userId={}", currentUserId);
                    byte[] userIdBytes = encryptUtils.uuidToBytes(currentUserId);
                    MerchantExtend merchantExtend = merchantExtendRepository.findByUserId(userIdBytes)
                            .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_NOT_EXIST));
                    merchantId = encryptUtils.bytesToUuid(merchantExtend.getMerchantId());
                }
                currentRole = "MERCHANT";
            } else if (tokenType == 3) {
                // 工作令牌（商家/店长/店员）
                storeId = jwtUtils.extractStoreId(token);
                merchantId = jwtUtils.extractMerchantId(token);
                String role = jwtUtils.extractRole(token);
                
                // 根据令牌角色判断当前用户角色
                if ("merchant".equals(role)) {
                    // 商家拥有最高权限，无需额外校验
                    currentRole = "MERCHANT";
                } else if ("manager".equals(role)) {
                    // 店长需要校验 staff_add 权限
                    currentRole = "STORE_MANAGER";
                    java.util.List<String> permissions = jwtUtils.extractPermissions(token);
                    if (!permissions.contains("staff_add")) {
                        return Result.fail("店长无员工添加权限");
                    }
                } else {
                    // 店员不允许添加员工
                    return Result.fail("店员无权添加员工");
                }
            } else {
                return Result.fail("令牌类型错误，需要普通令牌或工作令牌");
            }

            // 4. 调用Service
            StaffAddResponse response = staffService.addStaff(request, storeId, merchantId, currentUserId);

            return Result.success("员工添加成功", response);
        } catch (Exception e) {
            log.error("员工添加失败", e);
            return Result.fail("员工添加失败：" + e.getMessage());
        }
    }

    /**
     * 员工查询自己的工作店铺列表
     * GET /api/v1/staffs/work-stores
     * 
     * 权限：员工（普通令牌）
     * 校验：普通令牌有效，userId = 令牌中userId，身份为3-员工
     */
    @GetMapping("/work-stores")
    public Result<StoreListResponse> getWorkStores(
            @RequestParam String userId,
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String authorization) {
        
        try {
            // 1. 提取令牌
            String token = extractBearerToken(authorization);
            if (token == null) {
                return Result.fail("Authorization格式错误");
            }

            // 2. 获取令牌信息
            Integer tokenType = jwtUtils.extractTokenType(token);
            String tokenUserId = jwtUtils.extractUserId(token);

            // 3. 校验令牌类型：必须是普通令牌
            if (tokenType != 1) {
                return Result.fail("需要普通令牌");
            }

            // 4. 校验userId与令牌中userId一致
            if (!userId.equals(tokenUserId)) {
                return Result.fail("只能查询自己的工作店铺");
            }

            // 5. 校验用户身份：必须是员工（user_type=3）
            byte[] userIdBytes = encryptUtils.uuidToBytes(userId);
            User user = userRepository.findById(userIdBytes).orElse(null);
            if (user == null) {
                return Result.fail("用户不存在");
            }
            if (user.getUserType() != 3) {
                return Result.fail("仅员工可查询工作店铺列表");
            }

            // 6. 调用Service
            StoreListResponse response = staffService.getWorkStores(userId);

            return Result.success("工作店铺列表查询成功", response);
        } catch (Exception e) {
            log.error("工作店铺列表查询失败", e);
            return Result.fail("工作店铺列表查询失败：" + e.getMessage());
        }
    }

    /**
     * 员工信息查询（单个）
     * GET /api/v1/staffs/{staffId}
     * 
     * 权限：商家/店长/员工本人
     */
    @GetMapping("/{staffId}")
    public Result<StaffDetailResponse> getStaffDetail(
            @PathVariable String staffId,
            @RequestParam(required = false) String storeId,
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String authorization) {
        
        try {
            // 1. 提取令牌
            String token = extractBearerToken(authorization);
            if (token == null) {
                return Result.fail("Authorization格式错误");
            }

            // 2. 获取当前用户信息
            String currentUserId = jwtUtils.extractUserId(token);
            Integer tokenType = jwtUtils.extractTokenType(token);

            String finalStoreId;
            String currentRole;

            // 3. 根据令牌类型提取必要信息
            if (tokenType == 1) {
                // 普通令牌（商家或员工自查）
                String userRole = jwtUtils.extractRole(token);
                
                if ("MERCHANT".equals(userRole)) {
                    // 商家查询
                    if (storeId == null || storeId.isEmpty()) {
                        return Result.fail("商家使用普通令牌时，storeId 必填");
                    }
                    finalStoreId = storeId;
                    currentRole = "MERCHANT";
                } else {
                    // 员工自查（需要先查询工作关系获取storeId）
                    if (!staffId.equals(currentUserId)) {
                        return Result.fail("员工只能查询自己的信息");
                    }
                    
                    // 如果员工自查，需要通过storeId参数指定查询哪个店铺的信息
                    if (storeId == null || storeId.isEmpty()) {
                        return Result.fail("storeId 必填");
                    }
                    finalStoreId = storeId;
                    currentRole = "STAFF";  // 员工自查
                }
            } else if (tokenType == 3) {
                // 工作令牌（商家/店长/店员）
                finalStoreId = jwtUtils.extractStoreId(token);
                String role = jwtUtils.extractRole(token);
                // 商家工作令牌的role为"merchant"，店长为"manager"，店员为"employee"
                currentRole = role;
            } else {
                return Result.fail("令牌类型错误");
            }

            // 4. 调用Service
            StaffDetailResponse response = staffService.getStaffDetail(
                    staffId, finalStoreId, currentUserId, currentRole);

            return Result.success("员工信息查询成功", response);
        } catch (Exception e) {
            log.error("员工信息查询失败", e);
            return Result.fail("员工信息查询失败：" + e.getMessage());
        }
    }

    /**
     * 员工信息查询（列表）
     * GET /api/v1/staffs
     * 
     * 权限：商家/店长
     */
    @GetMapping
    public Result<StaffListResponse> getStaffList(
            @RequestParam(required = false) String storeId,
            @RequestParam(required = false) String staffRole,
            @RequestParam(required = false) String staffName,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String authorization) {
        
        try {
            // 1. 提取令牌
            String token = extractBearerToken(authorization);
            if (token == null) {
                return Result.fail("Authorization格式错误");
            }

            // 2. 获取令牌类型
            Integer tokenType = jwtUtils.extractTokenType(token);

            String finalStoreId;

            // 3. 根据令牌类型提取storeId
            if (tokenType == 1) {
                // 普通令牌（商家）
                if (storeId == null || storeId.isEmpty()) {
                    return Result.fail("商家使用普通令牌时，storeId 必填");
                }
                finalStoreId = storeId;
            } else if (tokenType == 3) {
                // 工作令牌（商家/店长）
                finalStoreId = jwtUtils.extractStoreId(token);
            } else {
                return Result.fail("令牌类型错误");
            }

            // 4. 校验分页参数
            if (pageNum < 1) {
                pageNum = 1;
            }
            if (pageSize < 1 || pageSize > 50) {
                pageSize = 10;
            }

            // 5. 调用Service
            StaffListResponse response = staffService.getStaffList(
                    finalStoreId, staffRole, staffName, pageNum, pageSize);

            return Result.success("员工列表查询成功", response);
        } catch (Exception e) {
            log.error("员工列表查询失败", e);
            return Result.fail("员工列表查询失败：" + e.getMessage());
        }
    }

    /**
     * 接口4：员工信息修改
     * 
     * @param staffId 员工ID
     * @param request 修改请求
     * @param authorization 令牌
     * @return 修改响应
     */
    @PutMapping("/{staffId}")
    public Result<StaffUpdateResponse> updateStaff(
            @PathVariable String staffId,
            @Valid @RequestBody StaffUpdateRequest request,
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String authorization) {
        try {
            // 1. 提取令牌
            String token = extractBearerToken(authorization);
            if (token == null) {
                return Result.fail("令牌格式错误");
            }

            // 2. 提取令牌信息
            String currentUserId = jwtUtils.extractUserId(token);
            Integer tokenType = jwtUtils.extractTokenType(token);

            String finalStoreId;
            String currentRole;

            // 3. 根据令牌类型提取必要信息
            if (tokenType == 1) {
                // 普通令牌（商家）
                String userRole = jwtUtils.extractRole(token);
                
                if (!"MERCHANT".equals(userRole)) {
                    return Result.fail("普通用户不能修改员工信息");
                }
                
                if (request.getStoreId() == null || request.getStoreId().isEmpty()) {
                    return Result.fail("商家使用普通令牌时，storeId 必填");
                }
                finalStoreId = request.getStoreId();
                currentRole = "MERCHANT";
            } else if (tokenType == 3) {
                // 工作令牌（商家/店长/店员）
                finalStoreId = jwtUtils.extractStoreId(token);
                String role = jwtUtils.extractRole(token);
                
                // 根据角色设置currentRole并进行权限校验
                if ("merchant".equals(role)) {
                    // 商家拥有最高权限
                    currentRole = "MERCHANT";
                } else if ("manager".equals(role)) {
                    // 店长可以修改员工信息
                    currentRole = "MANAGER";
                } else {
                    // 店员不能修改员工信息
                    return Result.fail("店员不能修改员工信息");
                }
            } else {
                return Result.fail("令牌类型错误");
            }

            // 4. 调用Service
            StaffUpdateResponse response = staffService.updateStaff(
                    staffId, request, finalStoreId, currentUserId, currentRole);

            return Result.success("员工信息修改成功", response);
        } catch (Exception e) {
            log.error("员工信息修改失败", e);
            return Result.fail("员工信息修改失败：" + e.getMessage());
        }
    }

    /**
     * 接口5：员工角色调整
     * 
     * @param staffId 员工ID
     * @param request 调整请求
     * @param authorization 管理令牌
     * @return 调整响应
     */
    @PutMapping("/{staffId}/role")
    public Result<StaffRoleAdjustResponse> adjustStaffRole(
            @PathVariable String staffId,
            @Valid @RequestBody StaffRoleAdjustRequest request,
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String authorization) {
        try {
            // 1. 提取令牌
            String token = extractBearerToken(authorization);
            if (token == null) {
                return Result.fail("令牌格式错误");
            }

            // 2. 验证令牌类型：必须是管理令牌
            Integer tokenType = jwtUtils.extractTokenType(token);
            if (!Integer.valueOf(4).equals(tokenType)) {
                return Result.fail("需要管理令牌，请先输入二级密码获取管理令牌");
            }

            // 3. 验证角色：必须是商家
            String role = jwtUtils.extractRole(token);
            if (!"MERCHANT".equals(role)) {
                return Result.fail("仅商家可以调整员工角色");
            }

            // 4. 提取操作者ID
            String operatorId = jwtUtils.extractUserId(token);

            // 5. 调用Service
            StaffRoleAdjustResponse response = staffService.adjustStaffRole(
                    staffId, request, operatorId);

            return Result.success("员工角色调整成功", response);
        } catch (Exception e) {
            log.error("员工角色调整失败", e);
            return Result.fail("员工角色调整失败：" + e.getMessage());
        }
    }

    /**
     * 接口6：员工权限修改
     * 
     * @param staffId 员工ID
     * @param request 修改请求
     * @param authorization 令牌
     * @return 修改响应
     */
    @PutMapping("/{staffId}/permission")
    public Result<StaffPermissionUpdateResponse> updateStaffPermission(
            @PathVariable String staffId,
            @Valid @RequestBody StaffPermissionUpdateRequest request,
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String authorization) {
        try {
            // 1. 提取令牌
            String token = extractBearerToken(authorization);
            if (token == null) {
                return Result.fail("令牌格式错误");
            }

            // 2. 提取令牌信息
            String currentUserId = jwtUtils.extractUserId(token);
            Integer tokenType = jwtUtils.extractTokenType(token);

            String finalStoreId;
            String currentRole;

            // 3. 根据令牌类型提取必要信息
            if (tokenType == 1) {
                // 普通令牌（商家）
                String userRole = jwtUtils.extractRole(token);
                
                if (!"MERCHANT".equals(userRole)) {
                    return Result.fail("普通用户不能修改员工权限");
                }
                
                if (request.getStoreId() == null || request.getStoreId().isEmpty()) {
                    return Result.fail("商家使用普通令牌时，storeId 必填");
                }
                finalStoreId = request.getStoreId();
                currentRole = "MERCHANT";
            } else if (tokenType == 3) {
                // 工作令牌（店长）
                finalStoreId = jwtUtils.extractStoreId(token);
                String role = jwtUtils.extractRole(token);
                currentRole = role;  // manager 或 employee
                
                // 员工不能修改权限
                if ("employee".equals(role)) {
                    return Result.fail("店员不能修改员工权限");
                }
            } else {
                return Result.fail("令牌类型错误");
            }

            // 4. 调用Service
            StaffPermissionUpdateResponse response = staffService.updateStaffPermission(
                    staffId, request, finalStoreId, currentUserId, currentRole);

            return Result.success("员工权限修改成功", response);
        } catch (Exception e) {
            log.error("员工权限修改失败", e);
            return Result.fail("员工权限修改失败：" + e.getMessage());
        }
    }

    /**
     * 接口7：员工解绑
     * PUT /api/v1/staffs/{staffId}/unbind
     * 
     * @param staffId 员工ID
     * @param request 解绑请求
     * @param authorization 管理令牌
     * @return 解绑响应
     */
    @PutMapping("/{staffId}/unbind")
    public Result<StaffUnbindResponse> unbindStaff(
            @PathVariable String staffId,
            @Valid @RequestBody StaffUnbindRequest request,
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String authorization) {
        try {
            // 1. 提取令牌
            String token = extractBearerToken(authorization);
            if (token == null) {
                return Result.fail("令牌格式错误");
            }

            // 2. 验证令牌类型：必须是管理令牌
            Integer tokenType = jwtUtils.extractTokenType(token);
            if (!Integer.valueOf(4).equals(tokenType)) {
                return Result.fail("需要管理令牌，请先输入二级密码获取管理令牌");
            }

            // 3. 验证角色：必须是商家
            String role = jwtUtils.extractRole(token);
            if (!"MERCHANT".equals(role)) {
                return Result.fail("仅商家可以解绑员工");
            }

            // 4. 提取操作者ID
            String operatorId = jwtUtils.extractUserId(token);

            // 5. 调用Service
            StaffUnbindResponse response = staffService.unbindStaff(
                    staffId, request, operatorId);

            return Result.success("员工解绑成功", response);
        } catch (Exception e) {
            log.error("员工解绑失败", e);
            return Result.fail("员工解绑失败：" + e.getMessage());
        }
    }

    /**
     * 提取Bearer令牌
     */
    private String extractBearerToken(String authorization) {
        if (authorization != null && authorization.startsWith(TokenConstants.BEARER_PREFIX)) {
            return authorization.substring(TokenConstants.BEARER_PREFIX.length());
        }
        return null;
    }
}

