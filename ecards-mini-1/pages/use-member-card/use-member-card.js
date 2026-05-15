// pages/use-member-card/use-member-card.js
const { get, post } = require('../../utils/request');
const tokenManager = require('../../utils/token');
const { encryptAES } = require('../../utils/encode');
const { validatePhone } = require('../../utils/validator-util');

Page({
  data: {
    storeId: '',
    storeName: '',
    hasWorkToken: false,

    // 搜索
    searchPhone: '',
    phoneError: '',
    searching: false,
    hasSearched: false,

    // 查询结果
    queryResult: {
      memberPhone: '',
      userId: null,
      localCards: [],
      crossStoreCards: []
    },

    // 卡种类型统计（用于显示对应表头）
    localCardTypes: { balance: false, times: false, expire: false, points: false },
    crossCardTypes: { balance: false, times: false, expire: false, points: false },

    // 详情弹窗
    showDetailModal: false,
    currentCard: null,
    currentTab: 'detail', // detail|consume|recharge|expire|points

    // 冻结/解冻弹窗
    showFreezeModal: false,
    freezeAction: '', // 'freeze' 或 'unfreeze'
    freezeReason: '',
    freezeReasonError: '',
    submittingFreeze: false,

    // 卡面展示弹窗
    showCardImageModal: false,
    cardImageData: {
      cardBgc: '',
      cardMask: ''
    },

    // 卡种详情缓存
    cardTypeDetailCache: {}, // { cardTypeId: { detail: {...}, timestamp: xxx } }

    // 预设项目
    presetRechargeList: [], // 预设充值项目
    presetConsumeList: [], // 预设消费项目
    showPresetSelector: false, // 显示预设项目选择器
    presetSelectorType: '', // 'recharge' 或 'consume'
    presetSelectorAnimating: false, // 动画状态
    
    // 单位数组
    units: ['', '元', '次', '天', '分'],

    // 划卡消费表单
    consumeAmount: '',
    consumeRemark: '',
    consumePoints: '',
    submittingConsume: false,

    // 充值表单
    rechargeAmount: '',
    rechargeRemark: '',
    submittingRecharge: false,

    // 延期表单
    expireAdjustType: 1, // 1-按天数延期, 2-设置到期日期
    expireDays: '',
    expireDate: '',
    expireRemark: '',
    submittingExpire: false,

    // 积分管理表单
    pointsChangeType: 'add', // add|reduce
    pointsChangeValue: '',
    pointsRemark: '',
    submittingPoints: false
  },

  onLoad(options) {
    const { storeId, storeName } = options;
    this.setData({
      storeId: storeId || '',
      storeName: decodeURIComponent(storeName || '未知店铺')
    });
    this.loadWorkToken();
  },

  async loadWorkToken() {
    const { storeId } = this.data;
    if (!storeId) {
      this.showCustomToast('店铺ID不存在', 'danger');
      return;
    }

    try {
      let workToken = await tokenManager.getWorkToken(storeId);
      if (!workToken) {
        await tokenManager.fetchWorkToken(storeId);
        workToken = await tokenManager.getWorkToken(storeId);
      }

      if (workToken) {
        this.setData({ hasWorkToken: true });
      } else {
        this.showCustomToast('获取工作令牌失败', 'danger');
      }
    } catch (error) {
      console.error('获取工作令牌失败:', error);
      this.showCustomToast('获取工作令牌失败', 'danger');
    }
  },

  onPhoneInput(e) {
    this.setData({
      searchPhone: e.detail.value,
      phoneError: ''
    });
  },

  async handleSearch() {
    const { searchPhone, storeId } = this.data;

    // 验证手机号
    if (!searchPhone) {
      this.setData({ phoneError: '请输入手机号' });
      return;
    }

    const result = validatePhone(searchPhone);
    if (!result.valid) {
      this.setData({ phoneError: result.message });
      return;
    }

    this.setData({ searching: true, phoneError: '' });

    try {
      const workToken = await tokenManager.getWorkToken(storeId);
      const encryptedPhone = encryptAES(searchPhone);

      const res = await get(
        '/v1/member-cards/query-by-phone',
        { 'Authorization': workToken },
        { storeId, memberPhone: encryptedPhone }
      );

      console.log('查询结果:', res);

      if (res.code === 200) {
        const { memberPhone, userId, localCards, crossStoreCards } = res.data;

        // 处理卡片数据
        const processedLocalCards = this.processCards(localCards || []);
        const processedCrossCards = this.processCards(crossStoreCards || []);

        // 统计卡种类型
        const localCardTypes = this.getCardTypes(processedLocalCards);
        const crossCardTypes = this.getCardTypes(processedCrossCards);

        this.setData({
          hasSearched: true,
          queryResult: {
            memberPhone,
            userId,
            localCards: processedLocalCards,
            crossStoreCards: processedCrossCards
          },
          localCardTypes,
          crossCardTypes
        });
      } else {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
      }
    } catch (error) {
      console.error('查询失败:', error);
      this.showCustomToast('查询失败，请重试', 'danger');
    } finally {
      this.setData({ searching: false });
    }
  },

  processCards(cards) {
    return cards.map(card => {
      const cardTtypeName = this.getCardTtypeName(card.cardTtype);
      const statusName = this.getStatusName(card.status);
      let displayValue = '';
      let expireDateDisplay = '';
      let remainingDays = null;

      // 根据卡种类型显示对应值
      switch (card.cardTtype) {
        case 1: // 余额卡
          displayValue = `${card.balance || 0}元`;
          break;
        case 2: // 次数卡
          displayValue = `${card.times || 0}次`;
          break;
        case 3: // 时效卡
          if (card.expireTime) {
            expireDateDisplay = card.expireTime.split('T')[0];
            // 计算剩余天数
            remainingDays = this.calculateRemainingDays(card.expireTime);
            displayValue = `${remainingDays}天`;
          } else {
            displayValue = '-';
          }
          break;
        case 4: // 积分卡
          displayValue = `${card.points || 0}分`;
          break;
        default:
          displayValue = '-';
      }

      return {
        ...card,
        cardTtypeName,
        statusName,
        displayValue,
        expireDateDisplay: expireDateDisplay || (card.expireTime ? card.expireTime.split('T')[0] : '-'),
        remainingDays
      };
    });
  },

  // 计算剩余天数
  calculateRemainingDays(expireTime) {
    if (!expireTime) return 0;
    
    const now = new Date();
    const expire = new Date(expireTime);
    const diffTime = expire - now;
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    
    // 未过期时至少为1，过期为0
    return diffDays > 0 ? diffDays : 0;
  },

  // 获取状态名称
  getStatusName(status) {
    const names = { 0: '未激活', 1: '正常', 2: '已过期', 3: '已冻结', 4: '已注销' };
    return names[status] || '未知';
  },

  getCardTtypeName(cardTtype) {
    const names = { 1: '余额卡', 2: '次数卡', 3: '时效卡', 4: '积分卡' };
    return names[cardTtype] || '未知类型';
  },

  getCardTypes(cards) {
    const types = { balance: false, times: false, expire: false, points: false };
    cards.forEach(card => {
      if (card.cardTtype === 1) types.balance = true;
      if (card.cardTtype === 2) types.times = true;
      if (card.cardTtype === 3) types.expire = true;
      if (card.cardTtype === 4) types.points = true;
    });
    return types;
  },

  // 显示卡面图片
  showCardImage(e) {
    const card = e.currentTarget.dataset.card;
    this.setData({
      showCardImageModal: true,
      cardImageData: {
        cardBgc: card.cardBgc || '',
        cardMask: card.cardMask || ''
      }
    });
  },

  // 隐藏卡面图片
  hideCardImageModal() {
    this.setData({
      showCardImageModal: false,
      cardImageData: {
        cardBgc: '',
        cardMask: ''
      }
    });
  },

  // 从详情中显示卡面图片
  showCardImageFromDetail() {
    const { currentCard } = this.data;
    this.setData({
      showCardImageModal: true,
      cardImageData: {
        cardBgc: currentCard.cardBgc || '',
        cardMask: currentCard.cardMask || ''
      }
    });
  },

  showCardDetail(e) {
    const card = e.currentTarget.dataset.card;
    
    // 计算明天的日期
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const tomorrowStr = tomorrow.toISOString().split('T')[0];
    
    this.setData({
      showDetailModal: true,
      currentCard: card,
      currentTab: 'detail',
      // 重置表单数据
      consumeAmount: '',
      consumeRemark: '',
      consumePoints: '',
      rechargeAmount: '',
      rechargeRemark: '',
      expireAdjustType: 1,
      expireDays: '',
      expireDate: tomorrowStr,
      expireRemark: '',
      pointsChangeType: 'add',
      pointsChangeValue: '',
      pointsRemark: '',
      // 重置预设项目
      presetRechargeList: [],
      presetConsumeList: []
    });

    // 加载卡种详情（获取预设项目）
    if (card.cardTypeId) {
      this.loadCardTypeDetail(card.cardTypeId);
    }
  },

  // 加载卡种详情（带缓存）
  async loadCardTypeDetail(cardTypeId) {
    const now = Date.now();
    const cache = this.data.cardTypeDetailCache[cardTypeId];
    
    // 检查缓存是否有效（30分钟 = 1800000毫秒）
    if (cache && (now - cache.timestamp) < 1800000) {
      console.log('使用缓存的卡种详情');
      this.parsePresetItems(cache.detail);
      return;
    }

    // 缓存无效或不存在，重新查询
    try {
      const { storeId } = this.data;
      const workToken = await tokenManager.getWorkToken(storeId);
      
      if (!workToken) {
        console.error('工作令牌无效');
        return;
      }
      
      const res = await get(
        '/v1/member-card-types/detail-query',
        { 'Authorization': workToken },
        { cardTypeId, storeId }
      );

      console.log('卡种详情查询结果:', res);

      if (res.code === 200) {
        const detail = res.data;
        
        // 更新缓存
        const newCache = { ...this.data.cardTypeDetailCache };
        newCache[cardTypeId] = {
          detail: detail,
          timestamp: now
        };
        this.setData({ cardTypeDetailCache: newCache });
        
        // 解析预设项目
        this.parsePresetItems(detail);
      } else {
        console.error('加载卡种详情失败:', res.message);
      }
    } catch (error) {
      console.error('加载卡种详情失败:', error);
    }
  },

  // 解析预设项目
  parsePresetItems(detail) {
    let presetRechargeList = [];
    let presetConsumeList = [];
    
    try {
      if (detail.presetRecharge) {
        presetRechargeList = JSON.parse(detail.presetRecharge);
      }
    } catch (e) {
      console.error('解析预设充值项目失败:', e);
    }

    try {
      // 注意：后端字段名是 presetCost，不是 presetConsume
      if (detail.presetCost) {
        presetConsumeList = JSON.parse(detail.presetCost);
      }
    } catch (e) {
      console.error('解析预设消费项目失败:', e);
    }

    this.setData({
      presetRechargeList,
      presetConsumeList
    });

    console.log('预设充值项目:', presetRechargeList);
    console.log('预设消费项目:', presetConsumeList);
  },

  hideDetailModal() {
    this.setData({
      showDetailModal: false,
      currentCard: null,
      currentTab: 'detail'
    });
  },

  // 切换tab
  switchTab(e) {
    const tab = e.currentTarget.dataset.tab;
    this.setData({ currentTab: tab });
  },

  // 显示冻结弹窗
  showFreezeDialog(e) {
    const action = e.currentTarget.dataset.action;
    this.setData({
      showFreezeModal: true,
      freezeAction: action,
      freezeReason: '',
      freezeReasonError: ''
    });
  },

  // 隐藏冻结弹窗
  hideFreezeModal() {
    this.setData({
      showFreezeModal: false,
      freezeAction: '',
      freezeReason: '',
      freezeReasonError: ''
    });
  },

  // 冻结原因输入
  onFreezeReasonInput(e) {
    this.setData({
      freezeReason: e.detail.value,
      freezeReasonError: ''
    });
  },

  // 提交冻结/解冻
  async handleFreezeSubmit() {
    const { freezeAction, freezeReason, currentCard, storeId, submittingFreeze } = this.data;

    if (submittingFreeze) return;

    // 检查currentCard是否存在
    if (!currentCard || !currentCard.memberCardId) {
      this.showCustomToast('会员卡信息错误，请重试', 'danger');
      this.hideFreezeModal();
      return;
    }

    // 冻结时验证原因
    if (freezeAction === 'freeze') {
      if (!freezeReason || freezeReason.trim().length === 0) {
        this.setData({ freezeReasonError: '请输入冻结原因' });
        return;
      }
      if (freezeReason.trim().length < 1 || freezeReason.trim().length > 50) {
        this.setData({ freezeReasonError: '冻结原因长度为1-50字' });
        return;
      }
    }

    // 解冻时原因非必填，但如果填写了也要验证长度
    if (freezeAction === 'unfreeze' && freezeReason && freezeReason.trim().length > 50) {
      this.setData({ freezeReasonError: '解冻原因长度不超过50字' });
      return;
    }

    this.setData({ submittingFreeze: true });

    try {
      const workToken = await tokenManager.getWorkToken(storeId);

      const url = freezeAction === 'freeze' 
        ? '/v1/member-cards/freeze' 
        : '/v1/member-cards/unfreeze';

      const requestData = {
        memberCardId: currentCard.memberCardId
      };

      // 根据操作类型添加原因参数
      if (freezeAction === 'freeze') {
        // 冻结：freezeReason 必填
        requestData.freezeReason = freezeReason.trim();
      } else {
        // 解冻：unfreezeReason 选填
        if (freezeReason && freezeReason.trim().length > 0) {
          requestData.unfreezeReason = freezeReason.trim();
        }
      }

      const res = await post(url, { 'Authorization': workToken }, requestData);

      if (res.code === 200) {
        this.showCustomToast(
          freezeAction === 'freeze' ? '冻结成功' : '解冻成功',
          'success'
        );
        
        // 更新当前卡片状态
        const newStatus = freezeAction === 'freeze' ? 3 : 1;
        const newStatusName = freezeAction === 'freeze' ? '已冻结' : '正常';
        
        this.setData({
          'currentCard.status': newStatus,
          'currentCard.statusName': newStatusName
        });

        // 更新列表中的卡片状态
        this.updateCardStatusInList(currentCard.memberCardId, newStatus, newStatusName);

        // 关闭弹窗
        this.hideFreezeModal();
      } else {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
      }
    } catch (error) {
      console.error('操作失败:', error);
      this.showCustomToast('操作失败，请重试', 'danger');
    } finally {
      this.setData({ submittingFreeze: false });
    }
  },

  // 更新列表中的卡片状态
  updateCardStatusInList(memberCardId, newStatus, newStatusName) {
    const { queryResult } = this.data;
    
    // 更新本店卡
    const updatedLocalCards = queryResult.localCards.map(card => {
      if (card.memberCardId === memberCardId) {
        return { ...card, status: newStatus, statusName: newStatusName };
      }
      return card;
    });

    // 更新跨店卡
    const updatedCrossCards = queryResult.crossStoreCards.map(card => {
      if (card.memberCardId === memberCardId) {
        return { ...card, status: newStatus, statusName: newStatusName };
      }
      return card;
    });

    this.setData({
      'queryResult.localCards': updatedLocalCards,
      'queryResult.crossStoreCards': updatedCrossCards
    });
  },

  showCustomToast(message, type = 'success') {
    const toast = this.selectComponent('#customToast');
    if (toast) {
      toast.showToast(message, type);
    }
  },

  // ==================== 划卡消费 ====================
  onConsumeAmountInput(e) {
    // 过滤非数字字符，允许小数点和0-9
    let value = e.detail.value.replace(/[^\d.]/g, '');
    // 只保留第一个小数点
    const parts = value.split('.');
    if (parts.length > 2) {
      value = parts[0] + '.' + parts.slice(1).join('');
    }
    this.setData({ consumeAmount: value });
  },

  onConsumeRemarkInput(e) {
    this.setData({ consumeRemark: e.detail.value });
  },

  onConsumePointsInput(e) {
    // 过滤非数字字符，只允许整数
    const value = e.detail.value.replace(/[^\d]/g, '');
    this.setData({ consumePoints: value });
  },

  async handleConsume() {
    const { currentCard, consumeAmount, consumeRemark, consumePoints, storeId, submittingConsume } = this.data;

    if (submittingConsume) return;

    // 验证
    if (!consumeAmount || parseFloat(consumeAmount) <= 0) {
      this.showCustomToast('请输入有效的消费金额/次数', 'danger');
      return;
    }

    if (parseFloat(consumeAmount) > 10000) {
      this.showCustomToast('单次消费不能超过10000', 'danger');
      return;
    }

    this.setData({ submittingConsume: true });

    try {
      const workToken = await tokenManager.getWorkToken(storeId);

      const requestData = {
        memberCardId: currentCard.memberCardId,
        storeId: storeId,
        consumeType: currentCard.cardTtype, // 1-金额, 2-次数
        amount: parseFloat(consumeAmount),
        remark: consumeRemark.trim() || '' // 允许空字符串
      };

      // 积分参数（选填）
      if (consumePoints && consumePoints.trim().length > 0) {
        requestData.points = parseInt(consumePoints);
      }

      const res = await post(
        '/v1/transactions/consume',
        { 'Authorization': workToken },
        requestData
      );

      console.log('消费结果:', res);

      if (res.code === 200) {
        this.showCustomToast('消费成功', 'success');
        
        // 更新卡片数据
        if (currentCard.cardTtype === 1) {
          this.setData({
            'currentCard.balance': res.data.balanceSnapshot,
            'currentCard.displayValue': `${res.data.balanceSnapshot}元`
          });
        } else if (currentCard.cardTtype === 2) {
          this.setData({
            'currentCard.times': res.data.balanceSnapshot,
            'currentCard.displayValue': `${res.data.balanceSnapshot}次`
          });
        }

        // 更新列表中的卡片
        this.updateCardInList(currentCard.memberCardId, {
          balance: res.data.balanceSnapshot,
          times: res.data.balanceSnapshot
        });

        // 清空表单
        this.setData({
          consumeAmount: '',
          consumeRemark: '',
          consumePoints: ''
        });

        // 切换回详情tab
        this.setData({ currentTab: 'detail' });
      } else {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
      }
    } catch (error) {
      console.error('消费失败:', error);
      this.showCustomToast('消费失败，请重试', 'danger');
    } finally {
      this.setData({ submittingConsume: false });
    }
  },

  // ==================== 充值 ====================
  onRechargeAmountInput(e) {
    // 过滤非数字字符，允许小数点和0-9
    let value = e.detail.value.replace(/[^\d.]/g, '');
    // 只保留第一个小数点
    const parts = value.split('.');
    if (parts.length > 2) {
      value = parts[0] + '.' + parts.slice(1).join('');
    }
    this.setData({ rechargeAmount: value });
  },

  onRechargeRemarkInput(e) {
    this.setData({ rechargeRemark: e.detail.value });
  },

  async handleRecharge() {
    const { currentCard, rechargeAmount, rechargeRemark, storeId, submittingRecharge } = this.data;

    if (submittingRecharge) return;

    // 验证
    if (!rechargeAmount || parseFloat(rechargeAmount) <= 0) {
      this.showCustomToast('请输入有效的充值金额/次数', 'danger');
      return;
    }

    if (parseFloat(rechargeAmount) > 10000) {
      this.showCustomToast('单次充值不能超过10000', 'danger');
      return;
    }

    this.setData({ submittingRecharge: true });

    try {
      const workToken = await tokenManager.getWorkToken(storeId);

      const requestData = {
        memberCardId: currentCard.memberCardId,
        storeId: storeId,
        rechargeType: currentCard.cardTtype, // 1-金额, 2-次数
        amount: parseFloat(rechargeAmount),
        remark: rechargeRemark.trim() || '' // 允许空字符串
      };

      const res = await post(
        '/v1/transactions/recharge',
        { 'Authorization': workToken },
        requestData
      );

      console.log('充值结果:', res);

      if (res.code === 200) {
        this.showCustomToast('充值成功', 'success');
        
        // 更新卡片数据
        if (currentCard.cardTtype === 1) {
          this.setData({
            'currentCard.balance': res.data.balanceSnapshot,
            'currentCard.displayValue': `${res.data.balanceSnapshot}元`
          });
        } else if (currentCard.cardTtype === 2) {
          this.setData({
            'currentCard.times': res.data.balanceSnapshot,
            'currentCard.displayValue': `${res.data.balanceSnapshot}次`
          });
        }

        // 更新列表中的卡片
        this.updateCardInList(currentCard.memberCardId, {
          balance: res.data.balanceSnapshot,
          times: res.data.balanceSnapshot
        });

        // 清空表单
        this.setData({
          rechargeAmount: '',
          rechargeRemark: ''
        });

        // 切换回详情tab
        this.setData({ currentTab: 'detail' });
      } else {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
      }
    } catch (error) {
      console.error('充值失败:', error);
      this.showCustomToast('充值失败，请重试', 'danger');
    } finally {
      this.setData({ submittingRecharge: false });
    }
  },

  // ==================== 延期 ====================
  onExpireAdjustTypeChange(e) {
    this.setData({ expireAdjustType: parseInt(e.detail.value) });
  },

  onExpireDaysInput(e) {
    // 过滤非数字字符，只允许整数
    const value = e.detail.value.replace(/[^\d]/g, '');
    this.setData({ expireDays: value });
  },

  onExpireDateChange(e) {
    this.setData({ expireDate: e.detail.value });
  },

  onExpireRemarkInput(e) {
    this.setData({ expireRemark: e.detail.value });
  },

  async handleExpireAdjust() {
    const { currentCard, expireAdjustType, expireDays, expireDate, expireRemark, storeId, submittingExpire } = this.data;

    if (submittingExpire) return;

    // 验证
    if (expireAdjustType === 1) {
      if (!expireDays || parseInt(expireDays) <= 0) {
        this.showCustomToast('请输入有效的延期天数', 'danger');
        return;
      }
    } else {
      if (!expireDate) {
        this.showCustomToast('请选择到期日期', 'danger');
        return;
      }
    }

    this.setData({ submittingExpire: true });

    try {
      const workToken = await tokenManager.getWorkToken(storeId);

      const requestData = {
        memberCardId: currentCard.memberCardId,
        storeId: storeId,
        adjustType: expireAdjustType,
        remark: expireRemark.trim() || '' // 允许空字符串
      };

      if (expireAdjustType === 1) {
        requestData.days = parseInt(expireDays);
      } else {
        // 将日期转换为 yyyy-MM-dd 23:59:59 格式
        requestData.expireTime = `${expireDate} 23:59:59`;
      }

      const res = await post(
        '/v1/transactions/expire-adjust',
        { 'Authorization': workToken },
        requestData
      );

      console.log('延期结果:', res);

      if (res.code === 200) {
        this.showCustomToast(expireAdjustType === 1 ? '延期成功' : '到期时间调整成功', 'success');
        
        // 更新卡片数据
        const newExpireTime = res.data.newExpireTime;
        const newRemainingDays = this.calculateRemainingDays(newExpireTime);
        
        this.setData({
          'currentCard.expireTime': newExpireTime,
          'currentCard.expireDateDisplay': newExpireTime.split(' ')[0],
          'currentCard.remainingDays': newRemainingDays,
          'currentCard.displayValue': `${newRemainingDays}天`
        });

        // 更新列表中的卡片
        this.updateCardInList(currentCard.memberCardId, {
          expireTime: newExpireTime,
          expireDateDisplay: newExpireTime.split(' ')[0],
          remainingDays: newRemainingDays,
          displayValue: `${newRemainingDays}天`
        });

        // 清空表单
        this.setData({
          expireDays: '',
          expireDate: '',
          expireRemark: ''
        });

        // 切换回详情tab
        this.setData({ currentTab: 'detail' });
      } else {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
      }
    } catch (error) {
      console.error('延期失败:', error);
      this.showCustomToast('延期失败，请重试', 'danger');
    } finally {
      this.setData({ submittingExpire: false });
    }
  },

  // ==================== 积分管理 ====================
  onPointsChangeTypeChange(e) {
    this.setData({ pointsChangeType: e.detail.value });
  },

  onPointsChangeValueInput(e) {
    // 过滤非数字字符，只允许整数
    const value = e.detail.value.replace(/[^\d]/g, '');
    this.setData({ pointsChangeValue: value });
  },

  onPointsRemarkInput(e) {
    this.setData({ pointsRemark: e.detail.value });
  },

  async handlePointsAdjust() {
    const { currentCard, pointsChangeType, pointsChangeValue, pointsRemark, storeId, submittingPoints } = this.data;

    if (submittingPoints) return;

    // 验证
    if (!pointsChangeValue || parseInt(pointsChangeValue) <= 0) {
      this.showCustomToast('请输入有效的积分变动值', 'danger');
      return;
    }

    if (parseInt(pointsChangeValue) > 10000) {
      this.showCustomToast('单次积分变动不能超过10000', 'danger');
      return;
    }

    // 变动原因改为选填，如果填写了则验证长度
    if (pointsRemark && pointsRemark.trim().length > 200) {
      this.showCustomToast('变动原因不能超过200字', 'danger');
      return;
    }

    this.setData({ submittingPoints: true });

    try {
      const workToken = await tokenManager.getWorkToken(storeId);

      // 计算实际变动值（减少时为负数）
      let pointsChange = parseInt(pointsChangeValue);
      if (pointsChangeType === 'reduce') {
        pointsChange = -pointsChange;
      }

      const requestData = {
        memberCardId: currentCard.memberCardId,
        storeId: storeId,
        pointsChange: pointsChange,
        remark: pointsRemark ? pointsRemark.trim() : '' // 如果为空则设置为空字符串
      };

      const res = await post(
        '/v1/points/adjust',
        { 'Authorization': workToken },
        requestData
      );

      console.log('积分调整结果:', res);

      if (res.code === 200) {
        this.showCustomToast('积分变动成功', 'success');
        
        // 更新卡片数据
        this.setData({
          'currentCard.points': res.data.pointsSnapshot
        });

        // 更新列表中的卡片（如果是积分卡）
        if (currentCard.cardTtype === 4) {
          this.updateCardInList(currentCard.memberCardId, {
            points: res.data.pointsSnapshot,
            displayValue: `${res.data.pointsSnapshot}分`
          });
        } else {
          this.updateCardInList(currentCard.memberCardId, {
            points: res.data.pointsSnapshot
          });
        }

        // 清空表单
        this.setData({
          pointsChangeValue: '',
          pointsRemark: ''
        });

        // 切换回详情tab
        this.setData({ currentTab: 'detail' });
      } else {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
      }
    } catch (error) {
      console.error('积分调整失败:', error);
      this.showCustomToast('积分调整失败，请重试', 'danger');
    } finally {
      this.setData({ submittingPoints: false });
    }
  },

  // 更新列表中的卡片数据
  updateCardInList(memberCardId, updates) {
    const { queryResult } = this.data;
    
    // 更新本店卡
    const updatedLocalCards = queryResult.localCards.map(card => {
      if (card.memberCardId === memberCardId) {
        return { ...card, ...updates };
      }
      return card;
    });

    // 更新跨店卡
    const updatedCrossCards = queryResult.crossStoreCards.map(card => {
      if (card.memberCardId === memberCardId) {
        return { ...card, ...updates };
      }
      return card;
    });

    this.setData({
      'queryResult.localCards': updatedLocalCards,
      'queryResult.crossStoreCards': updatedCrossCards
    });
  },

  // ==================== 预设项目选择器 ====================
  // 打开预设项目选择器
  openPresetSelector(e) {
    const type = e.currentTarget.dataset.type; // 'recharge' 或 'consume'
    this.setData({
      showPresetSelector: true,
      presetSelectorType: type
    }, () => {
      // 延迟10ms后添加show类，触发动画
      setTimeout(() => {
        this.setData({ presetSelectorAnimating: true });
      }, 10);
    });
  },

  // 关闭预设项目选择器
  closePresetSelector() {
    // 先移除show类，播放关闭动画
    this.setData({ presetSelectorAnimating: false });
    
    // 50ms后隐藏元素
    setTimeout(() => {
      this.setData({
        showPresetSelector: false,
        presetSelectorType: ''
      });
    }, 50);
  },

  // 选择预设项目
  selectPresetItem(e) {
    const item = e.currentTarget.dataset.item;
    const { presetSelectorType } = this.data;

    if (presetSelectorType === 'recharge') {
      // 充值：填充金额和备注
      this.setData({
        rechargeAmount: String(item.amount),
        rechargeRemark: item.itemDesc || item.itemName
      });
    } else if (presetSelectorType === 'consume') {
      // 消费：填充金额和备注
      this.setData({
        consumeAmount: String(item.amount),
        consumeRemark: item.itemDesc || item.itemName
      });
    }

    // 关闭选择器
    this.closePresetSelector();
  },

  // 阻止事件冒泡（空方法）
  stopPropagation() {
    // 阻止事件冒泡到mask层
  },

  // 跳转到交易记录
  goToTransRecords() {
    const { currentCard, storeId } = this.data;
    
    wx.navigateTo({
      url: `/pages/trans-card-records/trans-card-records?memberCardId=${currentCard.memberCardId}&tokenType=work&recordType=transaction&storeId=${storeId}&cardTypeName=${encodeURIComponent(currentCard.cardTypeName)}&cardType=${encodeURIComponent(currentCard.cardTtypeName)}&memberName=${encodeURIComponent(currentCard.memberName || '')}&phone=${encodeURIComponent(currentCard.memberPhone)}&balance=${currentCard.balance || 0}&points=${currentCard.points || 0}&times=${currentCard.times || 0}&expireTime=${encodeURIComponent(currentCard.expireDateDisplay || '')}&remainingDays=${currentCard.remainingDays || 0}&status=${currentCard.status}&originalStoreName=${encodeURIComponent(currentCard.originalStoreName || '')}`
    });
  },

  // 跳转到积分记录
  goToPointsRecords() {
    const { currentCard, storeId } = this.data;
    
    wx.navigateTo({
      url: `/pages/trans-card-records/trans-card-records?memberCardId=${currentCard.memberCardId}&tokenType=work&recordType=points&storeId=${storeId}&cardTypeName=${encodeURIComponent(currentCard.cardTypeName)}&cardType=${encodeURIComponent(currentCard.cardTtypeName)}&memberName=${encodeURIComponent(currentCard.memberName || '')}&phone=${encodeURIComponent(currentCard.memberPhone)}&balance=${currentCard.balance || 0}&points=${currentCard.points || 0}&times=${currentCard.times || 0}&expireTime=${encodeURIComponent(currentCard.expireDateDisplay || '')}&remainingDays=${currentCard.remainingDays || 0}&status=${currentCard.status}&originalStoreName=${encodeURIComponent(currentCard.originalStoreName || '')}`
    });
  }
});
