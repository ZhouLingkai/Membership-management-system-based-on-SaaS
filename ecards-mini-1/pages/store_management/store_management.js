// pages/store_management/store_management.js
const { request } = require('../../utils/request');
const tokenManager = require('../../utils/token');

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
    
    // 店铺列表
    storeList: [],
    loading: false
  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow() {
    this.loadStoreList();
  },

  /**
   * 加载店铺列表
   */
  async loadStoreList() {
    try {
      this.setData({ loading: true });
      wx.showLoading({ title: '加载中...' });
      
      // 获取商户信息
      const app = getApp();
      const userInfo = app.globalData.userInfo;
      console.log('[store]',userInfo)
      if (!userInfo || !userInfo.merchantInfo || !userInfo.merchantInfo.merchantId) {
        wx.hideLoading();
        this.setData({ loading: false });
        this.showCustomToast('请先完成商户认证', 'danger');
        return;
      }
      
      // 获取普通令牌
      const normalToken = await tokenManager.getNormalToken();
      if (!normalToken) {
        wx.hideLoading();
        this.setData({ loading: false });
        this.showCustomToast('令牌已过期，请重新登录', 'danger');
        return;
      }
      
      // 调用店铺列表查询接口
      const res = await request.get(
        '/v1/stores',
        {
          'Content-Type': 'application/json',
          'Authorization': normalToken
        },
        { merchantId: userInfo.merchantInfo.merchantId }
      );
      
      console.log('[店铺列表查询]', res);
      wx.hideLoading();
      this.setData({ loading: false });
      
      if (res.code === 200 && res.data) {
        this.setData({
          storeList: res.data.list || []
        });
      } else {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
      }
    } catch (error) {
      wx.hideLoading();
      this.setData({ loading: false });
      console.error('[店铺列表查询失败]', error);
      this.showCustomToast('加载失败，请稍后重试', 'danger');
    }
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
   * 跳转到绑定员工页面
   */
  goBindEmployee(e) {
    const { storeid, storename } = e.currentTarget.dataset;
    
    wx.navigateTo({
      url: `/pages/bind-employee/bind-employee?storeId=${storeid}&storeName=${encodeURIComponent(storename)}`
    });
  },

  /**
   * 跳转到卡种管理页面
   */
  goCardTypeManage(e) {
    const { storeid, storename } = e.currentTarget.dataset;
    wx.navigateTo({
      url: `/pages/card-type-manage/card-type-manage?storeId=${storeid}&storeName=${encodeURIComponent(storename)}`
    });
  },

  /**
   * 跳转到店铺详情页面
   */
  goStoreDetail(e) {
    const storeId = e.currentTarget.dataset.storeid;
    wx.navigateTo({
      url: `/pages/store-detail/store-detail?storeId=${storeId}`
    });
  },

  /**
   * 跳转到创建店铺页面
   */
  goCreateStore() {
    wx.navigateTo({
      url: '/pages/store-create/store-create'
    });
  },

  /**
   * 跳转到OSS测试页面
   */
  goOssTest() {
    wx.navigateTo({
      url: '/pages/oss-test/oss-test'
    });
  },

  /**
   * 下拉刷新
   */
  onPullDownRefresh() {
    this.loadStoreList().then(() => {
      wx.stopPullDownRefresh();
    });
  }
})