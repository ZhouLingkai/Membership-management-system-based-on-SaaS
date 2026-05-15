// pages/oss-test/oss-test.js
const ossUtil = require('../../utils/oss');
const tokenManager = require('../../utils/token');

Page({
  data: {
    // 测试前提
    isLogin: false,
    hasToken: false,
    stsStatus: 'pending', // pending, success, error
    stsMessage: '待测试',
    stsCredentials: null,
    
    // pathType 配置
    pathType: 'merchant',  // 默认值
    validPathTypes: ['merchant', 'user', 'member', 'card', 'employee', 'resource'],

    // 图片上传
    localImagePath: '',
    imageSize: '',
    generatedFileName: '',
    fullObjectName: '',
    uploadStatus: '', // '', uploading, success, error
    uploadProgress: 0,
    uploadMessage: '',

    // OSS展示
    ossObjectName: '',
    ossSignedUrl: '',
    showOssImage: false,
    countdown: 3,

    // 手动测试签名URL
    manualObjectName: '',
    manualSignedUrl: '',
    manualPathType: '',
    manualError: ''
  },

  onLoad() {
    this.checkLoginStatus();
  },

  onShow() {
    this.checkLoginStatus();
  },

  /**
   * 检查登录状态
   */
  async checkLoginStatus() {
    const app = getApp();
    const isLogin = app.globalData.isLogin || false;
    
    let hasToken = false;
    if (isLogin) {
      try {
        const token = await tokenManager.getNormalToken();
        hasToken = !!token;
        console.log('[OSS测试] 普通令牌:', token ? token.substring(0, 30) + '...' : '无');
      } catch (e) {
        console.error('[OSS测试] 获取令牌失败:', e);
      }
    }

    this.setData({
      isLogin,
      hasToken,
      stsStatus: 'pending',
      stsMessage: '待测试'
    });
  },

  /**
   * pathType 输入处理
   */
  onPathTypeInput(e) {
    this.setData({
      pathType: e.detail.value
    });
  },
  
  /**
   * 快速选择 pathType
   */
  selectPathType(e) {
    const type = e.currentTarget.dataset.type;
    this.setData({
      pathType: type
    });
  },
  
  /**
   * 清除 STS 缓存（内存 + Storage）
   */
  async clearStsCache() {
    await ossUtil.clearStsCredentials();
    this.setData({
      stsStatus: 'pending',
      stsMessage: '缓存已清除（内存 + Storage）',
      stsCredentials: null
    });
    this.showToast('缓存已清除', 'success');
  },

  /**
   * 测试获取STS凭证
   */
  async testStsCredentials() {
    const { pathType } = this.data;
    
    this.setData({
      stsStatus: 'pending',
      stsMessage: `获取中 (pathType: ${pathType})...`
    });

    try {
      console.log(`[OSS测试] 开始获取STS凭证, pathType: ${pathType}`);
      
      // 使用输入的 pathType 获取凭证
      const credentials = await ossUtil.getStsCredentials(pathType);
      
      console.log('[OSS测试] STS凭证获取成功:', credentials);
      
      // 检查 pathType 是否有效
      const isValidPath = ossUtil.isValidPathType(pathType);
      
      this.setData({
        stsStatus: 'success',
        stsMessage: `获取成功 (pathType: ${pathType}${!isValidPath ? ' → merchant' : ''})`,
        stsCredentials: credentials
      });

      this.showToast('STS凭证获取成功', 'success');
    } catch (error) {
      console.error('[OSS测试] STS凭证获取失败:', error);
      this.setData({
        stsStatus: 'error',
        stsMessage: `获取失败 (pathType: ${pathType}): ` + error.message,
        stsCredentials: null
      });

      this.showToast('获取失败: ' + error.message, 'error');
    }
  },

  /**
   * 选择图片
   */
  chooseImage() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const tempFile = res.tempFiles[0];
        const filePath = tempFile.tempFilePath;
        const fileSize = tempFile.size;

        // 生成文件名
        const ext = filePath.substring(filePath.lastIndexOf('.')) || '.jpg';
        const timestamp = Date.now();
        const fileName = `oss_test_${timestamp}${ext}`;

        // 完整路径
        const pathPrefix = this.data.stsCredentials?.pathPrefix || '';
        const fullObjectName = pathPrefix + fileName;

        // 格式化文件大小
        let sizeStr = '';
        if (fileSize < 1024) {
          sizeStr = fileSize + ' B';
        } else if (fileSize < 1024 * 1024) {
          sizeStr = (fileSize / 1024).toFixed(2) + ' KB';
        } else {
          sizeStr = (fileSize / (1024 * 1024)).toFixed(2) + ' MB';
        }

        this.setData({
          localImagePath: filePath,
          imageSize: sizeStr,
          generatedFileName: fileName,
          fullObjectName: fullObjectName,
          uploadStatus: '',
          uploadMessage: ''
        });

        console.log('[OSS测试] 选择图片:', {
          filePath,
          fileSize: sizeStr,
          fileName,
          fullObjectName
        });
      },
      fail: (err) => {
        if (!err.errMsg.includes('cancel')) {
          this.showToast('选择图片失败', 'error');
        }
      }
    });
  },

  /**
   * 预览本地图片
   */
  previewLocalImage() {
    if (this.data.localImagePath) {
      wx.previewImage({
        urls: [this.data.localImagePath],
        current: this.data.localImagePath
      });
    }
  },

  /**
   * 阻止事件冒泡
   */
  stopPropagation() {},

  /**
   * 上传到OSS
   */
  async uploadToOss() {
    const { localImagePath, generatedFileName, stsCredentials, pathType } = this.data;

    if (!localImagePath || !stsCredentials) {
      this.showToast('请先选择图片并获取STS凭证', 'warning');
      return;
    }

    this.setData({
      uploadStatus: 'uploading',
      uploadProgress: 0,
      uploadMessage: ''
    });

    try {
      console.log('[OSS测试] 开始上传...');
      console.log('[OSS测试] 文件路径:', localImagePath);
      console.log('[OSS测试] 目标文件名:', generatedFileName);
      console.log('[OSS测试] pathType:', pathType);

      // 使用当前选择的 pathType 上传
      const objectName = await ossUtil.uploadToOss(
        localImagePath,
        generatedFileName,
        {
          pathType: pathType,
          onProgress: (progress) => {
            this.setData({ uploadProgress: progress });
          }
        }
      );

      console.log('[OSS测试] 上传成功, objectName:', objectName);

      this.setData({
        uploadStatus: 'success',
        uploadMessage: '上传成功！objectName: ' + objectName,
        ossObjectName: objectName
      });

      this.showToast('上传成功', 'success');

      // 3秒后展示OSS图片
      this.startCountdown();

    } catch (error) {
      console.error('[OSS测试] 上传失败:', error);
      this.setData({
        uploadStatus: 'error',
        uploadMessage: '上传失败: ' + error.message
      });

      this.showToast('上传失败: ' + error.message, 'error');
    }
  },

  /**
   * 开始倒计时
   */
  startCountdown() {
    let countdown = 3;
    this.setData({ countdown, showOssImage: false });

    const timer = setInterval(() => {
      countdown--;
      if (countdown <= 0) {
        clearInterval(timer);
        this.setData({ countdown: 0 });
        this.loadOssImage();
      } else {
        this.setData({ countdown });
      }
    }, 1000);
  },

  /**
   * 加载OSS图片（生成签名URL）
   */
  async loadOssImage() {
    const { ossObjectName, stsCredentials } = this.data;

    if (!ossObjectName) return;

    try {
      console.log('[OSS测试] ========== 开始生成签名URL ==========');
      console.log('[OSS测试] objectName:', ossObjectName);
      console.log('[OSS测试] STS凭证:', JSON.stringify(stsCredentials, null, 2));
      
      const signedUrl = await ossUtil.generateSignedUrl(ossObjectName);
      
      console.log('[OSS测试] 最终签名URL:', signedUrl);
      console.log('[OSS测试] ========== 签名URL生成完成 ==========');
      
      // 解析URL便于调试
      if (signedUrl) {
        try {
          const urlObj = new URL(signedUrl);
          console.log('[OSS测试] URL解析:');
          console.log('  - 域名:', urlObj.origin);
          console.log('  - 路径:', urlObj.pathname);
          console.log('  - 参数:', urlObj.search);
        } catch (e) {}
      }

      this.setData({
        ossSignedUrl: signedUrl,
        showOssImage: true
      });

    } catch (error) {
      console.error('[OSS测试] 生成签名URL失败:', error);
      this.showToast('生成签名URL失败', 'error');
    }
  },
  
  /**
   * 手动刷新签名URL
   */
  async refreshSignedUrl() {
    // 清除STS缓存，重新获取
    ossUtil.clearStsCredentials();
    await this.testStsCredentials();
    await this.loadOssImage();
  },

  /**
   * 预览OSS图片
   */
  previewOssImage() {
    const { ossSignedUrl } = this.data;
    if (ossSignedUrl) {
      console.log('[OSS测试] 预览OSS图片:', ossSignedUrl);
      wx.previewImage({
        urls: [ossSignedUrl],
        current: ossSignedUrl
      });
    }
  },

  /**
   * 图片加载错误
   */
  onImageError(e) {
    console.error('[OSS测试] 图片加载失败:', e);
    this.showToast('图片加载失败，请检查签名URL', 'error');
  },

  /**
   * 复制文本
   */
  copyText(e) {
    const text = e.currentTarget.dataset.text;
    if (text) {
      wx.setClipboardData({
        data: text,
        success: () => {
          this.showToast('已复制到剪贴板', 'success');
        }
      });
    }
  },

  /**
   * 清除测试数据
   */
  clearTestData() {
    this.setData({
      localImagePath: '',
      imageSize: '',
      generatedFileName: '',
      fullObjectName: '',
      uploadStatus: '',
      uploadProgress: 0,
      uploadMessage: '',
      ossObjectName: '',
      ossSignedUrl: '',
      showOssImage: false,
      countdown: 3
    });

    this.showToast('已清除', 'success');
  },

  /**
   * 显示Toast
   */
  showToast(message, type = 'success') {
    const toast = this.selectComponent('#customToast');
    if (toast) {
      toast.showToast(message, type, 'slide', 2000);
    } else {
      wx.showToast({
        title: message,
        icon: type === 'success' ? 'success' : 'none'
      });
    }
  },

  /**
   * 手动输入ObjectName
   */
  onManualObjectNameInput(e) {
    this.setData({
      manualObjectName: e.detail.value,
      manualSignedUrl: '',
      manualPathType: '',
      manualError: ''
    });
  },

  /**
   * 测试手动输入的ObjectName生成签名URL
   */
  async testManualSignedUrl() {
    const { manualObjectName } = this.data;
    if (!manualObjectName) {
      this.showToast('请输入ObjectName', 'warning');
      return;
    }

    this.setData({
      manualSignedUrl: '',
      manualPathType: '',
      manualError: ''
    });

    try {
      console.log('[OSS测试] ========== 手动测试签名URL ==========');
      console.log('[OSS测试] 输入的ObjectName:', manualObjectName);
      
      // 推断pathType
      const pathType = ossUtil.inferPathTypeFromObjectName(manualObjectName);
      console.log('[OSS测试] 推断的pathType:', pathType);
      
      // 生成签名URL
      const signedUrl = await ossUtil.generateSignedUrl(manualObjectName);
      console.log('[OSS测试] 生成的签名URL:', signedUrl);
      console.log('[OSS测试] ========== 测试完成 ==========');

      this.setData({
        manualSignedUrl: signedUrl,
        manualPathType: pathType
      });

      if (signedUrl) {
        this.showToast('签名URL生成成功', 'success');
      } else {
        this.setData({ manualError: '生成签名URL返回空' });
        this.showToast('生成失败', 'error');
      }
    } catch (error) {
      console.error('[OSS测试] 手动测试失败:', error);
      this.setData({ manualError: error.message });
      this.showToast('测试失败: ' + error.message, 'error');
    }
  },

  /**
   * 预览手动测试的图片
   */
  previewManualImage() {
    const { manualSignedUrl } = this.data;
    if (manualSignedUrl) {
      wx.previewImage({
        urls: [manualSignedUrl],
        current: manualSignedUrl
      });
    }
  },

  /**
   * 手动测试图片加载错误
   */
  onManualImageError(e) {
    console.error('[OSS测试] 手动测试图片加载失败:', e);
    this.setData({ manualError: '图片加载失败，请检查签名URL或bucket权限' });
  }
});
