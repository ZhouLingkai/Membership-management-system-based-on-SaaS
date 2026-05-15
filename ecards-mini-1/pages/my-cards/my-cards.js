// pages/my-cards/my-cards.js
const { get } = require('../../utils/request');
const { getNormalToken } = require('../../utils/token');
const { formatTimestamp } = require('../../utils/time-util');

Page({
  data: {
    // 会员卡列表
    cardList: [],
    // 加载状态
    loading: true,
    // 分页参数
    pageNum: 1,
    pageSize: 20,
    total: 0,
    hasMore: true,
    // Toast相关
    showToast: false,
    toastMessage: '',
    toastType: 'success',
    toastAnimationType: 'slide'
  },

  onLoad(options) {
    this.loadCardList();
  },

  onShow() {
    // 每次显示页面时刷新列表（从详情页返回时可能状态已变）
    if (this.data.cardList.length > 0) {
      this.refreshList();
    }
  },

  onPullDownRefresh() {
    this.refreshList();
  },

  /**
   * 刷新列表
   */
  async refreshList() {
    this.setData({
      pageNum: 1,
      hasMore: true,
      cardList: []
    });
    await this.loadCardList();
    wx.stopPullDownRefresh();
  },

  /**
   * 加载会员卡列表
   * 接口7: GET /api/v1/member-cards/my-list
   */
  async loadCardList() {
    try {
      // 获取普通令牌
      const token = await getNormalToken();
      if (!token) {
        this.showCustomToast('请先登录', 'danger');
        setTimeout(() => {
          wx.navigateTo({ url: '/pages/login/index' });
        }, 1500);
        return;
      }

      this.setData({ loading: this.data.pageNum === 1 });

      const res = await get('/v1/member-cards/my-list', {
        'Content-Type': 'application/json',
        'Authorization': token
      }, {
        pageNum: this.data.pageNum,
        pageSize: this.data.pageSize
      });

      console.log('[my-cards] 会员卡列表响应:', res);

      if (res.code !== 200) {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
        this.setData({ loading: false });
        return;
      }

      const { list = [], total = 0 } = res.data || {};
      
      // 处理列表数据，格式化到期时间
      const processedList = list.map(card => ({
        ...card,
        expireTimeFormatted: card.expireTime ? this.formatExpireTime(card.expireTime) : '--'
      }));

      // 合并数据
      const newList = this.data.pageNum === 1 ? processedList : [...this.data.cardList, ...processedList];
      
      this.setData({
        cardList: newList,
        total: total,
        hasMore: newList.length < total,
        loading: false
      });

    } catch (error) {
      console.error('[my-cards] 加载失败:', error);
      this.showCustomToast('加载失败，请稍后重试', 'danger');
      this.setData({ loading: false });
    }
  },

  /**
   * 格式化到期时间
   */
  formatExpireTime(expireTime) {
    try {
      // 如果是ISO8601格式，转换为更友好的格式
      const date = new Date(expireTime);
      if (isNaN(date.getTime())) {
        return expireTime;
      }
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    } catch (e) {
      return expireTime;
    }
  },

  /**
   * 加载更多
   */
  loadMore() {
    if (this.data.hasMore && !this.data.loading) {
      this.setData({
        pageNum: this.data.pageNum + 1
      });
      this.loadCardList();
    }
  },

  /**
   * 跳转到会员卡详情页
   */
  goToDetail(e) {
    const card = e.currentTarget.dataset.card;
    wx.navigateTo({
      url: `/pages/cards-detail/cards-detail?memberCardId=${card.memberCardId}`
    });
  },

  /**
   * 显示自定义Toast
   */
  showCustomToast(message, type = 'success', animationType = 'slide', duration = 2000) {
    const toast = this.selectComponent('#customToast');
    if (toast) {
      toast.showToast(message, type, animationType, duration);
    }
  }
});
