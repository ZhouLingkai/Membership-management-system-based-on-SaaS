// pages/apply-for-card/apply-for-card.js
const { get, post } = require('../../utils/request');
const tokenManager = require('../../utils/token');
const { encryptAES } = require('../../utils/encode');
const { validatePhone } = require('../../utils/validator-util');

Page({
  data: {
    storeId: '',
    storeName: '',
    hasWorkToken: false,

    // 卡种列表
    cardTypeList: [],
    cardTypeLoading: false,
    selectedCardType: null,

    // 卡种详情缓存
    cardTypeDetailCache: {}, // { cardTypeId: { detail: {...}, timestamp: xxx } }

    // 预设充值项目
    presetRechargeList: [],
    selectedPresetIndex: -1, // -1表示未选中

    // 表单数据
    formData: {
      memberPhone: '',
      memberName: '',
      cardTypeId: null,
      initialBalance: '',
      initialTimes: '',
      initialPoints: '',
      expireDate: '',
      validDays: '' // 时效卡有效天数
    },

    // 表单验证
    phoneError: '',
    canSubmit: false,
    submitting: false,

    // 日期选择范围
    minExpireDate: '',
    maxExpireDate: '',

    // 成功弹窗
    showSuccessModal: false,
    successInfo: {},

    units: ['','元','次','天','分']
  },

  onLoad(options) {
    const { storeId, storeName } = options;
    
    // 设置日期范围
    const today = new Date();
    const minDate = new Date(today);
    minDate.setDate(minDate.getDate() + 1);
    const maxDate = new Date(today);
    maxDate.setFullYear(maxDate.getFullYear() + 60);

    this.setData({
      storeId: storeId || '',
      storeName: decodeURIComponent(storeName || '未知店铺'),
      minExpireDate: this.formatDate(minDate),
      maxExpireDate: this.formatDate(maxDate)
    });

    this.loadWorkToken();
  },

  formatDate(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
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
        this.loadCardTypeList();
      } else {
        this.showCustomToast('获取工作令牌失败', 'danger');
      }
    } catch (error) {
      console.error('获取工作令牌失败:', error);
      this.showCustomToast('获取工作令牌失败', 'danger');
    }
  },

  async loadCardTypeList() {
    this.setData({ cardTypeLoading: true });

    try {
      const { storeId } = this.data;
      const workToken = await tokenManager.getWorkToken(storeId);

      const res = await get(
        '/v1/member-card-types/list-query',
        { 'Authorization': workToken },
        { storeId, pageNum: 1, pageSize: 50 }
      );
        console.log("[apply for card]", res)
      if (res.code === 200) {
        const cardTypeList = res.data.list.map(item => ({
          ...item,
          cardTtypeName: this.getCardTtypeName(item.cardTtype)
        }));
        this.setData({ cardTypeList });
        
        // 默认选中第一个卡种
        if (cardTypeList.length > 0) {
          this.selectCardType({ currentTarget: { dataset: { cardType: cardTypeList[0] } } });
        }
      } else {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
      }
    } catch (error) {
      console.error('加载卡种列表失败:', error);
      this.showCustomToast('加载卡种列表失败', 'danger');
    } finally {
      this.setData({ cardTypeLoading: false });
    }
  },

  getCardTtypeName(cardTtype) {
    const names = { 1: '余额卡', 2: '次数卡', 3: '时效卡', 4: '积分卡' };
    return names[cardTtype] || '未知类型';
  },

  // 手机号输入
  onPhoneInput(e) {
    const value = e.detail.value;
    this.setData({
      'formData.memberPhone': value,
      phoneError: ''
    });
    this.checkCanSubmit();
  },

  // 验证手机号
  validatePhone() {
    const { memberPhone } = this.data.formData;
    if (!memberPhone) {
      this.setData({ phoneError: '请输入手机号' });
      return false;
    }
    
    const result = validatePhone(memberPhone);
    if (!result.valid) {
      this.setData({ phoneError: result.message });
      return false;
    }
    
    this.setData({ phoneError: '' });
    return true;
  },

  // 姓名输入
  onNameInput(e) {
    this.setData({ 'formData.memberName': e.detail.value });
  },

  // 选择卡种
  async selectCardType(e) {
    const cardType = e.currentTarget.dataset.cardType;
    this.setData({
      selectedCardType: cardType,
      'formData.cardTypeId': cardType.cardTypeId,
      'formData.initialBalance': '',
      'formData.initialTimes': '',
      'formData.initialPoints': '',
      'formData.validDays': '',
      presetRechargeList: [],
      selectedPresetIndex: -1
    });
    this.checkCanSubmit();
    // 加载卡种详情
    await this.loadCardTypeDetail(cardType.cardTypeId);
  },

  // 加载卡种详情（带缓存）
  async loadCardTypeDetail(cardTypeId) {
    const now = Date.now();
    const cache = this.data.cardTypeDetailCache[cardTypeId];
    
    // 检查缓存是否有效（30分钟 = 1800000毫秒）
    if (cache && (now - cache.timestamp) < 1800000) {
      console.log('使用缓存的卡种详情');
      this.parsePresetRecharge(cache.detail);
      return;
    }

    // 缓存无效或不存在，重新查询
    try {
      const { storeId } = this.data;
      const workToken = await tokenManager.getWorkToken(storeId);
      
      if (!workToken) {
        this.showCustomToast('工作令牌无效', 'danger');
        return;
      }
      
      const res = await get(
        '/v1/member-card-types/detail-query',
        { 'Authorization': workToken },
        { cardTypeId, storeId }
      );

      if (res.code === 200) {
        const detail = res.data;
        
        // 更新缓存
        const newCache = { ...this.data.cardTypeDetailCache };
        newCache[cardTypeId] = {
          detail: detail,
          timestamp: now
        };
        this.setData({ cardTypeDetailCache: newCache });
        
        // 解析预设充值项目
        this.parsePresetRecharge(detail);
      } else {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
      }
    } catch (error) {
      console.error('加载卡种详情失败:', error);
      this.showCustomToast('加载卡种详情失败', 'danger');
    }
  },

  // 解析预设充值项目
  parsePresetRecharge(detail) {
    let presetRechargeList = [];
    try {
      if (detail.presetRecharge) {
        presetRechargeList = JSON.parse(detail.presetRecharge);
      }
    } catch (e) {
      console.error('解析预设充值项目失败:', e);
    }
    this.setData({ presetRechargeList });
  },

  // 选择预设充值项目
  selectPresetItem(e) {
    const index = e.currentTarget.dataset.index;
    const currentIndex = this.data.selectedPresetIndex;
    const { selectedCardType } = this.data;
    
    // 如果点击的是已选中的项，则取消选中
    if (index === currentIndex) {
      this.setData({
        selectedPresetIndex: -1
      });
      // 根据卡种类型清空对应字段
      this.clearInitialValueByCardType(selectedCardType.cardTtype);
    } else {
      // 选中新项
      const item = this.data.presetRechargeList[index];
      this.setData({
        selectedPresetIndex: index
      });
      // 根据卡种类型填充对应字段
      this.fillInitialValueByCardType(selectedCardType.cardTtype, item.amount);
    }
  },

  // 根据卡种类型填充初始值
  fillInitialValueByCardType(cardTtype, amount) {
    switch (cardTtype) {
      case 1: // 余额卡
        this.setData({ 'formData.initialBalance': String(amount) });
        break;
      case 2: // 次数卡
        this.setData({ 'formData.initialTimes': String(amount) });
        break;
      case 3: // 时效卡
        this.setData({ 'formData.validDays': String(amount) });
        break;
      case 4: // 积分卡
        this.setData({ 'formData.initialPoints': String(amount) });
        break;
    }
  },

  // 根据卡种类型清空初始值
  clearInitialValueByCardType(cardTtype) {
    switch (cardTtype) {
      case 1: // 余额卡
        this.setData({ 'formData.initialBalance': '0' });
        break;
      case 2: // 次数卡
        this.setData({ 'formData.initialTimes': '0' });
        break;
      case 3: // 时效卡
        this.setData({ 'formData.validDays': '1' });
        break;
      case 4: // 积分卡
        this.setData({ 'formData.initialPoints': '0' });
        break;
    }
  },

  // 获取金额单位
  getAmountUnit(cardTtype) {
    const units = { 1: '元', 2: '次', 3: '天', 4: '分' };
    return units[cardTtype] || '';
  },

  // 获取金额前缀
  getAmountPrefix(cardTtype) {
    return cardTtype === 1 ? '¥' : '';
  },

  // 余额输入
  onBalanceInput(e) {
    this.setData({ 'formData.initialBalance': e.detail.value });
  },

  // 次数输入
  onTimesInput(e) {
    this.setData({ 'formData.initialTimes': e.detail.value });
  },

  // 积分输入
  onPointsInput(e) {
    this.setData({ 'formData.initialPoints': e.detail.value });
  },

  // 有效天数输入（时效卡）
  onValidDaysInput(e) {
    this.setData({ 'formData.validDays': e.detail.value });
  },

  // 到期日期选择
  onExpireDateChange(e) {
    this.setData({ 'formData.expireDate': e.detail.value });
  },

  // 检查是否可以提交
  checkCanSubmit() {
    const { memberPhone, cardTypeId } = this.data.formData;
    const canSubmit = memberPhone.length === 11 && cardTypeId !== null;
    this.setData({ canSubmit });
  },

  // 提交办卡
  async handleSubmit() {
    if (!this.validatePhone()) return;
    if (!this.data.formData.cardTypeId) {
      this.showCustomToast('请选择卡种', 'danger');
      return;
    }

    // 时效卡必须填写有效天数
    const { selectedCardType, formData } = this.data;
    if (selectedCardType.cardTtype === 3) {
      const days = parseInt(formData.validDays);
      if (!formData.validDays || isNaN(days) || days < 1) {
        this.showCustomToast('请输入有效天数（大于等于1）', 'danger');
        return;
      }
    }

    this.setData({ submitting: true });

    try {
      const { storeId } = this.data;
      const workToken = await tokenManager.getWorkToken(storeId);

      // 加密手机号
      const encryptedPhone = encryptAES(formData.memberPhone);

      // 构建请求体
      const requestBody = {
        storeId,
        cardTypeId: formData.cardTypeId,
        memberPhone: encryptedPhone,
        memberName: formData.memberName || undefined
      };

      // 根据卡种类型添加初始值
      if (selectedCardType.cardTtype === 1 && formData.initialBalance) {
        requestBody.initialBalance = parseFloat(formData.initialBalance);
      }
      if (selectedCardType.cardTtype === 2 && formData.initialTimes) {
        requestBody.initialTimes = parseInt(formData.initialTimes);
      }
      if (formData.initialPoints) {
        requestBody.initialPoints = parseInt(formData.initialPoints);
      }

      // 时效卡：根据有效天数计算expireTime
      if (selectedCardType.cardTtype === 3 && formData.validDays) {
        const days = parseInt(formData.validDays);
        const expireDate = new Date();
        expireDate.setDate(expireDate.getDate() + days);
        requestBody.expireTime = this.formatDate(expireDate) + 'T23:59:59';
      } else if (formData.expireDate) {
        // 其他卡种：使用手动选择的到期日期
        requestBody.expireTime = formData.expireDate + 'T23:59:59';
      }

      console.log('办卡请求:', requestBody);

      const res = await post(
        '/v1/member-cards/create-by-phone',
        { 'Authorization': workToken, 'Content-Type': 'application/json' },
        requestBody
      );

      console.log('办卡响应:', res);

      if (res.code === 200) {
        // 处理到期时间显示格式
        const successInfo = { ...res.data };
        if (successInfo.expireTime) {
          // 将 ISO8601 格式转换为 "YYYY-MM-DD HH:MM:SS"
          successInfo.expireTimeDisplay = successInfo.expireTime.replace('T', ' ');
        }
        
        this.setData({
          showSuccessModal: true,
          successInfo: successInfo
        });
      } else {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
      }
    } catch (error) {
      console.error('办卡失败:', error);
      this.showCustomToast('办卡失败，请重试', 'danger');
    } finally {
      this.setData({ submitting: false });
    }
  },

  // 关闭成功弹窗
  closeSuccessModal() {
    this.setData({ showSuccessModal: false });
  },

  // 继续办卡
  continueApply() {
    this.setData({
      showSuccessModal: false,
      formData: {
        memberPhone: '',
        memberName: '',
        cardTypeId: null,
        initialBalance: '',
        initialTimes: '',
        initialPoints: '',
        expireDate: '',
        validDays: ''
      },
      selectedCardType: null,
      canSubmit: false
    });
  },

  // 返回工作台
  goBack() {
    wx.navigateBack();
  },

  // 显示自定义Toast
  showCustomToast(message, type = 'success') {
    const toast = this.selectComponent('#customToast');
    if (toast) {
      toast.showToast(message, type);
    }
  }
});
