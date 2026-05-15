// pages/member-search-center/member-search-center.js
const { get } = require('../../utils/request');
const tokenManager = require('../../utils/token');
const { encryptAES } = require('../../utils/encode');
const { validatePhone } = require('../../utils/validator-util');

Page({
  data: {
    storeId: '',
    storeName: '',
    hasWorkToken: false,

    // 筛选选项
    cardScopeOptions: [
      { value: 'local', label: '本店卡' },
      { value: 'cross_store', label: '跨店卡' }
    ],
    cardScopeIndex: 0,

    cardTypeList: [],
    selectedCardTypeId: '',
    selectedCardTypeName: '',

    cardTtypeOptions: [
      { value: '', label: '不限' },
      { value: 1, label: '余额卡' },
      { value: 2, label: '次数卡' },
      { value: 3, label: '时效卡' },
      { value: 4, label: '积分卡' }
    ],
    cardTtypeIndex: 0,

    statusOptions: [
      { value: 1, label: '正常' },
      { value: '', label: '不限' },
      { value: 0, label: '未激活' },
      { value: 2, label: '已过期' },
      { value: 3, label: '已冻结' },
      { value: 4, label: '已注销' }
    ],
    statusIndex: 0,

    // 筛选输入
    filterPhone: '',
    filterName: '',
    startDate: '',
    endDate: '',

    // 自定义选择器弹窗
    showPickerPopup: false,
    pickerAnimating: false,
    pickerType: '',
    pickerTitle: '',
    pickerOptions: [],

    // 分页
    pageSizeOptions: [
      { value: 50, label: '50条/页' },
      { value: 25, label: '25条/页' },
      { value: 75, label: '75条/页' },
      { value: 100, label: '100条/页' }
    ],
    pageSizeIndex: 0,
    pageNum: 1,
    pageSize: 50,
    total: 0,
    totalPages: 0,

    // 查询结果
    loading: false,
    hasSearched: false,
    cardList: [],

    // 详情弹窗
    showDetailModal: false,
    currentCard: null
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
    try {
      const { storeId } = this.data;
      const workToken = await tokenManager.getWorkToken(storeId);

      const res = await get(
        '/v1/member-card-types/list-query',
        { 'Authorization': workToken },
        { storeId, pageNum: 1, pageSize: 50 }
      );

      if (res.code === 200) {
        this.setData({ cardTypeList: res.data.list || [] });
      }
    } catch (error) {
      console.error('加载卡种列表失败:', error);
    }
  },

  // 打开选择器弹窗
  openPicker(e) {
    const type = e.currentTarget.dataset.type;
    let title = '';
    let options = [];

    switch (type) {
      case 'cardScope':
        title = '选择卡范围';
        options = this.data.cardScopeOptions;
        break;
      case 'cardType':
        title = '选择卡种';
        break;
      case 'cardTtype':
        title = '选择卡类型';
        options = this.data.cardTtypeOptions;
        break;
      case 'status':
        title = '选择状态';
        options = this.data.statusOptions;
        break;
    }

    this.setData({
      showPickerPopup: true,
      pickerType: type,
      pickerTitle: title,
      pickerOptions: options
    }, () => {
      setTimeout(() => {
        this.setData({ pickerAnimating: true });
      }, 10);
    });
  },

  closePickerPopup() {
    this.setData({ pickerAnimating: false });
    setTimeout(() => {
      this.setData({ showPickerPopup: false });
    }, 250);
  },

  // 点击选项立即应用并关闭
  selectPickerOption(e) {
    const index = parseInt(e.currentTarget.dataset.index, 10);
    const { pickerType } = this.data;

    // 立即应用选择
    switch (pickerType) {
      case 'cardScope':
        this.setData({ cardScopeIndex: index });
        break;
      case 'cardTtype':
        this.setData({ cardTtypeIndex: index });
        break;
      case 'status':
        this.setData({ statusIndex: index });
        break;
    }

    this.closePickerPopup();
  },

  selectCardTypeOption(e) {
    const id = e.currentTarget.dataset.id;
    const name = e.currentTarget.dataset.name || '';
    
    this.setData({
      selectedCardTypeId: id,
      selectedCardTypeName: name
    });
    
    this.closePickerPopup();
  },

  onFilterPhoneInput(e) {
    this.setData({ filterPhone: e.detail.value });
  },

  onFilterNameInput(e) {
    this.setData({ filterName: e.detail.value });
  },

  onStartDateChange(e) {
    this.setData({ startDate: e.detail.value });
  },

  onEndDateChange(e) {
    this.setData({ endDate: e.detail.value });
  },

  resetDateFilter() {
    this.setData({ startDate: '', endDate: '' });
  },

  onPageSizeChange(e) {
    const index = e.detail.value;
    const pageSize = this.data.pageSizeOptions[index].value;
    this.setData({
      pageSizeIndex: index,
      pageSize,
      pageNum: 1
    });
    this.doSearch();
  },

  prevPage() {
    if (this.data.pageNum <= 1) return;
    this.setData({ pageNum: this.data.pageNum - 1 });
    this.doSearch();
  },

  nextPage() {
    if (this.data.pageNum >= this.data.totalPages) return;
    this.setData({ pageNum: this.data.pageNum + 1 });
    this.doSearch();
  },

  handleSearch() {
    // 验证手机号格式（如果填写了）
    if (this.data.filterPhone) {
      const result = validatePhone(this.data.filterPhone);
      if (!result.valid) {
        this.showCustomToast(result.message, 'danger');
        return;
      }
    }

    this.setData({ pageNum: 1 });
    this.doSearch();
  },

  async doSearch() {
    this.setData({ loading: true });

    try {
      const { 
        storeId, pageNum, pageSize,
        cardScopeOptions, cardScopeIndex,
        selectedCardTypeId,
        cardTtypeOptions, cardTtypeIndex,
        statusOptions, statusIndex,
        filterPhone, filterName, startDate, endDate
      } = this.data;

      const workToken = await tokenManager.getWorkToken(storeId);

      // 构建查询参数
      const params = {
        storeId,
        cardScope: cardScopeOptions[cardScopeIndex].value,
        pageNum,
        pageSize
      };

      // 可选参数
      if (selectedCardTypeId) params.cardTypeId = selectedCardTypeId;

      const cardTtype = cardTtypeOptions[cardTtypeIndex].value;
      if (cardTtype !== '') params.cardTtype = cardTtype;

      const status = statusOptions[statusIndex].value;
      if (status !== '') params.status = status;

      if (filterPhone) {
        params.memberPhone = encryptAES(filterPhone);
      }

      if (filterName) {
        params.memberName = filterName;
      }

      if (startDate) {
        params.startTime = startDate + 'T00:00:00';
      }

      if (endDate) {
        params.endTime = endDate + 'T23:59:59';
      }

      console.log('查询参数:', params);

      const res = await get(
        '/v1/member-cards/store-list',
        { 'Authorization': workToken },
        params
      );

      console.log('查询结果:', res);

      if (res.code === 200) {
        const { list, total } = res.data;
        const processedList = this.processCards(list || []);
        const totalPages = Math.ceil(total / pageSize);

        this.setData({
          hasSearched: true,
          cardList: processedList,
          total,
          totalPages
        });
      } else {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
      }
    } catch (error) {
      console.error('查询失败:', error);
      this.showCustomToast('查询失败，请重试', 'danger');
    } finally {
      this.setData({ loading: false });
    }
  },

  processCards(cards) {
    return cards.map(card => {
      const cardTtypeName = this.getCardTtypeName(card.cardTtype);
      let displayValue = '';
      let expireDateDisplay = '';

      switch (card.cardTtype) {
        case 1:
          displayValue = `¥${card.balance || 0}`;
          break;
        case 2:
          displayValue = `${card.times || 0}次`;
          break;
        case 3:
          if (card.expireTime) {
            expireDateDisplay = card.expireTime.split('T')[0];
            displayValue = expireDateDisplay;
          } else {
            displayValue = '-';
          }
          break;
        case 4:
          displayValue = `${card.points || 0}分`;
          break;
        default:
          displayValue = '-';
      }

      return {
        ...card,
        cardTtypeName,
        displayValue,
        expireDateDisplay: expireDateDisplay || (card.expireTime ? card.expireTime.split('T')[0] : '-')
      };
    });
  },

  getCardTtypeName(cardTtype) {
    const names = { 1: '余额卡', 2: '次数卡', 3: '时效卡', 4: '积分卡' };
    return names[cardTtype] || '未知类型';
  },

  showCardDetail(e) {
    const card = e.currentTarget.dataset.card;
    this.setData({
      showDetailModal: true,
      currentCard: card
    });
  },

  hideDetailModal() {
    this.setData({
      showDetailModal: false,
      currentCard: null
    });
  },

  showCustomToast(message, type = 'success') {
    const toast = this.selectComponent('#customToast');
    if (toast) {
      toast.showToast(message, type);
    }
  }
});
