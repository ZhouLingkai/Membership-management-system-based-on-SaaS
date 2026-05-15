// pages/merchant-privilege/merchant-privilege.js
Page({
  data: {
    // 导航栏高度相关
    statusBarHeight: 44,
    navBarHeight: 88,
    totalNavHeight: 132,
    
    // 当前选中的套餐：vip 或 svip
    selectedPlan: 'vip',
    
    // 当前选中的二级选项卡：compare 或 features
    currentSubTab: 'compare',
    
    // 是否同意协议
    agreedToTerms: false,
    
    // 权益对比数据
    compareData: [
      { name: '年费', free: '无', vip: '188/年', svip: '299/年' },
      { name: '会员数量', free: '200个', vip: '不限', svip: '不限' },
      { name: '预约系统', free: '不支持', vip: '基础预约', svip: '超级预约' },
      { name: '卡种数量', free: '1个/店', vip: '10个/店', svip: '不限' },
      { name: '店铺数量', free: '2个', vip: '3个', svip: '4个' },
      { name: '拓店费用', free: '不支持', vip: '100元/店', svip: '60元/店' },
      { name: '员工管理', free: '不支持', vip: '支持', svip: '支持' },
      { name: '短信通知', free: '0.1元/个', vip: '0.9元/个', svip: '0.8元/个' },
      { name: '批量导入', free: '支持', vip: '支持', svip: '支持' },
      { name: '批量导出', free: '不支持', vip: '支持', svip: '支持' }
    ]
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    // 计算导航栏高度
    this.getSystemInfo();
    
    // 可以从 options 中获取传入的参数，例如默认选中的套餐
    if (options.plan) {
      this.setData({
        selectedPlan: options.plan
      });
    }
  },

  /**
   * 获取系统信息并计算导航栏高度
   */
  getSystemInfo() {
    const systemInfo = wx.getSystemInfoSync();
    console.log('系统信息:', systemInfo);
    
    // 获取胶囊按钮信息
    const menuButtonInfo = wx.getMenuButtonBoundingClientRect();
    console.log('胶囊按钮信息:', menuButtonInfo);
    
    // 正确的px转rpx计算：rpx = px * 750 / 屏幕宽度px
    const pxToRpx = (px) => {
      return Math.round(px * 750 / systemInfo.windowWidth);
    };
    
    // 计算状态栏高度（px转rpx）
    const statusBarHeight = pxToRpx(systemInfo.statusBarHeight);
    
    // 计算导航栏内容高度（正确的计算公式）+ 额外高度
    let navBarHeight = 88 + 10; // 默认值 + 10rpx
    if (menuButtonInfo && menuButtonInfo.top && menuButtonInfo.height) {
      // 胶囊按钮顶部距离状态栏底部的距离
      const menuTopGap = menuButtonInfo.top - systemInfo.statusBarHeight;
      // 导航栏内容高度 = 胶囊按钮高度 + 2 × 上下间距
      const navBarHeightPx = menuButtonInfo.height + 2 * menuTopGap;
      navBarHeight = pxToRpx(navBarHeightPx) + 10; // 转换为rpx + 10rpx额外高度
    }
    
    // 总导航栏高度 = 状态栏高度 + 导航栏内容高度
    const totalNavHeight = statusBarHeight + navBarHeight;
    
    this.setData({
      statusBarHeight: statusBarHeight,
      navBarHeight: navBarHeight,
      totalNavHeight: totalNavHeight
    });
  },

  /**
   * 返回上一页
   */
  navigateBack() {
    wx.navigateBack({
      delta: 1
    });
  },

  /**
   * 选择套餐
   */
  selectPlan(e) {
    const plan = e.currentTarget.dataset.plan;
    this.setData({
      selectedPlan: plan
    });
  },

  /**
   * 切换二级选项卡
   */
  switchSubTab(e) {
    const tab = e.currentTarget.dataset.tab;
    this.setData({
      currentSubTab: tab
    });
  },

  /**
   * 切换协议同意状态
   */
  toggleAgreement() {
    this.setData({
      agreedToTerms: !this.data.agreedToTerms
    });
  },

  /**
   * 查看付费服务协议
   */
  viewAgreement() {
    wx.showToast({
      title: '点击了付费服务协议',
      icon: 'none',
      duration: 2000
    });
    // 预留功能：跳转到协议页面
    // wx.navigateTo({
    //   url: '/pages/agreement/agreement'
    // });
  },

  /**
   * 处理购买
   */
  handlePurchase() {
    if (!this.data.agreedToTerms) {
      wx.showToast({
        title: '请先同意付费服务协议',
        icon: 'none',
        duration: 2000
      });
      return;
    }

    const planName = this.data.selectedPlan === 'vip' ? 'VIP年卡' : 'SVIP年卡';
    const price = this.data.selectedPlan === 'vip' ? '198' : '299';
    
    wx.showModal({
      title: '确认购买',
      content: `确认购买${planName}（¥${price}）？`,
      success: (res) => {
        if (res.confirm) {
          // 预留功能：调用支付接口
          wx.showToast({
            title: '购买功能开发中...',
            icon: 'none',
            duration: 2000
          });
        }
      }
    });
  },

  /**
   * 生命周期函数--监听页面初次渲染完成
   */
  onReady() {},

  /**
   * 生命周期函数--监听页面显示
   */
  onShow() {},

  /**
   * 生命周期函数--监听页面隐藏
   */
  onHide() {},

  /**
   * 生命周期函数--监听页面卸载
   */
  onUnload() {},

  /**
   * 页面相关事件处理函数--监听用户下拉动作
   */
  onPullDownRefresh() {},

  /**
   * 页面上拉触底事件的处理函数
   */
  onReachBottom() {},

  /**
   * 用户点击右上角分享
   */
  onShareAppMessage() {}
})
