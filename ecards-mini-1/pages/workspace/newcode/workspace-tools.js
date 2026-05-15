/**
 * 工作中心 - 新版工具箱 JS 逻辑
 * 需要合并到 workspace.js 和 merchant-workspace.js 的 data 和 methods 中
 */

// ==================== 工具配置数据 ====================
// 添加到 Page data 中
const TOOLS_DATA = {
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
      iconBg: 'linear-gradient(135deg, #66bb6a 0%, #2e7d32 100%)',
      implemented: true
    },
    {
      action: 'guest_account',
      name: '散客记账',
      describe: '记账工具',
      labels: '前台',
      labelsArray: ['前台'],
      icon: '/assets/workspace/guest.png',
      iconBg: 'linear-gradient(135deg, #66bb6a 0%, #2e7d32 100%)',
      implemented: false
    },
    {
      action: 'quick_card',
      name: '快捷办卡',
      describe: '为用户快速办理电子会员卡',
      labels: '前台',
      labelsArray: ['前台'],
      icon: '/assets/workspace/card.png',
      iconBg: 'linear-gradient(135deg, #66bb6a 0%, #2e7d32 100%)',
      implemented: true
    },
    {
      action: 'member_center',
      name: '会员中心',
      describe: '多条件筛选查询会员卡的工具',
      labels: '前台',
      labelsArray: ['前台'],
      icon: '/assets/workspace/member.png',
      iconBg: 'linear-gradient(135deg, #66bb6a 0%, #2e7d32 100%)',
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
      icon: '/assets/workspace/marketing.png',
      iconBg: 'linear-gradient(135deg, #f06292 0%, #c2185b 100%)',
      implemented: true
    },
    {
      action: 'activity_marketing',
      name: '活动营销',
      describe: '创建、管理各种活动',
      labels: '经营',
      labelsArray: ['经营'],
      icon: '/assets/workspace/marketing.png',
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
      implemented: false
    }
  ]
};

// ==================== 方法 ====================
// 添加到 Page methods 中

/**
 * 初始化工具列表（在 onLoad 或 onShow 中调用）
 */
function initToolsList() {
  this.setData({
    allTools: TOOLS_DATA.allTools,
    currentFilter: 'all',
    viewMode: 'list'
  });
  this.filterTools('all');
}

/**
 * 筛选标签点击
 */
function onFilterTap(e) {
  const filter = e.currentTarget.dataset.filter;
  this.setData({ currentFilter: filter });
  this.filterTools(filter);
}

/**
 * 视图模式切换
 */
function onViewModeTap(e) {
  const mode = e.currentTarget.dataset.mode;
  this.setData({ viewMode: mode });
}

/**
 * 筛选工具列表
 */
function filterTools(filter) {
  const { allTools } = this.data;
  let filtered = [];
  
  if (filter === 'all') {
    filtered = allTools;
  } else {
    filtered = allTools.filter(tool => {
      // 检查标签是否包含筛选条件
      return tool.labelsArray.includes(filter);
    });
  }
  
  this.setData({ filteredTools: filtered });
}

/**
 * 工具点击处理
 */
function handleToolTap(e) {
  const action = e.currentTarget.dataset.action;
  const { currentStore, allTools } = this.data;
  
  if (!currentStore) {
    this.showCustomToast('请先选择店铺', 'danger');
    return;
  }
  
  const tool = allTools.find(t => t.action === action);
  const storeId = currentStore.id;
  const storeName = encodeURIComponent(currentStore.name);
  
  // 根据功能跳转到对应页面
  switch (action) {
    case 'mobile_card': // 手机号划卡
      wx.navigateTo({
        url: `/pages/use-member-card/use-member-card?storeId=${storeId}&storeName=${storeName}`
      });
      break;
    case 'quick_card': // 快捷办卡
      wx.navigateTo({
        url: `/pages/apply-for-card/apply-for-card?storeId=${storeId}&storeName=${storeName}`
      });
      break;
    case 'member_center': // 会员中心
      wx.navigateTo({
        url: `/pages/member-search-center/member-search-center?storeId=${storeId}&storeName=${storeName}`
      });
      break;
    case 'card_manage': // 卡种管理
      wx.navigateTo({
        url: `/pages/card-type-manage/card-type-manage?storeId=${storeId}&storeName=${storeName}`
      });
      break;
    case 'staff_manage': // 员工管理
      wx.navigateTo({
        url: `/pages/bind-employee/bind-employee?storeId=${storeId}&storeName=${storeName}`
      });
      break;
    case 'business_data': // 经营数据
      wx.navigateTo({
        url: `/pages/transactions-statistics/transactions-statistics?queryType=store&storeId=${storeId}&storeName=${storeName}`
      });
      break;
    default:
      // 未实现的功能
      const toolName = tool ? tool.name : action;
      wx.showToast({
        title: `${toolName}功能开发中`,
        icon: 'none',
        duration: 2000
      });
      break;
  }
}

// 导出配置和方法（供参考）
module.exports = {
  TOOLS_DATA,
  methods: {
    initToolsList,
    onFilterTap,
    onViewModeTap,
    filterTools,
    handleToolTap
  }
};
