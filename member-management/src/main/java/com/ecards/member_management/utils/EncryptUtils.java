package com.ecards.member_management.utils;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;
import java.util.UUID;

/**
 * 加密工具类
 * 提供AES-256-CBC加密、Argon2密码加密、UUID与BINARY(16)转换功能
 */
@Slf4j
@Component
public class EncryptUtils {

    /**
     * AES密钥（从配置文件读取）
     */
    private String aesSecretKey;

    /**
     * Argon2密码编码器
     * 参数：saltLength=16, hashLength=32, parallelism=1, memory=65536, iterations=2
     * 优化：迭代次数由3降为2，提升密码验证速度（预期提速30-35%）
     */
    private final Argon2PasswordEncoder argon2Encoder = 
            new Argon2PasswordEncoder(16, 32, 1, 65536, 2);

    /**
     * AES-CBC 参数
     */
    private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int CBC_IV_LENGTH = 16; // CBC使用16字节IV

    static {
        // 注册BouncyCastle Provider
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * 注入AES密钥配置
     *
     * @param secretKey 配置文件中的密钥
     */
    @Value("${encryption.aes.secret-key}")
    public void setAesSecretKey(String secretKey) {
        this.aesSecretKey = secretKey;
    }

    // ==================== AES-256-CBC 加密/解密方法 ====================

    /**
     * AES-256-CBC 加密（用于加密手机号）
     *
     * @param plainText 明文
     * @return Base64编码的密文（包含IV和密文）
     * @throws Exception 加密失败时抛出异常
     */
    public String encryptAES(String plainText) {
        try {
            if (plainText == null || plainText.isEmpty()) {
                return null;
            }

            // 解码Base64密钥
            byte[] keyBytes = Base64.getDecoder().decode(aesSecretKey);
            SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");

            // 生成随机IV（16字节）
            byte[] iv = new byte[CBC_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            // 初始化Cipher
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);

            // 加密
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 将IV和密文组合：IV + 密文
            byte[] combined = new byte[CBC_IV_LENGTH + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, CBC_IV_LENGTH);
            System.arraycopy(cipherText, 0, combined, CBC_IV_LENGTH, cipherText.length);

            // Base64编码
            return Base64.getEncoder().encodeToString(combined);

        } catch (Exception e) {
            log.error("AES加密失败: plainText={}", plainText, e);
            throw new RuntimeException("AES加密失败", e);
        }
    }
    /**
     * AES-256-CBC 解密（用于解密手机号）
     *
     * @param encryptedText Base64编码的密文（包含IV和密文）
     * @return 明文
     * @throws Exception 解密失败时抛出异常
     */
    public String decryptAES(String encryptedText) {
        try {
            if (encryptedText == null || encryptedText.isEmpty()) {
                return null;
            }

            // Base64 解码（前端是 Base64(IV + ciphertext)）
            byte[] combined = Base64.getDecoder().decode(encryptedText);

            // 分离 IV 和 密文
            final int CBC_IV_LENGTH = 16; // AES-CBC 固定 16 字节 IV
            if (combined.length <= CBC_IV_LENGTH) {
                throw new IllegalArgumentException("密文长度非法，缺少IV或数据内容");
            }

            byte[] iv = new byte[CBC_IV_LENGTH];
            byte[] cipherText = new byte[combined.length - CBC_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, CBC_IV_LENGTH);
            System.arraycopy(combined, CBC_IV_LENGTH, cipherText, 0, cipherText.length);

            // Base64 解码密钥（32字节 = AES-256）
            byte[] keyBytes = Base64.getDecoder().decode(aesSecretKey);
            if (keyBytes.length != 32) {
                throw new IllegalArgumentException("密钥长度必须为32字节(AES-256)");
            }

            SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            // 初始化 Cipher（模式与前端一致：AES/CBC/PKCS5Padding）
            final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);

            // 解密
            byte[] plainText = cipher.doFinal(cipherText);
            log.info("解密后的明文: {}", new String(plainText, StandardCharsets.UTF_8));    
            return new String(plainText, StandardCharsets.UTF_8);

        } catch (Exception e) {
            
            log.error("AES解密失败: encryptedText={}", encryptedText, e);
            throw new RuntimeException("AES解密失败", e);
        }
    }

    // /**
    //  * AES-256-CBC 解密（用于解密手机号）
    //  *
    //  * @param encryptedText Base64编码的密文（包含IV和密文）
    //  * @return 明文
    //  * @throws Exception 解密失败时抛出异常
    //  */
    // public String decryptAES(String encryptedText) {
        
    //     try {
    //         if (encryptedText == null || encryptedText.isEmpty()) {
    //             return null;
    //         }

    //         // Base64解码
    //         byte[] combined = Base64.getDecoder().decode(encryptedText);

    //         // 分离IV和密文
    //         byte[] iv = new byte[CBC_IV_LENGTH];
    //         byte[] cipherText = new byte[combined.length - CBC_IV_LENGTH];
    //         System.arraycopy(combined, 0, iv, 0, CBC_IV_LENGTH);
    //         System.arraycopy(combined, CBC_IV_LENGTH, cipherText, 0, cipherText.length);

    //         // 解码Base64密钥
    //         byte[] keyBytes = Base64.getDecoder().decode(aesSecretKey);
    //         SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");

    //         // 初始化Cipher
    //         Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
    //         IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
    //         cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);

    //         // 解密
    //         byte[] plainText = cipher.doFinal(cipherText);

    //         return new String(plainText, StandardCharsets.UTF_8);

    //     } catch (Exception e) {
    //         log.error("AES解密失败: encryptedText={}", encryptedText, e);
    //         throw new RuntimeException("AES解密失败", e);
    //     }
    
        
    // }

    // ==================== Argon2 密码加密/校验方法 ====================

    /**
     * Argon2 密码加密
     *
     * @param rawPassword 原始密码
     * @return 加密后的密码哈希值
     */
    public String encryptPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        
        // ===== 性能计时开始 (临时调试代码) =====
        long startTime = System.currentTimeMillis();
        String encodedPassword = argon2Encoder.encode(rawPassword);
        long duration = System.currentTimeMillis() - startTime;
        log.info("【Argon2性能】密码加密耗时: {}ms", duration);
        // ===== 性能计时结束 =====
        
        return encodedPassword;
    }

    /**
     * Argon2 密码校验
     *
     * @param rawPassword     原始密码
     * @param encodedPassword 加密后的密码哈希值
     * @return true-密码匹配，false-密码不匹配
     */
    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        
        // ===== 性能计时开始 (临时调试代码) =====
        long startTime = System.currentTimeMillis();
        boolean matches = argon2Encoder.matches(rawPassword, encodedPassword);
        long duration = System.currentTimeMillis() - startTime;
        log.info("【Argon2性能】密码校验耗时: {}ms, 结果: {}", duration, matches ? "匹配" : "不匹配");
        // ===== 性能计时结束 =====
        
        return matches;
    }

    // ==================== UUID 与 BINARY(16) 转换方法 ====================

    /**
     * UUID 转 BINARY(16)
     * 适配 user_id、merchant_id、store_id 等敏感ID数据库存储格式
     *
     * @param uuid UUID字符串
     * @return BINARY(16)字节数组
     */
    public byte[] uuidToBytes(String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            return null;
        }
        try {
            UUID uuidObj = UUID.fromString(uuid);
            ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
            byteBuffer.putLong(uuidObj.getMostSignificantBits());
            byteBuffer.putLong(uuidObj.getLeastSignificantBits());
            return byteBuffer.array();
        } catch (IllegalArgumentException e) {
            log.error("UUID转换失败: uuid={}", uuid, e);
            throw new IllegalArgumentException("无效的UUID格式: " + uuid, e);
        }
    }

    /**
     * UUID对象转BINARY(16)
     *
     * @param uuid UUID对象
     * @return BINARY(16)字节数组
     */
    public byte[] uuidToBytes(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
        byteBuffer.putLong(uuid.getMostSignificantBits());
        byteBuffer.putLong(uuid.getLeastSignificantBits());
        return byteBuffer.array();
    }

    /**
     * BINARY(16) 转 UUID字符串
     * 适配数据库读取后代码层UUID类型处理
     *
     * @param bytes BINARY(16)字节数组
     * @return UUID字符串
     */
    public String bytesToUuid(byte[] bytes) {
        if (bytes == null || bytes.length != 16) {
            return null;
        }
        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
            long mostSigBits = byteBuffer.getLong();
            long leastSigBits = byteBuffer.getLong();
            UUID uuid = new UUID(mostSigBits, leastSigBits);
            return uuid.toString();
        } catch (Exception e) {
            log.error("BINARY转UUID失败", e);
            throw new IllegalArgumentException("无效的BINARY(16)数据", e);
        }
    }

    /**
     * BINARY(16) 转 UUID对象
     *
     * @param bytes BINARY(16)字节数组
     * @return UUID对象
     */
    public UUID bytesToUuidObject(byte[] bytes) {
        if (bytes == null || bytes.length != 16) {
            return null;
        }
        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
            long mostSigBits = byteBuffer.getLong();
            long leastSigBits = byteBuffer.getLong();
            return new UUID(mostSigBits, leastSigBits);
        } catch (Exception e) {
            log.error("BINARY转UUID对象失败", e);
            throw new IllegalArgumentException("无效的BINARY(16)数据", e);
        }
    }

    /**
     * 生成随机UUID字符串
     *
     * @return UUID字符串
     */
    public String generateUuid() {
        return UUID.randomUUID().toString();
    }

    /**
     * 生成用于AES加密的随机密钥（Base64编码，32字节）
     * 注意：此方法仅用于生成新密钥，不应在生产环境频繁调用
     *
     * @return Base64编码的密钥字符串
     */
    public String generateAESKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256, new SecureRandom());
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            log.error("生成AES密钥失败", e);
            throw new RuntimeException("生成AES密钥失败", e);
        }
    }
}

