// pages/card-type-manage/card-type-manage.js
const { get, post } = require('../../utils/request');
const tokenManager = require('../../utils/token');

Page({
  data: {
    storeId: '',
    storeName: '',
    hasWorkToken: false,
    
    // 卡种列表
    cardTypeList: [],
    total: 0,
    pageNum: 1,
    pageSize: 10,
    hasMore: true,
    loading: false,
    
    // 详情弹窗
    showDetailModal: false,
    currentCardType: null,
    
    // 预设项目解析后的列表
    presetRechargeList: [],
    presetCostList: [],
    
    // 通知选项
    notifyOptions: [
      { value: 0, label: '关闭通知' },
      { value: 1, label: '短信通知' },
      { value: 2, label: '订阅通知' },
      { value: 3, label: '推送通知' }
    ],
    notifyEnum: ['关闭通知','短信通知','订阅通知','推送通知'],
    cardTypeIcon: [' ', '💰','🔢','⏰','⭐']
  },
  // 
  async onLoad(options) {
    const { storeId, storeName } = options;
    this.setData({
      storeId: storeId || '',
      storeName: decodeURIComponent(storeName || '未知店铺')
    });
    
    // 获取工作令牌后加载卡种列表
    await this.loadWorkToken();
  },

  /**
   * 获取工作令牌
   */
  async loadWorkToken() {
    const { storeId } = this.data;
    if (!storeId) {
      this.showCustomToast('店铺ID不存在', 'danger');
      return;
    }

    try {
      // 先尝试从缓存获取
      let workToken = await tokenManager.getWorkToken(storeId);
      
      // 如果缓存没有，从服务器获取
      if (!workToken) {
        await tokenManager.fetchWorkToken(storeId);
        workToken = await tokenManager.getWorkToken(storeId);
      }

      if (workToken) {
        this.setData({ hasWorkToken: true });
        // 获取令牌成功后加载卡种列表
        this.loadCardTypeList();
      } else {
        this.setData({ hasWorkToken: false });
        this.showCustomToast('获取工作令牌失败', 'danger');
      }
    } catch (error) {
      console.error('获取工作令牌失败:', error);
      this.setData({ hasWorkToken: false });
      this.showCustomToast('获取工作令牌失败', 'danger');
    }
  },

  onShow() {
    // 每次显示页面时刷新列表（从创建页面返回时）
    if (this.data.hasWorkToken) {
      this.refreshCardTypeList();
    }
    
  },

  onPullDownRefresh() {
    this.refreshCardTypeList();
  },

  /**
   * 刷新卡种列表
   */
  refreshCardTypeList() {
    this.setData({
      cardTypeList: [],
      pageNum: 1,
      hasMore: true
    });
    this.loadCardTypeList();
  },

  /**
   * 加载卡种列表
   */
  async loadCardTypeList() {
    if (this.data.loading || !this.data.hasMore) return;

    this.setData({ loading: true });
    console.log(this.data)
    try {
      const { storeId, pageNum, pageSize } = this.data;
      const workToken = await tokenManager.getWorkToken(storeId);
      
      if (!workToken) {
        this.showCustomToast('工作令牌无效', 'danger');
        this.setData({ loading: false });
        return;
      }
      
      const res = await get(
        '/v1/member-card-types/list-query',
        { 'Authorization': workToken },
        { storeId, pageNum, pageSize }
      );

      if (res.code === 200) {
        const { list, total } = res.data;
        const newList = pageNum === 1 ? list : [...this.data.cardTypeList, ...list];
        
        this.setData({
          cardTypeList: newList,
          total: total,
          hasMore: newList.length < total,
          pageNum: pageNum + 1
        });
      } else {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
      }
    } catch (error) {
      console.error('加载卡种列表失败:', error);
      this.showCustomToast('加载卡种列表失败', 'danger');
    } finally {
      this.setData({ loading: false });
      wx.stopPullDownRefresh();
    }
  },

  /**
   * 点击卡种项
   */
  onCardTypeClick(e) {
    const { cardtypeid } = e.currentTarget.dataset;
    this.loadCardTypeDetail(cardtypeid);
  },

  /**
   * 加载卡种详情
   */
  async loadCardTypeDetail(cardTypeId) {
    try {
      wx.showLoading({ title: '加载中...' });
      
      const { storeId } = this.data;
      const workToken = await tokenManager.getWorkToken(storeId);
      
      if (!workToken) {
        this.showCustomToast('工作令牌无效', 'danger');
        wx.hideLoading();
        return;
      }
      
      const res = await get(
        '/v1/member-card-types/detail-query',
        { 'Authorization': workToken },
        { cardTypeId, storeId }
      );

      if (res.code === 200) {
        const cardType = res.data;
        
        // 解析预设项目JSON
        let presetRechargeList = [];
        let presetCostList = [];
        
        try {
          if (cardType.presetRecharge) {
            presetRechargeList = JSON.parse(cardType.presetRecharge);
          }
          if (cardType.presetCost) {
            presetCostList = JSON.parse(cardType.presetCost);
          }
        } catch (e) {
          console.error('解析预设项目失败:', e);
        }

        this.setData({
          currentCardType: cardType,
          presetRechargeList,
          presetCostList,
          showDetailModal: true
        });
      } else {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
      }
    } catch (error) {
      console.error('加载卡种详情失败:', error);
      this.showCustomToast('加载卡种详情失败', 'danger');
    } finally {
      wx.hideLoading();
    }
  },

  /**
   * 关闭详情弹窗
   */
  closeDetailModal() {
    this.setData({
      showDetailModal: false,
      currentCardType: null
    });
  },

  /**
   * 跳转创建卡种页面
   */
  goCreateCardType() {
    const { storeId, storeName } = this.data;
    wx.navigateTo({
      url: `/packageCardType/pages/card-type-create/card-type-create?storeId=${storeId}&storeName=${encodeURIComponent(storeName)}`
    });
  },

  /**
   * 跳转修改卡种页面
   */
  goUpdateCardType() {
    const { currentCardType, storeId, storeName } = this.data;
    if (!currentCardType) return;
    
    wx.navigateTo({
      url: `/packageCardType/pages/card-type-update/card-type-update?cardTypeId=${currentCardType.cardTypeId}&storeId=${storeId}&storeName=${encodeURIComponent(storeName)}`
    });
  },

  /**
   * 获取卡片背景样式类
   */
  getCardBgClass(cardBgc) {
    if (!cardBgc) return 'bg-default';
    if (cardBgc === '1') return 'bg-1';
    if (cardBgc === '2') return 'bg-2';
    if (cardBgc === '3') return 'bg-3';
    if (cardBgc === '4') return 'bg-4';
    return 'bg-default';
  },

  /**
   * 获取通知类型名称
   */
  getNotifyName(autoNotify) {
    const names = ['关闭', '短信', '订阅', '推送'];
    return names[autoNotify] || '未知';
  },

  /**
   * 阻止事件冒泡
   */
  stopPropagation() {},

  /**
   * 显示自定义Toast
   */
  showCustomToast(message, type = 'success') {
    const toast = this.selectComponent('#customToast');
    if (toast) {
      toast.showToast(message, type);
    }
  }
});
