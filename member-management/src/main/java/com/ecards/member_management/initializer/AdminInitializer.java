package com.ecards.member_management.initializer;

import com.ecards.member_management.config.AdminProperties;
import com.ecards.member_management.constants.AdminConstants;
import com.ecards.member_management.entity.Admin;
import com.ecards.member_management.repository.AdminRepository;
import com.ecards.member_management.utils.EncryptUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 管理员系统初始化器
 * 首次启动时自动创建超级管理员
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final AdminRepository adminRepository;
    private final AdminProperties adminProperties;
    private final EncryptUtils encryptUtils;

    @Override
    public void run(ApplicationArguments args) {
        try {
            initializeSuperAdmin();
        } catch (Exception e) {
            log.error("管理员系统初始化失败", e);
        }
    }

    /**
     * 初始化超级管理员
     */
    private void initializeSuperAdmin() {
        // 检查是否已存在管理员
        long adminCount = adminRepository.count();
        if (adminCount > 0) {
            log.info("管理员系统已初始化，跳过超管创建（当前管理员数量: {}）", adminCount);
            return;
        }

        log.info("========== 开始初始化管理员系统 ==========");

        // 从配置文件读取超管信息
        AdminProperties.Init initConfig = adminProperties.getInit();
        String phone = initConfig.getPhone();
        String account = initConfig.getAccount();
        String password = initConfig.getPassword();
        String sndPassword = initConfig.getSndPassword();

        // 验证配置完整性
        if (phone == null || account == null || password == null || sndPassword == null) {
            log.error("管理员初始化配置不完整，请检查 application.properties");
            return;
        }

        // 创建超级管理员
        Admin superAdmin = new Admin();
        superAdmin.setAdminId(encryptUtils.uuidToBytes(UUID.randomUUID().toString()));
        superAdmin.setPhone(phone);
        superAdmin.setAccount(account);
        superAdmin.setPassword(encryptUtils.encryptPassword(password));
        superAdmin.setSndPswd(encryptUtils.encryptPassword(sndPassword));
        superAdmin.setAdminRole(AdminConstants.Role.SUPER_ADMIN);
        superAdmin.setTokenVersion(1);
        superAdmin.setStatus(AdminConstants.Status.ENABLED);
        superAdmin.setCreateTime(LocalDateTime.now());
        superAdmin.setUpdateTime(LocalDateTime.now());
        superAdmin.setRemark("系统初始化自动创建");

        adminRepository.save(superAdmin);

        log.info("========== 超级管理员创建成功 ==========");
        log.info("账号: {}", account);
        log.info("手机号: {}", phone);
        log.info("默认密码: {} （请及时修改）", maskPassword(password));
        log.info("默认二级密码: {} （请及时修改）", maskPassword(sndPassword));
        log.info("==========================================");
    }

    /**
     * 掩码密码（仅显示前2位和后2位）
     */
    private String maskPassword(String password) {
        if (password == null || password.length() <= 4) {
            return "****";
        }
        return password.substring(0, 2) + "****" + password.substring(password.length() - 2);
    }
}

