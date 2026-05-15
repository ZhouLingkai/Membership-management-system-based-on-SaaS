// pages/trans-my-records/trans-my-records.js
const { get } = require('../../utils/request');
const tokenManager = require('../../utils/token');

Page({
  data: {
    // 记录列表
    recordList: [],
    
    // 分页参数
    pageNum: 1,
    pageSize: 20,
    total: 0,
    hasMore: true,
    noMoreData: false,
    
    // 加载状态
    loading: false,
    refreshing: false,
    
    // 下拉刷新冷却
    lastRefreshTime: 0,
    refreshCooldown: 0,
    
    // 日期筛选
    startDate: '',
    endDate: '',
    
    // 交易类型筛选
    transactionType: 0, // 0-全部, 1-充值, 2-消费
    transactionTypeOptions: [
      { label: '全部', value: 0 },
      { label: '充值', value: 1 },
      { label: '消费', value: 2 }
    ]
  },

  onLoad(options) {
    this.loadRecords();
  },

  onShow() {
    // 每次显示时刷新数据
    this.setData({
      recordList: [],
      pageNum: 1,
      hasMore: true,
      noMoreData: false
    });
    this.loadRecords();
  },

  // 加载记录
  async loadRecords(isLoadMore = false) {
    if (this.data.loading) return;
    if (isLoadMore && !this.data.hasMore) return;

    this.setData({ loading: true });

    try {
      const { pageNum, pageSize, startDate, endDate, transactionType } = this.data;
      
      // 获取普通令牌
      const token = await tokenManager.getNormalToken();
      if (!token) {
        this.showCustomToast('获取令牌失败', 'danger');
        this.setData({ loading: false });
        return;
      }

      // 构建请求参数
      const params = {
        pageNum,
        pageSize
      };

      if (startDate) params.startDate = startDate;
      if (endDate) params.endDate = endDate;
      if (transactionType > 0) params.transactionType = transactionType;

      const res = await get('/v1/transactions/my-records', { 'Authorization': token }, params);

      console.log('[trans-my-records] 查询结果:', res);

      if (res.code === 200) {
        const { list = [], total = 0 } = res.data;
        
        const newList = isLoadMore ? [...this.data.recordList, ...list] : list;
        const hasMore = newList.length < total;
        const noMoreData = list.length < pageSize;

        this.setData({
          recordList: newList,
          total,
          hasMore,
          noMoreData,
          pageNum: isLoadMore ? pageNum + 1 : pageNum
        });
      } else {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
      }
    } catch (error) {
      console.error('[trans-my-records] 加载失败:', error);
      this.showCustomToast('加载失败，请重试', 'danger');
    } finally {
      this.setData({ loading: false, refreshing: false });
    }
  },

  // 下拉刷新
  onPullDownRefresh() {
    const now = Date.now();
    const { lastRefreshTime, refreshCooldown } = this.data;
    
    // 检查冷却时间
    if (now - lastRefreshTime < refreshCooldown * 1000) {
      const remainingSeconds = Math.ceil((refreshCooldown * 1000 - (now - lastRefreshTime)) / 1000);
      this.showCustomToast(`请求过于频繁，请${remainingSeconds}秒后重试`, 'danger');
      wx.stopPullDownRefresh();
      return;
    }

    // 设置随机冷却时间（4-6秒）
    const newCooldown = Math.floor(Math.random() * 3) + 4;
    
    this.setData({
      refreshing: true,
      recordList: [],
      pageNum: 1,
      hasMore: true,
      noMoreData: false,
      lastRefreshTime: now,
      refreshCooldown: newCooldown
    });

    this.loadRecords().then(() => {
      wx.stopPullDownRefresh();
    });
  },

  // 上拉加载更多
  onReachBottom() {
    if (this.data.noMoreData || !this.data.hasMore) {
      return;
    }
    
    this.setData({ pageNum: this.data.pageNum + 1 });
    this.loadRecords(true);
  },

  // 开始日期选择
  onStartDateChange(e) {
    const date = e.detail.value;
    this.setData({ startDate: date });
  },

  // 结束日期选择
  onEndDateChange(e) {
    const date = e.detail.value;
    this.setData({ endDate: date });
  },

  // 重置所有日期
  resetAllDates() {
    this.setData({
      startDate: '',
      endDate: ''
    });
  },

  // 筛选查询
  handleFilter() {
    const { startDate, endDate } = this.data;
    
    // 验证日期
    if (startDate && endDate) {
      const start = new Date(startDate);
      const end = new Date(endDate);
      
      if (start > end) {
        this.showCustomToast('开始日期不能大于结束日期', 'danger');
        return;
      }
    }
    
    // 重新加载数据
    this.setData({
      recordList: [],
      pageNum: 1,
      hasMore: true,
      noMoreData: false
    });
    this.loadRecords();
  },

  // 交易类型选择
  onTransactionTypeChange(e) {
    const value = parseInt(e.detail.value);
    this.setData({ transactionType: value });
  },

  // 显示自定义Toast
  showCustomToast(message, type = 'success') {
    const toast = this.selectComponent('#customToast');
    if (toast) {
      toast.showToast(message, type);
    }
  }
});
