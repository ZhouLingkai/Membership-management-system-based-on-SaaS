// pages/upgrade-merchant/upgrade-merchant.js
const { request } = require('../../utils/request');
const tokenManager = require('../../utils/token');
const ossUtil = require('../../utils/oss');

Page({
  /**
   * 页面的初始数据
   */
  data: {
    // Toast相关
    showToast: false,
    toastMessage: '',
    toastType: 'success',
    toastAnimationType: 'slide',
    
    // 审核状态相关
    auditStatus: null, // WAIT-待审核, PASSED-通过, REJECTED-驳回, null-未提交
    auditInfo: null, // 审核信息
    showAuditView: false, // 是否显示审核状态页面
    
    // 当前选中的标签页
    currentTab: 'merchant',
    
    // 商家升级表单步骤
    merchantSteps: [
      '您当前的店铺规模是？',
      '您当前拥有的会员规模是？',
      '请选择您的营业执照！加速合作审核时间',
      '请输入准确的公司/品牌/店铺名称（一家），并拍摄一张门头店照（一家），加速合作审核时间',
      ''
    ],
    merchantCurrentStep: 0,
    
    // 商家升级表单数据
    merchantData: {
      shopScale: 0,
      memberScale: 0,
      licenseImage: '',      // OSS objectName
      licenseImageLocal: '', // 本地预览路径
      shopName: '',
      shopPhoto: '',         // OSS objectName
      shopPhotoLocal: ''     // 本地预览路径
    },
    
    // 上传状态
    uploadingLicense: false,
    uploadingShopPhoto: false,
    
    // 店铺规模选项
    shopScaleOptions: [
      { label: '1家店铺', value: 1 },
      { label: '2-5家店铺', value: 2 },
      { label: '6-10家店铺', value: 3 },
      { label: '10家以上店铺', value: 4 }
    ],
    
    // 会员规模选项
    memberScaleOptions: [
      { label: '100人以下', value: '100人以下' },
      { label: '100-500人', value: '100-500人' },
      { label: '500-1000人', value: '500-1000人' },
      { label: '1000-5000人', value: '1000-5000人' },
      { label: '5000人以上', value: '5000人以上' }
    ],
    
    // 免认证快速体验表单步骤
    quickSteps: [
      '请阅读免认证快速体验的规则',
      '请输入准确的公司/品牌/店铺名称作为您的商家名，并设置您的二级密码'
    ],
    quickCurrentStep: 0,
    
    // 免认证快速体验表单数据
    quickData: {
      rulesAgreed: false,
      merchantName: '',
      secondPassword: '',
      confirmPassword: ''
    }
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    console.log('商家升级页面加载');
  },

  /**
   * 生命周期函数--监听页面显示
   */
  async onShow() {
    // 查询审核状态
    await this.checkAuditStatus();
  },

  /**
   * 查询审核状态
   */
  async checkAuditStatus() {
    try {
      wx.showLoading({ title: '加载中...' });
      
      // 获取用户信息
      const app = getApp();
      const userInfo = app.globalData.userInfo;
      
      if (!userInfo || !userInfo.userId) {
        this.showCustomToast('用户信息不存在，请重新登录', 'danger');
        return;
      }
      
      // 获取普通令牌
      const normalToken = await tokenManager.getNormalToken();
      if (!normalToken) {
        this.showCustomToast('令牌已过期，请重新登录', 'danger');
        setTimeout(() => {
          wx.reLaunch({ url: '/pages/login/index' });
        }, 1500);
        return;
      }
      
      // 调用接口7：查询审核状态
      const res = await request.get(
        '/v1/merchants/qual-audit',
        {
          'Content-Type': 'application/json',
          'Authorization': normalToken
        },
        { userId: userInfo.userId }
      );
      
      console.log('[审核状态查询]', res);
      wx.hideLoading();
      
      if (res.code === 200 && res.data) {
        // 有审核记录
        this.setData({
          auditStatus: res.data.auditStatus,
          auditInfo: res.data,
          showAuditView: res.data.auditStatus === 'WAIT' // 待审核时显示审核页面
        });
      } else if (res.code === 404) {
        // 无审核记录，显示升级表单
        this.setData({
          auditStatus: null,
          auditInfo: null,
          showAuditView: false
        });
      } else {
        // 其他情况
        this.setData({
          showAuditView: false
        });
      }
    } catch (error) {
      wx.hideLoading();
      console.error('[审核状态查询失败]', error);
      // 查询失败，允许用户继续操作
      this.setData({
        showAuditView: false
      });
    }
  },

  /**
   * 显示自定义Toast
   */
  showCustomToast(message, type = 'success', animationType = 'slide', duration = 2000) {
    const toast = this.selectComponent('#customToast');
    if (toast) {
      toast.showToast(message, type, animationType, duration);
    }
  },

  /**
   * 切换标签页
   */
  switchTab(e) {
    const tab = e.currentTarget.dataset.tab;
    if (this.data.currentTab === tab) return;
    
    this.setData({
      currentTab: tab
    });
  },

  /**
   * 商家升级表单 - 步骤变化事件
   */
  onMerchantStepChange(e) {
    const { currentStep } = e.detail;
    this.setData({
      merchantCurrentStep: currentStep
    });
  },

  /**
   * 商家升级表单 - 店铺规模选择
   */
  onShopScaleChange(e) {
    // radio返回的是字符串，需要转为数字
    const value = Number(e.detail.value);
    this.setData({
      'merchantData.shopScale': value
    });
    
    // 更新步骤状态为已完成
    const stepForm = this.selectComponent('#merchantStepForm');
    if (stepForm) {
      stepForm.updateStepStatus(true);
    }
  },

  /**
   * 商家升级表单 - 会员规模选择
   */
  onMemberScaleChange(e) {
    // 直接使用字符串值
    const value = e.detail.value;
    this.setData({
      'merchantData.memberScale': value
    });
    
    // 更新步骤状态为已完成
    const stepForm = this.selectComponent('#merchantStepForm');
    if (stepForm) {
      stepForm.updateStepStatus(true);
    }
  },

  /**
   * 商家升级表单 - 店铺名称输入
   */
  onShopNameInput(e) {
    const value = e.detail.value;
    this.setData({
      'merchantData.shopName': value
    });
    
    // 检查当前步骤是否完成
    const { shopName, shopPhoto } = this.data.merchantData;
    const stepForm = this.selectComponent('#merchantStepForm');
    if (stepForm) {
      stepForm.updateStepStatus(!!shopName && !!shopPhoto);
    }
  },

  /**
   * 商家升级表单 - 上传营业执照
   */
  async uploadLicense() {
    try {
      // 选择图片
      const chooseRes = await new Promise((resolve, reject) => {
        wx.chooseImage({
          count: 1,
          sizeType: ['compressed'],
          sourceType: ['album', 'camera'],
          success: resolve,
          fail: reject
        });
      });
      
      const tempFilePath = chooseRes.tempFilePaths[0];
      
      // 先显示本地预览
      this.setData({
        'merchantData.licenseImageLocal': tempFilePath,
        uploadingLicense: true
      });
      
      wx.showLoading({ title: '上传中...' });
      
      // 生成文件名并上传到 OSS（使用 merchant 路径）
      const ext = tempFilePath.substring(tempFilePath.lastIndexOf('.')) || '.jpg';
      const fileName = ossUtil.generateFileName('license', ext);
      const objectName = await ossUtil.uploadToOss(tempFilePath, fileName, { pathType: 'merchant' });
      
      wx.hideLoading();
      
      // 保存 OSS 路径
      this.setData({
        'merchantData.licenseImage': objectName,
        uploadingLicense: false
      });
      
      // 更新步骤状态为已完成
      const stepForm = this.selectComponent('#merchantStepForm');
      if (stepForm) {
        stepForm.updateStepStatus(true);
      }
      
      wx.showToast({
        title: '上传成功',
        icon: 'success'
      });
      
      console.log('[UpgradeMerchant] 营业执照上传成功:', objectName);
      
    } catch (err) {
      wx.hideLoading();
      this.setData({ uploadingLicense: false });
      
      if (err.errMsg && err.errMsg.includes('cancel')) {
        return; // 用户取消选择
      }
      
      console.error('[UpgradeMerchant] 上传营业执照失败:', err);
      this.showCustomToast('上传失败，请重试', 'danger');
    }
  },

  /**
   * 商家升级表单 - 上传门头照
   */
  async uploadShopPhoto() {
    try {
      // 选择图片
      const chooseRes = await new Promise((resolve, reject) => {
        wx.chooseImage({
          count: 1,
          sizeType: ['compressed'],
          sourceType: ['album', 'camera'],
          success: resolve,
          fail: reject
        });
      });
      
      const tempFilePath = chooseRes.tempFilePaths[0];
      
      // 先显示本地预览
      this.setData({
        'merchantData.shopPhotoLocal': tempFilePath,
        uploadingShopPhoto: true
      });
      
      wx.showLoading({ title: '上传中...' });
      
      // 生成文件名并上传到 OSS（使用 merchant 路径）
      const ext = tempFilePath.substring(tempFilePath.lastIndexOf('.')) || '.jpg';
      const fileName = ossUtil.generateFileName('storePhoto', ext);
      const objectName = await ossUtil.uploadToOss(tempFilePath, fileName, { pathType: 'merchant' });
      
      wx.hideLoading();
      
      // 保存 OSS 路径
      this.setData({
        'merchantData.shopPhoto': objectName,
        uploadingShopPhoto: false
      });
      
      // 检查当前步骤是否完成
      const { shopName } = this.data.merchantData;
      const stepForm = this.selectComponent('#merchantStepForm');
      if (stepForm) {
        stepForm.updateStepStatus(!!shopName && !!objectName);
      }
      
      wx.showToast({
        title: '上传成功',
        icon: 'success'
      });
      
      console.log('[UpgradeMerchant] 门头照上传成功:', objectName);
      
    } catch (err) {
      wx.hideLoading();
      this.setData({ uploadingShopPhoto: false });
      
      if (err.errMsg && err.errMsg.includes('cancel')) {
        return; // 用户取消选择
      }
      
      console.error('[UpgradeMerchant] 上传门头照失败:', err);
      this.showCustomToast('上传失败，请重试', 'danger');
    }
  },

  /**
   * 商家升级表单 - 下一步
   */
  merchantNextStep() {
    const stepForm = this.selectComponent('#merchantStepForm');
    if (stepForm) {
      const success = stepForm.nextStep();
      if (!success) {
        wx.showToast({
          title: '已是最后一步',
          icon: 'none'
        });
      }
    }
  },

  /**
   * 商家升级表单 - 上一步
   */
  merchantPrevStep() {
    const stepForm = this.selectComponent('#merchantStepForm');
    if (stepForm) {
      const success = stepForm.prevStep();
      if (!success) {
        wx.showToast({
          title: '已是第一步',
          icon: 'none'
        });
      }
    }
  },

  /**
   * 商家升级表单 - 提交申请（接口1：applicationType=2 直接认证通道）
   */
  async submitMerchantUpgrade() {
    const { merchantData } = this.data;
    
    // 验证所有步骤数据
    if (!merchantData.shopScale || !merchantData.memberScale || 
        !merchantData.licenseImage || !merchantData.shopName || 
        !merchantData.shopPhoto) {
      this.showCustomToast('请完成所有步骤', 'danger');
      return;
    }
    
    try {
      wx.showLoading({ title: '提交中...' });
      
      // 获取用户信息
      const app = getApp();
      const userInfo = app.globalData.userInfo;
      
      if (!userInfo || !userInfo.userId) {
        this.showCustomToast('用户信息不存在，请重新登录', 'danger');
        return;
      }
      
      // 获取普通令牌
      const normalToken = await tokenManager.getNormalToken();
      if (!normalToken) {
        this.showCustomToast('令牌已过期，请重新登录', 'danger');
        return;
      }
      
      // 图片已在选择时上传到 OSS，这里直接使用 OSS objectName
      const businessLicense = merchantData.licenseImage;
      const storePhotos = merchantData.shopPhoto;
      
      console.log('[UpgradeMerchant] 提交数据:', {
        businessLicense,
        storePhotos,
        shopName: merchantData.shopName
      });
      
      // 调用接口1：商户注册（applicationType=2 直接认证通道）
      const res = await request.post(
        '/v1/merchants/registration',
        {
          'Content-Type': 'application/json',
          'Authorization': normalToken
        },
        {
          userId: userInfo.userId,
          applicationType: 2,
          numStores: merchantData.shopScale,
          numMembers: merchantData.memberScale,
          storeName: merchantData.shopName,
          storePhotos: storePhotos,
          businessLicense: businessLicense
        }
      );
      
      console.log('[商户注册]', res);
      wx.hideLoading();
      
      if (res.code === 200) {
        this.showCustomToast(res.message || '提交成功，请等待审核', 'success');
        
        // 重新查询审核状态
        setTimeout(() => {
          this.checkAuditStatus();
        }, 1500);
      } else {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
      }
    } catch (error) {
      wx.hideLoading();
      console.error('[商户注册失败]', error);
      this.showCustomToast('提交失败，请稍后重试', 'danger');
    }
  },

  /**
   * 免认证快速体验表单 - 步骤变化事件
   */
  onQuickStepChange(e) {
    const { currentStep } = e.detail;
    this.setData({
      quickCurrentStep: currentStep
    });
  },

  /**
   * 免认证快速体验表单 - 规则同意
   */
  onRulesAgreeChange(e) {
    const agreed = e.detail.value.length > 0;
    this.setData({
      'quickData.rulesAgreed': agreed
    });
    
    // 更新步骤状态
    const stepForm = this.selectComponent('#quickStepForm');
    if (stepForm) {
      stepForm.updateStepStatus(agreed);
    }
  },

  /**
   * 免认证快速体验表单 - 商家名称输入
   */
  onMerchantNameInput(e) {
    const value = e.detail.value;
    this.setData({
      'quickData.merchantName': value
    });
    
    // 检查当前步骤是否完成
    this.checkQuickStepComplete();
  },

  /**
   * 免认证快速体验表单 - 二级密码输入
   */
  onSecondPasswordInput(e) {
    const value = e.detail.value;
    this.setData({
      'quickData.secondPassword': value
    });
    
    // 检查当前步骤是否完成
    this.checkQuickStepComplete();
  },

  /**
   * 免认证快速体验表单 - 确认密码输入
   */
  onConfirmPasswordInput(e) {
    const value = e.detail.value;
    this.setData({
      'quickData.confirmPassword': value
    });
    
    // 检查当前步骤是否完成
    this.checkQuickStepComplete();
  },

  /**
   * 检查免认证快速体验表单当前步骤是否完成
   */
  checkQuickStepComplete() {
    const { merchantName, secondPassword, confirmPassword } = this.data.quickData;
    const stepForm = this.selectComponent('#quickStepForm');
    
    if (stepForm) {
      const isComplete = !!merchantName && 
                        secondPassword.length === 6 && 
                        confirmPassword.length === 6 &&
                        secondPassword === confirmPassword;
      stepForm.updateStepStatus(isComplete);
    }
  },

  /**
   * 免认证快速体验表单 - 下一步
   */
  quickNextStep() {
    const stepForm = this.selectComponent('#quickStepForm');
    if (stepForm) {
      const success = stepForm.nextStep();
      if (!success) {
        wx.showToast({
          title: '已是最后一步',
          icon: 'none'
        });
      }
    }
  },

  /**
   * 免认证快速体验表单 - 上一步
   */
  quickPrevStep() {
    const stepForm = this.selectComponent('#quickStepForm');
    if (stepForm) {
      const success = stepForm.prevStep();
      if (!success) {
        wx.showToast({
          title: '已是第一步',
          icon: 'none'
        });
      }
    }
  },

  /**
   * 免认证快速体验表单 - 提交申请（接口1：applicationType=1 免认证通道）
   */
  async submitQuickUpgrade() {
    const { quickData } = this.data;
    
    // 验证数据
    if (!quickData.rulesAgreed) {
      this.showCustomToast('请先同意规则', 'danger');
      return;
    }
    
    if (!quickData.merchantName) {
      this.showCustomToast('请输入商家名称', 'danger');
      return;
    }
    
    if (quickData.secondPassword.length < 8) {
      this.showCustomToast('密码长度至少8位', 'danger');
      return;
    }
    
    if (quickData.secondPassword !== quickData.confirmPassword) {
      this.showCustomToast('两次密码输入不一致', 'danger');
      return;
    }
    
    try {
      wx.showLoading({ title: '提交中...' });
      
      // 获取用户信息
      const app = getApp();
      const userInfo = app.globalData.userInfo;
      
      if (!userInfo || !userInfo.userId) {
        this.showCustomToast('用户信息不存在，请重新登录', 'danger');
        return;
      }
      
      // 获取普通令牌
      const normalToken = await tokenManager.getNormalToken();
      if (!normalToken) {
        this.showCustomToast('令牌已过期，请重新登录', 'danger');
        return;
      }
      
      // 调用接口1：商户注册（applicationType=1 免认证通道）
      const res = await request.post(
        '/v1/merchants/registration',
        {
          'Content-Type': 'application/json',
          'Authorization': normalToken
        },
        {
          userId: userInfo.userId,
          applicationType: 1,
          merchantName: quickData.merchantName,
          sndPswd: quickData.secondPassword
        }
      );
      
      console.log('[免认证快速体验]', res);
      wx.hideLoading();
      
      if (res.code === 200) {
        this.showCustomToast(res.message || '开通成功，体验期7天', 'success');
        
        // 成功后跳转到商家中心或刷新页面
        setTimeout(() => {
          wx.switchTab({ url: '/pages/merchant-mine/merchant-mine' });
        }, 1500);
      } else {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
      }
    } catch (error) {
      wx.hideLoading();
      console.error('[免认证快速体验失败]', error);
      this.showCustomToast('提交失败，请稍后重试', 'danger');
    }
  }
})
