// pages/member-info/member-info.js
const { request } = require('../../utils/request');
const tokenManager = require('../../utils/token');
const { clear: clearStorage } = require('../../utils/storage-utils');
const ossUtil = require('../../utils/oss');
const { decryptAES } = require('../../utils/encode');

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
    
    // 用户信息
    userInfo: {},
    phoneDisplay: '', // 脱敏后的手机号
    avatarUrl: '',    // 头像签名URL
    memberAvatarUrl: '', // 会员头像签名URL
    
    // 编辑弹窗
    showEditMask: false,
    editField: '',
    editTitle: '',
    editValue: '',
    
    // 密码重置弹窗
    showResetMask: false,
    resetPhone: '',
    resetCode: '',
    resetNewPassword: '',
    resetConfirmPassword: '',
    codeCountdown: 0,
    
    // 图片查看
    showImageMask: false,
    viewImageUrl: '',
    
    // 头像操作
    showAvatarActionMask: false,
    currentAvatarType: '', // 'avatar' 或 'memberAvatar'
    
    // 上传进度
    showUploadProgress: false,
    uploadProgress: 0
  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow() {
    this.loadUserInfo();
  },

  /**
   * 加载用户信息
   */
  async loadUserInfo() {
    const app = getApp();
    const globalUserInfo = app.globalData.userInfo || {};
    
    // 检查是否需要从接口获取详细信息
    // 如果已有invitedCode字段，说明之前查询过详细信息
    if (globalUserInfo.invitedCode !== undefined) {
      console.log('[MemberInfo] 使用缓存的用户信息');
      await this.renderUserInfo(globalUserInfo);
    } else {
      console.log('[MemberInfo] 需要从接口获取详细信息');
      await this.fetchUserInfo();
    }
  },

  /**
   * 从接口获取用户信息（接口6）
   */
  async fetchUserInfo() {
    try {
      wx.showLoading({ title: '加载中...' });
      
      const normalToken = await tokenManager.getNormalToken();
      if (!normalToken) {
        this.showCustomToast('令牌已过期，请重新登录', 'danger');
        return;
      }
      
      const res = await request.get(
        '/v1/users/info',
        {
          'Content-Type': 'application/json',
          'Authorization': normalToken
        }
      );
      
      console.log('[用户信息查询]', res);
      wx.hideLoading();
      
      if (res.code === 200 && res.data && res.data.userInfo) {
        // 更新globalData
        const app = getApp();
        app.globalData.userInfo = {
          ...app.globalData.userInfo,
          ...res.data.userInfo
        };
        
        await this.renderUserInfo(res.data.userInfo);
      } else {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
      }
    } catch (error) {
      wx.hideLoading();
      console.error('[用户信息查询失败]', error);
      this.showCustomToast('加载失败，请稍后重试', 'danger');
    }
  },

  /**
   * 渲染用户信息到页面
   */
  async renderUserInfo(userInfo) {
    // 处理手机号：如果不是11位，说明是加密的，需要解密
    let phoneDisplay = '--';
    if (userInfo.phone) {
      let phone = userInfo.phone;
      
      // 如果手机号不是11位，尝试解密
      if (phone.length !== 11) {
        try {
          phone = decryptAES(phone);
          console.log('[MemberInfo] 手机号解密成功');
        } catch (e) {
          console.warn('[MemberInfo] 手机号解密失败:', e.message);
        }
      }
      
      // 脱敏显示
      if (phone.length === 11) {
        phoneDisplay = phone.substring(0, 3) + '****' + phone.substring(7);
      } else {
        phoneDisplay = phone;
      }
    }
    
    // 生成头像签名URL
    let avatarUrl = '';
    let memberAvatarUrl = '';
    
    if (userInfo.avatar) {
      try {
        avatarUrl = await ossUtil.generateSignedUrl(userInfo.avatar);
        console.log('[MemberInfo] 用户头像URL:', avatarUrl);
      } catch (e) {
        console.error('[MemberInfo] 用户头像签名失败:', e);
        avatarUrl = '';
      }
    }
    
    if (userInfo.memberAvatar) {
      try {
        memberAvatarUrl = await ossUtil.generateSignedUrl(userInfo.memberAvatar);
        console.log('[MemberInfo] 会员头像URL:', memberAvatarUrl);
      } catch (e) {
        console.error('[MemberInfo] 会员头像签名失败:', e);
        memberAvatarUrl = '';
      }
    }
    
    this.setData({
      userInfo: userInfo,
      phoneDisplay: phoneDisplay,
      avatarUrl: avatarUrl,
      memberAvatarUrl: memberAvatarUrl
    });
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
   * 点击顶部头像
   */
  onAvatarTap() {
    const { userInfo } = this.data;
    if (userInfo.avatar) {
      // 有头像，显示操作面板
      this.setData({
        showAvatarActionMask: true,
        currentAvatarType: 'avatar'
      });
    } else {
      // 没有头像，直接上传
      this.uploadAvatar('avatar');
    }
  },

  /**
   * 点击信息列表中的头像项
   */
  onAvatarItemTap(e) {
    const { type } = e.currentTarget.dataset;
    const { userInfo } = this.data;
    const hasAvatar = type === 'avatar' ? userInfo.avatar : userInfo.memberAvatar;
    
    if (hasAvatar) {
      // 有头像，显示操作面板
      this.setData({
        showAvatarActionMask: true,
        currentAvatarType: type
      });
    } else {
      // 没有头像，直接上传
      this.uploadAvatar(type);
    }
  },

  /**
   * 查看头像大图
   */
  onViewAvatar() {
    const { currentAvatarType, avatarUrl, memberAvatarUrl } = this.data;
    const url = currentAvatarType === 'avatar' ? avatarUrl : memberAvatarUrl;
    
    this.setData({
      showAvatarActionMask: false,
      showImageMask: true,
      viewImageUrl: url
    });
  },

  /**
   * 上传头像
   */
  onUploadAvatar() {
    const { currentAvatarType } = this.data;
    this.setData({ showAvatarActionMask: false });
    this.uploadAvatar(currentAvatarType);
  },

  /**
   * 执行头像上传
   */
  async uploadAvatar(type) {
    try {
      const prefix = type === 'avatar' ? 'avatar' : 'member_avatar';
      
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
      
      // 调用接口更新头像
      await this.updateUserInfo(type, objectName);
      
    } catch (error) {
      this.setData({ showUploadProgress: false });
      
      if (error.message !== '用户取消选择') {
        this.showCustomToast(error.message || '上传失败', 'danger');
      }
    }
  },

  /**
   * 关闭头像操作面板
   */
  closeAvatarActionMask() {
    this.setData({ showAvatarActionMask: false });
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
      editValue: value || ''
    });
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
   * 提交编辑（接口7）
   */
  async submitEdit() {
    const { editField, editValue, userInfo } = this.data;
    
    if (!editValue.trim()) {
      this.showCustomToast('请输入内容', 'danger');
      return;
    }
    
    await this.updateUserInfo(editField, editValue.trim());
    this.closeEditMask();
  },

  /**
   * 更新用户信息（接口7）
   */
  async updateUserInfo(field, value) {
    try {
      wx.showLoading({ title: '保存中...' });
      
      const normalToken = await tokenManager.getNormalToken();
      if (!normalToken) {
        this.showCustomToast('令牌已过期，请重新登录', 'danger');
        return;
      }
      
      const app = getApp();
      const userId = app.globalData.userInfo?.userId;
      
      if (!userId) {
        this.showCustomToast('用户信息异常', 'danger');
        return;
      }
      
      // 构建请求体
      const requestData = { userId: userId };
      requestData[field] = value;
      
      const res = await request.put(
        '/v1/users/info',
        {
          'Content-Type': 'application/json',
          'Authorization': normalToken
        },
        requestData
      );
      
      console.log('[用户信息修改]', res);
      wx.hideLoading();
      
      if (res.code === 200) {
        this.showCustomToast('修改成功', 'success');
        
        // 更新globalData
        app.globalData.userInfo[field] = value;
        
        // 重新渲染
        await this.renderUserInfo(app.globalData.userInfo);
      } else {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
      }
    } catch (error) {
      wx.hideLoading();
      console.error('[用户信息修改失败]', error);
      this.showCustomToast('修改失败，请稍后重试', 'danger');
    }
  },

  /**
   * 复制邀请码
   */
  onCopyInviteCode() {
    const { userInfo } = this.data;
    if (!userInfo.inviteCode) {
      this.showCustomToast('暂无邀请码', 'warning');
      return;
    }
    
    wx.setClipboardData({
      data: userInfo.inviteCode,
      success: () => {
        this.showCustomToast('邀请码已复制', 'success');
      }
    });
  },

  /**
   * 显示重置密码弹窗
   */
  showResetModal() {
    const app = getApp();
    const userInfo = app.globalData.userInfo;
    
    this.setData({
      showResetMask: true,
      resetPhone: this.data.phoneDisplay || '',
      resetCode: '',
      resetNewPassword: '',
      resetConfirmPassword: '',
      codeCountdown: 0
    });
  },

  /**
   * 关闭重置密码弹窗
   */
  closeResetMask() {
    this.setData({
      showResetMask: false,
      resetCode: '',
      resetNewPassword: '',
      resetConfirmPassword: ''
    });
    
    // 清除倒计时
    if (this.codeTimer) {
      clearInterval(this.codeTimer);
      this.codeTimer = null;
    }
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
    const app = getApp();
    const phone = app.globalData.userInfo?.phone;
    
    if (!phone) {
      this.showCustomToast('手机号不存在', 'danger');
      return;
    }
    
    try {
      // TODO: 调用发送验证码接口
      // const res = await request.post('/v1/sms/send', ...);
      
      this.showCustomToast('验证码已发送', 'success');
      
      // 开始倒计时
      this.setData({ codeCountdown: 60 });
      this.codeTimer = setInterval(() => {
        if (this.data.codeCountdown > 0) {
          this.setData({ codeCountdown: this.data.codeCountdown - 1 });
        } else {
          clearInterval(this.codeTimer);
          this.codeTimer = null;
        }
      }, 1000);
      
    } catch (error) {
      this.showCustomToast('发送失败，请稍后重试', 'danger');
    }
  },

  /**
   * 提交密码重置（接口9）
   */
  async submitPasswordReset() {
    const { resetCode, resetNewPassword, resetConfirmPassword } = this.data;
    
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
      
      const app = getApp();
      const phone = app.globalData.userInfo?.phone;
      
      // 调用接口9：密码重置
      const res = await request.post(
        '/v1/users/password/reset',
        {
          'Content-Type': 'application/json',
          'X-Device-ID': app.globalData.deviceId
        },
        {
          phone: phone, // TODO: 需要AES加密
          verifyCode: resetCode,
          newPassword: resetNewPassword,
          platform: 'MINI_PROGRAM'
        }
      );
      
      console.log('[密码重置]', res);
      wx.hideLoading();
      
      if (res.code === 200) {
        this.showCustomToast('密码重置成功，请重新登录', 'success');
        this.closeResetMask();
        
        // 密码重置后需要重新登录
        setTimeout(() => {
          this.performLogout(false);
        }, 1500);
      } else {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
      }
    } catch (error) {
      wx.hideLoading();
      console.error('[密码重置失败]', error);
      this.showCustomToast('重置失败，请稍后重试', 'danger');
    }
  },

  /**
   * 退出登录（接口8）
   */
  async handleLogout() {
    wx.showModal({
      title: '确认退出',
      content: '确定要退出登录吗？',
      confirmColor: '#ff4444',
      success: async (result) => {
        if (result.confirm) {
          await this.performLogout(true);
        }
      }
    });
  },

  /**
   * 执行退出登录
   */
  async performLogout(callApi = true) {
    try {
      wx.showLoading({ title: '退出中...' });
      
      const app = getApp();
      const userInfo = app.globalData.userInfo;
      
      if (callApi && userInfo && userInfo.userId) {
        const normalToken = await tokenManager.getNormalToken();
        const autoLoginToken = await tokenManager.getAutoLoginToken();
        
        if (normalToken) {
          // 调用接口8：用户主动退出
          await request.delete(
            '/v1/users/logout',
            {
              'Content-Type': 'application/json',
              'Authorization': normalToken,
              'X-Device-ID': app.globalData.deviceId
            },
            {
              userId: userInfo.userId,
              autoLoginToken: autoLoginToken || '',
              logoutAllDevices: false,
              platform: 'MINI_PROGRAM'
            }
          );
        }
      }
      
      // 清除本地数据
      await this.clearLocalData();
      
    } catch (error) {
      console.error('[退出登录失败]', error);
      // 即使失败也清除本地数据
      await this.clearLocalData();
    }
  },

  /**
   * 清除本地数据并跳转
   */
  async clearLocalData() {
    try {
      await clearStorage();
      
      // 清除OSS凭证缓存
      ossUtil.clearStsCredentials();
      
      const app = getApp();
      const deviceId = app.globalData.deviceId;
      
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
      app.globalData.currentUserType = 1;
      app.globalData.actualUserType = 0;
      app.globalData.selectedTabIndex = 3;
      
      app.initTabBarConfig();
      
      wx.hideLoading();
      this.showCustomToast('已退出登录', 'success');
      
      setTimeout(() => {
        wx.switchTab({
          url: '/pages/profile/profile',
          success: () => {
            app.refreshTabBar();
          }
        });
      }, 1000);
      
    } catch (error) {
      console.error('[清除本地数据失败]', error);
      wx.hideLoading();
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
    this.closeEditMask();
    this.closeResetMask();
  },

  /**
   * 阻止事件冒泡
   */
  stopPropagation() {
    // 阻止事件冒泡到mask
  },

  /**
   * 页面卸载时清理
   */
  onUnload() {
    if (this.codeTimer) {
      clearInterval(this.codeTimer);
      this.codeTimer = null;
    }
  }
})
