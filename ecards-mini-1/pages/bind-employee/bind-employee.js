// pages/bind-employee/bind-employee.js
const tokenManager = require('../../utils/token');
const { get, post, put } = require('../../utils/request');
const { decryptAES } = require('../../utils/encode');

Page({
  /**
   * 页面的初始数据
   */
  data: {
    // 店铺信息
    storeId: '',
    storeName: '',
    
    // 工作令牌状态
    hasWorkToken: false,
    workToken: '',
    
    // 员工列表
    employeeList: [],
    totalEmployees: 0,
    pageNum: 1,
    pageSize: 20,
    loading: false,
    hasMore: true,
    
    // 员工详情弹窗
    showDetailModal: false,
    currentEmployee: null,
    isEditing: false,
    editForm: {
      staffName: '',
      remark: ''
    },
    
    // 添加员工弹窗
    showAddModal: false,
    addForm: {
      staffPhone: '',
      staffInviteCode: '',
      staffRole: 'STAFF',
      staffName: '',
      remark: ''
    },
    
    // 角色选项
    roleOptions: [
      { value: 'STAFF', label: '普通员工' },
      { value: 'STORE_MANAGER', label: '店长' }
    ],
    
    // 添加员工步骤表单
    addSteps: ['基本信息', '角色权限', '确认提交'],
    addCurrentStep: 0,
    scrollTop: 0,  // 控制scroll-view滚动位置
    
    // 权限选项配置
    permissionOptions: {
      manager: [
        { key: 'staff_add', label: '员工添加', checked: false }
      ],
      employee: [
        { key: 'member_card_create', label: '会员卡创建', checked: false },
        { key: 'transaction_recharge', label: '会员卡充值', checked: false },
        { key: 'reservation_disable', label: '停用预约资源', checked: false },
        { key: 'reservation_occupy', label: '线下占用预约资源', checked: false }
      ]
    },
    
    // 权限编辑弹窗
    showPermissionModal: false,
    editPermissions: []
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    const { storeId, storeName } = options;
    
    if (!storeId) {
      this.showCustomToast('店铺ID不存在', 'danger');
      return;
    }
    
    this.setData({
      storeId,
      storeName: storeName ? decodeURIComponent(storeName) : ''
    });
    
    // 获取工作令牌
    this.loadWorkToken();
  },

  /**
   * 加载工作令牌（先从缓存获取，失败再从后端获取）
   */
  async loadWorkToken() {
    const { storeId } = this.data;
    
    if (!storeId) {
      return;
    }
    
    try {
      // 第一步：尝试从缓存获取工作令牌
      const cachedToken = await tokenManager.getWorkToken(storeId);
      
      if (cachedToken) {
        this.setData({
          hasWorkToken: true,
          workToken: cachedToken
        });
        // 获取令牌成功后加载员工列表
        this.loadEmployeeList();
        return;
      }
      
      // 第二步：缓存中没有，从后端获取
      await this.fetchWorkTokenFromServer();
      
    } catch (error) {
      this.setData({ hasWorkToken: false });
      this.showCustomToast(error.message || '获取工作令牌失败', 'danger');
    }
  },

  /**
   * 从后端请求工作令牌
   */
  async fetchWorkTokenFromServer() {
    const { storeId } = this.data;
    
    try {
      const response = await tokenManager.fetchWorkToken(storeId);
      
      // fetchWorkToken内部已经保存了令牌，这里再次调用getWorkToken获取
      if (response && response.token) {
        const workToken = await tokenManager.getWorkToken(storeId);
        
        if (workToken) {
          this.setData({
            hasWorkToken: true,
            workToken: workToken
          });
          // 获取令牌成功后加载员工列表
          this.loadEmployeeList();
          return;
        }
      }
      
      this.setData({ hasWorkToken: false });
      this.showCustomToast('获取工作令牌失败', 'danger');
    } catch (error) {
      this.setData({ hasWorkToken: false });
      this.showCustomToast(error.message || '获取工作令牌失败', 'danger');
    }
  },

  // ==================== 员工列表查询 ====================
  
  /**
   * 加载员工列表
   */
  async loadEmployeeList(isRefresh = true) {
    if (this.data.loading) return;
    
    const { storeId, workToken, pageNum, pageSize } = this.data;
    
    if (!workToken) {
      this.showCustomToast('请先获取工作令牌', 'danger');
      return;
    }
    
    this.setData({ loading: true });
    
    try {
      const res = await get(
        '/v1/staffs',
        {
          'Content-Type': 'application/json',
          'Authorization': workToken
        },
        {
          storeId: storeId,
          pageNum: isRefresh ? 1 : pageNum,
          pageSize: pageSize
        }
      );
      
      console.log('员工列表响应:', res);
      
      if (res.code !== 200) {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
        this.setData({ loading: false });
        return;
      }
      
      const { list = [], total = 0, pageNum: currentPage } = res.data || {};
      
      // 解密手机号
      const decryptedList = list.map(item => ({
        ...item,
        staffPhone: item.staffPhone ? this.safeDecrypt(item.staffPhone) : '暂无'
      }));
      
      if (isRefresh) {
        this.setData({
          employeeList: decryptedList,
          totalEmployees: total,
          pageNum: 2,
          hasMore: decryptedList.length < total,
          loading: false
        });
      } else {
        const newList = [...this.data.employeeList, ...decryptedList];
        this.setData({
          employeeList: newList,
          pageNum: currentPage + 1,
          hasMore: newList.length < total,
          loading: false
        });
      }
      
    } catch (error) {
      console.error('加载员工列表失败:', error);
      this.showCustomToast('加载员工列表失败', 'danger');
      this.setData({ loading: false });
    }
  },
  
  /**
   * 安全解密手机号
   */
  safeDecrypt(encrypted) {
    try {
      return decryptAES(encrypted);
    } catch (e) {
      console.warn('解密手机号失败:', e);
      return encrypted; // 返回原值
    }
  },
  
  /**
   * 下拉刷新
   */
  async onPullDownRefresh() {
    await this.loadEmployeeList(true);
    wx.stopPullDownRefresh();
  },
  
  /**
   * 上拉加载更多
   */
  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadEmployeeList(false);
    }
  },

  // ==================== 员工详情查询 ====================
  
  /**
   * 点击员工卡片，查看详情
   */
  async onEmployeeClick(e) {
    const { staffid } = e.currentTarget.dataset;
    
    if (!staffid) {
      this.showCustomToast('员工ID不存在', 'danger');
      return;
    }
    
    await this.loadEmployeeDetail(staffid);
  },
  
  /**
   * 加载员工详情
   */
  async loadEmployeeDetail(staffId) {
    const { storeId, workToken } = this.data;
    
    if (!workToken) {
      this.showCustomToast('请先获取工作令牌', 'danger');
      return;
    }
    
    wx.showLoading({ title: '加载中...' });
    
    try {
      const res = await get(
        `/v1/staffs/${staffId}`,
        {
          'Content-Type': 'application/json',
          'Authorization': workToken
        },
        { storeId: storeId }
      );
      
      console.log('员工详情响应:', res);
      
      wx.hideLoading();
      
      if (res.code !== 200) {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
        return;
      }
      
      const employee = res.data;
      // 解密手机号
      if (employee.staffPhone) {
        employee.staffPhone = this.safeDecrypt(employee.staffPhone);
      }
      
      this.setData({
        currentEmployee: employee,
        showDetailModal: true,
        isEditing: false,
        editForm: {
          staffName: employee.staffName || '',
          remark: employee.remark || ''
        }
      });
      
    } catch (error) {
      wx.hideLoading();
      console.error('加载员工详情失败:', error);
      this.showCustomToast('加载员工详情失败', 'danger');
    }
  },
  
  /**
   * 关闭详情弹窗
   */
  closeDetailModal() {
    this.setData({
      showDetailModal: false,
      currentEmployee: null,
      isEditing: false
    });
  },
  
  /**
   * 切换编辑模式
   */
  toggleEditMode() {
    const { isEditing, currentEmployee } = this.data;
    
    if (isEditing) {
      // 取消编辑，恢复原值
      this.setData({
        isEditing: false,
        editForm: {
          staffName: currentEmployee.staffName || '',
          remark: currentEmployee.remark || ''
        }
      });
    } else {
      this.setData({ isEditing: true });
    }
  },
  
  /**
   * 编辑表单输入
   */
  onEditInput(e) {
    const { field } = e.currentTarget.dataset;
    const { value } = e.detail;
    
    this.setData({
      [`editForm.${field}`]: value
    });
  },

  // ==================== 员工信息修改 ====================
  
  /**
   * 保存员工信息修改
   */
  async saveEmployeeInfo() {
    const { currentEmployee, editForm, storeId, workToken } = this.data;
    
    if (!currentEmployee || !currentEmployee.staffId) {
      this.showCustomToast('员工信息不存在', 'danger');
      return;
    }
    
    // 检查是否有修改
    if (editForm.staffName === (currentEmployee.staffName || '') && 
        editForm.remark === (currentEmployee.remark || '')) {
      this.showCustomToast('未做任何修改', 'danger');
      return;
    }
    
    wx.showLoading({ title: '保存中...' });
    
    try {
      const requestBody = { storeId };
      
      // 只传递有修改的字段
      if (editForm.staffName !== (currentEmployee.staffName || '')) {
        requestBody.staffName = editForm.staffName;
      }
      if (editForm.remark !== (currentEmployee.remark || '')) {
        requestBody.remark = editForm.remark;
      }
      
      const res = await put(
        `/v1/staffs/${currentEmployee.staffId}`,
        {
          'Content-Type': 'application/json',
          'Authorization': workToken
        },
        requestBody
      );
      
      console.log('修改员工信息响应:', res);
      
      wx.hideLoading();
      
      if (res.code !== 200) {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
        return;
      }
      
      this.showCustomToast('修改成功', 'success');
      
      // 更新当前员工信息
      this.setData({
        isEditing: false,
        'currentEmployee.staffName': editForm.staffName,
        'currentEmployee.remark': editForm.remark
      });
      
      // 刷新员工列表
      this.loadEmployeeList(true);
      
    } catch (error) {
      wx.hideLoading();
      console.error('修改员工信息失败:', error);
      this.showCustomToast('修改员工信息失败', 'danger');
    }
  },

  // ==================== 员工添加（绑定） ====================
  
  /**
   * 打开添加员工弹窗
   */
  onBindEmployee() {
    if (!this.data.hasWorkToken) {
      this.showCustomToast('请先获取工作令牌', 'danger');
      return;
    }
    
    // 重置权限选项
    const resetPermissions = {
      manager: this.data.permissionOptions.manager.map(item => ({ ...item, checked: false })),
      employee: this.data.permissionOptions.employee.map(item => ({ ...item, checked: false }))
    };
    
    this.setData({
      showAddModal: true,
      addCurrentStep: 0,
      addForm: {
        staffPhone: '',
        staffInviteCode: '',
        staffRole: 'STAFF',
        staffName: '',
        remark: ''
      },
      permissionOptions: resetPermissions
    });
  },
  
  /**
   * 关闭添加员工弹窗
   */
  closeAddModal() {
    this.setData({
      showAddModal: false,
      addCurrentStep: 0,
      addForm: {
        staffPhone: '',
        staffInviteCode: '',
        staffRole: 'STAFF',
        staffName: '',
        remark: ''
      }
    });
  },
  
  /**
   * 添加表单输入
   */
  onAddInput(e) {
    const { field } = e.currentTarget.dataset;
    const { value } = e.detail;
    
    this.setData({
      [`addForm.${field}`]: value
    });
  },
  
  /**
   * 角色选择（单选器）
   */
  onRoleChange(e) {
    const value = e.currentTarget.dataset.value;
    this.setData({
      'addForm.staffRole': value
    });
  },
  
  /**
   * 步骤变化事件
   */
  onAddStepChange(e) {
    this.setData({ addCurrentStep: e.detail.currentStep });
  },
  
  /**
   * 进入下一步
   */
  goToNextStep() {
    const { addForm, addCurrentStep } = this.data;
    
    // 验证第一步表单
    if (addCurrentStep === 0) {
      if (!addForm.staffName || addForm.staffName.trim() === '') {
        this.showCustomToast('请输入员工姓名', 'danger');
        return;
      }
      
      if (!addForm.staffPhone || addForm.staffPhone.length !== 11) {
        this.showCustomToast('请输入正确的11位手机号', 'danger');
        return;
      }
      
      if (!addForm.staffInviteCode) {
        this.showCustomToast('请输入邀请码', 'danger');
        return;
      }
    }
    
    // 前进到下一步，重置滚动位置
    this.setData({ 
      addCurrentStep: addCurrentStep + 1,
      scrollTop: 0
    });
  },
  
  /**
   * 返回上一步
   */
  goToPrevStep() {
    const { addCurrentStep } = this.data;
    if (addCurrentStep > 0) {
      this.setData({ 
        addCurrentStep: addCurrentStep - 1,
        scrollTop: 0
      });
    }
  },
  
  /**
   * 权限复选框变化（添加员工）
   */
  onPermissionChange(e) {
    const { key } = e.currentTarget.dataset;
    const { addForm, permissionOptions } = this.data;
    const roleKey = addForm.staffRole === 'STORE_MANAGER' ? 'manager' : 'employee';
    
    const permissions = permissionOptions[roleKey].map(item => ({
      ...item,
      checked: item.key === key ? !item.checked : item.checked
    }));
    
    this.setData({
      [`permissionOptions.${roleKey}`]: permissions
    });
    
    // 更新已选权限名称
    this.updateSelectedPermissionNames();
  },
  
  /**
   * 更新已选权限名称（用于信息确认展示）
   */
  updateSelectedPermissionNames() {
    const { addForm, permissionOptions } = this.data;
    const roleKey = addForm.staffRole === 'STORE_MANAGER' ? 'manager' : 'employee';
    const selectedNames = permissionOptions[roleKey]
      .filter(item => item.checked)
      .map(item => item.label);
    
    this.setData({
      selectedPermissionNames: selectedNames.length > 0 ? selectedNames.join('、') : ''
    });
  },
  
  /**
   * 构建权限JSON字符串
   */
  buildPermissionJson() {
    const { addForm, permissionOptions } = this.data;
    const roleKey = addForm.staffRole === 'STORE_MANAGER' ? 'manager' : 'employee';
    const selectedPermissions = permissionOptions[roleKey]
      .filter(item => item.checked)
      .map(item => item.key);
    
    return JSON.stringify({ [roleKey]: selectedPermissions });
  },
  
  /**
   * 提交添加员工
   */
  async submitAddEmployee() {
    const { addForm, storeId, workToken } = this.data;
    
    // 表单验证 - 员工姓名必填
    if (!addForm.staffName || addForm.staffName.trim() === '') {
      this.showCustomToast('请输入员工姓名', 'danger');
      return;
    }
    
    if (!addForm.staffPhone || addForm.staffPhone.length !== 11) {
      this.showCustomToast('请输入正确的11位手机号', 'danger');
      return;
    }
    
    if (!addForm.staffInviteCode) {
      this.showCustomToast('请输入邀请码', 'danger');
      return;
    }
    
    wx.showLoading({ title: '添加中...' });
    
    try {
      const requestBody = {
        storeId: storeId,
        staffPhone: addForm.staffPhone,
        staffInviteCode: addForm.staffInviteCode,
        staffRole: addForm.staffRole,
        staffName: addForm.staffName,
        staffPermission: this.buildPermissionJson()
      };
      
      // 可选字段
      if (addForm.remark) {
        requestBody.remark = addForm.remark;
      }
      
      console.log('添加员工请求体:', requestBody);
      
      const res = await post(
        '/v1/staffs',
        {
          'Content-Type': 'application/json',
          'Authorization': workToken
        },
        requestBody
      );
      
      console.log('添加员工响应:', res);
      
      wx.hideLoading();
      
      if (res.code !== 200) {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
        return;
      }
      
      this.showCustomToast('员工添加成功', 'success');
      
      // 关闭弹窗并刷新列表
      this.closeAddModal();
      this.loadEmployeeList(true);
      
    } catch (error) {
      wx.hideLoading();
      console.error('添加员工失败:', error);
      this.showCustomToast('添加员工失败', 'danger');
    }
  },
  
  // ==================== 权限修改 ====================
  
  /**
   * 打开权限编辑弹窗
   */
  openPermissionModal() {
    const { currentEmployee, permissionOptions } = this.data;
    const roleKey = currentEmployee.staffRole === 'STORE_MANAGER' ? 'manager' : 'employee';
    
    // 解析当前权限
    let currentPerms = [];
    try {
      const permObj = JSON.parse(currentEmployee.staffPermission || '{}');
      currentPerms = permObj[roleKey] || [];
    } catch (e) {
      console.warn('解析权限失败:', e);
    }
    
    // 设置复选框状态
    const editPermissions = permissionOptions[roleKey].map(item => ({
      ...item,
      checked: currentPerms.includes(item.key)
    }));
    
    this.setData({
      showPermissionModal: true,
      editPermissions
    });
  },
  
  /**
   * 关闭权限编辑弹窗
   */
  closePermissionModal() {
    this.setData({ 
      showPermissionModal: false,
      editPermissions: []
    });
  },
  
  /**
   * 权限编辑复选框变化
   */
  onEditPermissionChange(e) {
    const { key } = e.currentTarget.dataset;
    const editPermissions = this.data.editPermissions.map(item => ({
      ...item,
      checked: item.key === key ? !item.checked : item.checked
    }));
    this.setData({ editPermissions });
  },
  
  /**
   * 保存权限修改
   */
  async savePermission() {
    const { currentEmployee, editPermissions, storeId, workToken } = this.data;
    const roleKey = currentEmployee.staffRole === 'STORE_MANAGER' ? 'manager' : 'employee';
    
    const selectedPermissions = editPermissions
      .filter(item => item.checked)
      .map(item => item.key);
    
    const newPermission = JSON.stringify({ [roleKey]: selectedPermissions });
    
    wx.showLoading({ title: '保存中...' });
    
    try {
      const res = await put(
        `/v1/staffs/${currentEmployee.staffId}/permission`,
        {
          'Content-Type': 'application/json',
          'Authorization': workToken
        },
        {
          storeId,
          newPermission
        }
      );
      
      console.log('修改权限响应:', res);
      
      wx.hideLoading();
      
      if (res.code !== 200) {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
        return;
      }
      
      this.showCustomToast('权限修改成功', 'success');
      
      // 更新当前员工权限
      this.setData({
        'currentEmployee.staffPermission': newPermission
      });
      
      this.closePermissionModal();
      this.loadEmployeeList(true);
      
    } catch (error) {
      wx.hideLoading();
      console.error('修改权限失败:', error);
      this.showCustomToast('修改权限失败', 'danger');
    }
  },

  // ==================== 通用方法 ====================
  
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
   * 阻止事件冒泡
   */
  stopPropagation() {
    // 空函数，用于阻止事件冒泡
  },
  
  /**
   * 获取角色显示文本
   */
  getRoleText(role) {
    const roleMap = {
      'STAFF': '普通员工',
      'STORE_MANAGER': '店长'
    };
    return roleMap[role] || role;
  },

  /**
   * 页面卸载时清理
   */
  onUnload() {
    // 页面卸载时无需特殊清理
  }
})
