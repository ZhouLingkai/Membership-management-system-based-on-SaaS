// pages/cards-detail/cards-detail.js
const { get, post } = require('../../utils/request');
const { getNormalToken } = require('../../utils/token');

Page({
  data: {
    // 会员卡ID
    memberCardId: '',
    // 会员卡详情
    cardDetail: {},
    // 加载状态
    loading: true,
    // 格式化后的时间
    expireTimeFormatted: '--',
    openCardTimeFormatted: '--',
    activateTimeFormatted: '--',
    // 冻结弹窗
    showFreezePopup: false,
    freezeReason: '',
    // 解冻弹窗
    showUnfreezePopup: false,
    unfreezeReason: '',
    // 操作中状态
    operating: false,
    // Toast相关
    showToast: false,
    toastMessage: '',
    toastType: 'success',
    toastAnimationType: 'slide'
  },

  onLoad(options) {
    if (options.memberCardId) {
      this.setData({ memberCardId: options.memberCardId });
      this.loadCardDetail();
    } else {
      this.showCustomToast('参数错误', 'danger');
      setTimeout(() => {
        wx.navigateBack();
      }, 1500);
    }
  },

  /**
   * 加载会员卡详情
   * 接口9: GET /api/v1/member-cards/detail
   */
  async loadCardDetail() {
    try {
      const token = await getNormalToken();
      if (!token) {
        this.showCustomToast('请先登录', 'danger');
        setTimeout(() => {
          wx.navigateTo({ url: '/pages/login/index' });
        }, 1500);
        return;
      }

      this.setData({ loading: true });

      const res = await get('/v1/member-cards/detail', {
        'Content-Type': 'application/json',
        'Authorization': token
      }, {
        memberCardId: this.data.memberCardId
      });

      console.log('[cards-detail] 会员卡详情响应:', res);

      if (res.code !== 200) {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
        this.setData({ loading: false });
        return;
      }

      const cardDetail = res.data || {};

      this.setData({
        cardDetail: cardDetail,
        expireTimeFormatted: this.formatDateTime(cardDetail.expireTime),
        openCardTimeFormatted: this.formatDateTime(cardDetail.openCardTime),
        activateTimeFormatted: this.formatDateTime(cardDetail.activateTime),
        loading: false
      });

    } catch (error) {
      console.error('[cards-detail] 加载失败:', error);
      this.showCustomToast('加载失败，请稍后重试', 'danger');
      this.setData({ loading: false });
    }
  },

  /**
   * 格式化日期时间
   */
  formatDateTime(dateTimeStr) {
    if (!dateTimeStr) return '--';
    try {
      const date = new Date(dateTimeStr);
      if (isNaN(date.getTime())) return dateTimeStr;
      
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const hour = String(date.getHours()).padStart(2, '0');
      const minute = String(date.getMinutes()).padStart(2, '0');
      
      return `${year}-${month}-${day} ${hour}:${minute}`;
    } catch (e) {
      return dateTimeStr;
    }
  },

  /**
   * 显示冻结弹窗
   */
  showFreezeModal() {
    this.setData({
      showFreezePopup: true,
      freezeReason: ''
    });
  },

  /**
   * 隐藏冻结弹窗
   */
  hideFreezeModal() {
    this.setData({
      showFreezePopup: false,
      freezeReason: ''
    });
  },

  /**
   * 阻止冒泡
   */
  preventBubble() {},

  /**
   * 冻结原因输入
   */
  onFreezeReasonInput(e) {
    this.setData({
      freezeReason: e.detail.value
    });
  },

  /**
   * 冻结会员卡
   * 接口12: POST /api/v1/member-cards/freeze
   */
  async handleFreeze() {
    const reasonLength = this.data.freezeReason.length;
    if (reasonLength < 1 || reasonLength > 50) {
      this.showCustomToast('冻结原因必须为1-50个字符', 'danger');
      return;
    }

    if (this.data.operating) return;

    try {
      this.setData({ operating: true });

      const token = await getNormalToken();
      if (!token) {
        this.showCustomToast('请先登录', 'danger');
        return;
      }

      const res = await post('/v1/member-cards/freeze', {
        'Content-Type': 'application/json',
        'Authorization': token
      }, {
        memberCardId: this.data.memberCardId,
        freezeReason: this.data.freezeReason
      });

      console.log('[cards-detail] 冻结响应:', res);

      if (res.code !== 200) {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
        return;
      }

      this.showCustomToast('会员卡已冻结', 'success');
      this.hideFreezeModal();
      
      // 刷新详情
      this.loadCardDetail();

    } catch (error) {
      console.error('[cards-detail] 冻结失败:', error);
      this.showCustomToast('操作失败，请稍后重试', 'danger');
    } finally {
      this.setData({ operating: false });
    }
  },

  /**
   * 显示解冻弹窗
   */
  showUnfreezeModal() {
    this.setData({
      showUnfreezePopup: true,
      unfreezeReason: ''
    });
  },

  /**
   * 隐藏解冻弹窗
   */
  hideUnfreezeModal() {
    this.setData({
      showUnfreezePopup: false,
      unfreezeReason: ''
    });
  },

  /**
   * 解冻原因输入
   */
  onUnfreezeReasonInput(e) {
    this.setData({
      unfreezeReason: e.detail.value
    });
  },

  /**
   * 解冻会员卡
   * 接口13: POST /api/v1/member-cards/unfreeze
   */
  async handleUnfreeze() {
    const reasonLength = this.data.unfreezeReason.length;
    if (reasonLength > 50) {
      this.showCustomToast('解冻原因不能超过50个字符', 'danger');
      return;
    }

    if (this.data.operating) return;
    try {
      this.setData({ operating: true });

      const token = await getNormalToken();
      if (!token) {
        this.showCustomToast('请先登录', 'danger');
        return;
      }

      const requestBody = {
        memberCardId: this.data.memberCardId
      };
      
      // 只有当解冻原因不为空时才添加到请求体
      if (this.data.unfreezeReason) {
        requestBody.unfreezeReason = this.data.unfreezeReason;
      }

      const res = await post('/v1/member-cards/unfreeze', {
        'Content-Type': 'application/json',
        'Authorization': token
      }, requestBody);

      console.log('[cards-detail] 解冻响应:', res);

      if (res.code !== 200) {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
        return;
      }

      this.showCustomToast('会员卡已解冻', 'success');
      this.hideUnfreezeModal();
      
      // 刷新详情
      this.loadCardDetail();

    } catch (error) {
      console.error('[cards-detail] 解冻失败:', error);
      this.showCustomToast('操作失败，请稍后重试', 'danger');
    } finally {
      this.setData({ operating: false });
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
   * 跳转到交易记录
   */
  goToTransRecords() {
    const { cardDetail, memberCardId } = this.data;
    
    // 格式化到期时间（只取日期部分）
    const expireTime = cardDetail.expireTime ? cardDetail.expireTime.split(' ')[0] : '';
    
    wx.navigateTo({
      url: `/pages/trans-card-records/trans-card-records?memberCardId=${memberCardId}&tokenType=normal&recordType=transaction&cardTypeName=${encodeURIComponent(cardDetail.cardTypeName || '')}&cardType=${encodeURIComponent(cardDetail.cardTtypeName || '')}&memberName=${encodeURIComponent(cardDetail.memberName || '')}&phone=${encodeURIComponent(cardDetail.memberPhone || '')}&balance=${cardDetail.balance || 0}&points=${cardDetail.points || 0}&times=${cardDetail.times || 0}&expireTime=${encodeURIComponent(expireTime)}&status=${cardDetail.status || 1}&originalStoreName=${encodeURIComponent(cardDetail.originalStoreName || '')}`
    });
  },

  /**
   * 跳转到积分记录
   */
  goToPointsRecords() {
    const { cardDetail, memberCardId } = this.data;
    
    // 格式化到期时间（只取日期部分）
    const expireTime = cardDetail.expireTime ? cardDetail.expireTime.split(' ')[0] : '';
    
    wx.navigateTo({
      url: `/pages/trans-card-records/trans-card-records?memberCardId=${memberCardId}&tokenType=normal&recordType=points&cardTypeName=${encodeURIComponent(cardDetail.cardTypeName || '')}&cardType=${encodeURIComponent(cardDetail.cardTtypeName || '')}&memberName=${encodeURIComponent(cardDetail.memberName || '')}&phone=${encodeURIComponent(cardDetail.memberPhone || '')}&balance=${cardDetail.balance || 0}&points=${cardDetail.points || 0}&times=${cardDetail.times || 0}&expireTime=${encodeURIComponent(expireTime)}&status=${cardDetail.status || 1}&originalStoreName=${encodeURIComponent(cardDetail.originalStoreName || '')}`
    });
  }
});
