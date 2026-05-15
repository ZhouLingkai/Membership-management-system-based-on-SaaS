// pages/card-type-update/card-type-update.js
const { get, post } = require('../../../utils/request');
const tokenManager = require('../../../utils/token');
const cardHelper = require('../../util/card-helper');
const cardCanvasMixin = require('../../util/card-canvas-mixin');
const ossUtil = require('../../../utils/oss');

Page({
  data: {
    // ========== Tab控制 ==========
    currentTab: 'basic',  // 'basic' | 'design'
    
    // ========== 基础信息 ==========
    cardTypeId: '',
    storeId: '',
    storeName: '',
    cardType: 1,  // 卡种类型值: 1=余额卡, 2=次数卡, 3=时效卡, 4=积分卡
    cardTypeName: '',  // 卡种类型名称（余额卡/次数卡/时效卡/积分卡）
    
    // 预设项目配置（根据卡种类型动态变化）
    presetAmountLabel: '金额',  // 金额字段标签
    presetAmountPlaceholder: '请输入金额',  // 金额字段placeholder
    showPresetRecharge: true,  // 是否显示预设充值项目
    showPresetCost: true,  // 是否显示预设消费项目
    
    // 原始数据（用于对比）
    originalData: null,
    oldCardBgc: '',  // 旧卡片背景
    oldCardMask: '',  // 旧卡片蒙版
    
    // 表单数据
    form: {
      cardTypeName: '',
      description: '',
      autoNotify: 1,
      crossStore: 0
    },
    
    // 预设充值/消费项目
    presetRechargeList: [],
    presetCostList: [],
    originalPresetRecharge: '[]',  // 统一为JSON字符串格式
    originalPresetCost: '[]',  // 统一为JSON字符串格式
    
    // 是否有基础信息修改
    hasBasicChanges: false,
    
    // ========== 背景设计 ==========
    bgType: 'gradient',
    bgGradient: 'darkblack',
    bgGradientDirection: 'diagonal',
    bgAdvanced: 'black',
    customBgUrl: '',
    customBgTempPath: '',
    bgGradientStyle: '',
    
    // ========== 卡面设计 ==========
    titleText: '',
    titlePosition: 'left-top',
    titleColorType: 'solid',
    titleColor: '#fafafa',
    titleGradient: 'gold',
    titleFont: 'default',
    titleSize: 'medium',
    
    showVip: false,
    vipPosition: 'center',
    vipColorType: 'solid',
    vipColor: '#fafafa',
    vipGradient: 'gold',
    vipAdvanced: '3D',
    vipFont: 'default',
    vipSize: 'medium',
    
    showPattern: false,
    selectedPattern: 'dragon',
    patternPosition: 'right-bottom',
    patternSize: 'medium',
    
    // ========== 预览控制 ==========
    showPreview: true,
    showBackground: true,
    maskImageUrl: '',
    
    // ========== Canvas ==========
    canvas: null,
    ctx: null,
    
    // ========== OSS ==========
    stsToken: null,
    randomCode: '',
    
    // ========== 字体选项 ==========
    fontOptions: [],
    
    // 卡种类型名称映射
    cardTypeNames: {
      0: '[未知错误]',
      1: '余额卡',
      2: '次数卡',
      3: '时效卡',
      4: '积分卡'
    },
    // 卡种类型图标映射
    cardTypeIcons: {
      0: '',
      1: '💰',
      2: '🔢',
      3: '⏰',
      4: '⭐'
    },
    cardTypeIcon: '💰'
  },

  onLoad(options) {
    const { cardTypeId, storeId, storeName } = options;
    this.setData({
      cardTypeId: cardTypeId || '',
      storeId: storeId || '',
      storeName: decodeURIComponent(storeName || '未知店铺')
    });
    
    // 初始化平台配置
    this.initPlatform();
    
    // 并发加载卡种详情（不阻塞渲染）
    this.loadCardTypeDetail();
    
  },

  async onShow() {
    // 获取STS令牌
    try {
      const stsToken = await ossUtil.getStsCredentials('card');
      this.setData({ stsToken });
    } catch (error) {
      console.error('[卡种修改] 获取STS令牌失败:', error);
    }
    
    // 生成随机码
    this.generateRandomCode();
  },

  /**
   * 页面首次渲染完成
   */
  onReady() {
    // 初始化Canvas（仅在设计Tab时），延时500ms确保DOM完全渲染
    if (this.data.currentTab === 'design') {
      setTimeout(() => {
        this.initCanvas();
      }, 500);
    }
  },

  /**
   * 初始化平台相关配置
   */
  async initPlatform() {
    try {
      const deviceRes = await wx.getDeviceInfo();
      const { platform } = deviceRes;
      let fontOptions = [];
      
      if (platform === 'ios') {
        fontOptions = [
          { label: '默认', value: 'default' },
          { label: '苹方', value: 'PingFang SC' },
          { label: '华文黑体', value: 'STHeiti' }
        ];
      } else if (platform === 'android') {
        fontOptions = [
          { label: '默认', value: 'default' },
          { label: '思源黑体', value: 'Noto Sans CJK' },
          { label: '华文黑体', value: 'STHeiti' }
        ];
      } else {
        fontOptions = [
          { label: '默认', value: 'default' },
          { label: '微软雅黑', value: 'Microsoft YaHei' },
          { label: '黑体', value: 'SimHei' }
        ];
      }
      
      this.setData({ fontOptions });
    } catch (error) {
      console.error('[卡种修改] 获取平台信息失败:', error);
      this.setData({
        fontOptions: [
          { label: '默认', value: 'default' },
          { label: '微软雅黑', value: 'Microsoft YaHei' },
          { label: '黑体', value: 'SimHei' }
        ]
      });
    }
  },

  /**
   * 生成随机码
   */
  generateRandomCode() {
    const chars = '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
    let code = '';
    for (let i = 0; i < 6; i++) {
      code += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    this.setData({ randomCode: code });
  },

  /**
   * Tab切换
   */
  switchTab(e) {
    const tab = e.currentTarget.dataset.tab;
    this.setData({ currentTab: tab });
    
    // 切换到设计Tab时初始化Canvas
    if (tab === 'design' && !this.data.canvas) {
      setTimeout(() => {
        this.initCanvas();
        this.updateBgGradientStyle();
        this.autoGenerateMask();
      }, 100);
    }
  },

  /**
   * 加载卡种详情
   */
  async loadCardTypeDetail() {
    try {
      wx.showLoading({ title: '加载中...' });
      
      const { cardTypeId, storeId } = this.data;
      const workToken = await tokenManager.getWorkToken(storeId);
      
      if (!workToken) {
        this.showCustomToast('工作令牌无效', 'danger');
        wx.hideLoading();
        return;
      }
      
      const res = await get(
        '/v1/member-card-types/detail-query',
        { 'Authorization': workToken },
        { cardTypeId, storeId }
      );

      console.log('[卡种详情]', res);
      
      if (res.code === 200) {
        const cardType = res.data;
        const cardTypeValue = cardType.cardTtype || 1;
        
        // 保存原始数据
        const originalData = {
          cardTypeName: cardType.cardTypeName,
          description: cardType.description || '',
          autoNotify: cardType.autoNotify,
          crossStore: cardType.crossStore
        };
        
        // 解析预设项目
        let presetRechargeList = [];
        let presetCostList = [];
        try {
          if (cardType.presetRecharge) {
            presetRechargeList = JSON.parse(cardType.presetRecharge);
          }
          if (cardType.presetCost) {
            presetCostList = JSON.parse(cardType.presetCost);
          }
        } catch (e) {
          console.error('解析预设项目失败:', e);
        }
        
        // 根据卡种类型设置预设项目配置
        const presetConfig = this.getPresetConfigByCardType(cardTypeValue);
        
        // 设置表单数据和旧卡片数据
        // 原始数据统一为JSON字符串格式，确保比较时格式一致
        this.setData({
          originalData: originalData,
          form: { ...originalData },
          cardType: cardTypeValue,
          cardTypeName: this.data.cardTypeNames[cardTypeValue] || '会员卡',
          cardTypeIcon: this.data.cardTypeIcons[cardTypeValue] || '💳',
          oldCardBgc: cardType.cardBgc || '',
          oldCardMask: cardType.cardMask || '',
          presetRechargeList: presetRechargeList,
          presetCostList: presetCostList,
          originalPresetRecharge: JSON.stringify(presetRechargeList),
          originalPresetCost: JSON.stringify(presetCostList),
          ...presetConfig
        });
        
        // 尝试回显cardBgc（如果是渐变色或高级图片）
        this.tryEchoCardBgc(cardType.cardBgc);
      } else {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
      }
      
      wx.hideLoading();
    } catch (error) {
      console.error('加载卡种详情失败:', error);
      this.showCustomToast('加载失败', 'danger');
      wx.hideLoading();
    }
    console.log(this.data)
  },

  /**
   * 根据卡种类型获取预设项目配置
   */
  getPresetConfigByCardType(cardType) {
    // 1=余额卡, 2=次数卡, 3=时效卡, 4=积分卡
    const configs = {
      1: { // 余额卡
        presetAmountLabel: '金额',
        presetAmountPlaceholder: '请输入金额',
        showPresetRecharge: true,
        showPresetCost: true
      },
      2: { // 次数卡
        presetAmountLabel: '次数',
        presetAmountPlaceholder: '请输入次数',
        showPresetRecharge: true,
        showPresetCost: true
      },
      3: { // 时效卡
        presetAmountLabel: '时长',
        presetAmountPlaceholder: '请输入天数',
        showPresetRecharge: true,
        showPresetCost: false
      },
      4: { // 积分卡
        presetAmountLabel: '积分',
        presetAmountPlaceholder: '请输入积分',
        showPresetRecharge: false,
        showPresetCost: false
      }
    };
    return configs[cardType] || configs[1];
  },

  /**
   * 尝试回显cardBgc（渐变色或高级图片）
   */
  tryEchoCardBgc(cardBgc) {
    if (!cardBgc) return;
    
    const firstChar = cardBgc.charAt(0);
    
    // 渐变色（数字开头，格式：{direct}_{seq}）
    if (/^\d/.test(firstChar)) {
      const parts = cardBgc.split('_');
      if (parts.length === 2) {
        const direct = parseInt(parts[0]);
        const seq = parseInt(parts[1]);
        
        // 方向映射
        const directionMap = { 1: 'horizontal', 2: 'vertical', 3: 'diagonal' };
        const direction = directionMap[direct] || 'diagonal';
        
        // 渐变色映射
        const gradientMap = {
          1: 'darkblack', 2: 'gold', 3: 'winered', 4: 'originalgold',
          5: 'blue', 6: 'red', 7: 'orange', 8: 'purple', 9: 'sunset'
        };
        const gradient = gradientMap[seq] || 'darkblack';
        
        this.setData({
          bgType: 'gradient',
          bgGradient: gradient,
          bgGradientDirection: direction
        });
      }
    }
    // 本地高级图片（以bg开头）
    else if (cardBgc.startsWith('bg')) {
      const name = cardBgc.replace('bg_', '').replace('.png', '');
      this.setData({
        bgType: 'advanced',
        bgAdvanced: name
      });
    }
    // OSS图片或其他情况不回显
  },

  /**
   * 表单输入
   */
  onInput(e) {
    const { field } = e.currentTarget.dataset;
    const { value } = e.detail;
    this.setData({
      [`form.${field}`]: value
    }, () => {
      this.checkBasicChanges();
    });
  },

  /**
   * 标题文字输入
   */
  onTitleTextInput(e) {
    this.setData({
      titleText: e.detail.value
    }, () => {
      this.autoGenerateMask();
    });
  },

  /**
   * 单选框变更
   */
  onRadioChange(e) {
    const { field, value } = e.currentTarget.dataset;
    
    // 基础信息表单字段
    if (field === 'autoNotify' || field === 'crossStore') {
      this.setData({
        [`form.${field}`]: value
      }, () => {
        this.checkBasicChanges();
      });
    } else {
      // 卡面设计字段
      this.setData({
        [field]: value
      }, () => {
        if (field === 'bgGradientDirection') {
          this.updateBgGradientStyle();
        }
        this.autoGenerateMask();
      });
    }
  },

  /**
   * 渐变色变更
   */
  onGradientChange(e) {
    const { field, value } = e.currentTarget.dataset;
    this.setData({
      [field]: value
    }, () => {
      this.updateBgGradientStyle();
      this.autoGenerateMask();
    });
  },

  /**
   * 颜色选择变更
   */
  onColorChange(e) {
    const { field, value } = e.currentTarget.dataset;
    this.setData({
      [field]: value
    }, () => {
      this.autoGenerateMask();
    });
  },

  /**
   * 高级背景变更
   */
  onAdvancedBgChange(e) {
    const value = e.currentTarget.dataset.value;
    this.setData({
      bgAdvanced: value
    });
  },

  /**
   * 高级VIP变更
   */
  onAdvancedVipChange(e) {
    const value = e.currentTarget.dataset.value;
    this.setData({
      vipAdvanced: value
    }, () => {
      this.autoGenerateMask();
    });
  },

  /**
   * VIP开关变更
   */
  onVipSwitchChange(e) {
    this.setData({
      showVip: e.detail.value
    }, () => {
      this.autoGenerateMask();
    });
  },

  /**
   * 图案开关变更
   */
  onPatternSwitchChange(e) {
    this.setData({
      showPattern: e.detail.value
    }, () => {
      this.autoGenerateMask();
    });
  },

  /**
   * 选择图案
   */
  onSelectPattern(e) {
    const pattern = e.currentTarget.dataset.pattern;
    this.setData({
      selectedPattern: pattern
    }, () => {
      this.autoGenerateMask();
    });
  },

  /**
   * 预览开关
   */
  onTogglePreview() {
    this.setData({
      showPreview: !this.data.showPreview
    });
  },

  /**
   * 背景显示开关
   */
  onToggleBackground() {
    this.setData({
      showBackground: !this.data.showBackground
    });
  },

  /**
   * 上传自定义背景
   */
  async onUploadCustomBg() {
    try {
      const res = await new Promise((resolve, reject) => {
        wx.chooseMedia({
          count: 1,
          mediaType: ['image'],
          sourceType: ['album', 'camera'],
          success: resolve,
          fail: reject
        });
      });
      
      if (res.tempFiles && res.tempFiles.length > 0) {
        const tempFilePath = res.tempFiles[0].tempFilePath;
        this.setData({
          customBgUrl: tempFilePath,
          customBgTempPath: tempFilePath
        });
        this.showCustomToast('背景图片已选择', 'success');
      }
    } catch (error) {
      console.error('[卡种修改] 选择背景图片失败:', error);
    }
  },

  /**
   * 预设项目输入
   */
  onPresetInput(e) {
    const { index, field, type } = e.currentTarget.dataset;
    const value = e.detail.value;
    const listKey = type === 'recharge' ? 'presetRechargeList' : 'presetCostList';
    const list = [...this.data[listKey]];
    
    if (field === 'amount') {
      list[index][field] = parseFloat(value) || 0;
    } else {
      list[index][field] = value;
    }
    
    this.setData({ [listKey]: list }, () => {
      this.checkBasicChanges();
    });
  },

  /**
   * 添加充值项目
   */
  addRechargeItem() {
    const list = [...this.data.presetRechargeList];
    list.push({ itemName: '', itemDesc: '', amount: 0 });
    this.setData({ presetRechargeList: list }, () => {
      this.checkBasicChanges();
    });
  },

  /**
   * 删除充值项目
   */
  removeRechargeItem(e) {
    const { index } = e.currentTarget.dataset;
    const list = [...this.data.presetRechargeList];
    list.splice(index, 1);
    this.setData({ presetRechargeList: list }, () => {
      this.checkBasicChanges();
    });
  },

  /**
   * 添加消费项目
   */
  addCostItem() {
    const list = [...this.data.presetCostList];
    list.push({ itemName: '', itemDesc: '', amount: 0 });
    this.setData({ presetCostList: list }, () => {
      this.checkBasicChanges();
    });
  },

  /**
   * 删除消费项目
   */
  removeCostItem(e) {
    const { index } = e.currentTarget.dataset;
    const list = [...this.data.presetCostList];
    list.splice(index, 1);
    this.setData({ presetCostList: list }, () => {
      this.checkBasicChanges();
    });
  },

  /**
   * 取消
   */
  onCancel() {
    wx.navigateBack();
  },

  /**
   * 提交基础信息修改
   */
  async submitBasicInfo() {
    // 验证表单
    if (!this.validateForm()) {
      return;
    }

    // 获取修改的字段
    const changedFields = this.getChangedFields();
    
    if (Object.keys(changedFields).length === 0) {
      this.showCustomToast('没有修改任何内容', 'danger');
      return;
    }

    try {
      wx.showLoading({ title: '保存中...' });

      const { cardTypeId, storeId } = this.data;
      
      // 构造请求体（只包含修改的字段）
      const requestBody = {
        cardTypeId: cardTypeId,
        storeId: storeId,
        ...changedFields
      };

      const workToken = await tokenManager.getWorkToken(storeId);
      
      if (!workToken) {
        this.showCustomToast('工作令牌无效', 'danger');
        wx.hideLoading();
        return;
      }
      
      console.log('[基础信息修改] 请求体:', requestBody);
      
      const res = await post(
        '/v1/member-card-types/set',
        { 
          'Content-Type': 'application/json',
          'Authorization': workToken
        },
        requestBody
      );

      console.log('[基础信息修改] 响应:', res);
      wx.hideLoading();

      if (res.code === 200) {
        this.showCustomToast('保存成功', 'success');
        // 更新原始数据，重置修改状态
        this.setData({
          originalData: { ...this.data.form },
          originalPresetRecharge: JSON.stringify(this.data.presetRechargeList),
          originalPresetCost: JSON.stringify(this.data.presetCostList),
          hasBasicChanges: false
        });
      } else {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
      }
    } catch (error) {
      console.error('保存失败:', error);
      this.showCustomToast('保存失败', 'danger');
      wx.hideLoading();
    }
  },

  /**
   * 提交背景修改（带确认弹窗）
   */
  submitBackground() {
    wx.showModal({
      title: '确认修改',
      content: '是否确认修改背景？修改后旧设计将被覆盖！',
      confirmText: '确认修改',
      cancelText: '取消',
      success: (res) => {
        if (res.confirm) {
          this.doSubmitBackground();
        }
      }
    });
  },

  /**
   * 执行背景修改
   */
  async doSubmitBackground() {
    try {
      wx.showLoading({ title: '上传中...' });

      const { cardTypeId, storeId, randomCode, customBgTempPath, bgType, bgGradient, bgGradientDirection, bgAdvanced } = this.data;
      
      let cardBgc = '';
      
      // 判断背景类型
      if (customBgTempPath) {
        // 自定义背景，上传到OSS
        const timestamp = Math.floor(Date.now() / 3600000);
        const fileName = `bkgd_${timestamp}_${randomCode}.png`;
        
        // 上传到OSS，pathType='card'，返回完整的objectName（包含pathPrefix）
        const objectName = await ossUtil.uploadToOss(customBgTempPath, fileName, { pathType: 'card' });
        cardBgc = `/${objectName}`;  // 存储格式：/card/userId/bkgd_xxx.png
      } else if (bgType === 'gradient') {
        // 渐变色背景
        cardBgc = cardHelper.buildCardBgc(bgGradientDirection, bgGradient);
      } else if (bgType === 'advanced') {
        // 高级背景
        cardBgc = `bg_${bgAdvanced}.png`;
      }
      
      if (!cardBgc) {
        this.showCustomToast('请先选择背景', 'danger');
        wx.hideLoading();
        return;
      }
      
      const workToken = await tokenManager.getWorkToken(storeId);
      
      if (!workToken) {
        this.showCustomToast('工作令牌无效', 'danger');
        wx.hideLoading();
        return;
      }
      
      const requestBody = {
        cardTypeId: cardTypeId,
        storeId: storeId,
        cardBgc: cardBgc
      };
      
      console.log('[背景修改] 请求体:', requestBody);
      
      const res = await post(
        '/v1/member-card-types/set',
        { 
          'Content-Type': 'application/json',
          'Authorization': workToken
        },
        requestBody
      );

      console.log('[背景修改] 响应:', res);
      wx.hideLoading();

      if (res.code === 200) {
        this.showCustomToast('背景修改成功', 'success');
        // 更新旧卡片展示
        this.setData({ oldCardBgc: cardBgc });
      } else {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
      }
    } catch (error) {
      console.error('背景修改失败:', error);
      this.showCustomToast('背景修改失败', 'danger');
      wx.hideLoading();
    }
  },

  /**
   * 提交卡面修改（带确认弹窗）
   */
  submitMask() {
    wx.showModal({
      title: '确认修改',
      content: '是否确认修改卡面？修改后旧设计将被覆盖！',
      confirmText: '确认修改',
      cancelText: '取消',
      success: (res) => {
        if (res.confirm) {
          this.doSubmitMask();
        }
      }
    });
  },

  /**
   * 执行卡面修改
   */
  async doSubmitMask() {
    try {
      wx.showLoading({ title: '生成中...' });

      const { cardTypeId, storeId, randomCode, titleText, showVip, showPattern } = this.data;
      
      let cardMask = '';
      
      // 判断是否需要生成蒙版
      if (!titleText && !showVip && !showPattern) {
        // 空蒙版
        cardMask = '';
      } else {
        // 生成蒙版图片并上传
        const maskTempPath = await this.generateMaskImage();
        
        if (maskTempPath) {
          const timestamp = Math.floor(Date.now() / 3600000);
          const fileName = `mask_${timestamp}_${randomCode}.png`;
          
          // 上传到OSS，pathType='card'，返回完整的objectName（包含pathPrefix）
          const objectName = await ossUtil.uploadToOss(maskTempPath, fileName, { pathType: 'card' });
          cardMask = `/${objectName}`;  // 存储格式：/card/userId/mask_xxx.png
        }
      }
      
      const workToken = await tokenManager.getWorkToken(storeId);
      
      if (!workToken) {
        this.showCustomToast('工作令牌无效', 'danger');
        wx.hideLoading();
        return;
      }
      
      const requestBody = {
        cardTypeId: cardTypeId,
        storeId: storeId,
        cardMask: cardMask
      };
      
      console.log('[卡面修改] 请求体:', requestBody);
      
      const res = await post(
        '/v1/member-card-types/set',
        { 
          'Content-Type': 'application/json',
          'Authorization': workToken
        },
        requestBody
      );

      console.log('[卡面修改] 响应:', res);
      wx.hideLoading();

      if (res.code === 200) {
        this.showCustomToast('卡面修改成功', 'success');
        // 更新旧卡片展示
        this.setData({ oldCardMask: cardMask });
      } else {
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
      }
    } catch (error) {
      console.error('卡面修改失败:', error);
      this.showCustomToast('卡面修改失败', 'danger');
      wx.hideLoading();
    }
  },

  /**
   * 生成蒙版图片
   */
  async generateMaskImage() {
    return new Promise((resolve) => {
      const { canvas } = this.data;
      if (!canvas) {
        resolve(null);
        return;
      }
      
      wx.canvasToTempFilePath({
        canvas: canvas,
        fileType: 'png',
        success: (res) => {
          resolve(res.tempFilePath);
        },
        fail: (err) => {
          console.error('生成蒙版图片失败:', err);
          resolve(null);
        }
      });
    });
  },

  /**
   * 验证表单
   */
  validateForm() {
    const { form, presetRechargeList, presetCostList, showPresetRecharge, showPresetCost, presetAmountLabel } = this.data;

    // 验证卡种名称
    if (!form.cardTypeName || form.cardTypeName.trim() === '') {
      this.showCustomToast('请输入卡种名称', 'danger');
      return false;
    }

    if (form.cardTypeName.length < 2 || form.cardTypeName.length > 50) {
      this.showCustomToast('卡种名称长度应为2-50字', 'danger');
      return false;
    }

    // 验证描述长度
    if (form.description && form.description.length > 500) {
      this.showCustomToast('卡种描述最多500字', 'danger');
      return false;
    }

    // 验证预设充值项目（仅当显示时验证）
    if (showPresetRecharge) {
      for (let i = 0; i < presetRechargeList.length; i++) {
        const item = presetRechargeList[i];
        if (!item.itemName || item.itemName.trim() === '') {
          this.showCustomToast(`请输入充值项目${i + 1}的名称`, 'danger');
          return false;
        }
        if (!item.amount || isNaN(parseFloat(item.amount))) {
          this.showCustomToast(`请输入充值项目${i + 1}的${presetAmountLabel}`, 'danger');
          return false;
        }
      }
    }

    // 验证预设消费项目（仅当显示时验证）
    if (showPresetCost) {
      for (let i = 0; i < presetCostList.length; i++) {
        const item = presetCostList[i];
        if (!item.itemName || item.itemName.trim() === '') {
          this.showCustomToast(`请输入消费项目${i + 1}的名称`, 'danger');
          return false;
        }
        if (!item.amount || isNaN(parseFloat(item.amount))) {
          this.showCustomToast(`请输入消费项目${i + 1}的${presetAmountLabel}`, 'danger');
          return false;
        }
      }
    }

    return true;
  },

  /**
   * 获取修改的字段（智能对比）
   */
  getChangedFields() {
    const { originalData, form } = this.data;
    const changedFields = {};

    // 对比每个字段
    if (form.cardTypeName !== originalData.cardTypeName) {
      changedFields.cardTypeName = form.cardTypeName;
    }

    if (form.description !== originalData.description) {
      changedFields.description = form.description;
    }

    if (form.autoNotify !== originalData.autoNotify) {
      changedFields.autoNotify = form.autoNotify;
    }

    if (form.crossStore !== originalData.crossStore) {
      changedFields.crossStore = form.crossStore;
    }

    // 对比预设充值项目
    const currentRecharge = JSON.stringify(this.data.presetRechargeList);
    if (currentRecharge !== this.data.originalPresetRecharge) {
      changedFields.presetRecharge = currentRecharge;
    }

    // 对比预设消费项目
    const currentCost = JSON.stringify(this.data.presetCostList);
    if (currentCost !== this.data.originalPresetCost) {
      changedFields.presetCost = currentCost;
    }

    console.log('修改的字段:', changedFields);
    return changedFields;
  },

  /**
   * 检查基础信息是否有修改
   */
  checkBasicChanges() {
    const changedFields = this.getChangedFields();
    const hasChanges = Object.keys(changedFields).length > 0;
    if (this.data.hasBasicChanges !== hasChanges) {
      this.setData({ hasBasicChanges: hasChanges });
    }
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

  // ========== Canvas相关方法（从cardCanvasMixin导入） ==========
  
  /**
   * 初始化Canvas
   */
  initCanvas() {
    return new Promise((resolve, reject) => {
      try {
        const query = this.createSelectorQuery();
        query.select('#maskCanvas')
          .fields({ node: true, size: true })
          .exec((res) => {
            if (res && res[0]) {
              const canvas = res[0].node;
              const ctx = canvas.getContext('2d');
              
              const dpr = wx.getSystemInfoSync().pixelRatio;
              canvas.width = 856 * dpr;
              canvas.height = 540 * dpr;
              ctx.scale(dpr, dpr);
              
              this.setData({ canvas, ctx });
              console.log('[卡种修改] Canvas初始化成功');
              resolve();
            } else {
              console.error('[卡种修改] Canvas节点查询失败');
              reject(new Error('Canvas节点查询失败'));
            }
          });
      } catch (error) {
        console.error('[卡种修改] Canvas初始化失败:', error);
        reject(error);
      }
    });
  },

  /**
   * 更新背景渐变样式
   */
  updateBgGradientStyle() {
    const { bgGradient, bgGradientDirection } = this.data;
    
    const gradientMap = {
      'darkblack': ['#111111', '#222222', '#111111'],
      'gold': ['#B8860B', '#D4AF37'],
      'winered': ['#8B0000', '#9E2B25'],
      'originalgold': ['#FDEB71', '#F8D800'],
      'blue': ['#ABDCFF', '#0396FF'],
      'red': ['#FEB692', '#EA5455'],
      'orange': ['#FCCF31', '#F55555'],
      'purple': ['#F761A1', '#8C1BAB'],
      'sunset': ['#FFA8A8', '#FCFF00']
    };
    
    const colors = gradientMap[bgGradient] || ['#FFFFFF', '#FFFFFF'];
    
    // 根据方向生成CSS渐变
    let direction;
    if (bgGradientDirection === 'horizontal') {
      direction = 'to right';
    } else if (bgGradientDirection === 'vertical') {
      direction = 'to bottom';
    } else {
      direction = '135deg';
    }
    
    // 生成渐变字符串
    let gradientStyle;
    if (colors.length === 2) {
      gradientStyle = `background: linear-gradient(${direction}, ${colors[0]}, ${colors[1]});`;
    } else if (colors.length === 3) {
      gradientStyle = `background: linear-gradient(${direction}, ${colors[0]}, ${colors[1]}, ${colors[2]});`;
    }
    
    this.setData({ bgGradientStyle: gradientStyle });
  },

  /**
   * 自动生成蒙版
   */
  autoGenerateMask() {
    cardCanvasMixin.autoGenerateMask.call(this);
  },

  /**
   * 绘制标题
   */
  drawTitle(ctx) {
    return cardCanvasMixin.drawTitle.call(this, ctx);
  },

  /**
   * 绘制VIP
   */
  drawVip(ctx) {
    return cardCanvasMixin.drawVip.call(this, ctx);
  },

  /**
   * 绘制VIP图片
   */
  drawVipImage(ctx) {
    return cardCanvasMixin.drawVipImage.call(this, ctx);
  },

  /**
   * 绘制图案
   */
  drawPattern(ctx) {
    return cardCanvasMixin.drawPattern.call(this, ctx);
  },

  /**
   * 创建渐变色
   */
  createGradient(ctx, gradientType, x, y, width, direction) {
    return cardCanvasMixin.createGradient.call(this, ctx, gradientType, x, y, width, direction);
  }
});
