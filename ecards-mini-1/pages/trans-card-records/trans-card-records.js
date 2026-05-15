// pages/trans-card-records/trans-card-records.js
const { get } = require('../../utils/request');
const tokenManager = require('../../utils/token');

Page({
  data: {
    // 页面参数
    memberCardId: '',
    tokenType: 'normal', // 'normal' or 'work'
    recordType: 'transaction', // 'transaction' or 'points'
    storeId: '',
    
    // 会员卡信息
    cardInfo: {
      cardTypeName: '',
      cardType: '',
      memberName: '',
      phone: '',
      balance: 0,
      points: 0,
      status: 1
    },
    
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
    showDatePicker: false,
    datePickerType: '', // 'start' or 'end'
    
    // 状态映射
    statusMap: {
      0: '未激活',
      1: '正常',
      2: '已过期',
      3: '已冻结',
      4: '已注销'
    }
  },

  onLoad(options) {
    const {
      memberCardId,
      tokenType = 'normal',
      recordType = 'transaction',
      storeId = '',
      cardTypeName = '',
      cardType = '',
      memberName = '',
      phone = '',
      balance = 0,
      points = 0,
      times = 0,
      expireTime = '',
      remainingDays = 0,
      status = 1,
      originalStoreName = ''
    } = options;

    this.setData({
      memberCardId,
      tokenType,
      recordType,
      storeId,
      cardInfo: {
        cardTypeName: decodeURIComponent(cardTypeName || ''),
        cardType: decodeURIComponent(cardType || ''),
        memberName: decodeURIComponent(memberName || ''),
        phone: decodeURIComponent(phone || ''),
        balance: parseFloat(balance) || 0,
        points: parseInt(points) || 0,
        times: parseInt(times) || 0,
        expireTime: decodeURIComponent(expireTime || ''),
        remainingDays: parseInt(remainingDays) || 0,
        status: parseInt(status) || 1,
        originalStoreName: decodeURIComponent(originalStoreName || '')
      }
    });
    this.setData({
        'cardInfo.expireTime': this.data.cardInfo.expireTime.substring(0, 10)
    })
  },

  onShow() {
    // 清空之前的数据，重新加载
    this.setData({
      recordList: [],
      pageNum: 1,
      hasMore: true,
      noMoreData: false
    });
    this.loadRecords();
    this.setData({
        'cardInfo.expireTime': this.data.cardInfo.expireTime.substring(0, 10)
    })
  },

  onHide() {
    // 清空数据，防止下次进入混淆
    this.setData({
      recordList: [],
      pageNum: 1,
      hasMore: true,
      noMoreData: false
    });
  },

  // 加载记录
  async loadRecords(isLoadMore = false) {
    if (this.data.loading) return;
    if (isLoadMore && !this.data.hasMore) return;

    this.setData({ loading: true });

    try {
      const { memberCardId, tokenType, recordType, storeId, pageNum, pageSize, startDate, endDate } = this.data;
      
      // 获取令牌
      let token;
      if (tokenType === 'work') {
        token = await tokenManager.getWorkToken(storeId);
      } else {
        token = await tokenManager.getNormalToken();
      }

      if (!token) {
        this.showCustomToast('获取令牌失败', 'danger');
        this.setData({ loading: false });
        return;
      }

      // 构建请求参数
      const params = {
        memberCardId,
        pageNum,
        pageSize
      };

      // 工作令牌需要传递storeId
      if (tokenType === 'work' && storeId) {
        params.storeId = storeId;
      }

      if (startDate) params.startDate = startDate;
      if (endDate) params.endDate = endDate;

      // 选择接口
      const url = recordType === 'transaction' 
        ? '/v1/transactions/card-records' 
        : '/v1/points/records';

      const res = await get(url, { 'Authorization': token }, params);

      console.log('[trans-card-records] 查询结果:', res);

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
      console.error('[trans-card-records] 加载失败:', error);
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

  // 显示日期选择器
  showDateSelector(e) {
    const type = e.currentTarget.dataset.type;
    this.setData({
      showDatePicker: true,
      datePickerType: type
    });
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

  // 格式化时间
  formatTime(timeStr) {
    if (!timeStr) return '--';
    return timeStr.replace('T', ' ').substring(0, 19);
  },

  // 显示自定义Toast
  showCustomToast(message, type = 'success') {
    const toast = this.selectComponent('#customToast');
    if (toast) {
      toast.showToast(message, type);
    }
  }
});
