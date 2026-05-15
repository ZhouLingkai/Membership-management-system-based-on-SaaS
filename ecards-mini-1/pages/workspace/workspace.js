// pages/workspace/workspace.js
const { getCurrentTime } = require('../../utils/time-util.js');
const { request } = require('../../utils/request');
const tokenManager = require('../../utils/token');
const { get: storageGet, set: storageSet } = require('../../utils/storage-utils');

// 店铺头像颜色方案（循环使用）
const AVATAR_COLOR_SCHEMES = [
  { bg: '#E3F2FD', text: '#1976D2' },  // 浅蓝 + 天蓝
  { bg: '#F3E5F5', text: '#7B1FA2' },  // 浅紫 + 紫色
  { bg: '#FFF8E1', text: '#F57C00' },  // 浅黄 + 橘色
  { bg: '#E8F5E9', text: '#388E3C' },  // 浅绿 + 绿色
  { bg: '#FCE4EC', text: '#C2185B' },  // 浅粉 + 粉色
  { bg: '#E0F7FA', text: '#00838F' },  // 浅青 + 青色
];

// 字号配置系统
const FONT_SIZE_CONFIGS = {
  large: {
    p1: 48,
    p2: 42,
    p3: 38,
    p4: 34,
    headerRowHeight: 160,
    headerRowWidth: 185,
    headerColHeight: 200,
    headerColWidth: 210
  },
  medium: {
    p1: 42,
    p2: 36,
    p3: 32,
    p4: 30,
    headerRowHeight: 140,
    headerRowWidth: 160,
    headerColHeight: 150,
    headerColWidth: 180
  }
};

// 操作类型映射
const OPERATE_TYPE_MAP = {
  1: { text: '用户预约', color: 'blue' },
  2: { text: '线下占用', color: 'yellow' },
  3: { text: '资源停用', color: 'pink' }
};

// 星期映射
const WEEKDAY_MAP = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];

Page({

    /**
     * 页面的初始数据
     */
    data: {
      showStoreCard: false, // 是否显示店铺选择卡片
      storeListMaxHeight: 600, // 店铺列表最大高度
      // 系统信息
      statusBarHeight: 0, // 状态栏高度
      navBarHeight: 88, // 导航栏内容高度
      totalNavHeight: 0, // 总导航栏高度（状态栏+导航栏）
      
      // Tab切换
      activeTab: 'workspace', // 'workspace' | 'reservation'
      
      // 字号配置 (0: medium, 1: large)
      fontSizeIndex: 1,
      sizeConfig: FONT_SIZE_CONFIGS.large,
      
      // 预约系统数据
      reservationData: null,
      processedResources: [], // 处理后的资源列表
      cellMatrix: [], // 单元格矩阵
      emptyMessage: '加载中...',
      
      // 滚动同步
      scrollLeft: 0,
      scrollTop: 0,
      
      // 日期显示
      displayDate: {
        monthDay: '',
        year: ''
      },
      selectedQueryDate: '', // 当前查询日期
      availableDates: [], // 可选日期列表
      showDatePickerModal: false,
      
      // 单元格选择
      selectedCells: [], // 已选中的单元格 [{row, col, timeSlot}]
      selectedResourceId: null,
      selectedResourceName: '',
      selectedTimeSlots: [], // 已选时间段字符串数组
      showBottomForm: false, // 控制底部表单显示（用于动画）
      tableScrollTop: 0, // 表格滚动位置（垂直）
      tableScrollLeft: 0, // 表格滚动位置（水平）
      tableBodyWidth: 0, // 表格总宽度（预计算）
      headerRowAnimation: null, // 表头行动画
      headerColAnimation: null, // 表头列动画
      
      // 弹窗
      showDetailModal: false,
      detailInfo: {},
      showResourceModal: false,
      resourceDetailInfo: {},
      // 当前选中的店铺
      currentStore: null,
      // 店铺列表（从后端获取）
      storeList: [],
      // 店铺列表加载状态
      storeListLoading: false,
      // 是否已获取工作令牌
      hasWorkToken: false,
      // 当前筛选标签
      currentFilter: 'all',
      // 当前视图模式: 'list' 一列模式, 'grid' 四列模式
      viewMode: 'list',
      // 筛选后的工具列表
      filteredTools: [],
      // 所有工具列表
      allTools: [
        {
          action: 'mobile_card',
          name: '手机号划卡',
          describe: '手机号/扫码查找会员卡，进行充值、消费、积分、交易记录查询、冻结等操作',
          labels: '前台',
          labelsArray: ['前台'],
          icon: '/assets/workspace/mobile.png',
          iconBg: 'linear-gradient(135deg, #ffb74d 0%, #f57c00 100%)',
          
          implemented: true
        },
        {
          action: 'guest_account',
          name: '散客记账',
          describe: '记账工具',
          labels: '前台',
          labelsArray: ['前台'],
          icon: '/assets/workspace/guest.png',
          iconBg: 'linear-gradient(135deg, #ffb74d 0%, #f57c00 100%)',
          implemented: false
        },
        {
          action: 'quick_card',
          name: '快捷办卡',
          describe: '为用户快速办理电子会员卡',
          labels: '前台',
          labelsArray: ['前台'],
          icon: '/assets/workspace/card.png',
          iconBg: 'linear-gradient(135deg, #ffb74d 0%, #f57c00 100%)',
          implemented: true
        },
        {
          action: 'member_center',
          name: '会员中心',
          describe: '多条件筛选查询会员卡的工具',
          labels: '前台',
          labelsArray: ['前台'],
          icon: '/assets/workspace/member.png',
          iconBg: 'linear-gradient(135deg, #ffb74d 0%, #f57c00 100%)',
          implemented: true
        },
        {
          action: 'card_manage',
          name: '卡种管理',
          describe: '管理店铺内会员卡类型，提供查询、创建、修改功能',
          labels: '后台',
          labelsArray: ['后台'],
          icon: '/assets/workspace/card-type.png',
          iconBg: 'linear-gradient(135deg, #ba68c8 0%, #7b1fa2 100%)',
          implemented: true
        },
        {
          action: 'staff_manage',
          name: '员工管理',
          describe: '管理店铺员工，查看、添加、移除员工',
          labels: '后台',
          labelsArray: ['后台'],
          icon: '/assets/workspace/staff.png',
          iconBg: 'linear-gradient(135deg, #ba68c8 0%, #7b1fa2 100%)',
          implemented: true
        },
        {
          action: 'business_data',
          name: '经营数据',
          describe: '查看近期经营数据，通过图表数据分析会员、营收情况',
          labels: '经营',
          labelsArray: ['经营'],
          icon: '/assets/workspace/data.png',
          iconBg: 'linear-gradient(135deg, #f06292 0%, #c2185b 100%)',
          implemented: true
        },
        {
          action: 'activity_marketing',
          name: '活动营销',
          describe: '创建、管理各种活动',
          labels: '经营',
          labelsArray: ['经营'],
          icon: '/assets/workspace/activity.png',
          iconBg: 'linear-gradient(135deg, #f06292 0%, #c2185b 100%)',
          implemented: false
        },
        {
          action: 'reservation_message',
          name: '预约消息',
          describe: '简单预约系统，通过消息预订',
          labels: '预约,前台',
          labelsArray: ['预约', '前台'],
          icon: '/assets/workspace/appointment.png',
          iconBg: 'linear-gradient(135deg, #42a5f5 0%, #1976d2 100%)',
          implemented: false
        },
        {
          action: 'reservation_system',
          name: '预约系统',
          describe: '高级预约系统，实时预约店内资源，提供管理预约模板、资源的工具',
          labels: '预约,后台',
          labelsArray: ['预约', '后台'],
          icon: '/assets/workspace/template.png',
          iconBg: 'linear-gradient(135deg, #42a5f5 0%, #1976d2 100%)',
          implemented: true
        }
      ]
    },
  
    /**
     * 生命周期函数--监听页面加载
     */
    onLoad(options) {
      console.log('员工工作台页面加载');
      
      // 获取系统信息，计算导航栏高度
      this.getSystemInfo();
      // 初始化工具列表
      this.initToolsList();
      // 加载店铺列表
      this.loadStoreList();
    },
  
    // 获取系统信息，计算导航栏高度
    getSystemInfo() {
      const systemInfo = wx.getSystemInfoSync();
      console.log('系统信息:', systemInfo);
      
      // 获取胶囊按钮信息
      const menuButtonInfo = wx.getMenuButtonBoundingClientRect();
      console.log('胶囊按钮信息:', menuButtonInfo);
      
      // 正确的px转rpx计算：rpx = px * 750 / 屏幕宽度px
      const pxToRpx = (px) => {
        return Math.round(px * 750 / systemInfo.windowWidth);
      };
      
      // 计算状态栏高度（px转rpx）
      const statusBarHeight = pxToRpx(systemInfo.statusBarHeight);
      
      // 计算导航栏内容高度（正确的计算公式）+ 额外高度
      let navBarHeight = 88 + 10; // 默认值 + 10rpx
      if (menuButtonInfo && menuButtonInfo.top && menuButtonInfo.height) {
        // 胶囊按钮顶部距离状态栏底部的距离
        const menuTopGap = menuButtonInfo.top - systemInfo.statusBarHeight;
        // 导航栏内容高度 = 胶囊按钮高度 + 2 × 上下间距
        const navBarHeightPx = menuButtonInfo.height + 2 * menuTopGap;
        navBarHeight = pxToRpx(navBarHeightPx) + 10; // 转换为rpx + 10rpx额外高度
      }
      
      // 总导航栏高度 = 状态栏高度 + 导航栏内容高度
      const totalNavHeight = statusBarHeight + navBarHeight;
      
      this.setData({
        statusBarHeight: statusBarHeight,
        navBarHeight: navBarHeight,
        totalNavHeight: totalNavHeight
      });
    },
  
    /**
     * 生命周期函数--监听页面显示
     */
    onShow() {
      const app = getApp();
      
      // 更新 tabBar 状态
      app.updateTabBarSelected('pages/workspace/workspace');
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
     * 加载店铺列表（员工）
     */
    async loadStoreList() {
      try {
        this.setData({ storeListLoading: true });
        
        // 获取用户信息
        const app = getApp();
        const userInfo = app.globalData.userInfo;
        if (!userInfo || !userInfo.userId) {
          this.setData({ storeListLoading: false });
          this.showCustomToast('用户信息不存在，请重新登录', 'danger');
          return;
        }
        
        // 获取普通令牌
        const normalToken = await tokenManager.getNormalToken();
        if (!normalToken) {
          this.setData({ storeListLoading: false });
          this.showCustomToast('令牌已过期，请重新登录', 'danger');
          return;
        }
        
        // 调用员工工作店铺查询接口
        const res = await request.get(
          '/v1/staffs/work-stores',
          {
            'Content-Type': 'application/json',
            'Authorization': normalToken
          },
          { userId: userInfo.userId }
        );
        
        console.log('[员工工作店铺列表查询]', res);
        this.setData({ storeListLoading: false });
        
        if (res.code === 200 && res.data) {
          const rawList = res.data.list || [];
          // 处理店铺列表，添加文字头像信息
          const storeList = this.processStoreList(rawList);
          this.setData({ storeList });
          
          // 首次选择店铺
          await this.initStoreSelection(storeList);
        } else {
          this.showCustomToast(res.message || '获取工作店铺列表失败', 'danger');
        }
      } catch (error) {
        console.error('[工作店铺列表加载失败]', error);
        this.setData({ storeListLoading: false });
        this.showCustomToast('加载失败，请稍后重试', 'danger');
      }
    },

    /**
     * 处理店铺列表，添加文字头像信息
     */
    processStoreList(rawList) {
      return rawList.map((store, index) => {
        // 取店名前两个字作为头像文字
        const avatarText = store.storeName ? store.storeName.substring(0, 2) : '店铺';
        // 循环使用颜色方案
        const colorScheme = AVATAR_COLOR_SCHEMES[index % AVATAR_COLOR_SCHEMES.length];
        
        return {
          id: store.storeId,
          name: store.storeName,
          type: store.storeType,
          status: store.storeStatus,
          createTime: store.createTime,
          avatarText: avatarText,
          avatarBgColor: colorScheme.bg,
          avatarTextColor: colorScheme.text
        };
      });
    },

    /**
     * 初始化店铺选择（首次加载时）
     */
    async initStoreSelection(storeList) {
      if (!storeList || storeList.length === 0) {
        this.showCustomToast('暂无工作店铺', 'warning');
        return;
      }
      
      // 尝试从Storage获取上次选择的店铺
      let selectedStore = null;
      try {
        const lastWorkStoreId = await storageGet('lastWorkStore');
        if (lastWorkStoreId) {
          selectedStore = storeList.find(s => s.id === lastWorkStoreId);
        }
      } catch (e) {
        console.log('[读取lastWorkStore失败]', e);
      }
      
      // 如果没有找到，默认选择第一个
      if (!selectedStore) {
        selectedStore = storeList[0];
      }
      
      this.setData({ currentStore: selectedStore });
      
      // 获取工作令牌
      await this.loadWorkToken(selectedStore.id);
    },

    /**
     * 获取工作令牌
     */
    async loadWorkToken(storeId) {
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
          console.log('[工作令牌获取成功]', storeId);
        } else {
          this.setData({ hasWorkToken: false });
          this.showCustomToast('获取工作令牌失败', 'danger');
        }
      } catch (error) {
        console.error('[获取工作令牌失败]', error);
        this.setData({ hasWorkToken: false });
        this.showCustomToast('获取工作令牌失败', 'danger');
      }
    },
  
    // 切换店铺选择卡片显示状态
    toggleStoreCard() {
      this.setData({
        showStoreCard: !this.data.showStoreCard
      });
    },
  
    // 隐藏店铺选择卡片
    hideStoreCard() {
      this.setData({
        showStoreCard: false
      });
    },
  
    // 选择店铺
    async selectStore(e) {
      const store = e.currentTarget.dataset.store;
      console.log('[选择店铺]', store);
      
      // 如果选择的是当前店铺，仅关闭卡片
      if (this.data.currentStore && store.id === this.data.currentStore.id) {
        this.setData({ showStoreCard: false });
        return;
      }
      
      this.setData({
        currentStore: store,
        showStoreCard: false,
        hasWorkToken: false
      });
      
      // 更新Storage中的lastWorkStore
      try {
        await storageSet('lastWorkStore', store.id);
      } catch (e) {
        console.log('[保存lastWorkStore失败]', e);
      }
      
      // 获取新店铺的工作令牌
      await this.loadWorkToken(store.id);
      
      this.showCustomToast(`已切换到${store.name}`, 'success');
    },
  
    // 处理二维码名片点击
    handleQRCard() {
      console.log('点击二维码名片');
      
      wx.showToast({
        title: '二维码名片功能',
        icon: 'none',
        duration: 2000
      });
      
      // 预留跳转页面
      // wx.navigateTo({
      //   url: '/pages/qr-card/qr-card'
      // });
    },

    // ==================== Tab切换 ====================
    switchTab(e) {
      const tab = e.currentTarget.dataset.tab;
      if (tab === this.data.activeTab) return;
      
      this.setData({ activeTab: tab });
      
      // 切换到预约系统时加载数据
      if (tab === 'reservation' && !this.data.reservationData) {
        this.initReservationSystem();
      }
    },

    // ==================== 预约系统初始化 ====================
    async initReservationSystem() {
      // 初始化日期
      const today = this.formatDate(new Date());
      this.setData({ selectedQueryDate: today });
      this.updateDisplayDate(today);
      
      // 加载数据
      await this.loadReservationData();
    },

    // 格式化日期为 YYYY-MM-DD
    formatDate(date) {
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    },

    // 更新显示日期
    updateDisplayDate(dateStr) {
      const date = new Date(dateStr);
      const month = date.getMonth() + 1;
      const day = date.getDate();
      const year = date.getFullYear();
      
      this.setData({
        displayDate: {
          monthDay: `${month}月${day}日`,
          year: `${year}年`
        }
      });
    },

    // ==================== 模拟数据 ====================
    getSimulationData() {
      return new Promise((resolve) => {
        setTimeout(() => {
          resolve({
            code: 200,
            message: '查询成功',
            data: {
              queryDate: this.data.selectedQueryDate || '2025-11-26',
              advanceDays: 7,
              forbiddenDays: ['周六', '周日', '2025-11-28', '2025-11-29'],
              timeList: [
                '08:00-09:00', '09:00-10:00', '10:00-11:00', '11:00-12:00',
                '13:00-14:00', '14:00-15:00', '15:00-16:00', '16:00-17:00',
                '17:00-18:00', '18:00-19:00'
              ],
              cancelRule: ['60:0.1', '180:5'],
              resourceDetails: [
                {
                  resourceId: 1001,
                  resourceName: '1楼 1号桌',
                  isReservable: 1,
                  resourceRestriction: { minContinuousTime: 30, maxContinuousTime: 180, supportCardTypes: 1, unitPrice: 10.00 },
                  reservationList: [
                    { startTime: '08:00:00', endTime: '09:00:00', operateType: 1, moreInfo: { userId: 'uuid-user-123', userPhone: '17056698484', transactionId: 1002, remark: '会晚到10分钟' } },
                    { startTime: '10:00:00', endTime: '12:00:00', operateType: 2, moreInfo: { userId: 'uuid-staff-456', userPhone: '13800138000', transactionId: 0, remark: '线下客户占用' } }
                  ]
                },
                {
                  resourceId: 1002,
                  resourceName: '1楼 2号桌',
                  isReservable: 1,
                  resourceRestriction: { minContinuousTime: 30, maxContinuousTime: 240, supportCardTypes: 1, unitPrice: 12.00 },
                  reservationList: [
                    { startTime: '09:00:00', endTime: '10:00:00', operateType: 1, moreInfo: { userId: 'uuid-user-789', userPhone: '13912345678', transactionId: 1003, remark: '' } }
                  ]
                },
                {
                  resourceId: 1003,
                  resourceName: '2楼-VIP包间A',
                  isReservable: 1,
                  resourceRestriction: { minContinuousTime: 60, maxContinuousTime: 300, supportCardTypes: 2, unitPrice: 50.00 },
                  reservationList: [
                    { startTime: '14:00:00', endTime: '17:00:00', operateType: 3, moreInfo: { userId: 'uuid-admin-001', userPhone: '', transactionId: 0, remark: '设备维护中' } }
                  ]
                },
                {
                  resourceId: 1004,
                  resourceName: '2楼 VIP包间B',
                  isReservable: 1,
                  resourceRestriction: { minContinuousTime: 60, maxContinuousTime: 300, supportCardTypes: 2, unitPrice: 50.00 },
                  reservationList: []
                },
                {
                  resourceId: 1005,
                  resourceName: '户外露台区-1号桌',
                  isReservable: 1,
                  resourceRestriction: { minContinuousTime: 30, maxContinuousTime: 120, supportCardTypes: 1, unitPrice: 8.00 },
                  reservationList: [
                    { startTime: '08:00:00', endTime: '10:00:00', operateType: 1, moreInfo: { userId: 'uuid-user-abc', userPhone: '15811112222', transactionId: 1004, remark: '需要遮阳伞' } },
                    { startTime: '15:00:00', endTime: '16:00:00', operateType: 2, moreInfo: { userId: 'uuid-staff-def', userPhone: '18633334444', transactionId: 0, remark: '老客户临时占用' } }
                  ]
                },
                {
                  resourceId: 1006,
                  resourceName: '3楼会议室',
                  isReservable: 0,
                  resourceRestriction: { minContinuousTime: 60, maxContinuousTime: 480, supportCardTypes: 3, unitPrice: 100.00 },
                  reservationList: []
                },
                {
                  resourceId: 1007,
                  resourceName: '健身房 跑步机区',
                  isReservable: 1,
                  resourceRestriction: { minContinuousTime: 30, maxContinuousTime: 120, supportCardTypes: 1, unitPrice: 15.00 },
                  reservationList: [
                    { startTime: '08:00:00', endTime: '09:00:00', operateType: 1, moreInfo: { userId: 'uuid-user-111', userPhone: '13755556666', transactionId: 1005, remark: '' } },
                    { startTime: '13:00:00', endTime: '15:00:00', operateType: 1, moreInfo: { userId: 'uuid-user-222', userPhone: '13877778888', transactionId: 1006, remark: '团体预约' } }
                  ]
                }
              ]
            }
          });
        }, 200);
      });
    },

    // ==================== 加载预约数据 ====================
    async loadReservationData() {
      try {
        const res = await this.getSimulationData();
        
        if (res.code !== 200) {
          this.setData({ emptyMessage: res.message || '加载失败' });
          return;
        }

        const data = res.data;
        
        // 检查timeList
        if (!data.timeList || data.timeList.length === 0) {
          this.setData({ emptyMessage: '暂无可用预约模板', reservationData: null });
          return;
        }

        // 过滤可预约资源
        const reservableResources = (data.resourceDetails || []).filter(r => r.isReservable === 1);
        
        if (reservableResources.length === 0) {
          this.setData({ emptyMessage: '暂无可用资源', reservationData: null });
          return;
        }

        // 处理时间段格式
        const processedTimeList = data.timeList.map(slot => {
          const parts = slot.split('-');
          return { original: slot, start: parts[0], end: parts[1] };
        });

        // 处理资源名称
        const processedResources = reservableResources.map(resource => ({
          ...resource,
          displayName: this.processResourceName(resource.resourceName)
        }));

        // 构建单元格矩阵
        const cellMatrix = this.buildCellMatrix(processedResources, processedTimeList, data.timeList, data.queryDate);

        // 计算可选日期
        const availableDates = this.calculateAvailableDates(data.advanceDays, data.forbiddenDays);

        // 预先计算表格总宽度
        const tableBodyWidth = processedTimeList.length * this.data.sizeConfig.headerRowWidth + this.data.sizeConfig.headerColWidth;

        this.setData({
          reservationData: { ...data, timeList: processedTimeList, resourceDetails: reservableResources },
          processedResources,
          cellMatrix,
          availableDates,
          tableBodyWidth,
          emptyMessage: ''
        });

        // 数据加载完成后，根据当前时间自动定位
        setTimeout(() => {
          this.autoPositionByTime(data.queryDate, processedTimeList);
        }, 100);

      } catch (error) {
        console.error('加载预约数据失败:', error);
        this.setData({ emptyMessage: '加载失败，请重试' });
      }
    },

    // ==================== 根据当前时间自动定位 ====================
    autoPositionByTime(queryDate, timeList) {
      const { sizeConfig } = this.data;
      const systemInfo = wx.getSystemInfoSync();
      const rpxToPx = systemInfo.windowWidth / 750;
      const colWidth = sizeConfig.headerRowWidth * rpxToPx;
      
      const { dateStr: today, timeStr } = getCurrentTime();
      const currentTime = timeStr.substring(0, 5);
      
      if (queryDate !== today) {
        this.setData({ tableScrollLeft: 0, tableScrollTop: 0 });
        setTimeout(() => this.alignHeadersOnly(0, 0), 350);
        return;
      }
      
      if (currentTime < timeList[0].start) {
        this.setData({ tableScrollLeft: 0, tableScrollTop: 0 });
        setTimeout(() => this.alignHeadersOnly(0, 0), 350);
        return;
      }
      
      if (currentTime >= timeList[timeList.length - 1].end) {
        const maxScrollLeft = timeList.length * colWidth;
        this.setData({ tableScrollLeft: maxScrollLeft, tableScrollTop: 0 });
        setTimeout(() => {
          const actualLeft = this._lastScrollLeft || maxScrollLeft;
          this.alignHeadersOnly(actualLeft, 0);
        }, 350);
        return;
      }
      
      for (let i = 0; i < timeList.length; i++) {
        const slot = timeList[i];
        if (currentTime >= slot.start && currentTime < slot.end) {
          const targetScrollLeft = i * colWidth;
          this.setData({ tableScrollLeft: targetScrollLeft, tableScrollTop: 0 });
          setTimeout(() => this.alignHeadersOnly(targetScrollLeft, 0), 350);
          return;
        }
      }
    },

    // ==================== 资源名称处理 ====================
    processResourceName(name) {
      let trimmed = name.replace(/^[\s-]+|[\s-]+$/g, '');
      
      const separators = [];
      for (let i = 0; i < trimmed.length; i++) {
        if (trimmed[i] === ' ' || trimmed[i] === '-') {
          separators.push(i);
        }
      }
      
      if (separators.length === 0) {
        return { line1: trimmed, line2: '' };
      }
      
      if (separators.length === 1) {
        const pos = separators[0];
        return { line1: trimmed.substring(0, pos), line2: trimmed.substring(pos + 1) };
      }
      
      const midIndex = Math.floor(trimmed.length / 2);
      let nearestPos = separators[0];
      let minDistance = Math.abs(separators[0] - midIndex);
      
      for (let i = 1; i < separators.length; i++) {
        const distance = Math.abs(separators[i] - midIndex);
        if (distance < minDistance) {
          minDistance = distance;
          nearestPos = separators[i];
        }
      }
      
      return { line1: trimmed.substring(0, nearestPos), line2: trimmed.substring(nearestPos + 1) };
    },

    // ==================== 构建单元格矩阵 ====================
    buildCellMatrix(resources, processedTimeList, originalTimeList, queryDate) {
      const matrix = [];
      const { dateStr: today, timeStr } = getCurrentTime();
      const currentTime = timeStr.substring(0, 5);
      const isToday = queryDate === today;
      
      resources.forEach((resource, rowIndex) => {
        const row = [];
        const reservationMap = this.buildReservationMap(resource.reservationList, originalTimeList);
        
        processedTimeList.forEach((timeSlot, colIndex) => {
          const cellInfo = reservationMap[colIndex];
          
          if (cellInfo) {
            row.push({
              status: 'reserved',
              operateType: cellInfo.operateType,
              displayText: OPERATE_TYPE_MAP[cellInfo.operateType]?.text || '已占用',
              reservationInfo: cellInfo,
              isSelected: false,
              isAdjacent: false,
              isFirst: false,
              spanCount: 1,
              firstCol: colIndex
            });
          } else {
            const isExpired = isToday && timeSlot.end <= currentTime;
            row.push({
              status: isExpired ? 'expired' : 'available',
              operateType: 0,
              displayText: isExpired ? '已过期' : '可预约',
              reservationInfo: null,
              isSelected: false,
              isAdjacent: false,
              isFirst: false,
              spanCount: 1,
              firstCol: colIndex
            });
          }
        });
        
        matrix.push(row);
      });
      
      this.mergeReservationCells(matrix);
      return matrix;
    },

    // 合并连续预约单元格
    mergeReservationCells(matrix) {
      const cellWidth = this.data.sizeConfig.headerRowWidth;
      
      matrix.forEach((row, rowIndex) => {
        let i = 0;
        while (i < row.length) {
          const cell = row[i];
          
          if (cell.status === 'reserved' && cell.reservationInfo) {
            const reservationId = cell.reservationInfo.id || `${cell.reservationInfo.startTime}-${cell.reservationInfo.endTime}`;
            let spanCount = 1;
            
            while (i + spanCount < row.length) {
              const nextCell = row[i + spanCount];
              if (nextCell.status !== 'reserved' || !nextCell.reservationInfo) break;
              const nextReservationId = nextCell.reservationInfo.id || `${nextCell.reservationInfo.startTime}-${nextCell.reservationInfo.endTime}`;
              if (nextReservationId !== reservationId) break;
              spanCount++;
            }
            
            cell.isFirst = true;
            cell.spanCount = spanCount;
            cell.spanWidth = spanCount * cellWidth - 8;
            cell.firstCol = i;
            
            for (let j = 1; j < spanCount; j++) {
              row[i + j].isFirst = false;
              row[i + j].spanCount = 0;
              row[i + j].spanWidth = 0;
              row[i + j].firstCol = i;
            }
            
            i += spanCount;
          } else {
            cell.isFirst = true;
            i++;
          }
        }
      });
    },

    // 构建预约映射
    buildReservationMap(reservationList, timeList) {
      const map = {};
      if (!reservationList || reservationList.length === 0) return map;
      
      reservationList.forEach(reservation => {
        const startTimeStr = reservation.startTime.substring(0, 5);
        const endTimeStr = reservation.endTime.substring(0, 5);
        
        timeList.forEach((slot, index) => {
          const [slotStart, slotEnd] = slot.split('-');
          if (this.isTimeInRange(slotStart, slotEnd, startTimeStr, endTimeStr)) {
            map[index] = { ...reservation, moreInfo: reservation.moreInfo };
          }
        });
      });
      
      return map;
    },

    // 判断时间段是否在预约范围内
    isTimeInRange(slotStart, slotEnd, resStart, resEnd) {
      const toMinutes = (timeStr) => {
        const [h, m] = timeStr.split(':').map(Number);
        return h * 60 + m;
      };
      
      const slotStartMin = toMinutes(slotStart);
      const slotEndMin = toMinutes(slotEnd);
      const resStartMin = toMinutes(resStart);
      const resEndMin = toMinutes(resEnd);
      
      return slotStartMin >= resStartMin && slotEndMin <= resEndMin;
    },

    // ==================== 日期计算 ====================
    calculateAvailableDates(advanceDays, forbiddenDays) {
      const dates = [];
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      
      for (let i = 0; i <= advanceDays; i++) {
        const date = new Date(today);
        date.setDate(today.getDate() + i);
        
        const dateStr = this.formatDate(date);
        const weekdayIndex = date.getDay();
        const weekdayStr = WEEKDAY_MAP[weekdayIndex];
        
        let flag = 0;
        if (forbiddenDays && forbiddenDays.length > 0) {
          if (forbiddenDays.includes(weekdayStr)) flag++;
          if (forbiddenDays.includes(dateStr)) flag++;
        }
        
        const disabled = flag === 1;
        
        dates.push({
          date: dateStr,
          display: `${date.getMonth() + 1}月${date.getDate()}日`,
          weekday: weekdayStr,
          disabled
        });
      }
      
      return dates;
    },

    // ==================== 滚动同步 ====================
    recordScrollPosition(data) {
      this._lastScrollTop = data.scrollTop || 0;
      this._lastScrollLeft = data.scrollLeft || 0;
    },

    onScrollEnd(e) {
      const { scrollLeft, scrollTop } = e.detail;
      console.log('onScrollEnd:', scrollLeft, scrollTop);
    },

    alignHeadersOnly(targetLeft, targetTop) {
      const rowAnimation = wx.createAnimation({ duration: 0 });
      rowAnimation.translateX(-targetLeft).step();
      
      const colAnimation = wx.createAnimation({ duration: 0 });
      colAnimation.translateY(-targetTop).step();
      
      this.setData({
        headerRowAnimation: rowAnimation.export(),
        headerColAnimation: colAnimation.export()
      });
    },

    forceAlignHeaders(scrollLeft, scrollTop) {
      const left = scrollLeft !== undefined ? scrollLeft : (this._lastScrollLeft || 0);
      const top = scrollTop !== undefined ? scrollTop : (this._lastScrollTop || 0);
      this.setData({ tableScrollLeft: left, tableScrollTop: top });
    },

    // ==================== 日期选择器 ====================
    showDatePicker() {
      this.setData({ showDatePickerModal: true });
    },

    hideDatePicker() {
      this.setData({ showDatePickerModal: false });
    },

    onDateItemTap(e) {
      const { date, disabled } = e.currentTarget.dataset;
      if (disabled) return;
      this.selectDate(e);
    },

    async selectDate(e) {
      const date = e.currentTarget.dataset.date;
      if (!date) return;
      
      this.setData({ selectedQueryDate: date, showDatePickerModal: false });
      this.updateDisplayDate(date);
      this.clearSelection();
      await this.loadReservationData();
    },

    // ==================== 单元格点击 ====================
    onCellTap(e) {
      const { row, col, resourceId, timeSlot, firstCol } = e.currentTarget.dataset;
      const cell = this.data.cellMatrix[row][col];
      const clickY = e.detail?.y || e.touches?.[0]?.clientY || 0;
      
      this._currentScrollTop = this._lastScrollTop || 0;
      
      if (cell.status === 'expired') return;
      
      if (cell.status === 'reserved') {
        const firstCell = this.data.cellMatrix[row][firstCol];
        this.showReservationDetail(row, firstCol, firstCell);
        return;
      }
      
      const { selectedCells, selectedResourceId } = this.data;
      
      if (selectedCells.length === 0) {
        this.selectCell(row, col, resourceId, timeSlot, clickY);
        return;
      }
      
      if (resourceId !== selectedResourceId) {
        this.switchResourceSelection(row, col, resourceId, timeSlot);
        return;
      }
      
      const existingIndex = selectedCells.findIndex(c => c.row === row && c.col === col);
      
      if (existingIndex !== -1) {
        if (selectedCells.length === 1) {
          this.clearSelection();
        } else {
          this.deselectFromIndex(existingIndex);
        }
        return;
      }
      
      const isAdjacent = selectedCells.some(c => Math.abs(c.col - col) === 1);
      
      if (isAdjacent) {
        this.addToSelection(row, col, timeSlot);
      } else {
        this.switchResourceSelection(row, col, resourceId, timeSlot);
      }
    },

    switchResourceSelection(row, col, resourceId, timeSlot) {
      const { cellMatrix, selectedCells } = this.data;
      const resource = this.data.processedResources[row];
      const oldRow = selectedCells.length > 0 ? selectedCells[0].row : -1;
      
      if (selectedCells.length > 0) {
        const timeListLength = this.data.reservationData?.timeList?.length || 0;
        for (let c = 0; c < timeListLength; c++) {
          cellMatrix[oldRow][c].isSelected = false;
          cellMatrix[oldRow][c].isAdjacent = false;
        }
      }
      
      cellMatrix[row][col].isSelected = true;
      this.updateAdjacentHints(row, [col], cellMatrix);
      
      this.setData({
        cellMatrix,
        selectedCells: [{ row, col, timeSlot }],
        selectedResourceId: resourceId,
        selectedResourceName: resource.resourceName,
        selectedTimeSlots: [timeSlot]
      });
      
      if (oldRow !== row) {
        setTimeout(() => { this.scrollToRowCenter(row); }, 50);
      }
    },

    selectCell(row, col, resourceId, timeSlot, clickY = 0) {
      const resource = this.data.processedResources[row];
      const cellMatrix = this.data.cellMatrix;
      
      cellMatrix[row][col].isSelected = true;
      this.updateAdjacentHints(row, [col], cellMatrix);
      
      this.setData({
        cellMatrix,
        selectedCells: [{ row, col, timeSlot }],
        selectedResourceId: resourceId,
        selectedResourceName: resource.resourceName,
        selectedTimeSlots: [timeSlot],
        showBottomForm: true
      });
      
      setTimeout(() => { this.scrollToRowCenter(row); }, 130);
    },

    scrollToRowCenter(rowIndex) {
      const { sizeConfig, totalNavHeight } = this.data;
      const systemInfo = wx.getSystemInfoSync();
      const rpxToPx = systemInfo.windowWidth / 750;
      
      const tableTopPx = (totalNavHeight + 80 + sizeConfig.headerRowHeight) * rpxToPx;
      const bottomOffsetPx = 48 + 240 * rpxToPx;
      const tableBottomPx = systemInfo.windowHeight - bottomOffsetPx;
      const visibleHeight = tableBottomPx - tableTopPx;
      const visibleCenterY = visibleHeight / 2;
      
      const rowHeight = sizeConfig.headerColHeight * rpxToPx;
      const rowCenterY = rowIndex * rowHeight + rowHeight / 2;
      const targetScrollTop = Math.max(0, rowCenterY - visibleCenterY);
      
      this.setData({ tableScrollTop: targetScrollTop });
      
      setTimeout(() => {
        const actualTop = this._lastScrollTop || 0;
        const actualLeft = this._lastScrollLeft || 0;
        this.alignHeadersOnly(actualLeft, actualTop);
      }, 350);
    },

    addToSelection(row, col, timeSlot) {
      const { selectedCells, cellMatrix } = this.data;
      
      const newSelectedCells = [...selectedCells, { row, col, timeSlot }];
      newSelectedCells.sort((a, b) => a.col - b.col);
      
      cellMatrix[row][col].isSelected = true;
      
      const cols = newSelectedCells.map(c => c.col);
      this.updateAdjacentHints(row, cols, cellMatrix);
      
      const selectedTimeSlots = newSelectedCells.map(c => c.timeSlot);
      
      this.setData({ cellMatrix, selectedCells: newSelectedCells, selectedTimeSlots });
      
      setTimeout(() => { this.scrollToColCenter(col); }, 50);
    },

    scrollToColCenter(colIndex) {
      const { sizeConfig } = this.data;
      const systemInfo = wx.getSystemInfoSync();
      const rpxToPx = systemInfo.windowWidth / 750;
      
      const tableLeftPx = sizeConfig.headerColWidth * rpxToPx;
      const visibleWidth = systemInfo.windowWidth - tableLeftPx;
      const visibleCenterX = visibleWidth / 2;
      
      const colWidth = sizeConfig.headerRowWidth * rpxToPx;
      const colCenterX = colIndex * colWidth + colWidth / 2;
      const targetScrollLeft = Math.max(0, colCenterX - visibleCenterX);
      
      this.setData({ tableScrollLeft: targetScrollLeft });
      
      setTimeout(() => {
        const actualTop = this._lastScrollTop || 0;
        const actualLeft = this._lastScrollLeft || 0;
        this.alignHeadersOnly(actualLeft, actualTop);
      }, 350);
    },

    deselectFromIndex(index) {
      const { selectedCells, cellMatrix } = this.data;
      const row = selectedCells[0].row;
      
      const sortedCells = [...selectedCells].sort((a, b) => a.col - b.col);
      const clickedCol = selectedCells[index].col;
      const sortedIndex = sortedCells.findIndex(c => c.col === clickedCol);
      
      for (let i = sortedIndex; i < sortedCells.length; i++) {
        cellMatrix[row][sortedCells[i].col].isSelected = false;
        cellMatrix[row][sortedCells[i].col].isAdjacent = false;
      }
      
      const remainingCells = sortedCells.slice(0, sortedIndex);
      
      if (remainingCells.length === 0) {
        this.clearSelection();
        return;
      }
      
      const cols = remainingCells.map(c => c.col);
      this.updateAdjacentHints(row, cols, cellMatrix);
      
      const selectedTimeSlots = remainingCells.map(c => c.timeSlot);
      
      this.setData({ cellMatrix, selectedCells: remainingCells, selectedTimeSlots });
    },

    updateAdjacentHints(row, selectedCols, cellMatrix) {
      const timeListLength = this.data.reservationData.timeList.length;
      
      for (let col = 0; col < timeListLength; col++) {
        cellMatrix[row][col].isAdjacent = false;
      }
      
      const adjacentCols = new Set();
      selectedCols.forEach(col => {
        if (col > 0) adjacentCols.add(col - 1);
        if (col < timeListLength - 1) adjacentCols.add(col + 1);
      });
      
      selectedCols.forEach(col => adjacentCols.delete(col));
      
      adjacentCols.forEach(col => {
        if (cellMatrix[row][col].status === 'available') {
          cellMatrix[row][col].isAdjacent = true;
        }
      });
    },

    clearSelection() {
      const { cellMatrix, selectedCells } = this.data;
      
      if (selectedCells.length > 0) {
        const row = selectedCells[0].row;
        const timeListLength = this.data.reservationData?.timeList?.length || 0;
        
        for (let col = 0; col < timeListLength; col++) {
          cellMatrix[row][col].isSelected = false;
          cellMatrix[row][col].isAdjacent = false;
        }
      }
      
      this.setData({
        cellMatrix,
        selectedCells: [],
        selectedResourceId: null,
        selectedResourceName: '',
        selectedTimeSlots: [],
        showBottomForm: false
      });
    },

    // ==================== 已预约单元格点击 ====================
    onReservedCellTap(e) {
      const { row, col } = e.currentTarget.dataset;
      const cell = this.data.cellMatrix[row][col];
      this.showReservationDetail(row, col, cell);
    },

    showReservationDetail(row, col, cell) {
      if (!cell || !cell.reservationInfo) return;
      
      const info = cell.reservationInfo;
      const moreInfo = info.moreInfo || {};
      
      this.setData({
        showDetailModal: true,
        detailInfo: {
          operateType: info.operateType,
          operateTypeText: OPERATE_TYPE_MAP[info.operateType]?.text || '未知类型',
          startTime: info.startTime.substring(0, 5),
          endTime: info.endTime.substring(0, 5),
          userPhone: moreInfo.userPhone || '',
          remark: moreInfo.remark || ''
        }
      });
    },

    hideDetailModal() {
      this.setData({ showDetailModal: false });
    },

    // ==================== 资源详情 ====================
    showResourceDetail(e) {
      const resource = e.currentTarget.dataset.resource;
      this.setData({ showResourceModal: true, resourceDetailInfo: resource });
    },

    hideResourceModal() {
      this.setData({ showResourceModal: false });
    },

    // ==================== 底部表单操作 ====================
    handleOfflineOccupy() {
      const form = this.buildFormData('occupy');
      console.log('===== 线下占用表单 =====');
      console.log('resourceId:', form.resourceId);
      console.log('reservationDate:', form.reservationDate);
      console.log('timeSlots:', form.timeSlots);
      console.log('customerPhone: (需用户输入)');
      console.log('remark: (可选)');
      console.log('========================');
      
      wx.showToast({ title: '表单已打印到控制台', icon: 'none' });
    },

    handleResourceDisable() {
      const form = this.buildFormData('disable');
      console.log('===== 资源停用表单 =====');
      console.log('resourceId:', form.resourceId);
      console.log('reservationDate:', form.reservationDate);
      console.log('timeSlots:', form.timeSlots);
      console.log('reason: (需用户输入, 5-100字符)');
      console.log('========================');
      
      wx.showToast({ title: '表单已打印到控制台', icon: 'none' });
    },

    buildFormData(type) {
      const { selectedResourceId, selectedTimeSlots, selectedQueryDate } = this.data;
      
      const sortedSlots = [...selectedTimeSlots].sort((a, b) => {
        const aStart = a.split('-')[0];
        const bStart = b.split('-')[0];
        return aStart.localeCompare(bStart);
      });
      
      return {
        resourceId: selectedResourceId,
        reservationDate: selectedQueryDate,
        timeSlots: sortedSlots
      };
    },

    // ==================== 切换字号配置 ====================
    toggleFontSize() {
      const newIndex = this.data.fontSizeIndex === 0 ? 1 : 0;
      const configKey = newIndex === 0 ? 'medium' : 'large';
      
      this.setData({
        fontSizeIndex: newIndex,
        sizeConfig: FONT_SIZE_CONFIGS[configKey]
      });
    },
    // ==================== 展示工具配置 ====================
    initToolsList() {
      this.filterTools('all');
    },
    /**
     * 筛选标签点击
     */
    onFilterTap(e) {
      const filter = e.currentTarget.dataset.filter;
      this.setData({ currentFilter: filter });
      this.filterTools(filter);
    },

    /**
     * 视图模式切换
     */
    onViewModeTap(e) {
      const mode = e.currentTarget.dataset.mode;
      this.setData({ viewMode: mode });
    },

    /**
     * 筛选工具列表
     */
    filterTools(filter) {
      const { allTools } = this.data;
      let filtered = [];
      
      if (filter === 'all') {
        filtered = allTools;
      } else {
        filtered = allTools.filter(tool => {
          return tool.labelsArray.includes(filter);
        });
      }
      
      this.setData({ filteredTools: filtered });
    },

    /**
     * 工具点击处理
     */
    handleToolTap(e) {
      const action = e.currentTarget.dataset.action;
      const { currentStore, allTools } = this.data;
      
      if (!currentStore) {
        this.showCustomToast('请先选择店铺', 'danger');
        return;
      }
      
      const tool = allTools.find(t => t.action === action);
      const storeId = currentStore.id;
      const storeName = encodeURIComponent(currentStore.name);
      
      switch (action) {
        case 'mobile_card':
          wx.navigateTo({
            url: `/pages/use-member-card/use-member-card?storeId=${storeId}&storeName=${storeName}`
          });
          break;
        case 'quick_card':
          wx.navigateTo({
            url: `/pages/apply-for-card/apply-for-card?storeId=${storeId}&storeName=${storeName}`
          });
          break;
        case 'member_center':
          wx.navigateTo({
            url: `/pages/member-search-center/member-search-center?storeId=${storeId}&storeName=${storeName}`
          });
          break;
        case 'card_manage':
          wx.navigateTo({
            url: `/pages/card-type-manage/card-type-manage?storeId=${storeId}&storeName=${storeName}`
          });
          break;
        case 'staff_manage':
          wx.navigateTo({
            url: `/pages/bind-employee/bind-employee?storeId=${storeId}&storeName=${storeName}`
          });
          break;
        case 'business_data':
          wx.navigateTo({
            url: `/pages/transactions-statistics/transactions-statistics?queryType=store&storeId=${storeId}&storeName=${storeName}`
          });
          break;
        case 'reservation_system':
          wx.navigateTo({
            url: `/pages/advanced-reservation/advanced-reservation?storeId=${storeId}&storeName=${storeName}`
          });
          break;
        default:
          const toolName = tool ? tool.name : action;
          wx.showToast({
            title: `${toolName}功能开发中`,
            icon: 'none',
            duration: 2000
          });
          break;
      }
    },
  })
  