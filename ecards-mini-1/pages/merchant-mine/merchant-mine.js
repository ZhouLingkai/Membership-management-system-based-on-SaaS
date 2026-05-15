// pages/merchant-mine/merchant-mine.js
const app = getApp();
const ossUtil = require('../../utils/oss');

Page({
  /**
   * 页面的初始数据
   */
  data: {
    userType: 0,  // 0表示未初始化
    actualUserType: 0,
    merchantInfo: {certification:1, merchantLevel: 2},
    avatarUrl: '', // 签名后的头像URL
    // VIP特权数据
    vipPrivileges: [
      { icon: '/assets/images/privilege-booking.png', text: '高级预约' },
      { icon: '/assets/images/privilege-analytics.png', text: '数据分析' },
      { icon: '/assets/images/privilege-chain.png', text: '多店连锁' },
      { icon: '/assets/images/privilege-exclusive.png', text: '专属权益' }
    ],
    // 推荐功能数据
    recommendFeatures: [
      { icon: '/assets/images/feature-members.png', text: '我的会员卡', type: 'members' },
      { icon: '/assets/images/feature-booking.png', text: '我的预约', type: 'booking' },
      { icon: '/assets/images/feature-records.png', text: '交易记录', type: 'records' }
    ]
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    this.loadMerchantInfo();
  },

  /**
   * 生命周期函数--监听页面初次渲染完成
   */
  onReady() {

  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow() {
    const app = getApp();
    const currentUserType = app.globalData.currentUserType;
    const actualUserType = app.globalData.actualUserType;
    
    console.log('[MerchantMine] 用户类型检查:', { 
      页面保存的类型: this.data.userType, 
      当前显示类型: currentUserType,
      实际用户类型: actualUserType
    });
    
    // 非商家用户(userType !== 2)不应该访问merchant-mine页面，重定向到profile
    if (currentUserType !== 2) {
      console.log('[MerchantMine] 非商家用户，重定向到 profile');
      wx.switchTab({
        url: '/pages/profile/profile'
      });
      return;
    }
    
    // 更新用户信息（先更新数据）
    this.setData({
      userType: currentUserType,
      actualUserType: actualUserType
    });
    
    // 更新 tabBar 状态
    app.updateTabBarSelected('pages/merchant-mine/merchant-mine');
    
    // 同步更新 tabBar 组件
    if (typeof this.getTabBar === 'function') {
      const tabBar = this.getTabBar();
      if (tabBar) {
        tabBar.updateTabBar();
      }
    }
    
    // 刷新商户信息
    this.loadMerchantInfo();
  },

  /**
   * 加载商户信息
   */
  async loadMerchantInfo() {
    const userInfo = app.globalData.userInfo;
    let merchantInfo = this.data.merchantInfo;
    
    if (userInfo && userInfo.merchantInfo) {
      merchantInfo = {
        avatar: userInfo.avatar,
        merchantName: userInfo.merchantInfo.merchantName,
        certification: userInfo.merchantInfo.certification,
        merchantLevel: userInfo.merchantInfo.merchantLevel
      };
    }
    
    // 计算状态文本并设置到data中
    const certificationText = this.getCertificationText(merchantInfo.certification);
    const merchantLevelText = this.getMerchantLevelText(merchantInfo.merchantLevel);
    
    // 生成头像签名URL
    let avatarUrl = '';
    if (merchantInfo.avatar) {
      try {
        avatarUrl = await ossUtil.generateSignedUrl(merchantInfo.avatar);
        console.log('[MerchantMine] 头像URL生成成功');
      } catch (e) {
        console.error('[MerchantMine] 头像URL生成失败:', e);
      }
    }
    
    this.setData({
      merchantInfo: merchantInfo,
      certificationText: certificationText,
      merchantLevelText: merchantLevelText,
      avatarUrl: avatarUrl
    });
  },

  /**
   * 获取认证状态文本
   */
  getCertificationText(certification) {
    const statusMap = {
      1: '已认证',
      2: '未认证测试中',
      3: '审核中',
      4: '审核拒绝',
      5: '测试期过期',
      6: '认证存疑',
      7: '封禁中',
      13: '过期审核中',
      14: '过期审核拒绝'
    };
    return statusMap[certification] || '未认证';
  },

  /**
   * 获取商家等级文本
   */
  getMerchantLevelText(level) {
    const levelMap = {
      1: '普通商家',
      2: 'VIP商家',
      3: '超级VIP商家'
    };
    return levelMap[level] || '普通商家';
  },

  /**
   * 跳转到商户信息页面
   */
  goToMerchantInfo() {
    wx.navigateTo({
      url: '/pages/merchant-info/merchant-info'
    });
  },

  /**
   * 跳转到商家特权页面
   */
  goToPrivilege() {
    wx.navigateTo({
      url: '/pages/merchant-privilege/merchant-privilege'
    });
  },

  /**
   * 处理推荐功能点击
   */
  handleRecommendTap(e) {
    const type = e.currentTarget.dataset.type;
    
    // 根据类型跳转到不同页面
    switch (type) {
      case 'members':
        wx.navigateTo({ url: '/pages/my-cards/my-cards' });
        break;
      case 'booking':
        wx.showToast({ title: '我的预约功能开发中', icon: 'none' });
        break;
      case 'records':
        wx.navigateTo({ url: '/pages/trans-my-records/trans-my-records' });
        break;
      default:
        break;
    }
  },

  /**
   * 生命周期函数--监听页面初次渲染完成
   */
  onReady() {

  },

  /**
   * 生命周期函数--监听页面隐藏
   */
  onHide() {

  },

  /**
   * 生命周期函数--监听页面卸载
   */
  onUnload() {

  },

  /**
   * 页面相关事件处理函数--监听用户下拉动作
   */
  onPullDownRefresh() {

  },

  /**
   * 页面上拉触底事件的处理函数
   */
  onReachBottom() {

  },

  /**
   * 用户点击右上角分享
   */
  onShareAppMessage() {

  },

  // 检查用户权限
  checkUserPermission() {
    const app = getApp();
    const actualUserType = app.globalData.actualUserType;
    
    // 只有商家用户可以访问此页面
    if (actualUserType !== 2) {
      wx.showToast({
        title: '暂无访问权限',
        icon: 'none'
      });
      
      // 跳转到普通用户首页
      setTimeout(() => {
        wx.switchTab({
          url: '/pages/member-home/member-home'
        });
      }, 1500);
    }
  }
})
