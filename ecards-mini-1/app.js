// app.js
// const { generateDeviceId } = require('./utils/request');

App({
  globalData: {
    isLogin: false,
    userInfo: {
        userType: 0,
    }, // 用户简单信息，存储登录后给的信息
    userInfoDetail: null, // 用户关键信息，暂存查询详情后的信息
    deviceId: null,
    normalToken: null,
    inviteCode: null,
    tokens: {
        normalToken: null, // 存储令牌和过期时间
        workTokens: null, // 是一个数组哦
        privilegeToken: null,
        managerToken: null,
        autoLoginToken: null, // 自动登录令牌
    },
    // tabBar 相关状态
    currentUserType: 1, // 当前显示的用户类型 1-普通用户 2-商家 3-员工
    actualUserType: 1,  // 用户实际类型（用于权限判断）
    tabBarConfig: null, // 当前 tabBar 配置
    selectedTabIndex: 0 // 当前选中的 tab 索引
  },
  onLaunch() {
    // 生成设备ID并存入 globalData
    // const deviceId = generateDeviceId();
    // this.globalData.deviceId = deviceId;
    this.globalData.deviceId = 'temp-device-id';
    
    // 先进行基础初始化（使用默认值）
    this.initUserAndTabBar();
    
    // 延迟执行自动登录，确保App实例完全初始化
    setTimeout(async () => {
      await this.tryAutoLogin();
      // 自动登录完成后重新初始化tabBar（如果登录成功，此时userType已正确更新）
      this.initUserAndTabBar();
    }, 100);
  },

  // 尝试自动登录
  async tryAutoLogin() {
    const { getNormalToken, getAutoLoginToken, fetchNormalTokenByAutoLogin } = require('./utils/token');
    const { request } = require('./utils/request');
    
    try {
      console.log('[app.js] 开始尝试自动登录...');
      
      // ① 检查是否有有效的普通令牌
      const normalToken = await getNormalToken();
      if (normalToken) {
        console.log('[app.js] 发现有效普通令牌，获取用户信息...');
        
        // 请求用户基本信息（不需要userId参数）
        const userInfoResponse = await request.get('/v1/users/info', {
          'Authorization': normalToken,
          'Content-Type': 'application/json'
        });

        console.log(userInfoResponse)

        // 检查响应是否成功，并正确解析用户信息
        if (userInfoResponse && userInfoResponse.code === 200 && userInfoResponse.data) {
          // 保存用户信息到globalData并更新用户类型
          this.globalData.userInfo = userInfoResponse.data.userInfo;
          this.globalData.isLogin = true;
          // 更新用户类型
          const userType = userInfoResponse.data.userInfo.userType || 1;
          this.globalData.currentUserType = userType;
          this.globalData.actualUserType = userType;
          console.log('[app.js] 自动登录成功，用户信息已更新, userType:', userType);
          return;
        } else {
          // 普通令牌存在但获取用户信息失败，可能令牌已失效，清理令牌
          console.log('[app.js] 普通令牌存在但获取用户信息失败，清理令牌');
          this.globalData.tokens.normalToken = null;
          wx.removeStorageSync('normalToken');
        }
      }
      
      // ② 如果没有有效普通令牌，检查自动登录令牌
      console.log('[app.js] 无有效普通令牌，检查自动登录令牌...');
      const autoLoginToken = await getAutoLoginToken();
      if (autoLoginToken) {
        console.log('[app.js] 发现有效自动登录令牌，尝试获取新的普通令牌...');
        
        // 使用自动登录令牌获取新的普通令牌
        const autoLoginResponse = await fetchNormalTokenByAutoLogin();
        if (autoLoginResponse && autoLoginResponse.code === 200 && autoLoginResponse.data) {
          // 保存用户信息到globalData（fetchNormalTokenByAutoLogin已经保存了令牌）
          this.globalData.userInfo = autoLoginResponse.data.userInfo;
          this.globalData.isLogin = true;
          // 更新用户类型
          const userType = autoLoginResponse.data.userInfo.userType || 1;
          this.globalData.currentUserType = userType;
          this.globalData.actualUserType = userType;
          
          console.log('[app.js] 自动登录成功，用户信息已更新, userType:', userType);
          return;
        } else {
          // 自动登录令牌失效，清理令牌
          console.log('[app.js] 自动登录令牌失效，清理令牌');
          this.globalData.tokens.autoLoginToken = null;
          wx.removeStorageSync('autoLoginToken');
        }
      }
      
      // ③ 都没有有效令牌，设置为未登录状态
      console.log('[app.js] 无有效令牌，设置为未登录状态');
      this.globalData.userInfo = { userType: 0 }; // 未登录状态
      this.globalData.isLogin = false;
      
    } catch (error) {
      console.error('[app.js] 自动登录失败:', error);
      // 出错时也设置为未登录状态
      this.globalData.userInfo = { userType: 0 };
      this.globalData.isLogin = false;
    } finally {
      console.log(this.globalData)
    }
  },

  // 获取不同用户类型的 tabBar 配置
  getTabBarConfig(userType) {
    const configs = {
      1: [ // 普通用户和员工共用
        {
          pagePath: "pages/member-home/member-home",
          text: "首页",
          iconPath: "/assets/icons/member-home.png",
          selectedIconPath: "/assets/icons/member-home-active.png"
        },
        {
          pagePath: "pages/card-manage/card-manage",
          text: "卡管理",
          iconPath: "/assets/icons/card-manage.png",
          selectedIconPath: "/assets/icons/card-manage-active.png"
        },
        {
          pagePath: "pages/workspace/workspace",
          text: "工作台",
          iconPath: "/assets/icons/workspace.png",
          selectedIconPath: "/assets/icons/workspace-active.png"
        },
        {
          pagePath: "pages/profile/profile",
          text: "我的", 
          iconPath: "/assets/icons/mine.png",
          selectedIconPath: "/assets/icons/mine-active.png"
        }
      ],
      2: [ // 商家
        {
          pagePath: "pages/merchant-home/merchant-home",
          text: "商家首页",
          iconPath: "/assets/icons/merchant-home.png",
          selectedIconPath: "/assets/icons/merchant-home-active.png"
        },
        {
          pagePath: "pages/merchant-workspace/merchant-workspace",
          text: "工作台",
          iconPath: "/assets/icons/workspace.png",
          selectedIconPath: "/assets/icons/workspace-active.png"
        },
        {
          pagePath: "pages/store_management/store_management",
          text: "后台管理",
          iconPath: "/assets/icons/manage.png",
          selectedIconPath: "/assets/icons/manage-active.png"
        },
        {
          pagePath: "pages/merchant-mine/merchant-mine",
          text: "我的",
          iconPath: "/assets/icons/mine.png",
          selectedIconPath: "/assets/icons/mine-active.png"
        }
      ],
      3: [ // 员工使用普通用户配置
        {
          pagePath: "pages/member-home/member-home",
          text: "首页",
          iconPath: "/assets/icons/member-home.png",
          selectedIconPath: "/assets/icons/member-home-active.png"
        },
        {
          pagePath: "pages/card-manage/card-manage",
          text: "卡管理",
          iconPath: "/assets/icons/card-manage.png",
          selectedIconPath: "/assets/icons/card-manage-active.png"
        },
        {
          pagePath: "pages/workspace/workspace",
          text: "工作台",
          iconPath: "/assets/icons/workspace.png",
          selectedIconPath: "/assets/icons/workspace-active.png"
        },
        {
          pagePath: "pages/profile/profile",
          text: "我的", 
          iconPath: "/assets/icons/mine.png",
          selectedIconPath: "/assets/icons/mine-active.png"
        }
      ]
    };
    return configs[userType] || configs[1];
  },

  // 初始化用户状态和 tabBar
  initUserAndTabBar() {
    // 从 userInfo 中获取用户类型
    const userType = this.globalData.userInfo?.userType || 0;
    
    // 如果是未登录用户（userType = 0），默认显示普通用户 tabBar
    const displayUserType = userType === 0 ? 1 : userType;
    
    console.log('[app.js] initUserAndTabBar:', {
      userInfoType: userType,
      displayUserType: displayUserType
    });
    
    // 设置用户类型
    this.globalData.currentUserType = displayUserType;
    this.globalData.actualUserType = userType; // 实际类型保持原值，用于权限判断
    
    // 初始化 tabBar 配置
    this.initTabBarConfig();
    
    // 触发 tabBar 组件更新
    this.refreshTabBar();
  },

  // 初始化 tabBar 配置
  initTabBarConfig() {
    this.globalData.tabBarConfig = this.getTabBarConfig(this.globalData.currentUserType);
    console.log('[app.js] tabBarConfig 已更新为 userType:', this.globalData.currentUserType);
  },
  
  // 刷新所有页面的 tabBar 组件
  refreshTabBar() {
    const pages = getCurrentPages();
    if (pages.length > 0) {
      const currentPage = pages[pages.length - 1];
      if (typeof currentPage.getTabBar === 'function') {
        const tabBarInstance = currentPage.getTabBar();
        if (tabBarInstance && typeof tabBarInstance.updateTabBar === 'function') {
          console.log('[app.js] 触发 tabBar 组件更新');
          tabBarInstance.updateTabBar();
        }
      }
    }
  },


  // 新思路：统一的 tabBar 状态管理
  setTabBarSelected(index, fromClick = false) {
    // 确保索引有效
    const config = this.globalData.tabBarConfig;
    if (!config || index < 0 || index >= config.length) {
      console.error('setTabBarSelected: 无效的索引', index);
      return;
    }
    this.globalData.selectedTabIndex = index;
    
    // 立即更新 tabBar 组件 - 通过当前页面获取
    const pages = getCurrentPages();
    if (pages.length > 0) {
      const currentPage = pages[pages.length - 1];
      if (typeof currentPage.getTabBar === 'function') {
        const tabBarInstance = currentPage.getTabBar();
        if (tabBarInstance) {
          tabBarInstance.setData({
            selected: index
          });
        }
      }
    }
  },

  // 根据页面路径更新 tabBar 选中状态
  updateTabBarSelected(pagePath) {
    const config = this.globalData.tabBarConfig;
    if (!config) {
      return;
    }
    
    const selectedIndex = config.findIndex(item => item.pagePath === pagePath);
    if (selectedIndex !== -1) {
      this.setTabBarSelected(selectedIndex, false);
    } else {
      console.warn(`updateTabBarSelected: 页面 ${pagePath} 不在当前 tabBar 配置中`);
      
      // 如果是商家用户访问了普通用户的页面，重定向到对应的商家页面
      if (this.globalData.currentUserType === 2) {
        if (pagePath === 'pages/profile/profile') {
          wx.switchTab({
            url: '/pages/merchant-mine/merchant-mine'
          });
        } else if (pagePath === 'pages/workspace/workspace') {
          wx.switchTab({
            url: '/pages/merchant-workspace/merchant-workspace'
          });
        }
      }
    }
  },

  // 获取当前页面对应的 tabBar 索引
  getCurrentTabIndex() {
    const currentPages = getCurrentPages();
    if (currentPages.length === 0) return 0;
    
    const currentPage = currentPages[currentPages.length - 1];
    const currentRoute = currentPage.route;
    const config = this.globalData.tabBarConfig;
    
    if (!config) return 0;
    
    const index = config.findIndex(item => item.pagePath === currentRoute);
    return index !== -1 ? index : 0;
  },

  // 用户登录成功后调用此函数
  onUserLogin(userInfo) {
    // 更新用户信息
    this.globalData.userInfo = userInfo;
    this.globalData.currentUserType = userInfo.userType;
    this.globalData.actualUserType = userInfo.userType;
    
    // 重新初始化 tabBar 配置
    this.initTabBarConfig();
    
    // 更新自定义 tabBar
    const pages = getCurrentPages();
    if (pages.length > 0) {
      const currentPage = pages[pages.length - 1];
      if (typeof currentPage.getTabBar === 'function') {
        const tabBarInstance = currentPage.getTabBar();
        if (tabBarInstance) {
          tabBarInstance.updateTabBar();
        }
      }
    }
    
    // 跳转到对应的首页
    const config = this.globalData.tabBarConfig;
    if (config && config.length > 0) {
      wx.switchTab({
        url: '/' + config[0].pagePath
      });
    }
  },

  // 升级为商家（1 -> 2）
  upgradeToMerchant() {
    console.log('用户升级为商家');
    
    // 更新用户信息
    this.globalData.userInfo.userType = 2;
    this.globalData.currentUserType = 2;
    this.globalData.actualUserType = 2;
    
    // 重新初始化 tabBar 配置
    this.initTabBarConfig();
    
    // 重置选中索引为0（商家首页）
    this.globalData.selectedTabIndex = 0;
    
    // 更新自定义 tabBar
    const pages = getCurrentPages();
    if (pages.length > 0) {
      const currentPage = pages[pages.length - 1];
      if (typeof currentPage.getTabBar === 'function') {
        const tabBarInstance = currentPage.getTabBar();
        if (tabBarInstance) {
          tabBarInstance.updateTabBar();
        }
      }
    }
    
    // 跳转到商家首页
    wx.switchTab({
      url: '/pages/merchant-home/merchant-home',
      success: () => {
        // 确保状态同步
        this.setTabBarSelected(0, true);
      }
    });
  },

  // 测试函数：切换用户类型（用于开发测试）
  testSwitchUserType(userType) {
    console.log(`测试切换用户类型到: ${userType}`);
    
    // 更新用户信息
    this.globalData.userInfo.userType = userType;
    this.globalData.currentUserType = userType;
    this.globalData.actualUserType = userType;
    
    // 重新初始化 tabBar 配置
    this.initTabBarConfig();
    
    // 重置选中索引为0
    this.globalData.selectedTabIndex = 0;
    
    // 更新自定义 tabBar
    const pages = getCurrentPages();
    if (pages.length > 0) {
      const currentPage = pages[pages.length - 1];
      if (typeof currentPage.getTabBar === 'function') {
        const tabBarInstance = currentPage.getTabBar();
        if (tabBarInstance) {
          tabBarInstance.updateTabBar();
        }
      }
    }
    
    // 跳转到对应的首页
    const config = this.globalData.tabBarConfig;
    if (config && config.length > 0) {
      wx.switchTab({
        url: '/' + config[0].pagePath,
        success: () => {
          // 确保状态同步
          this.setTabBarSelected(0, true);
        }
      });
    }
  }
})
