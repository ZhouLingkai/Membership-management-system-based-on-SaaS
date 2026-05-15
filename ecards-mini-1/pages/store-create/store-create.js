// pages/store-create/store-create.js
const { request } = require('../../utils/request');
const tokenManager = require('../../utils/token');
const ossUtil = require('../../utils/oss');

Page({
  /**
   * 页面的初始数据
   */
  data: {
    // 步骤配置
    steps: [
      '请填写店铺基本信息',
      '请上传一张门头店照',
      '请上传一张营业执照',
      '请填写联系方式等信息'
    ],
    currentStep: 0,
    
    // 表单数据
    formData: {
      storeName: '',
      storeType: '',
      storeAddress: '',
      storePhotos: '',
      storePhotosLocal: '',
      businessLicense: '',
      businessLicenseLocal: '',
      contactPhone: '',
      contactWx: '',
      appointment: 0,
      businessHours: '',
      openStoreTime: ''
    },
    
    // 地址相关
    region: [],
    detailAddress: '',
    
    // 建店时间
    openStoreDate: '',
    
    // 店铺类型选项
    storeTypeOptions: ['理发店', '美容店', '美甲店', '棋牌店', '台球店', '桌游店', '便利店', '餐饮店', '其他'],
    showStoreTypeList: false,
    
    // 上传状态
    showUploadProgress: false,
    uploadProgress: 0,
    
    // 提交状态
    submitting: false
  },

  /**
   * 计算属性：是否可以提交
   */
  get canSubmit() {
    const { formData } = this.data;
    return formData.storeName && 
           formData.storePhotos && 
           formData.businessLicense && 
           formData.contactPhone && 
           formData.contactWx;
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    // 初始化
  },

  /**
   * 步骤变化事件
   */
  onStepChange(e) {
    const { currentStep } = e.detail;
    this.setData({ currentStep });
  },

  /**
   * 输入变化
   */
  onInputChange(e) {
    const { field } = e.currentTarget.dataset;
    const value = e.detail.value;
    
    this.setData({
      [`formData.${field}`]: value
    });
    
    // 如果是店铺类型输入，隐藏下拉列表
    if (field === 'storeType') {
      this.setData({ showStoreTypeList: false });
    }
    
    this.updateStepStatus();
  },

  /**
   * 店铺类型输入框获得焦点
   */
  onStoreTypeFocus() {
    this.setData({ showStoreTypeList: true });
  },

  /**
   * 选择店铺类型
   */
  onStoreTypeSelect(e) {
    const value = e.currentTarget.dataset.value;
    this.setData({
      'formData.storeType': value,
      showStoreTypeList: false
    });
  },

  /**
   * 地区选择
   */
  onRegionChange(e) {
    const region = e.detail.value;
    this.setData({ region });
    this.updateAddress();
  },

  /**
   * 详细地址输入
   */
  onDetailAddressInput(e) {
    this.setData({ detailAddress: e.detail.value });
    this.updateAddress();
  },

  /**
   * 更新完整地址
   */
  updateAddress() {
    const { region, detailAddress } = this.data;
    const fullAddress = region.join('') + detailAddress;
    this.setData({
      'formData.storeAddress': fullAddress
    });
  },

  /**
   * 预约功能选择
   */
  onAppointmentChange(e) {
    const value = parseInt(e.currentTarget.dataset.value);
    this.setData({
      'formData.appointment': value
    });
    this.updateStepStatus();
  },

  /**
   * 建店时间选择
   */
  onOpenTimeChange(e) {
    const date = e.detail.value;
    this.setData({
      openStoreDate: date,
      'formData.openStoreTime': date + 'T00:00:00'
    });
  },

  /**
   * 上传门头店照
   */
  async uploadStorePhoto() {
    await this.uploadImage('storePhotos', 'store_photo');
  },

  /**
   * 上传营业执照
   */
  async uploadLicense() {
    await this.uploadImage('businessLicense', 'license');
  },

  /**
   * 通用图片上传
   */
  async uploadImage(field, prefix) {
    try {
      // 选择图片
      const chooseRes = await new Promise((resolve, reject) => {
        wx.chooseMedia({
          count: 1,
          mediaType: ['image'],
          sourceType: ['album', 'camera'],
          success: resolve,
          fail: reject
        });
      });
      
      const tempFile = chooseRes.tempFiles[0];
      const filePath = tempFile.tempFilePath;
      
      // 显示本地预览
      this.setData({
        [`formData.${field}Local`]: filePath,
        showUploadProgress: true,
        uploadProgress: 0
      });
      
      // 生成文件名并上传
      const ext = filePath.substring(filePath.lastIndexOf('.')) || '.jpg';
      const fileName = ossUtil.generateFileName(prefix, ext);
      
      const objectName = await ossUtil.uploadToOss(filePath, fileName, {
        pathType: 'merchant',
        onProgress: (progress) => {
          this.setData({ uploadProgress: progress });
        }
      });
      
      this.setData({
        [`formData.${field}`]: objectName,
        showUploadProgress: false
      });
      
      this.showCustomToast('上传成功', 'success');
      this.updateStepStatus();
      
    } catch (error) {
      this.setData({ showUploadProgress: false });
      
      if (error.errMsg && error.errMsg.includes('cancel')) {
        return;
      }
      
      console.error('[上传失败]', error);
      this.showCustomToast('上传失败，请重试', 'danger');
    }
  },

  /**
   * 更新步骤状态
   */
  updateStepStatus() {
    const { currentStep, formData } = this.data;
    const stepForm = this.selectComponent('#storeStepForm');
    
    if (!stepForm) return;
    
    let isComplete = false;
    
    switch (currentStep) {
      case 0:
        isComplete = !!formData.storeName;
        break;
      case 1:
        isComplete = !!formData.storePhotos;
        break;
      case 2:
        isComplete = !!formData.businessLicense;
        break;
      case 3:
        isComplete = !!formData.contactPhone && !!formData.contactWx;
        break;
    }
    
    stepForm.updateStepStatus(isComplete);
    
    // 更新canSubmit
    this.setData({
      canSubmit: formData.storeName && 
                 formData.storePhotos && 
                 formData.businessLicense && 
                 formData.contactPhone && 
                 formData.contactWx
    });
  },

  /**
   * 下一步
   */
  nextStep() {
    const stepForm = this.selectComponent('#storeStepForm');
    if (stepForm) {
      stepForm.nextStep();
    }
  },

  /**
   * 上一步
   */
  prevStep() {
    const stepForm = this.selectComponent('#storeStepForm');
    if (stepForm) {
      stepForm.prevStep();
    }
  },

  /**
   * 提交表单
   */
  async submitForm() {
    const { formData, submitting } = this.data;
    
    if (submitting) return;
    
    // 验证必填项
    if (!formData.storeName) {
      this.showCustomToast('请输入店铺名称', 'danger');
      return;
    }
    if (!formData.storePhotos) {
      this.showCustomToast('请上传门头店照', 'danger');
      return;
    }
    if (!formData.businessLicense) {
      this.showCustomToast('请上传营业执照', 'danger');
      return;
    }
    if (!formData.contactPhone) {
      this.showCustomToast('请输入联系电话', 'danger');
      return;
    }
    if (!formData.contactWx) {
      this.showCustomToast('请输入联系微信', 'danger');
      return;
    }
    
    try {
      this.setData({ submitting: true });
      wx.showLoading({ title: '提交中...' });
      
      const app = getApp();
      const userInfo = app.globalData.userInfo;
      
      if (!userInfo || !userInfo.merchantInfo || !userInfo.merchantInfo.merchantId) {
        wx.hideLoading();
        this.setData({ submitting: false });
        this.showCustomToast('商户信息不存在', 'danger');
        return;
      }
      
      const normalToken = await tokenManager.getNormalToken();
      if (!normalToken) {
        wx.hideLoading();
        this.setData({ submitting: false });
        this.showCustomToast('令牌已过期，请重新登录', 'danger');
        return;
      }
      
      // 构建请求数据
      const requestData = {
        merchantId: userInfo.merchantInfo.merchantId,
        storeName: formData.storeName,
        storePhotos: formData.storePhotos,
        businessLicense: formData.businessLicense,
        contactPhone: formData.contactPhone,
        contactWx: formData.contactWx
      };
      
      // 可选字段
      if (formData.storeType) {
        requestData.storeType = formData.storeType;
      }
      if (formData.storeAddress) {
        requestData.storeAddress = formData.storeAddress;
      }
      if (formData.businessHours) {
        requestData.businessHours = formData.businessHours;
      }
      if (formData.openStoreTime) {
        requestData.openStoreTime = formData.openStoreTime;
      }
      if (formData.appointment !== undefined) {
        requestData.appointment = formData.appointment;
      }
      
      // 调用创建店铺接口
      const res = await request.post(
        '/v1/stores',
        {
          'Content-Type': 'application/json',
          'Authorization': normalToken
        },
        requestData
      );
      
      console.log('[店铺创建]', res);
      wx.hideLoading();
      this.setData({ submitting: false });
      
      if (res.code === 200) {
        this.showCustomToast('店铺创建成功', 'success');
        
        // 返回店铺管理页面
        setTimeout(() => {
          wx.navigateBack();
        }, 1500);
      } else {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
      }
    } catch (error) {
      wx.hideLoading();
      this.setData({ submitting: false });
      console.error('[店铺创建失败]', error);
      this.showCustomToast('创建失败，请稍后重试', 'danger');
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
  }
})
