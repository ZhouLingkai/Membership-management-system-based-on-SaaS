package com.ecards.member_management.constants;

/**
 * 文件相关常量
 * 定义文件上传的文件名、路径等常量
 */
public class FileConstants {

    /**
     * 文件名常量 - 营业执照
     */
    public static final String BUSINESS_LICENSE = "businessLicense";

    /**
     * 文件名常量 - 门店照片
     */
    public static final String STORE_PHOTO = "storePhoto";

    /**
     * 文件名常量 - 用户头像（预留）
     */
    public static final String AVATAR = "avatar";

    /**
     * OSS上传基础路径 - 商户相关文件
     */
    public static final String MERCHANT_BASE_PATH = "merchant/";

    /**
     * 允许的图片格式
     */
    public static final String[] ALLOWED_IMAGE_TYPES = {
            "image/jpeg",
            "image/jpg",
            "image/png"
    };

    /**
     * 最大文件大小（5MB）
     */
    public static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private FileConstants() {
        // 私有构造函数，防止实例化
    }
}

