// pages/login/index.js
const { post } = require('../../utils/request');
const { validatePhone, validatePassword } = require('../../utils/validator-util');
const { encryptAES } = require('../../utils/encode');
const { saveNormalToken, saveAutoLoginToken } = require('../../utils/token');
const app = getApp();

Page({
    /**
     * 页面的初始数据
     */
    data: {
        // 当前标签页：login 或 register
        currentTab: 'login',
        // 是否显示找回密码表单
        showResetPassword: false,

        // 登录表单数据
        phone: '13800137000',
        password: 'zhouge666',
        showPassword: true,
        rememberMe: false,

        // 注册表单数据
        registerPhone: '13800137000',
        registerPassword: 'zhouge666',
        registerShowPassword: true,
        secondPassword: '',
        verifyCode: '',
        nickname: '',
        invitedCode: '',
        registerRememberMe: false,
        passwordLevel: '',
        passwordLevelOrder: 0,
        // 找回密码表单数据
        resetPhone: '',
        resetVerifyCode: '',
        resetShowPassword: false,
        newPassword: '',
        resetPasswordLevel: '',
        resetPasswordLevelOrder: 0,

        // 验证码倒计时
        verifyCodeCountdown: 0,
        resetVerifyCodeCountdown: 0,

        // 提交状态
        isSubmitting: false,

        // 自定义消息提示
        showToast: false,
        toastMessage: '',
        toastType: 'success',
        toastAnimationType: 'slide',

        // 密码输错记录（本地存储key前缀）
        loginErrorKeyPrefix: 'login_error_',
    },

    /**
     * 生命周期函数--监听页面加载
     */
    onLoad(options) {
        // 如果有传入手机号，填充到登录表单
        if (options.phone) {
            this.setData({
                phone: options.phone
            });
        }
        // console.log(encryptAES('13053379392'))
        // console.log(this.generateMemberCode())
    },

    /**
     * 切换标签页
     */
    switchTab(e) {
        const tab = e.currentTarget.dataset.tab;
        if (this.data.currentTab === tab) return;
        if (this.data.currentTab == 'register' && this.data.registerPhone.length == 11) {
            this.setData({
                phone: this.data.registerPhone
            })
        }
        this.setData({
            currentTab: tab,
            showResetPassword: false
        });
    },

    /**
     * 显示找回密码表单
     */
    showResetPasswordForm() {
        const phone = this.data.phone;
        this.setData({
            showResetPassword: true,
            resetPhone: phone
        });
    },

    /**
     * 返回登录表单
     */
    backToLogin() {
        this.setData({
            showResetPassword: false,
            resetVerifyCode: '',
            newPassword: ''
        });
    },

    /**
     * 输入框事件处理
     */
    onPhoneInput(e) {
        this.setData({
            phone: e.detail.value
        });
    },
    onResetPhoneInput(e) {
        this.setData({
            resetPhone: e.detail.value
        });
    },
    onPasswordInput(e) {
        this.setData({
            password: e.detail.value
        });
    },

    onRememberMeChange(e) {
        this.setData({
            rememberMe: e.detail.value.length > 0
        });
    },

    onRegisterPhoneInput(e) {
        this.setData({
            registerPhone: e.detail.value
        });

    },

    onRegisterPasswordInput(e) {
        this.setData({
            registerPassword: e.detail.value
        });

        const passwordRes = validatePassword(e.detail.value);
        let levelOrder = 0;
        if (e.detail.value == '') {
            this.setData({
                passwordLevel: '',
                passwordLevelOrder: 0
            })
            return
        }
        if (passwordRes.level == 'danger') {
            levelOrder = 1;
        } else if (passwordRes.level == 'weak') {
            levelOrder = 2;
        } else if (passwordRes.level == 'medium') {
            levelOrder = 3;
        } else if (passwordRes.level == 'strong') {
            levelOrder = 4;
        }
        this.setData({
            passwordLevel: passwordRes.level,
            passwordLevelOrder: levelOrder
        })
    },

    onVerifyCodeInput(e) {
        this.setData({
            verifyCode: e.detail.value
        });
    },

    onNicknameInput(e) {
        this.setData({
            nickname: e.detail.value
        });
    },

    onInvitedCodeInput(e) {
        this.setData({
            invitedCode: e.detail.value
        });
    },

    onRegisterRememberMeChange(e) {
        this.setData({
            registerRememberMe: e.detail.value.length > 0
        });
    },

    onResetVerifyCodeInput(e) {
        this.setData({
            resetVerifyCode: e.detail.value
        });
    },

    onNewPasswordInput(e) {
        this.setData({
            newPassword: e.detail.value
        });

        // 密码强度检测
        const passwordRes = validatePassword(e.detail.value);
        let levelOrder = 0;
        if (e.detail.value == '') {
            this.setData({
                resetPasswordLevel: '',
                resetPasswordLevelOrder: 0
            })
            return
        }
        if (passwordRes.level == 'danger') {
            levelOrder = 1;
        } else if (passwordRes.level == 'weak') {
            levelOrder = 2;
        } else if (passwordRes.level == 'medium') {
            levelOrder = 3;
        } else if (passwordRes.level == 'strong') {
            levelOrder = 4;
        }
        this.setData({
            resetPasswordLevel: passwordRes.level,
            resetPasswordLevelOrder: levelOrder
        })
    },

    /**
     * 显示自定义消息提示
     */
    showCustomToast(message, type = 'success', animationType = 'slide') {
        // 先隐藏，确保动画能重新触发
        this.setData({
            showToast: false
        });

        // 下一帧再显示
        setTimeout(() => {
            this.setData({
                showToast: true,
                toastMessage: message,
                toastType: type,
                toastAnimationType: animationType
            });
        }, 50);
    },

    /**
     * 检查手机号是否被禁止登录（5分钟内）
     */
    checkPhoneBlocked(phone) {
        const errorKey = `${this.data.loginErrorKeyPrefix}${phone}`;
        const errorData = wx.getStorageSync(errorKey);

        if (!errorData) {
            return { blocked: false };
        }

        const { count, lastErrorTime } = errorData;
        const now = Date.now();
        const fiveMinutes = 5 * 60 * 1000;

        // 如果超过5分钟，清除记录
        if (now - lastErrorTime > fiveMinutes) {
            wx.removeStorageSync(errorKey);
            return { blocked: false };
        }

        // 如果连续5次输错，且未超过5分钟，禁止登录
        if (count >= 5) {
            const remainingMinutes = Math.ceil((fiveMinutes - (now - lastErrorTime)) / 60000);
            return {
                blocked: true,
                message: `连续5次输错，${remainingMinutes}分钟后再试`
            };
        }

        return { blocked: false, count };
    },

    /**
     * 记录密码输错
     */
    recordLoginError(phone) {
        const errorKey = `${this.data.loginErrorKeyPrefix}${phone}`;
        const errorData = wx.getStorageSync(errorKey) || { count: 0, lastErrorTime: 0 };
        const now = Date.now();
        const fiveMinutes = 5 * 60 * 1000;

        // 如果上次错误超过5分钟，重置计数
        if (now - errorData.lastErrorTime > fiveMinutes) {
            errorData.count = 0;
        }

        errorData.count += 1;
        errorData.lastErrorTime = now;

        // 存储错误记录（5分钟过期）
        wx.setStorageSync(errorKey, errorData);

        // 提示用户
        if (errorData.count === 3) {
            this.showCustomToast('连续3次输错密码，还有2次机会', 'danger');
        } else if (errorData.count === 4) {
            this.showCustomToast('连续4次输错密码，还有1次机会', 'danger');
        } else if (errorData.count === 5) {
            this.showCustomToast('连续5次输错，5分钟后再试', 'danger');
        }
    },

    /**
     * 清除密码输错记录（登录成功时调用）
     */
    clearLoginError(phone) {
        const errorKey = `${this.data.loginErrorKeyPrefix}${phone}`;
        wx.removeStorageSync(errorKey);
    },

    /**
     * 处理登录
     */
    async handleLogin() {
        if (this.data.isSubmitting) return;

        const { phone, password, rememberMe } = this.data;

        // 校验手机号
        const phoneRes = validatePhone(phone);
        if (!phoneRes.valid) {
            this.showCustomToast(phoneRes.message, 'danger');
            return;
        }

        // 校验密码强度
        const passwordRes = validatePassword(password);
        if (!passwordRes.valid) {
            this.showCustomToast(passwordRes.message, 'danger');
            return;
        }

        // 检查是否被禁止登录
        const blockCheck = this.checkPhoneBlocked(phone);
        if (blockCheck.blocked) {
            this.showCustomToast(blockCheck.message, 'danger');
            return;
        }

        this.setData({ isSubmitting: true });

        try {
            // 加密手机号
            const encryptedPhone = encryptAES(phone);
            // 调用登录接口
            const res = await post(
                '/v1/users/login',
                { 'Content-Type': 'application/json' },
                {
                    phone: encryptedPhone,
                    password: password,
                    rememberMe: rememberMe,
                    platform: 'MINI_PROGRAM'
                }
            );
            console.log(res)
            // 登录失败
            if(!(res.code == 200 || res.code == 0)) {
                if(res.message == "密码错误") {
                    this.recordLoginError(phone);
                }
                this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
                return
            }
            // 提示成功
            this.showCustomToast('登录成功', 'success');
            
            // 存储 token 和用户信息到 globalData
            if (res.data.normalToken && res.data.tokenExpireTime) {
                saveNormalToken(res.data.normalToken, res.data.tokenExpireTime)
            }
            if(res.data.autoLoginToken && res.data.autoExpireTime) {
                saveAutoLoginToken(res.data.autoLoginToken, res.data.autoExpireTime)
            }
            if (res.data.userInfo) {
                app.globalData.isLogin = true
                // 调用 app.onUserLogin() 来正确更新用户类型和 tabBar 配置
                app.onUserLogin(res.data.userInfo);
                console.log('[登录] 用户信息已更新:', app.globalData)
            }
            // 清除密码输错记录
            this.clearLoginError(phone);
            
        } catch (error) {
            console.error('登录失败:', error);

            // 记录密码输错
            this.recordLoginError(phone);

            // 显示错误信息
            const errorMsg = error.message || '登录失败，请稍后重试';
            this.showCustomToast(errorMsg, 'danger');
        } finally {
            this.setData({ isSubmitting: false });
        }
    },

    /**
     * 获取验证码（注册/找回密码共用）
     */
    async getVerifyCode(isReset = false) {
        const phone = isReset ? this.data.resetPhone : this.data.registerPhone;
        // 校验手机号
        const phoneRes = validatePhone(phone);
        if (!phoneRes.valid) {
            this.showCustomToast(phoneRes.message, 'danger');
            return;
        }

        // 检查倒计时
        const countdownKey = isReset ? 'resetVerifyCodeCountdown' : 'verifyCodeCountdown';
        if (this.data[countdownKey] > 0) {
            return;
        }

        try {
            // 加密手机号
            const encryptedPhone = encryptAES(phone);
            // 如果是注册，需要手机号没有注册过，如果是找回，需要手机号注册过
            const resCheck = await post(
                '/v1/users/checkPhone',
                { 'Content-Type': 'application/json' },
                {
                    phone: encryptedPhone
                }
            )
            if(resCheck.code == 200 || resCheck.code == 0) {
                if(resCheck.data.registered == true){ // 已经注册
                    if(isReset == false){ // 注册
                        this.showCustomToast('该手机号已经注册，请直接登录', 'danger');
                        return
                    }
                }else {
                    if(isReset == true){ // 找回
                        this.showCustomToast('该手机号还未注册', 'danger');
                        return
                    }
                }
            }else {
                this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
                return
            }
            
            // 调用获取验证码接口
            const res = await post(
                '/v1/users/verify-code',
                { 'Content-Type': 'application/json' },
                {
                    phone: encryptedPhone,
                    platform: 'MINI_PROGRAM'
                }
            );
            if(!(res.code == 200 || res.code == 0)) {
                this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
                return
            }
            // 成功提示
            this.showCustomToast('获取验证码成功，5分钟内有效', 'success');

            // 开始倒计时
            let countdown = 60;
            this.setData({ [countdownKey]: countdown });

            const timer = setInterval(() => {
                countdown--;
                if (countdown <= 0) {
                    clearInterval(timer);
                    this.setData({ [countdownKey]: 0 });
                } else {
                    this.setData({ [countdownKey]: countdown });
                }
            }, 1000);

            // 检查剩余次数提示
            if (res.remainingRetries !== undefined && res.remainingRetries <= 2) {
                setTimeout(() => {
                    this.showCustomToast(`今日还可用${res.remainingRetries}次获取验证码功能`, 'danger');
                }, 500);
            }

        } catch (error) {
            console.error('获取验证码失败:', error);
            const errorMsg = error.message || '获取验证码失败，请稍后重试';
            this.showCustomToast(errorMsg, 'danger');
        }
    },

    /**
     * 获取注册验证码
     */
    async getRegisterVerifyCode() {
        await this.getVerifyCode(false);
    },

    /**
     * 获取找回密码验证码
     */
    async getResetVerifyCode() {
        await this.getVerifyCode(true);
    },

    generateMemberCode(options = {}) {
        try {
          // 1. 定义允许的字符集（排除4和b/B）
          const numbers = '012356789'; // 排除数字4
          const lowercase = 'acdefghijklmnopqrstuvwxyz'; // 排除小写b
          const uppercase = 'ACDEFGHIJKLMNOPQRSTUVWXYZ'; // 排除大写B
          
          // 2. 根据配置确定字符集
          let chars = numbers;
          if (options.uppercase) {
            chars += uppercase; // 全大写模式
          } else {
            chars += lowercase + uppercase; // 大小写混合模式（默认）
          }
          
          const charsLength = chars.length;
          if (charsLength === 0) {
            throw new Error('字符集不能为空');
          }
          
          // 3. 生成5位随机字符 - 修复版
          let randomStr = '';
          
          // 方法1：使用Math.random()（兼容性好）
          for (let i = 0; i < 5; i++) {
            const randomIndex = Math.floor(Math.random() * charsLength);
            randomStr += chars[randomIndex];
          }

          return `会员用户${randomStr}`;
          
        } catch (error) {
          console.error('生成会员码失败:', error.message);
          // 失败时返回一个默认值（避免崩溃）
          return `会员用户${Math.floor(Math.random() * 100000).toString().padStart(5, '0')}`;
        }
      },
    /**
     * 处理注册
     */
    async handleRegister() {
        if (this.data.isSubmitting) return;

        const { registerPhone, registerPassword, verifyCode, nickname, invitedCode, registerRememberMe } = this.data;

        // 校验手机号
        const phoneRes = validatePhone(registerPhone);
        if (!phoneRes.valid) {
            this.showCustomToast(phoneRes.message, 'danger');
            return;
        }

        // 校验密码
        const passwordRes = validatePassword(registerPassword);
        if (!passwordRes.valid) {
            this.showCustomToast(passwordRes.message, 'danger');
            return;
        }

        // 校验验证码
        if (!verifyCode || verifyCode.length !== 6 || !/^\d{6}$/.test(verifyCode)) {
            this.showCustomToast('请输入6位数字验证码', 'danger');
            return;
        }

        // 校验昵称
        let trimmedNickname = (nickname || '').trim();
        if (!trimmedNickname) {
            trimmedNickname = this.generateMemberCode()
        }
        if (trimmedNickname.length > 50) {
            this.showCustomToast('昵称长度不能超过50位', 'danger');
            return;
        }

        this.setData({ isSubmitting: true });

        try {
            // 加密手机号
            const encryptedPhone = encryptAES(registerPhone);

            // 构建请求参数
            const requestData = {
                phone: encryptedPhone,
                password: registerPassword,
                verifyCode: verifyCode,
                nickname: trimmedNickname,
                rememberMe: registerRememberMe,
                platform: 'MINI_PROGRAM'
            };

            // 如果有邀请码，添加到请求中
            if (invitedCode && invitedCode.trim()) {
                requestData.invitedCode = invitedCode.trim();
            }

            // 调用注册接口
            const res = await post(
                '/v1/users/registration',
                { 'Content-Type': 'application/json' },
                requestData
            );

            console.log(res)
            // 注册失败
            if(!(res.code == 200 || res.code == 0)) {
                this.showCustomToast(`${res.code}: ${res.message}` , 'danger');
                return
            }
            // 提示成功
            this.showCustomToast('注册成功', 'success');

            // 存储 token 和用户信息到 globalData
            if (res.data.normalToken && res.data.tokenExpireTime) {
                saveNormalToken(res.data.normalToken, res.data.tokenExpireTime)
            }
            if(res.data.autoLoginToken && res.data.autoExpireTime) {
                saveAutoLoginToken(res.data.autoLoginToken, res.data.autoExpireTime)
            }
            if (res.data.userInfo) {
                app.globalData.isLogin = true
                // 调用 app.onUserLogin() 来正确更新用户类型和 tabBar 配置
                app.onUserLogin(res.data.userInfo);
                console.log('[注册] 用户信息已更新:', app.globalData)
            }

        } catch (error) {
            console.error('注册失败:', error);
            const errorMsg = error.message || '注册失败，请稍后重试';
            this.showCustomToast(errorMsg, 'danger');
        } finally {
            this.setData({ isSubmitting: false });
        }
    },

    /**
     * 处理找回密码
     */
    async handleResetPassword() {
        if (this.data.isSubmitting) return;

        const { resetPhone, resetVerifyCode, newPassword } = this.data;

        // 校验手机号
        const phoneRes = validatePhone(resetPhone);
        if (!phoneRes.valid) {
            this.showCustomToast(phoneRes.message, 'danger');
            return;
        }

        // 校验验证码
        if (!resetVerifyCode || resetVerifyCode.length !== 6 || !/^\d{6}$/.test(resetVerifyCode)) {
            this.showCustomToast('请输入6位数字验证码', 'danger');
            return;
        }

        // 校验新密码
        const passwordRes = validatePassword(newPassword);
        if (!passwordRes.valid) {
            this.showCustomToast(passwordRes.message, 'danger');
            return;
        }

        this.setData({ isSubmitting: true });

        try {
            // 加密手机号
            const encryptedPhone = encryptAES(resetPhone);

            // 调用密码重置接口
            const res = await post(
                '/v1/users/password/reset',
                { 'Content-Type': 'application/json' },
                {
                    phone: encryptedPhone,
                    verifyCode: resetVerifyCode,
                    newPassword: newPassword,
                    platform: 'MINI_PROGRAM'
                }
            );
            console.log(res)
            if(!(res.code == 200 || res.code == 0)) {
                this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
                return
            }
            // 重置成功
            this.showCustomToast('密码重置成功', 'success');

            // 自动切换回登录表单，填充手机号
            setTimeout(() => {
                this.setData({
                    showResetPassword: false,
                    phone: resetPhone,
                    resetVerifyCode: '',
                    newPassword: ''
                });
            }, 1500);

        } catch (error) {
            console.error('密码重置失败:', error);
            const errorMsg = error.message || '密码重置失败，请稍后重试';
            this.showCustomToast(errorMsg, 'danger');
        } finally {
            this.setData({ isSubmitting: false });
        }
    },
    toggleShowPassword() {
        this.setData({
            showPassword: !this.data.showPassword
        })
    },
    toggleShowRPassword() {
        this.setData({
            registerShowPassword: !this.data.registerShowPassword
        })
    },
    toggleResetShowPassword() {
        this.setData({
            resetShowPassword: !this.data.resetShowPassword
        })
    },
    handleError() {
        // 处理用户注册、登录
    }
});

