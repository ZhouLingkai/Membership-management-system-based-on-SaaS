// pages/transactions-statistics/transactions-statistics.js
const { post } = require('../../utils/request');
const tokenManager = require('../../utils/token');

Page({
  data: {
    // 页面参数
    queryType: 'store', // 'store' or 'merchant'
    storeId: '',
    storeName: '',
    merchantId: '',
    merchantName: '',
    
    // 快捷切换选项
    quickOptions: [
      { label: '近7日', value: 7, active: true },
      { label: '近30日', value: 30, active: false },
      { label: '本周', value: 17, active: false },
      { label: '本月', value: 32, active: false }
    ],
    selectedQuickOption: 7,
    
    // 某日数据快捷切换
    dayOptions: [
      { label: '昨日', value: 'yesterday', active: false },
      { label: '今日', value: 'today', active: true },
      { label: '自定义', value: 'custom', active: false }
    ],
    selectedDayOption: 'today',
    customDateLabel: '', // 自选日期显示标签
    
    // 统计数据
    statisticsData: null, // 原始数据数组
    currentDayData: null, // 当前选中日期的数据
    summaryData: null, // 汇总数据
    
    // 可选日期列表（从统计数据中提取）
    availableDates: [],
    showDatePicker: false,
    
    // 指标趋势
    trendOptions: [],
    selectedTrendIndex: 0,
    
    // Canvas相关
    canvasWidth: 0,
    canvasHeight: 180,
    chartData: [],
    selectedPoint: null, // {date, value, x, y}
    
    // 加载状态
    loading: false,
    refreshing: false
  },

  onLoad(options) {
    const {
      queryType = 'store',
      storeId = '',
      storeName = '',
      merchantId = '',
      merchantName = ''
    } = options;

    // 计算Canvas宽度（屏幕宽度的94%）
    const systemInfo = wx.getSystemInfoSync();
    const canvasWidth = systemInfo.windowWidth * 0.94;

    this.setData({
      queryType,
      storeId,
      storeName: decodeURIComponent(storeName || ''),
      merchantId,
      merchantName: decodeURIComponent(merchantName || ''),
      canvasWidth
    });
  },

  onShow() {
    // 加载数据
    this.loadStatistics();
  },

  // 加载统计数据
  async loadStatistics(isRefresh = false) {
    if (this.data.loading) return;

    this.setData({ 
      loading: true,
      refreshing: isRefresh
    });

    try {
      const { queryType, storeId, merchantId, selectedQuickOption } = this.data;
      
      // 获取工作令牌
      const token = await tokenManager.getWorkToken(storeId || merchantId);
      if (!token) {
        this.showCustomToast('获取令牌失败', 'danger');
        this.setData({ loading: false, refreshing: false });
        return;
      }


      // 构建请求参数
      const params = {
        dateRange: selectedQuickOption
      };

      if (queryType === 'store') {
        params.storeId = storeId;
      } else {
        params.merchantId = merchantId;
      }

      const res = await post(
        '/v1/transactions/trans-statistics',
        { 'Authorization': token },
        params
      );

      console.log('[流水数据统计]', res);

      if (res.code === 200 && res.data) {
        this.processStatisticsData(res.data);
      } else {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
      }
    } catch (error) {
      console.error('[加载统计数据失败]', error);
      this.showCustomToast('加载失败，请重试', 'danger');
    } finally {
      this.setData({ loading: false, refreshing: false });
      if (isRefresh) {
        wx.stopPullDownRefresh();
      }
    }
  },

  // 处理统计数据
  processStatisticsData(data) {
    const { queryType, selectedQuickOption } = this.data;
    
    // 获取统计数据数组
    let statistics = queryType === 'store' 
      ? (data.storeStatistics || [])
      : (data.merchantStatistics || []);

    if (statistics.length === 0) {
      this.showCustomToast('暂无数据', 'warning');
      return;
    }


    // 前端补充零数据（后端已补充今天和昨天，前端补充其他缺失日期）
    statistics = this.fillMissingDates(statistics, selectedQuickOption);

    // 提取可选日期
    const availableDates = statistics.map(item => item.date);

    // 获取今日数据（第一条是最新的）
    const todayData = statistics[0];
    
    // 计算汇总数据
    const summaryData = this.calculateSummary(statistics);

    // 构建趋势选项
    const trendOptions = this.buildTrendOptions(todayData);

    this.setData({
      statisticsData: statistics,
      currentDayData: todayData,
      summaryData,
      availableDates,
      trendOptions,
      selectedTrendIndex: 0
    });

    // 绘制图表
    this.drawChart();
  },

  // 前端补充缺失日期的零数据
  fillMissingDates(statistics, dateRange) {
    if (statistics.length === 0) return statistics;

    // 以第一条数据（今天）为基准
    const todayStr = statistics[0].date;
    const today = new Date(todayStr);
    
    // 检查全部数据，确定全局标记
    let haveBalanceCard = false;
    let haveCountCard = false;
    statistics.forEach(item => {
      if (item.haveBalanceCard) haveBalanceCard = true;
      if (item.haveCountCard) haveCountCard = true;
    });


    // 创建零数据对象模板
    const zeroBalanceData = {
      consumeAmount: 0,
      consumeCount: 0,
      rechargeAmount: 0,
      rechargeCount: 0,
      refundAmount: 0,
      refundCount: 0
    };
    
    const zeroCountData = {
      consumeTimes: 0,
      rechargeTimes: 0
    };

    // 统一所有数据的haveBalanceCard和haveCountCard标记
    statistics = statistics.map(item => ({
      ...item,
      haveBalanceCard,
      haveCountCard,
      balanceCardData: haveBalanceCard ? (item.balanceCardData || zeroBalanceData) : null,
      countCardData: haveCountCard ? (item.countCardData || zeroCountData) : null
    }));

    // 创建零数据模板
    const createZeroData = (dateStr) => ({
      date: dateStr,
      haveBalanceCard,
      haveCountCard,
      balanceCardData: haveBalanceCard ? {...zeroBalanceData} : null,
      countCardData: haveCountCard ? {...zeroCountData} : null,
      memberData: {
        newMembers: 0,
        newMemberCards: 0
      }
    });

    // 计算需要的日期范围
    let startDate, endDate;
    if (dateRange === 7) {
      // 近7日
      startDate = new Date(today);
      startDate.setDate(today.getDate() - 6);
      endDate = today;
    } else if (dateRange === 30) {
      // 近30日
      startDate = new Date(today);
      startDate.setDate(today.getDate() - 29);
      endDate = today;
    } else if (dateRange === 17) {
      // 本周（周一到今天）
      const dayOfWeek = today.getDay();
      const diff = dayOfWeek === 0 ? 6 : dayOfWeek - 1; // 周日特殊处理
      startDate = new Date(today);
      startDate.setDate(today.getDate() - diff);
      endDate = today;
    } else if (dateRange === 32) {
      // 本月（1号到今天）
      startDate = new Date(today.getFullYear(), today.getMonth(), 1);
      endDate = today;
    } else {
      return statistics;
    }


    // 生成完整日期列表
    const allDates = [];
    const current = new Date(startDate);
    while (current <= endDate) {
      allDates.push(this.formatDate(current));
      current.setDate(current.getDate() + 1);
    }

    // 创建日期映射
    const dataMap = {};
    statistics.forEach(item => {
      dataMap[item.date] = item;
    });

    // 补充缺失日期
    const result = allDates.map(date => {
      return dataMap[date] || createZeroData(date);
    });

    // 降序排列（最新的在前）
    return result.reverse();
  },

  // 格式化日期为 YYYY-MM-DD
  formatDate(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  },

  // 计算汇总数据
  calculateSummary(statistics) {
    const summary = {
      newMembers: 0,
      newMemberCards: 0,
      haveBalanceCard: false,
      haveCountCard: false,
      balanceCardData: {
        consumeAmount: 0,
        consumeCount: 0,
        rechargeAmount: 0,
        refundAmount: 0
      },
      countCardData: {
        consumeTimes: 0,
        rechargeTimes: 0
      }
    };

    statistics.forEach(item => {
      summary.newMembers += item.memberData?.newMembers || 0;
      summary.newMemberCards += item.memberData?.newMemberCards || 0;

      if (item.haveBalanceCard) {
        summary.haveBalanceCard = true;
        const bd = item.balanceCardData;
        summary.balanceCardData.consumeAmount += bd.consumeAmount || 0;
        summary.balanceCardData.consumeCount += bd.consumeCount || 0;
        summary.balanceCardData.rechargeAmount += bd.rechargeAmount || 0;
        summary.balanceCardData.refundAmount += bd.refundAmount || 0;
      }

      if (item.haveCountCard) {
        summary.haveCountCard = true;
        const cd = item.countCardData;
        summary.countCardData.consumeTimes += cd.consumeTimes || 0;
        summary.countCardData.rechargeTimes += cd.rechargeTimes || 0;
      }
    });

    return summary;
  },


  // 构建趋势选项
  buildTrendOptions(sampleData) {
    const options = [
      { label: '新增会员数量', key: 'newMembers', dataKey: 'memberData' }
    ];

    options.push({ label: '新办会员卡数量', key: 'newMemberCards', dataKey: 'memberData' });

    if (sampleData.haveBalanceCard) {
      options.push(
        { label: '消费统计', key: 'consumeAmount', dataKey: 'balanceCardData' },
        { label: '充值总额', key: 'rechargeAmount', dataKey: 'balanceCardData' },
        { label: '退款统计', key: 'refundAmount', dataKey: 'balanceCardData' }
      );
    }

    if (sampleData.haveCountCard) {
      options.push(
        { label: '次数消耗统计', key: 'consumeTimes', dataKey: 'countCardData' },
        { label: '次数充值统计', key: 'rechargeTimes', dataKey: 'countCardData' }
      );
    }

    return options;
  },

  // 快捷选项切换
  handleQuickOptionChange(e) {
    const value = e.currentTarget.dataset.value;
    const quickOptions = this.data.quickOptions.map(opt => ({
      ...opt,
      active: opt.value === value
    }));

    this.setData({
      quickOptions,
      selectedQuickOption: value
    });

    // 重新加载数据
    this.loadStatistics();
  },

  // 某日数据切换 - 今日/昨日
  handleDayOptionChange(e) {
    const value = e.currentTarget.dataset.value;
    const { statisticsData } = this.data;
    
    if (!statisticsData || statisticsData.length === 0) return;

    // 自定义走单独逻辑
    if (value === 'custom') {
      this.setData({ showDatePicker: true });
      return;
    }

    let targetData = null;
    if (value === 'today') {
      targetData = statisticsData[0];
    } else if (value === 'yesterday' && statisticsData.length > 1) {
      targetData = statisticsData[1];
    }

    if (targetData) {
      this.setData({
        dayOptions: this.data.dayOptions.map(opt => ({ ...opt, active: opt.value === value })),
        selectedDayOption: value,
        currentDayData: targetData,
        customDateLabel: ''
      });
    }
  },

  // 关闭日期选择弹窗
  closeDatePicker() {
    this.setData({ showDatePicker: false });
  },

  // 选择某个日期
  onSelectDateItem(e) {
    const date = e.currentTarget.dataset.date;
    const { statisticsData } = this.data;
    const targetData = statisticsData.find(item => item.date === date);
    
    if (targetData) {
      this.setData({
        dayOptions: this.data.dayOptions.map(opt => ({ ...opt, active: opt.value === 'custom' })),
        selectedDayOption: 'custom',
        currentDayData: targetData,
        customDateLabel: `${date} >`,
        showDatePicker: false
      });
    }
  },

  // 趋势指标切换
  handleTrendChange(e) {
    const index = e.currentTarget.dataset.index;
    this.setData({ selectedTrendIndex: index });
    this.drawChart();
  },

  // 绘制图表
  drawChart() {
    const { statisticsData, trendOptions, selectedTrendIndex, selectedQuickOption } = this.data;
    
    if (!statisticsData || statisticsData.length === 0 || !trendOptions[selectedTrendIndex]) {
      return;
    }

    const option = trendOptions[selectedTrendIndex];
    const chartData = this.extractChartData(statisticsData, option);

    this.setData({ chartData }, () => {
      this.renderCanvas(chartData, selectedQuickOption);
    });
  },

  // 提取图表数据
  extractChartData(statistics, option) {
    return statistics.map(item => {
      let value = 0;
      
      if (option.dataKey === 'memberData') {
        value = item.memberData?.[option.key] || 0;
      } else if (option.dataKey === 'balanceCardData') {
        value = item.balanceCardData?.[option.key] || 0;
      } else if (option.dataKey === 'countCardData') {
        value = item.countCardData?.[option.key] || 0;
      }

      return {
        date: item.date,
        value: value
      };
    }).reverse(); // 反转，让最早的在左边
  },


  // 渲染Canvas
  renderCanvas(chartData, dateRange) {
    const query = wx.createSelectorQuery();
    query.select('#trendCanvas')
      .fields({ node: true, size: true })
      .exec((res) => {
        if (!res[0]) return;

        const canvas = res[0].node;
        const ctx = canvas.getContext('2d');
        const dpr = wx.getSystemInfoSync().pixelRatio;
        
        canvas.width = res[0].width * dpr;
        canvas.height = res[0].height * dpr;
        ctx.scale(dpr, dpr);

        const width = res[0].width;
        const height = res[0].height;

        // 清空画布
        ctx.clearRect(0, 0, width, height);

        // 绘制图表
        this.drawLineChart(ctx, width, height, chartData, dateRange);
      });
  },

  // 绘制折线图
  drawLineChart(ctx, width, height, data, dateRange) {
    const padding = { top: 40, right: 50, bottom: 40, left: 20 };
    const chartWidth = width - padding.left - padding.right;
    const chartHeight = height - padding.top - padding.bottom;

    // 计算数据范围
    const values = data.map(d => d.value);
    const maxValue = Math.max(...values, 0);
    const minValue = 0;

    // 计算Y轴刻度
    const yTicks = this.calculateYTicks(minValue, maxValue);

    // 绘制坐标轴（竖轴在右侧）
    ctx.strokeStyle = '#E0E0E0';
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(width - padding.right, padding.top);
    ctx.lineTo(width - padding.right, height - padding.bottom);
    ctx.lineTo(padding.left, height - padding.bottom);
    ctx.stroke();

    // 绘制Y轴刻度和标签（右侧）
    ctx.fillStyle = '#666';
    ctx.font = '12px sans-serif';
    ctx.textAlign = 'left';
    yTicks.forEach(tick => {
      const y = height - padding.bottom - ((tick - minValue) / (yTicks[yTicks.length - 1] - minValue)) * chartHeight;
      ctx.fillText(tick.toString(), width - padding.right + 5, y + 4);
      
      // 绘制网格线
      ctx.strokeStyle = '#F0F0F0';
      ctx.beginPath();
      ctx.moveTo(padding.left, y);
      ctx.lineTo(width - padding.right, y);
      ctx.stroke();
    });


    // 本周/本月模式：生成完整横坐标（包括未来日期）
    let fullDateRange = data;
    if (dateRange === 17 || dateRange === 32) {
      fullDateRange = this.generateFullDateRange(data, dateRange);
    }

    // 计算X轴显示点
    const xPoints = this.calculateXPoints(fullDateRange.length, dateRange);

    // 绘制X轴标签
    ctx.fillStyle = '#666';
    ctx.textAlign = 'center';
    xPoints.forEach(point => {
      const x = padding.left + (point.index / (fullDateRange.length - 1)) * chartWidth;
      const date = fullDateRange[point.index].date;
      const label = this.formatDateLabel(date);
      ctx.fillText(label, x, height - padding.bottom + 20);
    });

    // 绘制折线（只绘制到今天）
    if (data.length > 0) {
      ctx.strokeStyle = '#42A5F5';
      ctx.lineWidth = 2;
      ctx.beginPath();

      // 添加边距，不紧贴竖轴
      const margin = chartWidth * 0.05;
      const effectiveWidth = chartWidth - 2 * margin;

      let hasStarted = false;
      data.forEach((point, index) => {
        const x = padding.left + margin + (index / (fullDateRange.length - 1)) * effectiveWidth;
        const y = height - padding.bottom - ((point.value - minValue) / (yTicks[yTicks.length - 1] - minValue)) * chartHeight;

        if (!hasStarted) {
          ctx.moveTo(x, y);
          hasStarted = true;
        } else {
          ctx.lineTo(x, y);
        }
      });

      ctx.stroke();

      // 绘制数据点（小点，0值不绘制）
      ctx.fillStyle = '#42A5F5';
      data.forEach((point, index) => {
        if (point.value === 0) return; // 0值不绘制圆点
        
        const x = padding.left + margin + (index / (fullDateRange.length - 1)) * effectiveWidth;
        const y = height - padding.bottom - ((point.value - minValue) / (yTicks[yTicks.length - 1] - minValue)) * chartHeight;

        ctx.beginPath();
        ctx.arc(x, y, 2, 0, 2 * Math.PI);
        ctx.fill();
      });
    }

    // 保存图表信息用于交互
    this.chartInfo = {
      padding,
      chartWidth,
      chartHeight,
      data,
      fullDateRange,
      minValue,
      maxValue: yTicks[yTicks.length - 1],
      margin: chartWidth * 0.05,
      yTicks
    };
  },


  // 生成完整日期范围（本周/本月）
  generateFullDateRange(data, dateRange) {
    if (data.length === 0) return data;
    
    // data[0]是最早的日期（已经reverse过）
    const firstDateStr = data[0].date;
    const firstDate = new Date(firstDateStr);
    
    let startDate, endDate;
    if (dateRange === 17) {
      // 本周：周一到周日（固定7天）
      startDate = new Date(firstDate);
      endDate = new Date(firstDate);
      endDate.setDate(firstDate.getDate() + 6); // 周一+6天=周日
    } else if (dateRange === 32) {
      // 本月：1号到月末（根据月份动态计算天数）
      startDate = new Date(firstDate.getFullYear(), firstDate.getMonth(), 1);
      endDate = new Date(firstDate.getFullYear(), firstDate.getMonth() + 1, 0); // 月末
    } else {
      return data;
    }

    // 生成完整日期列表
    const allDates = [];
    const current = new Date(startDate);
    
    while (current <= endDate) {
      allDates.push({
        date: this.formatDate(current),
        value: 0
      });
      current.setDate(current.getDate() + 1);
    }

    // 用实际数据替换
    const dataMap = {};
    data.forEach(item => {
      dataMap[item.date] = item;
    });

    return allDates.map(item => dataMap[item.date] || item);
  },

  // 计算Y轴刻度
  calculateYTicks(min, max) {
    const ticks = [0];
    const range = max - min;
    
    if (range === 0) {
      return [0, 10, 20, 30, 40, 50, 60];
    }

    const step = Math.ceil(range / 6);
    for (let i = 1; i <= 6; i++) {
      ticks.push(step * i);
    }

    return ticks;
  },

  // 计算X轴显示点
  calculateXPoints(dataLength, dateRange) {
    const points = [];

    if (dateRange === 7) {
      // 近7天：显示1、3、5、7
      [0, 2, 4, 6].forEach(i => {
        if (i < dataLength) points.push({ index: i });
      });
    } else if (dateRange === 17) {
      // 本周：固定显示7天（周一到周日），索引0、2、4、6对应周一、周三、周五、周日
      [0, 2, 4, 6].forEach(i => {
        if (i < dataLength) points.push({ index: i });
      });
    } else if (dateRange === 30) {
      // 近30天：显示1、8、15、22、30
      [0, 7, 14, 21, dataLength - 1].forEach(i => {
        if (i < dataLength && i >= 0) points.push({ index: i });
      });
    } else if (dateRange === 32) {
      // 本月：显示1号、8号、15号、22号、28号（固定索引0、7、14、21、27）
      [0, 7, 14, 21, 27].forEach(i => {
        if (i < dataLength) points.push({ index: i });
      });
    }

    return points;
  },

  // 格式化日期标签
  formatDateLabel(dateStr) {
    const parts = dateStr.split('-');
    return `${parts[1]}-${parts[2]}`;
  },


  // Canvas点击事件
  onCanvasTap(e) {
    if (!this.chartInfo) return;

    const { padding, chartWidth, chartHeight, data, fullDateRange, minValue, maxValue, margin } = this.chartInfo;

    // 获取点击位置
    const x = e.detail.x;
    const y = e.detail.y;

    // 判断是否在图表区域内
    if (x < padding.left || x > padding.left + chartWidth ||
        y < padding.top || y > padding.top + chartHeight) {
      return;
    }

    // 使用fullDateRange计算索引
    const dateRangeLength = fullDateRange.length;
    const effectiveWidth = chartWidth - 2 * margin;
    const relativeX = x - padding.left - margin;
    const index = Math.round((relativeX / effectiveWidth) * (dateRangeLength - 1));
    
    // 确保索引在data范围内
    if (index >= 0 && index < data.length) {
      const point = data[index];
      const pointX = padding.left + margin + (index / (dateRangeLength - 1)) * effectiveWidth;
      const pointY = padding.top + chartHeight - ((point.value - minValue) / (maxValue - minValue)) * chartHeight;

      this.setData({
        selectedPoint: {
          date: point.date,
          value: point.value,
          x: pointX,
          y: pointY,
          index: index
        }
      });

      // 重绘图表，添加竖线和大点
      this.redrawWithVerticalLine(pointX, index);
    }
  },

  // 重绘图表并添加竖线和大点
  redrawWithVerticalLine(x, selectedIndex) {
    const { chartData, selectedQuickOption } = this.data;
    
    const query = wx.createSelectorQuery();
    query.select('#trendCanvas')
      .fields({ node: true, size: true })
      .exec((res) => {
        if (!res[0]) return;

        const canvas = res[0].node;
        const ctx = canvas.getContext('2d');
        const dpr = wx.getSystemInfoSync().pixelRatio;
        
        canvas.width = res[0].width * dpr;
        canvas.height = res[0].height * dpr;
        ctx.scale(dpr, dpr);

        const width = res[0].width;
        const height = res[0].height;

        // 重绘图表
        this.drawLineChart(ctx, width, height, chartData, selectedQuickOption);

        // 绘制竖虚线
        const { padding, chartHeight, chartWidth, margin, data, fullDateRange, minValue, yTicks } = this.chartInfo;
        const dateRangeLength = fullDateRange.length;
        
        ctx.strokeStyle = '#FF5722';
        ctx.lineWidth = 1;
        ctx.setLineDash([5, 5]);
        ctx.beginPath();
        ctx.moveTo(x, padding.top);
        ctx.lineTo(x, padding.top + chartHeight);
        ctx.stroke();
        ctx.setLineDash([]);

        // 绘制选中点的大点（橙色）
        const effectiveWidth = chartWidth - 2 * margin;
        const pointX = padding.left + margin + (selectedIndex / (dateRangeLength - 1)) * effectiveWidth;
        const pointY = height - padding.bottom - ((data[selectedIndex].value - minValue) / (yTicks[yTicks.length - 1] - minValue)) * chartHeight;
        
        ctx.fillStyle = '#FF5722';
        ctx.beginPath();
        ctx.arc(pointX, pointY, 5, 0, 2 * Math.PI); // 大点半径5
        ctx.fill();
      });
  },

  // 清除选中点
  clearSelectedPoint() {
    this.setData({ selectedPoint: null });
    this.drawChart();
  },

  // Canvas触摸开始
  onCanvasTouchStart(e) {
    this.touchStartX = e.touches[0].x;
  },


  // Canvas触摸移动（滑动绘制竖线）
  onCanvasTouchMove(e) {
    if (!this.chartInfo) return;

    const { padding, chartWidth, chartHeight, data, fullDateRange, minValue, maxValue, margin } = this.chartInfo;
    const x = e.touches[0].x;
    const y = e.touches[0].y;

    // 判断是否在图表区域内
    if (x < padding.left || x > padding.left + chartWidth ||
        y < padding.top || y > padding.top + chartHeight) {
      return;
    }

    // 使用fullDateRange计算索引
    const dateRangeLength = fullDateRange.length;
    const effectiveWidth = chartWidth - 2 * margin;
    const relativeX = x - padding.left - margin;
    const index = Math.round((relativeX / effectiveWidth) * (dateRangeLength - 1));
    
    // 确保索引在data范围内
    if (index >= 0 && index < data.length) {
      const point = data[index];
      const pointX = padding.left + margin + (index / (dateRangeLength - 1)) * effectiveWidth;
      const pointY = padding.top + chartHeight - ((point.value - minValue) / (maxValue - minValue)) * chartHeight;

      this.setData({
        selectedPoint: {
          date: point.date,
          value: point.value,
          x: pointX,
          y: pointY,
          index: index
        }
      });

      // 实时重绘
      this.redrawWithVerticalLine(pointX, index);
    }
  },

  // Canvas触摸结束
  onCanvasTouchEnd(e) {
    // 保持当前选中状态
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.loadStatistics(true);
  },

  // 显示自定义Toast
  showCustomToast(message, type = 'success') {
    const toast = this.selectComponent('#customToast');
    if (toast) {
      toast.showToast(message, type);
    }
  }
});
