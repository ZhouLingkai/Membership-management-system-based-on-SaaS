// pages/merchant-home/merchant-home.js
Page({

  /**
   * 页面的初始数据
   */
  data: {
    showSwitchButton: true, // 商家可以切换到普通用户模式
    userType: 0  // 0表示未初始化
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    this.checkUserPermission();
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
    
    console.log('[MerchantHome] 用户类型检查:', { 
      当前显示类型: currentUserType
    });
    
    // 非商家用户(userType !== 2)不应该访问merchant-home页面，重定向到member-home
    if (currentUserType !== 2) {
      console.log('[MerchantHome] 非商家用户，重定向到 member-home');
      wx.switchTab({
        url: '/pages/member-home/member-home'
      });
      return;
    }
    
    // 更新页面数据
    this.setData({
      userType: currentUserType
    });
    
    // 更新 tabBar 状态
    app.updateTabBarSelected('pages/merchant-home/merchant-home');
    
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

  // 检查用户权限
  checkUserPermission() {
    const app = getApp();
    // 检查是否有商家权限
    if (app.globalData.actualUserType !== 2) {
      wx.showToast({
        title: '暂无访问权限',
        icon: 'none'
      });
      // 跳转到会员首页
      setTimeout(() => {
        wx.switchTab({
          url: '/pages/member-home/member-home'
        });
      }, 1500);
    }
  },

  // 跳转到经营数据页面
  goToBusinessData() {
    const app = getApp();
    const userInfo = app.globalData.userInfo;
    
    if (!userInfo || !userInfo.merchantInfo || !userInfo.merchantInfo.merchantId) {
      wx.showToast({
        title: '请先完成商户认证',
        icon: 'none'
      });
      return;
    }

    const merchantId = userInfo.merchantInfo.merchantId;
    const merchantName = encodeURIComponent(userInfo.merchantInfo.merchantName || '商家');

    wx.navigateTo({
      url: `/pages/transactions-statistics/transactions-statistics?queryType=merchant&merchantId=${merchantId}&merchantName=${merchantName}`
    });
  }

})