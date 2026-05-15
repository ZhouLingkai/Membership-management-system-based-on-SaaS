// pages/member-home/member-home.js
Page({

  /**
   * 页面的初始数据
   */
  data: {
    showSwitchButton: false, // 是否显示切换按钮
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
    
    console.log('[MemberHome] 用户类型检查:', { 
      当前显示类型: currentUserType
    });
    
    // 商家用户(userType=2)不应该访问member-home页面，重定向到merchant-home
    if (currentUserType === 2) {
      console.log('[MemberHome] 商家用户，重定向到 merchant-home');
      wx.switchTab({
        url: '/pages/merchant-home/merchant-home'
      });
      return;
    }
    
    // 更新页面数据
    this.setData({
      userType: currentUserType
    });
    
    // 更新 tabBar 状态
    app.updateTabBarSelected('pages/member-home/member-home');
    
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
    // 这里可以添加页面访问权限检查逻辑
  },

})