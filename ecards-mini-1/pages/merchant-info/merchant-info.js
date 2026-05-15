// pages/merchant-info/merchant-info.js
const { request } = require('../../utils/request');
const tokenManager = require('../../utils/token');
const { set: setStorage, remove: removeStorage, clear: clearStorage } = require('../../utils/storage-utils');
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
    
    // 商户信息（初始化为空对象，确保页面框架能显示）
    merchantInfo: {},
    
    // Mask弹窗相关
    showEditMask: false,
    editField: '', // 当前编辑的字段
    editTitle: '', // 弹窗标题
    editValue: '', // 编辑的值
    editType: 'text', // 输入类型: text/password
    
    // 二级密码修改弹窗
    showPasswordMask: false,
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
    
    // 二级密码重置弹窗
    showResetMask: false,
    resetPhone: '',
    resetCode: '',
    resetNewPassword: '',
    resetConfirmPassword: '',
    
    // 图片查看
    showImageMask: false,
    viewImageUrl: ''
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    this.loadMerchantInfo();
  },

  /**
   * 加载商户信息（接口2）
   */
  async loadMerchantInfo() {
    try {
      wx.showLoading({ title: '加载中...' });
      
      // 获取用户信息
      const app = getApp();
      const userInfo = app.globalData.userInfo;
      console.log(userInfo)
      if (!userInfo || !userInfo.merchantInfo || !userInfo.merchantInfo.merchantId) {
        this.showCustomToast('商户信息不存在', 'danger');
        return;
      }
      
      const merchantId = userInfo.merchantInfo.merchantId;
      
      // 获取普通令牌
      const normalToken = await tokenManager.getNormalToken();
      if (!normalToken) {
        this.showCustomToast('令牌已过期，请重新登录', 'danger');
        return;
      }
      
      // 调用接口2：商户基础信息查询
      const res = await request.get(
        '/v1/merchants/info',
        {
          'Content-Type': 'application/json',
          'Authorization': normalToken
        },
        { merchantId }
      );
      
      console.log('[商户信息查询]', res);
      wx.hideLoading();
      
      if (res.code === 200 && res.data) {
        this.setData({
          merchantInfo: res.data
        });
      } else {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
      }
    } catch (error) {
      wx.hideLoading();
      console.error('[商户信息查询失败]', error);
      this.showCustomToast('加载失败，请稍后重试', 'danger');
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
   * 点击信息项（可编辑字段）
   */
  onInfoItemTap(e) {
    const { field, title, value, editable } = e.currentTarget.dataset;
    
    if (!editable) return;
    
    this.setData({
      showEditMask: true,
      editField: field,
      editTitle: title,
      editValue: value || '',
      editType: 'text'
    });
  },

  /**
   * 点击查看图片
   */
  async onViewImage(e) {
    const { url } = e.currentTarget.dataset;
    
    if (!url) {
      this.showCustomToast('图片不存在', 'danger');
      return;
    }
    
    try {
      wx.showLoading({ title: '加载中...' });
      
      // 生成签名URL
      const signedUrl = await ossUtil.generateSignedUrl(url);
      
      wx.hideLoading();
      
      if (signedUrl) {
        this.setData({
          showImageMask: true,
          viewImageUrl: signedUrl
        });
      } else {
        this.showCustomToast('图片加载失败', 'danger');
      }
    } catch (error) {
      wx.hideLoading();
      console.error('[MerchantInfo] 图片签名失败:', error);
      this.showCustomToast('图片加载失败', 'danger');
    }
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
   * 编辑输入
   */
  onEditInput(e) {
    this.setData({
      editValue: e.detail.value
    });
  },

  /**
   * 提交编辑（接口3）
   */
  async submitEdit() {
    const { editField, editValue, merchantInfo } = this.data;
    
    if (!editValue) {
      this.showCustomToast('请输入内容', 'danger');
      return;
    }
    
    try {
      wx.showLoading({ title: '保存中...' });
      
      // 获取普通令牌
      const normalToken = await tokenManager.getNormalToken();
      if (!normalToken) {
        this.showCustomToast('令牌已过期，请重新登录', 'danger');
        return;
      }
      
      // 构建请求体
      const requestData = {
        merchantId: merchantInfo.merchantId
      };
      
      if (editField === 'contactEmail') {
        requestData.contactEmail = editValue;
      } else if (editField === 'merchantIntro') {
        requestData.merchantIntro = editValue;
      }
      
      // 调用接口3：商户基础信息修改
      const res = await request.put(
        '/v1/merchants/info',
        {
          'Content-Type': 'application/json',
          'Authorization': normalToken
        },
        requestData
      );
      
      console.log('[商户信息修改]', res);
      wx.hideLoading();
      
      if (res.code === 200) {
        this.showCustomToast('修改成功', 'success');
        this.closeEditMask();
        // 重新加载信息
        setTimeout(() => {
          this.loadMerchantInfo();
        }, 500);
      } else {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
      }
    } catch (error) {
      wx.hideLoading();
      console.error('[商户信息修改失败]', error);
      this.showCustomToast('修改失败，请稍后重试', 'danger');
    }
  },

  /**
   * 显示修改二级密码弹窗
   */
  showPasswordModal() {
    this.setData({
      showPasswordMask: true,
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    });
  },

  /**
   * 关闭修改二级密码弹窗
   */
  closePasswordMask() {
    this.setData({
      showPasswordMask: false,
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    });
  },

  /**
   * 修改二级密码输入
   */
  onPasswordInput(e) {
    const { field } = e.currentTarget.dataset;
    this.setData({
      [field]: e.detail.value
    });
  },

  /**
   * 提交修改二级密码（接口4）
   */
  async submitPasswordChange() {
    const { oldPassword, newPassword, confirmPassword, merchantInfo } = this.data;
    
    if (!oldPassword || !newPassword || !confirmPassword) {
      this.showCustomToast('请填写完整信息', 'danger');
      return;
    }
    
    if (newPassword.length < 8) {
      this.showCustomToast('新密码长度至少8位', 'danger');
      return;
    }
    
    if (newPassword !== confirmPassword) {
      this.showCustomToast('两次密码输入不一致', 'danger');
      return;
    }
    
    try {
      wx.showLoading({ title: '提交中...' });
      
      // 获取普通令牌
      const normalToken = await tokenManager.getNormalToken();
      if (!normalToken) {
        this.showCustomToast('令牌已过期，请重新登录', 'danger');
        return;
      }
      
      // 调用接口4：商户二级密码修改
      const res = await request.put(
        '/v1/merchants/snd-pwd',
        {
          'Content-Type': 'application/json',
          'Authorization': normalToken
        },
        {
          merchantId: merchantInfo.merchantId,
          oldSndPswd: oldPassword,
          newSndPswd: newPassword,
          confirmSndPswd: confirmPassword
        }
      );
      
      console.log('[二级密码修改]', res);
      wx.hideLoading();
      
      if (res.code === 200) {
        this.showCustomToast('密码修改成功', 'success');
        this.closePasswordMask();
      } else {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
      }
    } catch (error) {
      wx.hideLoading();
      console.error('[二级密码修改失败]', error);
      this.showCustomToast('修改失败，请稍后重试', 'danger');
    }
  },

  /**
   * 显示重置二级密码弹窗
   */
  showResetModal() {
    // 获取用户手机号
    const app = getApp();
    const userInfo = app.globalData.userInfo;
    
    this.setData({
      showResetMask: true,
      resetPhone: userInfo?.phone || '',
      resetCode: '',
      resetNewPassword: '',
      resetConfirmPassword: ''
    });
  },

  /**
   * 关闭重置二级密码弹窗
   */
  closeResetMask() {
    this.setData({
      showResetMask: false,
      resetCode: '',
      resetNewPassword: '',
      resetConfirmPassword: ''
    });
  },

  /**
   * 重置表单输入
   */
  onResetInput(e) {
    const { field } = e.currentTarget.dataset;
    this.setData({
      [field]: e.detail.value
    });
  },

  /**
   * 发送验证码
   */
  async sendVerifyCode() {
    const { resetPhone } = this.data;
    
    if (!resetPhone) {
      this.showCustomToast('手机号不能为空', 'danger');
      return;
    }
    
    try {
      // TODO: 调用发送验证码接口
      this.showCustomToast('验证码已发送', 'success');
    } catch (error) {
      this.showCustomToast('发送失败，请稍后重试', 'danger');
    }
  },

  /**
   * 提交重置二级密码（接口5）
   */
  async submitPasswordReset() {
    const { resetPhone, resetCode, resetNewPassword, resetConfirmPassword, merchantInfo } = this.data;
    
    if (!resetCode || !resetNewPassword || !resetConfirmPassword) {
      this.showCustomToast('请填写完整信息', 'danger');
      return;
    }
    
    if (resetNewPassword.length < 8) {
      this.showCustomToast('新密码长度至少8位', 'danger');
      return;
    }
    
    if (resetNewPassword !== resetConfirmPassword) {
      this.showCustomToast('两次密码输入不一致', 'danger');
      return;
    }
    
    try {
      wx.showLoading({ title: '提交中...' });
      
      // 调用接口5：商户二级密码重置（无需令牌）
      const res = await request.post(
        '/v1/merchants/snd-pwd/reset',
        {
          'Content-Type': 'application/json'
        },
        {
          merchantId: merchantInfo.merchantId,
          phone: resetPhone, // TODO: 需要AES加密
          verifyCode: resetCode,
          newSndPswd: resetNewPassword,
          platform: 'MINI_PROGRAM'
        }
      );
      
      console.log('[二级密码重置]', res);
      wx.hideLoading();
      
      if (res.code === 200) {
        this.showCustomToast('密码重置成功', 'success');
        this.closeResetMask();
      } else {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
      }
    } catch (error) {
      wx.hideLoading();
      console.error('[二级密码重置失败]', error);
      this.showCustomToast('重置失败，请稍后重试', 'danger');
    }
  },

  /**
   * 退出登录（接口6）
   */
  async handleLogout() {
    wx.showModal({
      title: '确认退出',
      content: '确定要退出登录吗？',
      confirmColor: '#ff4444',
      success: async (result) => {
        if (result.confirm) {
          await this.performLogout();
        }
      }
    });
  },

  /**
   * 执行退出登录
   */
  async performLogout() {
    try {
      wx.showLoading({ title: '退出中...' });
      
      // 获取用户信息
      const app = getApp();
      const userInfo = app.globalData.userInfo;
      
      if (!userInfo || !userInfo.userId) {
        // 直接清除本地数据
        await this.clearLocalData();
        return;
      }
      
      // 获取普通令牌和自动登录令牌
      const normalToken = await tokenManager.getNormalToken();
      const autoLoginToken = await tokenManager.getAutoLoginToken();
      
      if (!normalToken) {
        // 没有令牌，直接清除本地数据
        await this.clearLocalData();
        return;
      }
      
      // 调用接口6：用户主动退出
      const res = await request.delete(
        '/v1/users/logout',
        {
          'Content-Type': 'application/json',
          'Authorization': normalToken
        },
        {
          userId: userInfo.userId,
          autoLoginToken: autoLoginToken || '',
          logoutAllDevices: false,
          platform: 'MINI_PROGRAM'
        }
      );
      
      console.log('[退出登录]', res);
      
      // 无论接口是否成功，都清除本地数据
      await this.clearLocalData();
    } catch (error) {
      console.error('[退出登录失败]', error);
      // 即使失败也清除本地数据
      await this.clearLocalData();
    }
  },

  /**
   * 清除本地数据并跳转到未登录状态的profile页面
   */
  async clearLocalData() {
    try {
      // 清除所有本地存储
      await clearStorage();
      
      // 重置globalData为未登录状态
      const app = getApp();
      const deviceId = app.globalData.deviceId; // 保留设备ID
      
      app.globalData.isLogin = false;
      app.globalData.userInfo = { userType: 0 };
      app.globalData.tokens = {
        normalToken: null,
        workTokens: [],
        privilegeToken: null,
        managerToken: null,
        autoLoginToken: null
      };
      app.globalData.deviceId = deviceId;
      
      // 重置用户类型为未登录状态（显示普通用户tabBar）
      app.globalData.currentUserType = 1;  // 未登录显示普通用户tabBar
      app.globalData.actualUserType = 0;   // 实际类型为未登录
      app.globalData.selectedTabIndex = 3; // profile是第4个tab（索引3）
      
      // 重新初始化tabBar配置
      app.initTabBarConfig();
      
      console.log('[退出登录] globalData已重置:', {
        currentUserType: app.globalData.currentUserType,
        actualUserType: app.globalData.actualUserType,
        isLogin: app.globalData.isLogin
      });
      
      wx.hideLoading();
      this.showCustomToast('已退出登录', 'success');
      
      // 跳转到profile页面（未登录状态的"我的"页面）
      setTimeout(() => {
        wx.switchTab({ 
          url: '/pages/profile/profile',
          success: () => {
            // 刷新tabBar
            app.refreshTabBar();
          }
        });
      }, 1000);
    } catch (error) {
      console.error('[清除本地数据失败]', error);
      wx.hideLoading();
      // 出错也尝试跳转到profile
      wx.switchTab({ url: '/pages/profile/profile' });
    }
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
   * 点击mask背景关闭
   */
  onMaskTap() {
    // 关闭所有弹窗
    this.closeEditMask();
    this.closePasswordMask();
    this.closeResetMask();
    this.closeImageMask();
  },

  /**
   * 阻止事件冒泡
   */
  stopPropagation() {
    // 阻止事件冒泡到mask
  }
})