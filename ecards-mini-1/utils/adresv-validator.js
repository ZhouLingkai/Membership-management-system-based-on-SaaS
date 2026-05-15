/**
 * 高级预约系统表单校验工具
 * 提供前端表单校验和计算功能，减少无效请求
 */

const AdresvValidator = {
  
  /**
   * 1. 校验 reservation_time_list
   * @param {Array<string>} timeList - 时间段数组，如 ["08:00-09:00", "09:00-10:00"]
   * @returns {string|null} - 错误信息，null表示校验通过
   */
  validateReservationTimeList(timeList) {
    if (!Array.isArray(timeList) || timeList.length === 0) {
      return '时间段列表不能为空';
    }

    // 校验每个时间段格式
    const timeRegex = /^([01]\d|2[0-3]):([0-5]\d)-([01]\d|2[0-4]):([0-5]\d)$/;
    for (let i = 0; i < timeList.length; i++) {
      const slot = timeList[i];
      if (!timeRegex.test(slot)) {
        return `时间段格式错误: ${slot}，正确格式为 HH:mm-HH:mm`;
      }

      const [start, end] = slot.split('-');
      const [startHour, startMin] = start.split(':').map(Number);
      const [endHour, endMin] = end.split(':').map(Number);
      
      const startMinutes = startHour * 60 + startMin;
      const endMinutes = endHour * 60 + endMin;
      
      if (startMinutes >= endMinutes) {
        return `时间段不合法: ${slot}，开始时间必须早于结束时间`;
      }
    }

    // 校验时间段从小到大排序
    for (let i = 0; i < timeList.length - 1; i++) {
      const current = timeList[i].split('-')[0];
      const next = timeList[i + 1].split('-')[0];
      if (current >= next) {
        return '时间段必须按从小到大排序';
      }
    }

    // 校验时间段不能交叉
    for (let i = 0; i < timeList.length - 1; i++) {
      const currentEnd = timeList[i].split('-')[1];
      const nextStart = timeList[i + 1].split('-')[0];
      if (currentEnd > nextStart) {
        return `时间段交叉: ${timeList[i]} 和 ${timeList[i + 1]}`;
      }
    }

    return null;
  },

  /**
   * 2. 校验 cancel_rule
   * @param {Array<string>} cancelRule - 取消规则数组，如 ["60:0.1", "180:5"]
   * @returns {string|null} - 错误信息，null表示校验通过
   */
  validateCancelRule(cancelRule) {
    if (!Array.isArray(cancelRule) || cancelRule.length === 0) {
      return '取消规则不能为空';
    }

    const ruleRegex = /^\d+:(0\.\d+|\d+(\.\d+)?)$/;
    for (const rule of cancelRule) {
      if (!ruleRegex.test(rule)) {
        return `取消规则格式错误: ${rule}，正确格式为 分钟:费用`;
      }

      const [minutes, fee] = rule.split(':');
      const feeNum = parseFloat(fee);
      
      if (feeNum < 0) {
        return `违约费不能为负数: ${rule}`;
      }
      
      if (feeNum > 0.5 && feeNum < 1) {
        return `违约费必须 ≤0.5（比例）或 ≥1（固定金额）: ${rule}`;
      }
    }

    return null;
  },

  /**
   * 2.1 计算取消预约扣除金额
   * @param {number} amount - 预约金额
   * @param {Array<string>} cancelRule - 取消规则，如 ["60:0.1", "180:5"]
   * @param {number} minutesToStart - 距离预约开始时间的分钟数
   * @returns {number} - 扣除金额（保留一位小数）
   */
  calculateCancelFee(amount, cancelRule, minutesToStart) {
    if (!Array.isArray(cancelRule) || cancelRule.length === 0) {
      return 0;
    }

    // 遍历规则，找到第一个满足条件的规则（与后端逻辑一致）
    for (const rule of cancelRule) {
      const [minutes, fee] = rule.split(':');
      const minutesNum = parseInt(minutes);
      const feeNum = parseFloat(fee);
      
      if (minutesToStart <= minutesNum) {
        // 找到适用规则
        let cancelFee = 0;
        
        if (feeNum <= 0.5) {
          // 比例扣费
          cancelFee = amount * feeNum;
        } else {
          // 固定金额扣费
          cancelFee = feeNum;
        }
        
        return Math.round(cancelFee * 10) / 10; // 保留一位小数
      }
    }

    return 0; // 没有适用规则，不扣费
  },

  /**
   * 3. 校验 forbidden_days
   * @param {Array<string>} forbiddenDays - 禁止日期数组，如 ["周六", "周日", "2025-11-16"]
   * @returns {string|null} - 错误信息，null表示校验通过
   */
  validateForbiddenDays(forbiddenDays) {
    if (!Array.isArray(forbiddenDays)) {
      return 'forbidden_days 必须是数组';
    }

    if (forbiddenDays.length === 0) {
      return null; // 空数组表示支持所有日期
    }

    const validWeeks = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];
    const dateRegex = /^\d{4}-\d{2}-\d{2}$/;

    for (const item of forbiddenDays) {
      if (!validWeeks.includes(item) && !dateRegex.test(item)) {
        return `禁止日期格式错误: ${item}，必须是"周X"或"YYYY-MM-DD"`;
      }
    }

    return null;
  },

  /**
   * 3.1 判断某日期是否可预约（单个 forbidden_days 列表）
   * @param {Array<string>} forbiddenDays - 禁止日期数组
   * @param {string} date - 日期，格式 YYYY-MM-DD
   * @returns {boolean} - true表示可预约，false表示不可预约
   */
  isDateReservable(forbiddenDays, date) {
    if (!Array.isArray(forbiddenDays) || forbiddenDays.length === 0) {
      return true; // 空数组表示支持所有日期
    }

    // 计算日期是星期几
    const weekDay = this._getWeekDay(date);
    
    let flag = 0;
    
    // 检查星期是否在禁止列表中
    if (forbiddenDays.includes(weekDay)) {
      flag++;
    }
    
    // 检查具体日期是否在禁止列表中
    if (forbiddenDays.includes(date)) {
      flag++;
    }

    // flag=0 可预约，flag=1 不可预约，flag=2 可预约（负负得正）
    return flag !== 1;
  },

  /**
   * 3.2 判断某日期是否可预约（同时考虑模板和资源的 forbidden_days）
   * @param {Array<string>} templateForbiddenDays - 模板禁止日期
   * @param {Array<string>} resourceForbiddenDays - 资源禁止日期
   * @param {string} date - 日期，格式 YYYY-MM-DD
   * @param {number} customizeForbidden - 是否启用资源自定义禁止日期，0-否，1-是
   * @returns {boolean} - true表示可预约，false表示不可预约
   */
  isDateReservableWithCustomize(templateForbiddenDays, resourceForbiddenDays, date, customizeForbidden) {
    // 模板的 forbidden_days 必须通过
    const templateReservable = this.isDateReservable(templateForbiddenDays, date);
    
    if (customizeForbidden === 0) {
      // 不启用资源自定义，只看模板
      return templateReservable;
    }
    
    // 启用资源自定义，模板和资源都必须可预约
    const resourceReservable = this.isDateReservable(resourceForbiddenDays, date);
    return templateReservable && resourceReservable;
  },

  /**
   * 4. 计算连续预约时间（分钟）
   * @param {Array<string>} timeSlots - 时间段数组，如 ["08:00-08:50", "09:00-09:50"]
   * @returns {number} - 总时长（分钟）
   */
  calculateTotalDuration(timeSlots) {
    if (!Array.isArray(timeSlots) || timeSlots.length === 0) {
      return 0;
    }

    let totalMinutes = 0;

    for (const slot of timeSlots) {
      const [start, end] = slot.split('-');
      const [startHour, startMin] = start.split(':').map(Number);
      const [endHour, endMin] = end.split(':').map(Number);
      
      const startMinutes = startHour * 60 + startMin;
      const endMinutes = endHour * 60 + endMin;
      
      totalMinutes += (endMinutes - startMinutes);
    }

    return totalMinutes;
  },

  /**
   * 5. 校验 promotion_strategy
   * @param {Object} promotionStrategy - 优惠策略对象
   * @returns {string|null} - 错误信息，null表示校验通过
   */
  validatePromotionStrategy(promotionStrategy) {
    if (!promotionStrategy || typeof promotionStrategy !== 'object') {
      return null; // null 或空对象视为无优惠策略
    }

    // 校验 non_effective_dates
    if (promotionStrategy.non_effective_dates) {
      if (!Array.isArray(promotionStrategy.non_effective_dates)) {
        return 'non_effective_dates 必须是数组';
      }
      
      const dateRegex = /^\d{4}-\d{2}-\d{2}$/;
      for (const date of promotionStrategy.non_effective_dates) {
        if (!dateRegex.test(date)) {
          return `不生效日期格式错误: ${date}`;
        }
      }
    }

    // 校验 week
    if (promotionStrategy.week) {
      if (!Array.isArray(promotionStrategy.week)) {
        return 'week 必须是数组';
      }

      const validWeeks = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];
      const usedWeeks = new Set();

      for (const weekItem of promotionStrategy.week) {
        // 校验 effective_week
        if (!weekItem.effective_week || !validWeeks.includes(weekItem.effective_week)) {
          return `effective_week 必须是"周一"到"周日": ${weekItem.effective_week}`;
        }

        // 检查 effective_week 是否重复
        if (usedWeeks.has(weekItem.effective_week)) {
          return `effective_week 不能重复: ${weekItem.effective_week}`;
        }
        usedWeeks.add(weekItem.effective_week);

        // 校验 discount_time
        if (!Array.isArray(weekItem.discount_time)) {
          return 'discount_time 必须是数组';
        }

        const timeSlots = [];
        for (const discountItem of weekItem.discount_time) {
          // 校验 time_slot 格式
          const timeSlot = discountItem.time_slot;
          const timeRegex = /^([01]\d|2[0-3]):([0-5]\d)-([01]\d|2[0-4]):([0-5]\d)$/;
          if (!timeRegex.test(timeSlot)) {
            return `time_slot 格式错误: ${timeSlot}`;
          }

          timeSlots.push(timeSlot);

          // 校验 discount 格式
          const discount = discountItem.discount;
          if (!this._isValidDiscountRule(discount)) {
            return `discount 格式错误: ${discount}`;
          }
        }

        // 校验时间段不重复、不交叉
        const overlapError = this._checkTimeSlotOverlap(timeSlots);
        if (overlapError) {
          return overlapError;
        }
      }
    }

    return null;
  },

  /**
   * 5.1 计算优惠后的预约金额
   * @param {Object} promotionStrategy - 优惠策略
   * @param {string} reservationDate - 预约日期，格式 YYYY-MM-DD
   * @param {number} unitPrice - 最小时间段单价
   * @param {Array<string>} timeSlots - 预约时间段，如 ["08:00-08:50", "09:00-09:50"]
   * @returns {number} - 预计消费金额（保留一位小数）
   */
  calculateDiscountedPrice(promotionStrategy, reservationDate, unitPrice, timeSlots) {
    // 如果没有优惠策略，按原价计算
    if (!promotionStrategy || !promotionStrategy.week || promotionStrategy.week.length === 0) {
      return Math.round(unitPrice * timeSlots.length * 10) / 10;
    }

    // 检查预约日期是否在不生效日期列表中
    if (promotionStrategy.non_effective_dates && 
        promotionStrategy.non_effective_dates.includes(reservationDate)) {
      return Math.round(unitPrice * timeSlots.length * 10) / 10;
    }

    // 计算预约日期是星期几
    const weekDay = this._getWeekDay(reservationDate);

    // 查找对应星期的优惠策略
    let discountsTime = null;
    for (const weekItem of promotionStrategy.week) {
      if (weekItem.effective_week === weekDay) {
        discountsTime = weekItem.discount_time;
        break;
      }
    }

    // 如果没有找到对应星期的优惠，按原价计算
    if (!discountsTime || discountsTime.length === 0) {
      return Math.round(unitPrice * timeSlots.length * 10) / 10;
    }

    // 计算原价总额（用于满减判断）
    const originalTotal = unitPrice * timeSlots.length;

    // 遍历每个时间段，应用优惠
    let totalPrice = 0;
    for (const timeSlot of timeSlots) {
      const slotPrice = this._applyDiscount(timeSlot, discountsTime, unitPrice, originalTotal);
      totalPrice += slotPrice;
    }

    return Math.round(totalPrice * 10) / 10; // 保留一位小数
  },

  /**
   * 6. 校验日期格式
   * @param {string} date - 日期字符串
   * @returns {string|null} - 错误信息，null表示校验通过
   */
  validateDateFormat(date) {
    const dateRegex = /^\d{4}-\d{2}-\d{2}$/;
    if (!dateRegex.test(date)) {
      return '日期格式错误，正确格式为 YYYY-MM-DD';
    }

    // 校验日期是否合法
    const [year, month, day] = date.split('-').map(Number);
    const dateObj = new Date(year, month - 1, day);
    
    if (dateObj.getFullYear() !== year || 
        dateObj.getMonth() !== month - 1 || 
        dateObj.getDate() !== day) {
      return '日期不合法';
    }

    return null;
  },

  /**
   * 7. 校验时间段格式
   * @param {string} timeSlot - 时间段，如 "08:00-09:00"
   * @returns {string|null} - 错误信息，null表示校验通过
   */
  validateTimeSlotFormat(timeSlot) {
    const timeRegex = /^([01]\d|2[0-3]):([0-5]\d)-([01]\d|2[0-4]):([0-5]\d)$/;
    if (!timeRegex.test(timeSlot)) {
      return '时间段格式错误，正确格式为 HH:mm-HH:mm';
    }

    const [start, end] = timeSlot.split('-');
    const [startHour, startMin] = start.split(':').map(Number);
    const [endHour, endMin] = end.split(':').map(Number);
    
    const startMinutes = startHour * 60 + startMin;
    const endMinutes = endHour * 60 + endMin;
    
    if (startMinutes >= endMinutes) {
      return '开始时间必须早于结束时间';
    }

    return null;
  },

  /**
   * 8. 校验连续预约时间范围
   * @param {Array<string>} timeSlots - 时间段数组
   * @param {number} minTime - 最少连续时间（分钟）
   * @param {number} maxTime - 最大连续时间（分钟）
   * @returns {string|null} - 错误信息，null表示校验通过
   */
  validateContinuousTimeRange(timeSlots, minTime, maxTime) {
    const duration = this.calculateTotalDuration(timeSlots);
    
    if (duration < minTime) {
      return `预约时长不足，最少需要 ${minTime} 分钟，当前 ${duration} 分钟`;
    }
    
    if (duration > maxTime) {
      return `预约时长超限，最多允许 ${maxTime} 分钟，当前 ${duration} 分钟`;
    }

    return null;
  },

  /**
   * 9. 校验提前预约天数
   * @param {string} reservationDate - 预约日期，格式 YYYY-MM-DD
   * @param {number} advanceDays - 可提前预约天数
   * @returns {string|null} - 错误信息，null表示校验通过
   */
  validateAdvanceDays(reservationDate, advanceDays) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    const [year, month, day] = reservationDate.split('-').map(Number);
    const targetDate = new Date(year, month - 1, day);
    
    const diffTime = targetDate - today;
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    
    if (diffDays < 0) {
      return '不能预约过去的日期';
    }
    
    if (diffDays > advanceDays) {
      return `最多只能提前 ${advanceDays} 天预约`;
    }

    return null;
  },

  // ==================== 私有辅助方法 ====================

  /**
   * 获取日期是星期几
   * @private
   */
  _getWeekDay(dateStr) {
    const [year, month, day] = dateStr.split('-').map(Number);
    const date = new Date(year, month - 1, day);
    const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
    return weekDays[date.getDay()];
  },

  /**
   * 校验优惠规则格式
   * @private
   */
  _isValidDiscountRule(discount) {
    if (typeof discount !== 'string' || discount.length < 2) {
      return false;
    }

    const firstChar = discount.charAt(0);
    const value = discount.substring(1);

    if (firstChar === '*') {
      // 打折：*0.9
      const num = parseFloat(value);
      return !isNaN(num) && num > 0 && num <= 1;
    } else if (firstChar === '-') {
      // 减价：-10（必须大于0，不能等于0）
      const num = parseFloat(value);
      return !isNaN(num) && num > 0;
    } else if (firstChar === ':') {
      // 满减打折：:100*0.9
      const match = value.match(/^(\d+(?:\.\d+)?)\*(\d+(?:\.\d+)?)$/);
      if (!match) return false;
      const threshold = parseFloat(match[1]);
      const ratio = parseFloat(match[2]);
      return threshold > 0 && ratio > 0 && ratio <= 1;
    } else if (firstChar === '^') {
      // 满减：^100-10
      const match = value.match(/^(\d+(?:\.\d+)?)-(\d+(?:\.\d+)?)$/);
      if (!match) return false;
      const threshold = parseFloat(match[1]);
      const amount = parseFloat(match[2]);
      return threshold > 0 && amount > 0;
    }

    return false;
  },

  /**
   * 检查时间段是否重叠或交叉
   * @private
   */
  _checkTimeSlotOverlap(timeSlots) {
    if (timeSlots.length <= 1) {
      return null;
    }

    // 转换为分钟数进行比较
    const ranges = timeSlots.map(slot => {
      const [start, end] = slot.split('-');
      const [startHour, startMin] = start.split(':').map(Number);
      const [endHour, endMin] = end.split(':').map(Number);
      return {
        start: startHour * 60 + startMin,
        end: endHour * 60 + endMin,
        original: slot
      };
    });

    // 检查是否有重叠
    for (let i = 0; i < ranges.length; i++) {
      for (let j = i + 1; j < ranges.length; j++) {
        const a = ranges[i];
        const b = ranges[j];
        
        // 检查重叠：a的结束时间 > b的开始时间 且 a的开始时间 < b的结束时间
        if (a.end > b.start && a.start < b.end) {
          return `时间段重叠或交叉: ${a.original} 和 ${b.original}`;
        }
      }
    }

    return null;
  },

  /**
   * 对单个时间段应用优惠
   * @private
   */
  _applyDiscount(timeSlot, discountsTime, unitPrice, originalTotal) {
    // 查找该时间段是否完全在某个优惠时间段内
    for (const discountItem of discountsTime) {
      if (this._isTimeSlotInRange(timeSlot, discountItem.time_slot)) {
        // 应用优惠规则
        return this._applyDiscountRule(discountItem.discount, unitPrice, originalTotal);
      }
    }

    // 没有优惠，返回原价
    return unitPrice;
  },

  /**
   * 判断时间段是否完全在优惠时间段内
   * @private
   */
  _isTimeSlotInRange(timeSlot, discountTimeSlot) {
    const [slotStart, slotEnd] = timeSlot.split('-');
    const [discountStart, discountEnd] = discountTimeSlot.split('-');

    return slotStart >= discountStart && slotEnd <= discountEnd;
  },

  /**
   * 应用具体的优惠规则
   * @private
   */
  _applyDiscountRule(discount, unitPrice, originalTotal) {
    const firstChar = discount.charAt(0);
    const value = discount.substring(1);

    let discountedPrice = unitPrice;

    if (firstChar === '*') {
      // 打折
      const ratio = parseFloat(value);
      discountedPrice = unitPrice * ratio;
    } else if (firstChar === '-') {
      // 减价
      const amount = parseFloat(value);
      discountedPrice = unitPrice - amount;
    } else if (firstChar === ':') {
      // 满减打折
      const match = value.match(/^(\d+(?:\.\d+)?)\*(\d+(?:\.\d+)?)$/);
      const threshold = parseFloat(match[1]);
      const ratio = parseFloat(match[2]);
      
      if (originalTotal >= threshold) {
        discountedPrice = unitPrice * ratio;
      }
    } else if (firstChar === '^') {
      // 满减
      const match = value.match(/^(\d+(?:\.\d+)?)-(\d+(?:\.\d+)?)$/);
      const threshold = parseFloat(match[1]);
      const amount = parseFloat(match[2]);
      
      if (originalTotal >= threshold) {
        discountedPrice = unitPrice - amount;
      }
    }

    // 确保价格不为负数，保留一位小数
    return Math.max(0, Math.round(discountedPrice * 10) / 10);
  }
};

// 导出模块
module.exports = AdresvValidator;
