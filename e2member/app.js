// app.js
const {
    generateDeviceId
} = require('./utils/request');
const token = require('./utils/token');

App({
    globalData: {
        isLoggedIn: false,
        userInfo: null,
        userType: 0, // 0未登录，1商家，2员工
        // 令牌缓存（内存层，与 token.js 同步）
        tokens: {
            merchantLoginToken: null, // ①
            merchantAccessToken: null, // ②
            merchantWorkTokens: [], // ③ 数组
            staffLoginToken: null, // ④
            staffWorkToken: null, // ⑤
        },
    },
    onLaunch() {
        // 初始化设备ID（首次生成后缓存到Storage）
        generateDeviceId();

        // 尝试自动登录
        this.tryAutoLogin();
    },

    /**
     * 自动登录流程：
     * 1. 检查商家登录令牌①是否存在且未过期
     * 2. 用①换取商家访问令牌②（token.js 内部处理）
     * 3. 成功 → 标记已登录；失败 → 留在登录页
     */
    async tryAutoLogin() {
        try {
            const loginToken = await token.getMerchantLoginToken();
            if (!loginToken) {
                console.log('[app] 无登录令牌，等待用户登录');
                this.globalData.isLoggedIn = false;
                return;
            }

            // getMerchantAccessToken 内部会自动用①换取②
            const accessToken = await token.getMerchantAccessToken();
            if (accessToken) {
                console.log('[app] 自动登录成功');
                this.globalData.isLoggedIn = true;
                // TODO: 跳转到主页（主页就绪后启用）
                // wx.reLaunch({ url: '/pages/index/index' });
            } else {
                console.log('[app] 自动登录失败，需重新登录');
                this.globalData.isLoggedIn = false;
            }
        } catch (error) {
            console.error('[app] 自动登录异常:', error);
            this.globalData.isLoggedIn = false;
        }
    },


});