// pages/profile/profile.js
const ossUtil = require('../../utils/oss');

Page({

  /**
   * 页面的初始数据
   */
  data: {
    userType: 0,  // 0表示未初始化，避免与实际类型比较产生误判
    actualUserType: 0,
    isLogin: false,
    userInfo: {},
    avatarUrl: '', // 签名后的头像URL
    privileges: [
      { icon: '/assets/icons/shop-manage.png', text: '店铺管理' },
      { icon: '/assets/icons/staff-manage.png', text: '员工管理' },
      { icon: '/assets/icons/member-manage.png', text: '会员管理' },
      { icon: '/assets/icons/booking-system.png', text: '预约系统' },
      { icon: '/assets/icons/marketing-manage.png', text: '营销管理' }
    ],
    recommendFeatures: [
      { icon: '/assets/icons/member-card.png', text: '我的会员卡', type: 'memberCard' },
      { icon: '/assets/icons/my-booking.png', text: '我的预约', type: 'myBooking' },
      { icon: '/assets/icons/transaction-record.png', text: '交易记录', type: 'transaction' }
    ],
    // 自定义消息提示
    showToast: false,
    toastMessage: '',
    toastType: 'success',
    toastAnimationType: 'slide'
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {

  },

  /**
   * 生命周期函数--监听页面初次渲染完成
   */
  onReady() {

  },

  /**
   * 生命周期函数--监听页面显示
   */
  async onShow() {
    console.log('[Profile] onShow 被调用');
    const app = getApp();
    const currentUserType = app.globalData.currentUserType;
    const actualUserType = app.globalData.actualUserType;
    
    console.log('[Profile] 用户类型检查:', { 
      页面保存的类型: this.data.userType, 
      当前显示类型: currentUserType,
      实际用户类型: actualUserType
    });
    
    // 商家用户(userType=2)不应该访问profile页面，重定向到merchant-mine
    if (currentUserType === 2) {
      console.log('[Profile] 商家用户，重定向到 merchant-mine');
      wx.switchTab({
        url: '/pages/merchant-mine/merchant-mine'
      });
      return;
    }
    
    const userInfo = app.globalData.userInfo || {};
    const isLogin = app.globalData.isLogin || false;
    
    // 生成头像签名URL
    let avatarUrl = '';
    if (isLogin && userInfo.avatar) {
      try {
        avatarUrl = await ossUtil.generateSignedUrl(userInfo.avatar);
        console.log('[Profile] 头像URL生成成功:', avatarUrl);
      } catch (e) {
        console.error('[Profile] 头像URL生成失败:', e);
      }
    }
    
    // 更新用户信息（先更新数据）
    this.setData({
      userType: currentUserType,
      actualUserType: actualUserType,
      isLogin: isLogin,
      userInfo: userInfo,
      avatarUrl: avatarUrl
    });
    
    // 更新 tabBar 状态
    console.log('[Profile] 调用 app.updateTabBarSelected');
    app.updateTabBarSelected('pages/profile/profile');
    
    // 同步更新 tabBar 组件
    if (typeof this.getTabBar === 'function') {
      const tabBar = this.getTabBar();
      if (tabBar) {
        tabBar.updateTabBar();
      }
    }
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

  // 获取用户状态文本
  getUserStatusText() {
    const { isLogin, actualUserType } = this.data;
    if (!isLogin) return '未登录';
    
    switch (actualUserType) {
      case 1: return '普通用户';
      case 2: return '商家用户';
      case 3: return '员工用户';
      default: return '未知身份';
    }
  },

  // 处理用户信息卡片点击
  handleUserInfoTap() {
    const { isLogin } = this.data;
    
    if (!isLogin) {
      // 未登录，跳转到登录页
      wx.navigateTo({
        url: '/pages/login/index'
      });
    } else {
      // 已登录，跳转到个人信息页
      wx.navigateTo({
        url: '/pages/member-info/member-info'
      });
    }
  },

  // 处理商家升级卡片点击
  handleUpgradeTap() {
    const { isLogin } = this.data;
    
    // 检查用户是否登录
    if (!isLogin) {
      this.showCustomToast('请先完成登录', 'error', 'slide');
      return;
    }
    
    wx.navigateTo({
      url: '/pages/upgrade-merchant/upgrade-merchant'
    });
  },

  // 处理推荐功能点击
  handleRecommendTap(e) {
    const { type } = e.currentTarget.dataset;
    
    // 检查登录状态
    if (!this.data.isLogin) {
      this.showCustomToast('请先登录', 'danger');
      return;
    }
    
    // 根据type跳转到不同页面
    switch (type) {
      case 'memberCard':
        wx.navigateTo({ url: '/pages/my-cards/my-cards' });
        break;
      case 'myBooking':
        wx.showToast({ title: '我的预约功能开发中', icon: 'none' });
        break;
      case 'transaction':
        wx.navigateTo({ url: '/pages/trans-my-records/trans-my-records' });
        break;
      default:
        console.log('未知功能类型:', type);
    }
  },

  // 升级为商家
  upgradeToMerchant() {
    const app = getApp();
    
    // 检查当前是否为普通用户
    if (app.globalData.currentUserType !== 1) {
      wx.showToast({
        title: '只有普通用户可以升级为商家',
        icon: 'none'
      });
      return;
    }
    
    // 显示确认弹窗
    wx.showModal({
      title: '升级确认',
      content: '是否确认升级为商家用户？',
      success: (res) => {
        if (res.confirm) {
          // 调用升级函数
          app.upgradeToMerchant();
          
          wx.showToast({
            title: '升级成功！',
            icon: 'success'
          });
        }
      }
    });
  },

  // 显示自定义Toast
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

  // 跳转到OSS测试页面
  goToOssTest() {
    wx.navigateTo({
      url: '/pages/oss-test/oss-test'
    });
  },

  // 跳转到分包测试页面
  goToSubpackageIndex() {
    wx.navigateTo({
      url: '/packageCardType/pages/card-type-update/card-type-update'
    });
  }
})