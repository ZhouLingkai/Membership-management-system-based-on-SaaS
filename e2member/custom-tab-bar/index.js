Component({
  data: {
    selected: 0,
    color: "#7A7E83",
    selectedColor: "#397dd6",
    list: []
  },

  attached() {
    // 立即初始化，减少闪烁
    this.updateTabBar();
  },

  methods: {
    // 更新 tabBar 配置
    updateTabBar() {
      const app = getApp();
      if (app && app.globalData && app.globalData.tabBarConfig) {
        const newList = app.globalData.tabBarConfig;
        const newSelected = app.globalData.selectedTabIndex || 0;
        
        // 检查配置是否发生变化（比较第一项的pagePath来判断是否切换了用户类型）
        const currentFirstPath = this.data.list[0]?.pagePath || '';
        const newFirstPath = newList[0]?.pagePath || '';
        const configChanged = currentFirstPath !== newFirstPath;
        
        // 如果配置变化或选中项变化，则更新
        const needUpdate = configChanged || 
                          this.data.list.length !== newList.length || 
                          this.data.selected !== newSelected;
        
        if (needUpdate) {
          console.log('[TabBar] 更新配置:', { 
            configChanged, 
            oldPath: currentFirstPath, 
            newPath: newFirstPath,
            selected: newSelected 
          });
          this.setData({
            list: newList,
            selected: newSelected
          });
        }
      }
    },

    // 切换 tab
    switchTab(e) {
      // 安全检查
      if (!e || !e.currentTarget || !e.currentTarget.dataset) {
        console.error('switchTab: 无效的事件对象');
        return;
      }

      const data = e.currentTarget.dataset;
      const url = data.path;
      const index = parseInt(data.index);
      
      // 参数验证
      if (!url || isNaN(index)) {
        console.error('switchTab: 无效的参数', { url, index });
        return;
      }

      // 检查页面权限
      if (!this.checkPagePermission(url)) {
        wx.showToast({
          title: '暂无访问权限',
          icon: 'none'
        });
        return;
      }

      // 防止重复点击
      if (this.data.selected === index) {
        return;
      }

      const app = getApp();
      if (!app || !app.setTabBarSelected) {
        console.error('switchTab: app 或方法不存在');
        return;
      }
      
      // 使用统一的状态管理方法
      app.setTabBarSelected(index, true);

      // 执行页面跳转
      wx.switchTab({
        url: '/' + url,
        fail: (error) => {
          console.error('switchTab: 页面跳转失败', error);
          // 跳转失败时恢复原状态
          if (app.getCurrentTabIndex) {
            const currentIndex = app.getCurrentTabIndex();
            app.setTabBarSelected(currentIndex, true);
          }
        }
      });
    },

    // 检查页面访问权限
    checkPagePermission(pagePath) {
      const app = getApp();
      const actualUserType = app.globalData.actualUserType;
      const currentUserType = app.globalData.currentUserType;

      // 如果是商家页面，但用户实际不是商家，则无权限
      if (pagePath.includes('merchant-home') && actualUserType !== 2) {
        return false;
      }

      // 如果是后台管理页面，但用户实际不是商家，则无权限  
      if (pagePath.includes('admin-manage') && actualUserType !== 2) {
        return false;
      }

      // 工作台页面：普通用户、商家、员工都可以访问
      // 移除工作台的权限限制

      return true;
    }
  }
});
