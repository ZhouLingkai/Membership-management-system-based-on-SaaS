// pages/advanced-reservation-create/advanced-reservation-create.js
const { request } = require('../../utils/request');
const tokenManager = require('../../utils/token');

Page({
  data: {
    storeId: '',
    storeName: '',
    
    // 步骤1: 可预约时间段
    unitTime: 60, // 单位时间（分钟）
    restTime: 0, // 休息时长（分钟）
    startTime: '08:00',
    endTime: '22:00',
    generatedTimeList: [], // 生成的时间段列表
    
    // 步骤2: 取消规则
    cancelRules: [], // [{minutes: 60, type: 'percent', value: 10}]
    
    // 步骤3: 其他配置
    advanceDays: 7,
    selectedWeekDays: [], // 选中的周几
    weekDayOptions: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
    customizeForbidden: 1,
    
    submitting: false
  },

  onLoad(options) {
    const { storeId, storeName } = options;
    this.setData({
      storeId: storeId || '',
      storeName: decodeURIComponent(storeName || '未知店铺')
    });
  },

  // ========== 步骤1: 时间段生成 ==========
  
  // 单位时间输入
  onUnitTimeBlur(e) {
    let value = parseInt(e.detail.value) || 60;
    
    // 校验
    if (value < 30) value = 30;
    if (value > 240) value = 240;
    
    // 调整为5的倍数
    const remainder = value % 5;
    if (remainder !== 0) {
      value = remainder < 2.5 ? value - remainder : value + (5 - remainder);
    }
    
    this.setData({ unitTime: value });
  },

  // 休息时长输入
  onRestTimeBlur(e) {
    let value = parseInt(e.detail.value) || 0;
    
    // 校验
    if (value < 0) value = 0;
    if (value > 60) value = 60;
    
    // 调整为5的倍数
    const remainder = value % 5;
    if (remainder !== 0) {
      value = remainder < 2.5 ? value - remainder : value + (5 - remainder);
    }
    
    this.setData({ restTime: value });
  },

  // 开始时间选择
  onStartTimeChange(e) {
    this.setData({ startTime: e.detail.value });
  },

  // 结束时间选择
  onEndTimeChange(e) {
    this.setData({ endTime: e.detail.value });
  },

  // 一键生成时间段
  generateTimeSlots() {
    const { unitTime, restTime, startTime, endTime } = this.data;
    
    // 转换为分钟
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
      
      timeList.push({
        original: `${startStr}-${endStr}`,
        start: startStr,
        end: endStr,
        startMinutes: currentStart,
        endMinutes: currentEnd
      });
      
      currentStart = currentEnd + restTime;
    }
    
    if (timeList.length === 0) {
      this.showCustomToast('无法生成时间段，请调整参数', 'danger');
      return;
    }
    
    this.setData({ generatedTimeList: timeList });
    this.showCustomToast(`已生成${timeList.length}个时间段`, 'success');
  },

  // 分钟转时间字符串
  minutesToTime(minutes) {
    const hour = Math.floor(minutes / 60);
    const min = minutes % 60;
    return `${String(hour).padStart(2, '0')}:${String(min).padStart(2, '0')}`;
  },

  // 删除时间段
  deleteTimeSlot(e) {
    const { index } = e.currentTarget.dataset;
    const timeList = this.data.generatedTimeList;
    timeList.splice(index, 1);
    this.setData({ generatedTimeList: timeList });
  },

  // 修改时间段开始时间
  modifyTimeSlot(e) {
    const { index } = e.currentTarget.dataset;
    const timeList = this.data.generatedTimeList;
    const { unitTime, endTime } = this.data;
    
    wx.showModal({
      title: '修改开始时间',
      editable: true,
      placeholderText: '请输入新的开始时间(HH:MM)',
      success: (res) => {
        if (res.confirm && res.content) {
          const newStartTime = res.content.trim();
          
          // 验证格式
          if (!/^\d{2}:\d{2}$/.test(newStartTime)) {
            this.showCustomToast('时间格式错误，请输入HH:MM格式', 'danger');
            return;
          }
          
          const [hour, min] = newStartTime.split(':').map(Number);
          if (hour < 0 || hour > 23 || min < 0 || min > 59) {
            this.showCustomToast('时间范围错误', 'danger');
            return;
          }
          
          const newStartMinutes = hour * 60 + min;
          const oldStartMinutes = timeList[index].startMinutes;
          const delay = newStartMinutes - oldStartMinutes;
          
          // 只能推迟，不能提前
          if (delay < 0) {
            this.showCustomToast('只能推迟开始时间，不能提前', 'danger');
            return;
          }
          
          // 更新后续所有时间段
          const [endHour, endMin] = endTime.split(':').map(Number);
          const endMinutes = endHour * 60 + endMin;
          
          const newTimeList = [];
          for (let i = 0; i < timeList.length; i++) {
            if (i < index) {
              newTimeList.push(timeList[i]);
            } else {
              const newStart = timeList[i].startMinutes + delay;
              const newEnd = newStart + unitTime;
              
              // 超出营业时间则停止
              if (newEnd > endMinutes) {
                break;
              }
              
              newTimeList.push({
                original: `${this.minutesToTime(newStart)}-${this.minutesToTime(newEnd)}`,
                start: this.minutesToTime(newStart),
                end: this.minutesToTime(newEnd),
                startMinutes: newStart,
                endMinutes: newEnd
              });
            }
          }
          
          this.setData({ generatedTimeList: newTimeList });
          this.showCustomToast('修改成功', 'success');
        }
      }
    });
  },

  // ========== 步骤2: 取消规则 ==========
  
  // 添加取消规则
  addCancelRule() {
    wx.showActionSheet({
      itemList: ['百分比扣除', '固定金额扣除'],
      success: (res) => {
        const type = res.tapIndex === 0 ? 'percent' : 'fixed';
        
        wx.showModal({
          title: '设置取消规则',
          editable: true,
          placeholderText: '格式: 分钟数,扣除值 (如: 60,10)',
          success: (modalRes) => {
            if (modalRes.confirm && modalRes.content) {
              const parts = modalRes.content.split(',');
              if (parts.length !== 2) {
                this.showCustomToast('格式错误', 'danger');
                return;
              }
              
              const minutes = parseInt(parts[0]);
              const value = parseFloat(parts[1]);
              
              if (isNaN(minutes) || isNaN(value) || minutes <= 0) {
                this.showCustomToast('参数错误', 'danger');
                return;
              }
              
              if (type === 'percent' && (value < 1 || value > 50)) {
                this.showCustomToast('百分比范围1-50', 'danger');
                return;
              }
              
              const cancelRules = this.data.cancelRules;
              cancelRules.push({ minutes, type, value });
              this.setData({ cancelRules });
            }
          }
        });
      }
    });
  },

  // 删除取消规则
  deleteCancelRule(e) {
    const { index } = e.currentTarget.dataset;
    const cancelRules = this.data.cancelRules;
    cancelRules.splice(index, 1);
    this.setData({ cancelRules });
  },

  // ========== 步骤3: 其他配置 ==========
  
  // 提前天数选择
  onAdvanceDaysChange(e) {
    this.setData({ advanceDays: parseInt(e.detail.value) });
  },

  // 周几选择
  onWeekDayChange(e) {
    this.setData({ selectedWeekDays: e.detail.value });
  },

  // 自定义禁止日期开关
  onCustomizeForbiddenChange(e) {
    this.setData({ customizeForbidden: e.detail.value ? 1 : 0 });
  },

  // ========== 提交 ==========
  
  async submitForm() {
    const { storeId, generatedTimeList, cancelRules, advanceDays, selectedWeekDays, customizeForbidden } = this.data;
    
    // 校验
    if (generatedTimeList.length === 0) {
      this.showCustomToast('请先生成可预约时间段', 'danger');
      return;
    }
    
    if (cancelRules.length === 0) {
      this.showCustomToast('请至少添加一条取消规则', 'danger');
      return;
    }
    
    this.setData({ submitting: true });
    
    try {
      const workToken = await tokenManager.getWorkToken(storeId);
      
      // 构建请求体
      const reservationTimeList = generatedTimeList.map(item => item.original);
      const cancelRule = cancelRules.map(rule => {
        if (rule.type === 'percent') {
          return `${rule.minutes}:${rule.value / 100}`;
        } else {
          return `${rule.minutes}:${rule.value}`;
        }
      });
      
      const requestBody = {
        storeId,
        reservationTimeList,
        cancelRule,
        advanceDays,
        forbiddenDays: selectedWeekDays,
        customizeForbidden
      };
      
      console.log('[创建预约模板]请求体:', requestBody);
      
      const res = await request.post(
        '/v1/advanced-reservation/template',
        {
          'Authorization': workToken,
          'Content-Type': 'application/json'
        },
        requestBody
      );
      
      console.log('[创建预约模板]响应:', res);
      
      if (res.code === 200) {
        this.showCustomToast('创建成功', 'success');
        setTimeout(() => {
          wx.navigateBack();
        }, 1500);
      } else {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
      }
    } catch (error) {
      console.error('[创建模板失败]', error);
      this.showCustomToast('创建失败，请稍后重试', 'danger');
    } finally {
      this.setData({ submitting: false });
    }
  },

  showCustomToast(message, type = 'success', animationType = 'slide', duration = 2000) {
    const toast = this.selectComponent('#customToast');
    if (toast) {
      toast.showToast(message, type, animationType, duration);
    }
  }
});
