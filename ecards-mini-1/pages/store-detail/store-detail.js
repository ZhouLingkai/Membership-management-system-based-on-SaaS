// pages/store-detail/store-detail.js
const { request } = require('../../utils/request');
const tokenManager = require('../../utils/token');
const ossUtil = require('../../utils/oss');

Page({
  /**
   * 页面的初始数据
   */
  data: {
    // 店铺ID
    storeId: '',
    
    // 店铺信息
    storeInfo: {},
    storePhotoUrl: '',
    licenseUrl: '',
    
    // 用户身份
    isMerchant: false,
    
    // 编辑弹窗
    showEditMask: false,
    editField: '',
    editTitle: '',
    editValue: '',
    editType: 'input', // input, date, switch
    
    // 图片操作
    showImageActionMask: false,
    showImageMask: false,
    currentImageType: '',
    viewImageUrl: '',
    
    // 上传进度
    showUploadProgress: false,
    uploadProgress: 0
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    if (options.storeId) {
      this.setData({ storeId: options.storeId });
    }
    
    // 判断用户身份
    const app = getApp();
    const userType = app.globalData.userInfo?.userType || 0;
    this.setData({
      isMerchant: userType === 2
    });
  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow() {
    if (this.data.storeId) {
      this.loadStoreDetail();
    }
  },

  /**
   * 加载店铺详情
   */
  async loadStoreDetail() {
    try {
      wx.showLoading({ title: '加载中...' });
      
      const app = getApp();
      const userInfo = app.globalData.userInfo;
      
      if (!userInfo || !userInfo.merchantInfo || !userInfo.merchantInfo.merchantId) {
        wx.hideLoading();
        this.showCustomToast('商户信息不存在', 'danger');
        return;
      }
      
      const normalToken = await tokenManager.getNormalToken();
      if (!normalToken) {
        wx.hideLoading();
        this.showCustomToast('令牌已过期，请重新登录', 'danger');
        return;
      }
      
      // 调用店铺详情查询接口
      const res = await request.get(
        `/v1/stores/${this.data.storeId}`,
        {
          'Content-Type': 'application/json',
          'Authorization': normalToken
        },
        { merchantId: userInfo.merchantInfo.merchantId }
      );
      
      console.log('[店铺详情查询]', res);
      wx.hideLoading();
      
      if (res.code === 200 && res.data) {
        this.setData({ storeInfo: res.data });
        
        // 生成图片签名URL
        await this.generateImageUrls(res.data);
      } else {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
      }
    } catch (error) {
      wx.hideLoading();
      console.error('[店铺详情查询失败]', error);
      this.showCustomToast('加载失败，请稍后重试', 'danger');
    }
  },

  /**
   * 生成图片签名URL
   */
  async generateImageUrls(storeInfo) {
    try {
      if (storeInfo.storePhotos) {
        const storePhotoUrl = await ossUtil.generateSignedUrl(storeInfo.storePhotos);
        this.setData({ storePhotoUrl });
      }
      
      if (storeInfo.businessLicense) {
        const licenseUrl = await ossUtil.generateSignedUrl(storeInfo.businessLicense);
        this.setData({ licenseUrl });
      }
    } catch (error) {
      console.error('[生成图片URL失败]', error);
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
   * 点击头部店铺照片
   */
  onPhotoTap() {
    if (!this.data.isMerchant) return;
    
    if (this.data.storePhotoUrl) {
      this.setData({
        showImageActionMask: true,
        currentImageType: 'storePhotos'
      });
    } else {
      this.uploadImage('storePhotos');
    }
  },

  /**
   * 点击信息项
   */
  onInfoItemTap(e) {
    if (!this.data.isMerchant) return;
    
    const { field, title, value, type } = e.currentTarget.dataset;
    
    this.setData({
      showEditMask: true,
      editField: field,
      editTitle: title,
      editValue: value || '',
      editType: type || 'input'
    });
  },

  /**
   * 点击图片项
   */
  onImageItemTap(e) {
    const { type } = e.currentTarget.dataset;
    const hasImage = type === 'storePhotos' ? this.data.storePhotoUrl : this.data.licenseUrl;
    
    if (hasImage) {
      this.setData({
        showImageActionMask: true,
        currentImageType: type
      });
    } else if (this.data.isMerchant) {
      this.uploadImage(type);
    }
  },

  /**
   * 编辑输入
   */
  onEditInput(e) {
    this.setData({ editValue: e.detail.value });
  },

  /**
   * 日期选择
   */
  onDateChange(e) {
    this.setData({ editValue: e.detail.value + 'T00:00:00' });
  },

  /**
   * 开关选择
   */
  onSwitchChange(e) {
    const value = parseInt(e.currentTarget.dataset.value);
    this.setData({ editValue: value });
  },

  /**
   * 关闭编辑弹窗
   */
  closeEditMask() {
    this.setData({
      showEditMask: false,
      editField: '',
      editValue: ''
    });
  },

  /**
   * 提交编辑
   */
  async submitEdit() {
    const { editField, editValue } = this.data;
    
    if (editField !== 'appointment' && !editValue) {
      this.showCustomToast('请输入内容', 'danger');
      return;
    }
    
    await this.updateStoreInfo(editField, editValue);
    this.closeEditMask();
  },

  /**
   * 更新店铺信息
   */
  async updateStoreInfo(field, value) {
    try {
      wx.showLoading({ title: '保存中...' });
      
      const app = getApp();
      const userInfo = app.globalData.userInfo;
      
      const normalToken = await tokenManager.getNormalToken();
      if (!normalToken) {
        wx.hideLoading();
        this.showCustomToast('令牌已过期，请重新登录', 'danger');
        return;
      }
      
      // 构建请求体
      const requestData = { merchantId: userInfo.merchantInfo.merchantId };
      requestData[field] = value;
      
      const res = await request.put(
        `/v1/stores/${this.data.storeId}`,
        {
          'Content-Type': 'application/json',
          'Authorization': normalToken
        },
        requestData
      );
      
      console.log('[店铺信息修改]', res);
      wx.hideLoading();
      
      if (res.code === 200) {
        this.showCustomToast('修改成功', 'success');
        // 重新加载店铺详情
        await this.loadStoreDetail();
      } else {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
      }
    } catch (error) {
      wx.hideLoading();
      console.error('[店铺信息修改失败]', error);
      this.showCustomToast('修改失败，请稍后重试', 'danger');
    }
  },

  /**
   * 查看大图
   */
  onViewImage() {
    const { currentImageType, storePhotoUrl, licenseUrl } = this.data;
    const url = currentImageType === 'storePhotos' ? storePhotoUrl : licenseUrl;
    
    this.setData({
      showImageActionMask: false,
      showImageMask: true,
      viewImageUrl: url
    });
  },

  /**
   * 上传图片
   */
  async onUploadImage() {
    this.setData({ showImageActionMask: false });
    await this.uploadImage(this.data.currentImageType);
  },

  /**
   * 执行图片上传
   */
  async uploadImage(type) {
    try {
      const prefix = type === 'storePhotos' ? 'store_photo' : 'license';
      
      this.setData({
        showUploadProgress: true,
        uploadProgress: 0
      });
      
      const objectName = await ossUtil.chooseAndUploadImage({
        prefix: prefix,
        maxSize: 5,
        onProgress: (percent) => {
          this.setData({ uploadProgress: percent });
        }
      });
      
      this.setData({ showUploadProgress: false });
      
      // 更新店铺信息
      await this.updateStoreInfo(type, objectName);
      
    } catch (error) {
      this.setData({ showUploadProgress: false });
      
      if (error.message !== '用户取消选择') {
        this.showCustomToast(error.message || '上传失败', 'danger');
      }
    }
  },

  /**
   * 关闭图片操作面板
   */
  closeImageActionMask() {
    this.setData({ showImageActionMask: false });
  },

  /**
   * 关闭图片查看
   */
  closeImageMask() {
    this.setData({
      showImageMask: false,
      viewImageUrl: ''
    });
  },

  /**
   * 阻止事件冒泡
   */
  stopPropagation() {}
})
