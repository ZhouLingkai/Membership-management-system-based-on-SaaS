// pages/advanced-reservation/advanced-reservation.js
const { request } = require('../../utils/request');
const tokenManager = require('../../utils/token');

Page({
  data: {
    storeId: '',
    storeName: '',
    hasWorkToken: false,
    
    // Tab切换
    activeTab: 'template',
    
    // 模板数据
    templateData: null,
    templateLoading: false,
    templateError: '',
    lastRefreshTime: 0,
    refreshCooldown: false,
    
    // 取消规则展示（可读语言）
    displayCancelRules: [],
    
    // 修改弹窗
    showEditModal: false,
    editTab: 'reservationTimeList',
    editData: {
      reservationTimeList: [],
      cancelRule: [],
      cancelRuleParsed: [], // 解析后的规则 [{minutes, type, value}]
      advanceDays: 7,
      weekDays: [],
      specificDates: [],
      unitTime: 60,
      restTime: 0,
      startTime: '08:00',
      endTime: '22:00'
    },
    
    // 资源列表
    resourceList: [],
    resourceLoading: false,
    resourcePage: 1,
    resourcePageSize: 20,
    resourceTotal: 0,
    resourceHasMore: true,
    
    // 资源图片展示弹窗
    showResourceImageModal: false,
    currentResourceImage: ''
  },

  onLoad(options) {
    const { storeId, storeName } = options;
    this.setData({
      storeId: storeId || '',
      storeName: decodeURIComponent(storeName || '未知店铺')
    });
    
    this.loadWorkToken();
  },

  // 阻止事件冒泡
  stopPropagation() {
    // 空函数，仅用于阻止冒泡
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
        this.loadTemplate();
      } else {
        this.showCustomToast('获取工作令牌失败', 'danger');
      }
    } catch (error) {
      console.error('[获取工作令牌失败]', error);
      this.showCustomToast('获取工作令牌失败', 'danger');
    }
  },

  // 解析规则字符串为可读语言
  parseRuleToReadable(ruleStr) {
    // 格式: "60:0.1" 或 "60:10"
    const parts = ruleStr.split(':');
    if (parts.length !== 2) return ruleStr;
    
    const minutes = parseInt(parts[0]);
    const value = parseFloat(parts[1]);
    
    if (isNaN(minutes) || isNaN(value)) return ruleStr;
    
    // 判断是百分比还是固定金额：<=0.5为百分比，>=1为固定金额
    if (value <= 0.5) {
      const percent = Math.round(value * 100);
      return `预约开始前${minutes}分钟内取消，扣除${percent}%违约金`;
    } else {
      return `预约开始前${minutes}分钟内取消，扣除${value}元违约金`;
    }
  },

  // 解析规则字符串为对象
  parseRuleToObject(ruleStr) {
    const parts = ruleStr.split(':');
    if (parts.length !== 2) return null;
    
    const minutes = parseInt(parts[0]);
    const rawValue = parseFloat(parts[1]);
    
    if (isNaN(minutes) || isNaN(rawValue)) return null;
    
    // 判断是百分比还是固定金额
    if (rawValue <= 0.5) {
      return {
        minutes,
        type: 'percent',
        value: Math.round(rawValue * 100) // 0.1 -> 10
      };
    } else {
      return {
        minutes,
        type: 'fixed',
        value: rawValue
      };
    }
  },

  // 将对象转换为规则字符串
  objectToRuleStr(ruleObj) {
    if (ruleObj.type === 'percent') {
      return `${ruleObj.minutes}:${ruleObj.value / 100}`; // 10 -> 0.1
    } else {
      return `${ruleObj.minutes}:${ruleObj.value}`;
    }
  },

  // 加载模板
  async loadTemplate() {
    this.setData({ templateLoading: true, templateError: '' });

    try {
      const { storeId } = this.data;
      const workToken = await tokenManager.getWorkToken(storeId);

      const res = await request.get(
        '/v1/advanced-reservation/template',
        { 'Authorization': workToken },
        { storeId }
      );

      console.log('[查询高级预约模板]', res);

      if (res.code === 200) {
        const templateData = res.data || null;
        
        // 解析取消规则为可读语言
        let displayCancelRules = [];
        if (templateData && templateData.cancelRule) {
          displayCancelRules = templateData.cancelRule.map(rule => this.parseRuleToReadable(rule));
        }
        
        this.setData({
          templateData,
          displayCancelRules,
          templateLoading: false
        });
      } else if (res.code === 100012) {
        this.setData({
          templateData: null,
          displayCancelRules: [],
          templateLoading: false
        });
      } else {
        this.setData({
          templateError: res.message || '加载失败',
          templateLoading: false
        });
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
      }
    } catch (error) {
      console.error('[加载模板失败]', error);
      this.setData({
        templateError: '加载失败，请稍后重试',
        templateLoading: false
      });
    }
  },

  refreshTemplate() {
    if (this.data.refreshCooldown) return;

    this.setData({
      refreshCooldown: true,
      lastRefreshTime: Date.now()
    });

    this.loadTemplate();

    const cooldownTime = 4000 + Math.random() * 2000;
    setTimeout(() => {
      this.setData({ refreshCooldown: false });
    }, cooldownTime);
  },

  goToCreate() {
    wx.navigateTo({
      url: `/pages/advanced-reservation-create/advanced-reservation-create?storeId=${this.data.storeId}&storeName=${encodeURIComponent(this.data.storeName)}`
    });
  },

  switchTab(e) {
    const tab = e.currentTarget.dataset.tab;
    this.setData({ activeTab: tab });

    if (tab === 'resources' && this.data.resourceList.length === 0) {
      this.loadResources();
    }
  },

  async loadResources(loadMore = false) {
    if (this.data.resourceLoading) return;

    this.setData({ resourceLoading: true });

    try {
      const { storeId, resourcePage, resourcePageSize } = this.data;
      const workToken = await tokenManager.getWorkToken(storeId);

      const res = await request.get(
        '/v1/advanced-reservation/resources',
        { 'Authorization': workToken },
        {
          storeId,
          page: loadMore ? resourcePage : 1,
          pageSize: resourcePageSize
        }
      );

      console.log('[查询资源列表]', res);

      if (res.code === 200) {
        const newList = res.data.list || [];
        this.setData({
          resourceList: loadMore ? [...this.data.resourceList, ...newList] : newList,
          resourceTotal: res.data.total || 0,
          resourceHasMore: newList.length === resourcePageSize,
          resourceLoading: false
        });
      } else {
        this.setData({ resourceLoading: false });
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
      }
    } catch (error) {
      console.error('[加载资源列表失败]', error);
      this.setData({ resourceLoading: false });
      this.showCustomToast('加载失败', 'danger');
    }
  },

  loadMoreResources() {
    if (!this.data.resourceHasMore || this.data.resourceLoading) return;
    
    this.setData({ resourcePage: this.data.resourcePage + 1 });
    this.loadResources(true);
  },

  // 打开修改弹窗
  openEditModal(e) {
    const { field } = e.currentTarget.dataset;
    const { templateData } = this.data;
    
    // 解析取消规则为对象数组
    const cancelRuleParsed = (templateData.cancelRule || [])
      .map(rule => this.parseRuleToObject(rule))
      .filter(obj => obj !== null);
    
    const editData = {
      reservationTimeList: [...(templateData.reservationTimeList || [])],
      cancelRule: [...(templateData.cancelRule || [])],
      cancelRuleParsed,
      advanceDays: templateData.advanceDays || 7,
      weekDays: this.extractWeekDays(templateData.forbiddenDays || []),
      specificDates: this.extractSpecificDates(templateData.forbiddenDays || []),
      unitTime: 60,
      restTime: 0,
      startTime: '08:00',
      endTime: '22:00'
    };

    this.setData({
      showEditModal: true,
      editTab: field,
      editData
    });
  },

  extractWeekDays(forbiddenDays) {
    const weekDays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];
    return forbiddenDays.filter(day => weekDays.includes(day));
  },

  extractSpecificDates(forbiddenDays) {
    const weekDays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];
    return forbiddenDays.filter(day => !weekDays.includes(day));
  },

  closeEditModal() {
    this.setData({ showEditModal: false });
  },

  switchEditTab(e) {
    this.setData({ editTab: e.currentTarget.dataset.tab });
  },

  // ========== 时间段编辑 ==========
  onEditUnitTimeInput(e) {
    this.setData({ 'editData.unitTime': parseInt(e.detail.value) || 60 });
  },

  onEditRestTimeInput(e) {
    this.setData({ 'editData.restTime': parseInt(e.detail.value) || 0 });
  },

  onEditStartTimeChange(e) {
    this.setData({ 'editData.startTime': e.detail.value });
  },

  onEditEndTimeChange(e) {
    this.setData({ 'editData.endTime': e.detail.value });
  },

  generateEditTimeSlots() {
    let { unitTime, restTime, startTime, endTime } = this.data.editData;
    
    if (unitTime < 30) unitTime = 30;
    if (unitTime > 240) unitTime = 240;
    unitTime = Math.round(unitTime / 5) * 5;
    
    if (restTime < 0) restTime = 0;
    if (restTime > 60) restTime = 60;
    restTime = Math.round(restTime / 5) * 5;
    
    const [startHour, startMin] = startTime.split(':').map(Number);
    const [endHour, endMin] = endTime.split(':').map(Number);
    const startMinutes = startHour * 60 + startMin;
    const endMinutes = endHour * 60 + endMin;
    
    if (startMinutes >= endMinutes) {
      this.showCustomToast('结束时间必须大于开始时间', 'danger');
      return;
    }
    
    const timeList = [];
    let currentStart = startMinutes;
    
    while (currentStart + unitTime <= endMinutes) {
      const currentEnd = currentStart + unitTime;
      const startStr = this.minutesToTime(currentStart);
      const endStr = this.minutesToTime(currentEnd);
      timeList.push(`${startStr}-${endStr}`);
      currentStart = currentEnd + restTime;
    }
    
    if (timeList.length === 0) {
      this.showCustomToast('无法生成时间段', 'danger');
      return;
    }
    
    this.setData({
      'editData.reservationTimeList': timeList,
      'editData.unitTime': unitTime,
      'editData.restTime': restTime
    });
    this.showCustomToast(`已生成${timeList.length}个时间段`, 'success');
  },

  minutesToTime(minutes) {
    const hour = Math.floor(minutes / 60);
    const min = minutes % 60;
    return `${String(hour).padStart(2, '0')}:${String(min).padStart(2, '0')}`;
  },

  modifyEditTimeSlot(e) {
    const { index } = e.currentTarget.dataset;
    const timeList = this.data.editData.reservationTimeList;
    const currentSlot = timeList[index];
    const [startTime] = currentSlot.split('-');
    
    wx.showModal({
      title: '修改开始时间',
      editable: true,
      placeholderText: `当前: ${startTime}，输入新时间(HH:MM)`,
      success: (res) => {
        if (res.confirm && res.content) {
          const newStartTime = res.content.trim();
          if (!/^\d{2}:\d{2}$/.test(newStartTime)) {
            this.showCustomToast('格式错误，请输入HH:MM', 'danger');
            return;
          }
          
          const [oldH, oldM] = startTime.split(':').map(Number);
          const [newH, newM] = newStartTime.split(':').map(Number);
          const oldMinutes = oldH * 60 + oldM;
          const newMinutes = newH * 60 + newM;
          const delay = newMinutes - oldMinutes;
          
          if (delay < 0) {
            this.showCustomToast('只能推迟，不能提前', 'danger');
            return;
          }
          
          const [, endTime] = currentSlot.split('-');
          const [endH, endM] = endTime.split(':').map(Number);
          const unitTime = (endH * 60 + endM) - oldMinutes;
          
          const newTimeList = [];
          for (let i = 0; i < timeList.length; i++) {
            if (i < index) {
              newTimeList.push(timeList[i]);
            } else {
              const [s] = timeList[i].split('-');
              const [sH, sM] = s.split(':').map(Number);
              const newStart = sH * 60 + sM + delay;
              const newEnd = newStart + unitTime;
              
              if (newEnd > 24 * 60) break;
              
              newTimeList.push(`${this.minutesToTime(newStart)}-${this.minutesToTime(newEnd)}`);
            }
          }
          
          this.setData({ 'editData.reservationTimeList': newTimeList });
          this.showCustomToast('修改成功', 'success');
        }
      }
    });
  },

  deleteEditTimeSlot(e) {
    const { index } = e.currentTarget.dataset;
    const timeList = [...this.data.editData.reservationTimeList];
    timeList.splice(index, 1);
    this.setData({ 'editData.reservationTimeList': timeList });
  },

  // ========== 取消规则编辑 ==========
  addEditCancelRule() {
    const cancelRuleParsed = [...this.data.editData.cancelRuleParsed];
    cancelRuleParsed.push({
      minutes: 60,
      type: 'percent',
      value: 10
    });
    this.setData({ 'editData.cancelRuleParsed': cancelRuleParsed });
  },

  deleteEditCancelRule(e) {
    const { index } = e.currentTarget.dataset;
    const cancelRuleParsed = [...this.data.editData.cancelRuleParsed];
    cancelRuleParsed.splice(index, 1);
    this.setData({ 'editData.cancelRuleParsed': cancelRuleParsed });
  },

  // 规则输入变化
  onRuleInputChange(e) {
    const { index, field } = e.currentTarget.dataset;
    const value = e.detail.value;
    const key = `editData.cancelRuleParsed[${index}].${field}`;
    
    if (field === 'minutes') {
      this.setData({ [key]: parseInt(value) || 0 });
    } else if (field === 'value') {
      this.setData({ [key]: parseFloat(value) || 0 });
    }
  },

  // 切换规则类型（百分比/固定金额）
  toggleRuleType(e) {
    const { index } = e.currentTarget.dataset;
    const cancelRuleParsed = [...this.data.editData.cancelRuleParsed];
    const rule = cancelRuleParsed[index];
    
    if (rule.type === 'percent') {
      rule.type = 'fixed';
      rule.value = 10; // 默认10元
    } else {
      rule.type = 'percent';
      rule.value = 10; // 默认10%
    }
    
    this.setData({ 'editData.cancelRuleParsed': cancelRuleParsed });
  },

  // ========== 提前天数编辑 ==========
  onEditAdvanceDaysChange(e) {
    this.setData({ 'editData.advanceDays': parseInt(e.detail.value) + 1 });
  },

  // ========== 禁止日期编辑 ==========
  onEditWeekDayChange(e) {
    this.setData({ 'editData.weekDays': e.detail.value });
  },

  onAddSpecificDate(e) {
    const date = e.detail.value;
    if (!date) return;
    
    const specificDates = [...this.data.editData.specificDates];
    if (!specificDates.includes(date)) {
      specificDates.push(date);
      this.setData({ 'editData.specificDates': specificDates });
    }
  },

  deleteEditSpecificDate(e) {
    const { index } = e.currentTarget.dataset;
    const specificDates = [...this.data.editData.specificDates];
    specificDates.splice(index, 1);
    this.setData({ 'editData.specificDates': specificDates });
  },

  // ========== 提交修改 ==========
  async submitEdit() {
    const { editTab, editData, templateData, storeId } = this.data;
    
    try {
      const workToken = await tokenManager.getWorkToken(storeId);
      let requestBody = {};

      if (editTab === 'reservationTimeList') {
        if (editData.reservationTimeList.length === 0) {
          this.showCustomToast('时间段不能为空', 'danger');
          return;
        }
        requestBody.reservationTimeList = editData.reservationTimeList;
      } else if (editTab === 'cancelRule') {
        // 将解析后的规则转换回字符串
        const cancelRule = editData.cancelRuleParsed.map(rule => this.objectToRuleStr(rule));
        requestBody.cancelRule = cancelRule;
      } else if (editTab === 'advanceDays') {
        requestBody.advanceDays = editData.advanceDays;
      } else if (editTab === 'forbiddenDays') {
        requestBody.forbiddenDays = [...editData.weekDays, ...editData.specificDates];
      }

      const res = await request.put(
        `/v1/advanced-reservation/template/${templateData.reserveId}`,
        {
          'Authorization': workToken,
          'Content-Type': 'application/json'
        },
        requestBody
      );

      console.log('[修改模板]', res);

      if (res.code === 200) {
        this.showCustomToast('修改成功', 'success');
        this.closeEditModal();
        this.loadTemplate();
      } else {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
      }
    } catch (error) {
      console.error('[修改模板失败]', error);
      this.showCustomToast('修改失败', 'danger');
    }
  },

  // ========== 资源管理功能 ==========
  // 跳转到创建资源页面
  goToCreateResource() {
    wx.navigateTo({
      url: `/pages/adreserv-resource/adreserv-resource-create?storeId=${this.data.storeId}&storeName=${encodeURIComponent(this.data.storeName)}`
    });
  },

  // 跳转到编辑资源页面
  goToEditResource(e) {
    const resource = e.currentTarget.dataset.resource;
    wx.navigateTo({
      url: `/pages/adreserv-resource/adreserv-resource?storeId=${this.data.storeId}&storeName=${encodeURIComponent(this.data.storeName)}&resourceId=${resource.id}`
    });
  },

  // 显示资源图片
  showResourceImage(e) {
    const resource = e.currentTarget.dataset.resource;
    if (resource.resourceImg) {
      this.setData({
        showResourceImageModal: true,
        currentResourceImage: resource.resourceImg
      });
    }
  },

  // 隐藏资源图片弹窗
  hideResourceImageModal() {
    this.setData({
      showResourceImageModal: false,
      currentResourceImage: ''
    });
  },

  showCustomToast(message, type = 'success', animationType = 'slide', duration = 2000) {
    const toast = this.selectComponent('#customToast');
    if (toast) {
      toast.showToast(message, type, animationType, duration);
    }
  }
});
