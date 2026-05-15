// pages/login/login.js
const { request } = require('../../utils/request');
const { encryptAES } = require('../../utils/encode');
const token = require('../../utils/token');

const API = {
  VERIFY_CODE: '/api/v1/merchant/verifyCode',
  LOGIN: '/api/v1/merchant/login',
  PASSWORD_RESET: '/api/v1/merchant/password/reset'
};

Page({
  data: {
    // Tab状态
    currentTab: 'login',
    showResetPassword: false,

    // 登录表单
    phone: '',
    password: '',
    showPassword: true,
    rememberMe: true,

    // 注册表单
    registerPhone: '13053379392',
    registerPassword: 'zhouge666',
    registerShowPassword: true,
    verifyCode: '260414',
    nickname: '',
    invitedCode: '',
    registerRememberMe: true,
    passwordLevel: '',
    passwordLevelOrder: 0,

    // 找回密码表单
    resetPhone: '',
    resetVerifyCode: '',
    newPassword: '',
    resetShowPassword: true,
    resetPasswordLevel: '',
    resetPasswordLevelOrder: 0,

    // 验证码倒计时
    verifyCodeCountdown: 0,
    resetVerifyCodeCountdown: 0,

    // 提交状态
    isSubmitting: false,

    // Toast
    showToast: false,
    toastMessage: '',
    toastType: 'info',
    toastAnimationType: '',
  },
  // ==================== Tab 切换 ====================

  switchTab(e) {
    const tab = e.currentTarget.dataset.tab;
    this.setData({ currentTab: tab });
  },

  showResetPasswordForm() {
    this.setData({ showResetPassword: true });
  },

  backToLogin() {
    this.setData({ showResetPassword: false });
  },

  // ==================== 输入处理 ====================

  onPhoneInput(e) { this.setData({ phone: e.detail.value }); },
  onPasswordInput(e) { this.setData({ password: e.detail.value }); },
  onRegisterPhoneInput(e) { this.setData({ registerPhone: e.detail.value }); },
  onRegisterPasswordInput(e) {
    const pwd = e.detail.value;
    const { level, order } = this._checkPasswordStrength(pwd);
    this.setData({ registerPassword: pwd, passwordLevel: level, passwordLevelOrder: order });
  },
  onVerifyCodeInput(e) { this.setData({ verifyCode: e.detail.value }); },
  onNicknameInput(e) { this.setData({ nickname: e.detail.value }); },
  onInvitedCodeInput(e) { this.setData({ invitedCode: e.detail.value }); },
  onResetPhoneInput(e) { this.setData({ resetPhone: e.detail.value }); },
  onResetVerifyCodeInput(e) { this.setData({ resetVerifyCode: e.detail.value }); },
  onNewPasswordInput(e) {
    const pwd = e.detail.value;
    const { level, order } = this._checkPasswordStrength(pwd);
    this.setData({ newPassword: pwd, resetPasswordLevel: level, resetPasswordLevelOrder: order });
  },

  // ==================== 密码可见性切换 ====================

  toggleShowPassword() { this.setData({ showPassword: !this.data.showPassword }); },
  toggleShowRPassword() { this.setData({ registerShowPassword: !this.data.registerShowPassword }); },
  toggleResetShowPassword() { this.setData({ resetShowPassword: !this.data.resetShowPassword }); },

  // ==================== 记住登录 ====================

  onRememberMeChange(e) {
    this.setData({ rememberMe: e.detail.value.length > 0 });
  },
  onRegisterRememberMeChange(e) {
    this.setData({ registerRememberMe: e.detail.value.length > 0 });
  },

  // ==================== 密码强度检查 ====================

  _checkPasswordStrength(pwd) {
    if (!pwd) return { level: '', order: 0 };

    let types = 0;
    if (/[a-z]/.test(pwd)) types++;
    if (/[A-Z]/.test(pwd)) types++;
    if (/\d/.test(pwd)) types++;
    if (/[^a-zA-Z0-9]/.test(pwd)) types++;

    // 基本要求：≥8位 且 含字母+数字
    if (pwd.length < 8 || !(/[a-zA-Z]/.test(pwd) && /\d/.test(pwd))) {
      return { level: 'danger', order: 1 };
    }
    if (types <= 2) return { level: 'weak', order: 2 };
    if (types === 3) return { level: 'medium', order: 3 };
    return { level: 'strong', order: 4 };
  },

  // ==================== 表单验证 ====================

  _validatePhone(phone) {
    if (!phone) { this._showMsg('请输入手机号'); return false; }
    if (!/^1\d{10}$/.test(phone)) { this._showMsg('手机号格式不正确'); return false; }
    return true;
  },

  _validatePassword(password) {
    if (!password) { this._showMsg('请输入密码'); return false; }
    if (password.length < 8) { this._showMsg('密码至少8位'); return false; }
    if (!(/[a-zA-Z]/.test(password) && /\d/.test(password))) {
      this._showMsg('密码需包含字母和数字');
      return false;
    }
    return true;
  },

  _validateVerifyCode(code) {
    if (!code || code.length !== 6) {
      this._showMsg('请输入6位验证码');
      return false;
    }
    return true;
  },

  // ==================== 发送验证码 ====================

  async getRegisterVerifyCode() {
    if (!this._validatePhone(this.data.registerPhone)) return;
    await this._sendVerifyCode(this.data.registerPhone, 1, 'verifyCodeCountdown');
  },

  async getResetVerifyCode() {
    if (!this._validatePhone(this.data.resetPhone)) return;
    await this._sendVerifyCode(this.data.resetPhone, 2, 'resetVerifyCodeCountdown');
  },

  async _sendVerifyCode(phone, type, countdownKey) {
    try {
      this.setData({ isSubmitting: true });
      console.log(encryptAES(phone))
      const res = await request.post(API.VERIFY_CODE, { 'Content-Type': 'application/json' }, { phone: encryptAES(phone), type });
      if (!res || res.code !== 200) {
        this._showMsg(res?.message || '发送失败');
        return;
      }

      this._showMsg('验证码已发送', 'success');
      this._startCountdown(countdownKey, 60);
    } catch (error) {
      console.error('[login] 发送验证码失败:', error);
    } finally {
      this.setData({ isSubmitting: false });
    }
  },

  _startCountdown(key, seconds) {
    this.setData({ [key]: seconds });
    const timer = setInterval(() => {
      const current = this.data[key];
      if (current <= 1) {
        clearInterval(timer);
        this.setData({ [key]: 0 });
      } else {
        this.setData({ [key]: current - 1 });
      }
    }, 1000);
  },

  // ==================== 登录（type=2） ====================

  async handleLogin() {
    const { phone, password, rememberMe } = this.data;
    if (!this._validatePhone(phone)) return;
    if (!password) { this._showMsg('请输入密码'); return; }

    try {
      this.setData({ isSubmitting: true });
      const deviceId = wx.getStorageSync('deviceId') || '';

      const response = await request.post(API.LOGIN, { 'Content-Type': 'application/json' }, {
        type: 2,
        phone: encryptAES(phone),
        password: encryptAES(password),
        deviceId,
      });

      if (response && response.code === 200) {
        this._handleLoginSuccess(response.data, rememberMe);
      } else if (response) {
        this._showMsg(response.message || '登录失败');
      }
    } catch (error) {
      console.error('[login] 登录失败:', error);
    } finally {
      this.setData({ isSubmitting: false });
    }
  },

  // ==================== 注册（type=1） ====================

  async handleRegister() {
    const { registerPhone, registerPassword, verifyCode, registerRememberMe } = this.data;
    if (!this._validatePhone(registerPhone)) return;
    if (!this._validatePassword(registerPassword)) return;
    if (!this._validateVerifyCode(verifyCode)) return;

    try {
      this.setData({ isSubmitting: true });
      const deviceId = wx.getStorageSync('deviceId') || '';

      const response = await request.post(API.LOGIN, { 'Content-Type': 'application/json' }, {
        type: 1,
        phone: encryptAES(registerPhone),
        password: encryptAES(registerPassword),
        verifyCode,
        deviceId,
      });

      if (response && response.code === 200) {
        this._handleLoginSuccess(response.data, registerRememberMe);
      } else if (response) {
        this._showMsg(response.message || '注册失败');
      }
    } catch (error) {
      console.error('[login] 注册失败:', error);
    } finally {
      this.setData({ isSubmitting: false });
    }
  },

  /**
   * 登录/注册成功后统一处理
   * 保存令牌 → 更新全局状态 → 跳转主页
   */
  _handleLoginSuccess(data, rememberMe) {
    if (!data) return;

    // 保存令牌：rememberMe → 保存登录令牌①（7天自动登录）
    if (rememberMe && data.loginToken) {
      token.saveMerchantLoginToken(data.loginToken, data.loginTokenExpireTime);
    }
    if (data.accessToken) {
      token.saveMerchantAccessToken(data.accessToken, data.accessTokenExpireTime);
    }

    // 更新全局状态
    const app = getApp();
    if (app) {
      app.globalData.isLoggedIn = true;
      if (data.merchantInfo) {
        app.globalData.userInfo = data.merchantInfo;
      }
    }

    this._showMsg('登录成功', 'success');
    // TODO: 跳转到主页（主页就绪后启用）
    // setTimeout(() => wx.reLaunch({ url: '/pages/index/index' }), 500);
  },

  // ==================== 重置密码 ====================

  async handleResetPassword() {
    const { resetPhone, resetVerifyCode, newPassword } = this.data;
    if (!this._validatePhone(resetPhone)) return;
    if (!this._validatePassword(newPassword)) return;
    if (!this._validateVerifyCode(resetVerifyCode)) return;

    try {
      this.setData({ isSubmitting: true });

      const response = await request.post(API.PASSWORD_RESET, { 'Content-Type': 'application/json' }, {
        phone: encryptAES(resetPhone),
        verifyCode: resetVerifyCode,
        newPassword: encryptAES(newPassword),
      });

      if (response && response.code === 200) {
        this._showMsg('密码重置成功，请重新登录', 'success');
        token.clearAllTokens();
        setTimeout(() => {
          this.setData({
            showResetPassword: false,
            currentTab: 'login',
            phone: this.data.resetPhone,
            resetPhone: '',
            resetVerifyCode: '',
            newPassword: '',
            resetPasswordLevel: '',
            resetPasswordLevelOrder: 0,
          });
        }, 1500);
      } else if (response) {
        this._showMsg(response.message || '重置失败');
      }
    } catch (error) {
      console.error('[login] 重置密码失败:', error);
    } finally {
      this.setData({ isSubmitting: false });
    }
  },

  // ==================== Toast ====================

  _showMsg(message, type = 'error') {
    this.setData({
      showToast: true,
      toastMessage: message,
      toastType: type,
    });
    setTimeout(() => {
      this.setData({ showToast: false });
    }, 2500);
  },
});