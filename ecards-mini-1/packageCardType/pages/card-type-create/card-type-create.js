// pages/card-type-create/card-type-create.js

const { post } = require('../../../utils/request');

const tokenManager = require('../../../utils/token');

const ossUtil = require('../../../utils/oss');

const cardHelper = require('../../util/card-helper');
const cardCanvasMixin = require('../../util/card-canvas-mixin');

Page({
  // 混入Canvas方法
  ...cardCanvasMixin,
  data: {
    storeId: '',
    storeName: '',
    submitting: false,
    
    // 表单数据
    form: {
      cardTypeName: '',
      cardTtype: 1,
      description: '',
      cardBgc: '1',
      cardMask: '',
      autoNotify: 1,
      crossStore: 0
    },
    
    // 预设项目列表
    presetRechargeList: [],
    presetCostList: [],
    
    // 预设项目配置（根据卡种类型动态变化）
    presetAmountLabel: '金额',  // 金额字段标签
    presetAmountPlaceholder: '请输入金额',  // 金额字段placeholder
    showPresetRecharge: true,  // 是否显示预设充值项目
    showPresetCost: true,  // 是否显示预设消费项目
    
    // 卡种类型名称映射
    cardTypeNames: {
      1: '余额卡',
      2: '次数卡',
      3: '时效卡',
      4: '积分卡'
    },
    
    // ========== 背景设计 ==========
    bgType: 'gradient', // gradient渐变色, advanced高级
    bgGradient: 'darkblack', // 渐变色类型
    bgGradientDirection: 'diagonal', // 渐变方向: horizontal水平, vertical垂直, diagonal对角
    bgGradientStyle: '', // 渐变色样式字符串
    bgAdvanced: 'black', // 高级背景图片: black, white, red
    customBgUrl: '', // 自定义背景临时路径
    customBgOssName: '', // 自定义背景OSS路径
    
    // ========== 标题文字设置 ==========
    titleText: 'VIP会员卡',
    titlePosition: 'left-top',
    titleColor: '#FFFFFF',
    titleColorType: 'gradient',
    titleGradient: 'red', // 默认红色渐变
    titleFont: 'default',
    titleSize: 'medium',
    
    // 字体选项（根据平台动态设置）
    fontOptions: [],
    
    // ========== 预览控制 ==========
    showPreview: true,
    showBackground: true,
    
    // ========== VIP设置 ==========
    showVip: true,
    vipPosition: 'center-left',
    vipColor: '#FFD700',
    vipColorType: 'advanced',
    vipGradient: 'gold',
    vipAdvanced: 'white',
    vipFont: 'default',
    vipSize: 'xlarge',
    
    // ========== 图案选择 ==========
    showPattern: true,
    selectedPattern: 'dragon',
    patternPosition: 'right-bottom',
    patternSize: 'large',
    
    // ========== 蒙版图片 ==========
    maskImageUrl: '',
    
    // ========== Canvas实例 ==========
    canvas: null,
    ctx: null,
    
    // ========== 上传进度 ==========
    uploadProgress: 0,
    uploadStatus: '', // '', uploading, success, error
    uploadMessage: ''
  },

  onLoad(options) {
    const { storeId, storeName } = options;
    console.log('[卡种创建] onLoad 参数:', options);
    this.setData({
      storeId: storeId || '',
      storeName: decodeURIComponent(storeName || '未知店铺')
    });
    
    // 初始化平台信息
    this.initPlatform();
    
    // 更新渐变样式
    this.updateBgGradientStyle();
  },

  async onShow() {
    // 生成上传随机码
    this.uploadRandomCode = ossUtil.generateRandomCode();
    
    // 获取STS令牌（用于OSS上传）
    try {
      await ossUtil.getStsCredentials('card');
      console.log('[卡种创建] STS令牌获取成功');
    } catch (error) {
      console.error('[卡种创建] STS令牌获取失败:', error);
    }
    
    // 获取商家普通令牌（用于创建卡种）
    try {
      const token = await tokenManager.getNormalToken();
      console.log('[卡种创建] 商家令牌获取成功');
    } catch (error) {
      console.error('[卡种创建] 商家令牌获取失败:', error);
    }
    
  },

  /**
   * 页面首次渲染完成
   */
  onReady() {
    // 异步初始化Canvas并首次渲染
    this.initCanvasAndRender();
  },

  /**
   * 异步初始化Canvas并首次渲染
   */
  async initCanvasAndRender() {
    // 延时500ms确保DOM完全渲染
    setTimeout(async () => {
      try {
        await this.initCanvas();
        console.log('[卡种创建] Canvas初始化成功');
        await this.autoGenerateMask();
        console.log('[卡种创建] 首次蒙版生成完成');
      } catch (error) {
        console.error('[卡种创建] Canvas初始化或渲染失败:', error);
      }
    }, 500);
  },

  /**
   * 初始化平台信息
   */
  async initPlatform() {
    try {
      const deviceRes = await wx.getDeviceInfo();
      const { platform } = deviceRes;
      this.platform = platform;
      console.log('[卡种创建] 平台:', this.platform);
      
      // 根据平台设置字体选项
      let fontOptions = [
        { label: '默认', value: 'default' },
        { label: '宋体', value: 'songti' },
        { label: '黑体', value: 'heiti' }
      ];
      
      if (this.platform === 'ios') {
        fontOptions.push(
          { label: '苹方', value: 'pingfang' },
          { label: '冬青黑', value: 'hiragino' },
          { label: 'Times', value: 'times' }
        );
      } else if (this.platform === 'android') {
        fontOptions.push(
          { label: '微软雅黑', value: 'yahei' },
          { label: 'Roboto', value: 'roboto' }
        );
      }
      
      this.setData({ fontOptions });
    } catch (error) {
      console.error('[卡种创建] 获取平台信息失败:', error);
      this.platform = 'unknown';
      this.setData({
        fontOptions: [
          { label: '默认', value: 'default' },
          { label: '宋体', value: 'songti' },
          { label: '黑体', value: 'heiti' }
        ]
      });
    }
  },

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
            if (res && res[0] && res[0].node) {
              const canvas = res[0].node;
              const ctx = canvas.getContext('2d');
              
              const dpr = wx.getWindowInfo().pixelRatio || 2;
              canvas.width = 856 * dpr;
              canvas.height = 540 * dpr;
              ctx.scale(dpr, dpr);
              
              this.setData({ canvas, ctx });
              console.log('[卡种创建] Canvas初始化成功');
              resolve();
            } else {
              console.error('[卡种创建] Canvas节点查询失败, res=', res);
              reject(new Error('Canvas节点查询失败'));
            }
          });
      } catch (error) {
        console.error('[卡种创建] Canvas初始化失败:', error);
        reject(error);
      }
    });
  },

  /**
   * 表单输入
   */
  onInput(e) {
    const { field } = e.currentTarget.dataset;
    const { value } = e.detail;
    this.setData({
      [`form.${field}`]: value
    });
  },

  /**
   * 卡种类型变更
   */
  onTypeChange(e) {
    const { value } = e.currentTarget.dataset;
    const cardType = parseInt(value);
    const presetConfig = this.getPresetConfigByCardType(cardType);
    
    this.setData({
      'form.cardTtype': cardType,
      ...presetConfig
    });
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

      if (!res.tempFiles || res.tempFiles.length === 0) {
        this.showCustomToast('未选择图片', 'danger');
        return;
      }

      const tempFilePath = res.tempFiles[0].tempFilePath;
      console.log('[卡种创建] 选择图片:', tempFilePath);

      // 获取图片信息
      const imageInfo = await new Promise((resolve, reject) => {
        wx.getImageInfo({
          src: tempFilePath,
          success: resolve,
          fail: reject
        });
      });

      console.log('[卡种创建] 图片信息:', imageInfo);

      // 处理图片到目标比例
      const processedPath = await this.processImageToRatio(tempFilePath, imageInfo.width, imageInfo.height);
      console.log('[卡种创建] 处理后图片:', processedPath);

      // 压缩图片
      const compressedPath = await this.compressImageSmart(processedPath);
      console.log('[卡种创建] 压缩后图片:', compressedPath);

      // 更新UI
      this.setData({
        customBgUrl: compressedPath
      });

      this.showCustomToast('背景图片已选择', 'success');
    } catch (error) {
      console.error('[卡种创建] 上传自定义背景失败:', error);
      this.showCustomToast('图片处理失败', 'danger');
    }
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
    this.setData({
      [field]: value
    }, () => {
      // 如果是背景渐变方向变化，更新样式
      if (field === 'bgGradientDirection') {
        this.updateBgGradientStyle();
      }
      this.autoGenerateMask();
    });
  },

  /**
   * 颜色选择
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
   * 渐变色选择
   */
  onGradientChange(e) {
    const { field, value } = e.currentTarget.dataset;
    this.setData({
      [field]: value
    }, () => {
      // 如果是背景渐变色变化，更新样式
      if (field === 'bgGradient') {
        this.updateBgGradientStyle();
      }
      this.autoGenerateMask();
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
   * VIP开关
   */
  onVipSwitchChange(e) {
    this.setData({
      showVip: e.detail.value
    }, () => {
      this.autoGenerateMask();
    });
  },

  /**
   * 图案开关
   */
  onPatternSwitchChange(e) {
    this.setData({
      showPattern: e.detail.value
    }, () => {
      this.autoGenerateMask();
    });
  },

  /**
   * 背景类型切换
   */
  onBgTypeChange(e) {
    const { value } = e.currentTarget.dataset;
    this.setData({
      bgType: value
    });
  },

  /**
   * 高级背景选择
   */
  onAdvancedBgChange(e) {
    const { value } = e.currentTarget.dataset;
    this.setData({
      bgAdvanced: value
    });
  },

  /**
   * 预览切换
   */
  onTogglePreview() {
    this.setData({
      showPreview: !this.data.showPreview
    });
  },

  /**
   * 背景切换
   */
  onToggleBackground() {
    this.setData({
      showBackground: !this.data.showBackground
    });
  },

  /**
   * 图案选择
   */
  onSelectPattern(e) {
    const { pattern } = e.currentTarget.dataset;
    this.setData({
      selectedPattern: pattern
    }, () => {
      this.autoGenerateMask();
    });
  },

  /**
   * 高级VIP选择
   */
  onAdvancedVipChange(e) {
    const { value } = e.currentTarget.dataset;
    this.setData({
      vipAdvanced: value
    }, () => {
      this.autoGenerateMask();
    });
  },

  /**
   * 通知类型变更
   */
  onNotifyChange(e) {
    const { value } = e.currentTarget.dataset;
    this.setData({
      'form.autoNotify': parseInt(value)
    });
  },

  /**
   * 跨店通用变更
   */
  onCrossStoreChange(e) {
    this.setData({
      'form.crossStore': e.detail.value ? 1 : 0
    });
  },

  /**
   * 预设项目输入
   */
  onPresetInput(e) {
    const { index, field, type } = e.currentTarget.dataset;
    const { value } = e.detail;
    const listKey = type === 'recharge' ? 'presetRechargeList' : 'presetCostList';
    
    this.setData({
      [`${listKey}[${index}].${field}`]: value
    });
  },

  /**
   * 添加充值项目
   */
  addRechargeItem() {
    const { presetRechargeList } = this.data;
    this.setData({
      presetRechargeList: [...presetRechargeList, { itemName: '', amount: '', itemDesc: '' }]
    });
  },

  /**
   * 移除充值项目
   */
  removeRechargeItem(e) {
    const { index } = e.currentTarget.dataset;
    const { presetRechargeList } = this.data;
    
    if (presetRechargeList.length <= 1) {
      this.showCustomToast('至少保留一个充值项目', 'danger');
      return;
    }
    
    presetRechargeList.splice(index, 1);
    this.setData({ presetRechargeList });
  },

  /**
   * 添加消费项目
   */
  addCostItem() {
    const { presetCostList } = this.data;
    this.setData({
      presetCostList: [...presetCostList, { itemName: '', amount: '', itemDesc: '' }]
    });
  },

  /**
   * 移除消费项目
   */
  removeCostItem(e) {
    const { index } = e.currentTarget.dataset;
    const { presetCostList } = this.data;
    
    if (presetCostList.length <= 1) {
      this.showCustomToast('至少保留一个消费项目', 'danger');
      return;
    }
    
    presetCostList.splice(index, 1);
    this.setData({ presetCostList });
  },


  /**
   * 构造 cardBgc 字段
   */
  async buildCardBgc() {
    const { bgType, bgGradient, bgGradientDirection, bgAdvanced, customBgUrl } = this.data;
    
    let customBgOssPath = '';
    
    // 如果有自定义背景，上传到OSS
    if (customBgUrl) {
      customBgOssPath = await ossUtil.uploadCardImage(customBgUrl, 'bkgd', this.uploadRandomCode);
      console.log('[卡种创建] 背景上传成功:', customBgOssPath);
    }
    
    return cardHelper.buildCardBgc({
      bgType,
      bgGradient,
      bgGradientDirection,
      bgAdvanced,
      customBgOssPath
    });
  },

  /**
   * 构造 cardMask 字段
   */
  async buildCardMask() {
    const { titleText, showVip, showPattern, maskImageUrl } = this.data;
    
    // 判断是否为空蒙版
    const isEmpty = (!titleText || titleText.trim() === '') && !showVip && !showPattern;
    
    if (isEmpty) {
      return '';
    }
    
    // 上传蒙版到OSS
    if (maskImageUrl) {
      const maskOssPath = await ossUtil.uploadCardImage(maskImageUrl, 'mask', this.uploadRandomCode);
      console.log('[卡种创建] 蒙版上传成功:', maskOssPath);
      return maskOssPath;
    }
    
    return '';
  },

  /**
   * 显示确认对话框
   */
  showConfirmDialog(content) {
    return new Promise((resolve) => {
      wx.showModal({
        title: '提示',
        content: content,
        confirmText: '继续',
        cancelText: '取消',
        success: (res) => {
          resolve(res.confirm);
        },
        fail: () => {
          resolve(false);
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
      this.showCustomToast('卡种名称长度为2-50位', 'danger');
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
   * 提交表单
   */
  async submitForm() {
    if (!this.validateForm()) return;
    if (this.data.submitting) return;

    this.setData({ 
      submitting: true,
      uploadProgress: 0,
      uploadStatus: 'uploading',
      uploadMessage: '正在准备...'
    });

    try {
      const { form, storeId, presetRechargeList, presetCostList } = this.data;

      // 步骤1：构造 cardBgc（10%）
      this.setData({ uploadProgress: 10, uploadMessage: '正在处理背景...' });
      let cardBgc = '';
      try {
        cardBgc = await this.buildCardBgc();
      } catch (error) {
        console.error('构造cardBgc失败:', error);
        const confirmed = await this.showConfirmDialog('背景图片上传失败，是否跳过并继续创建？');
        if (!confirmed) {
          this.setData({ submitting: false, uploadStatus: 'error', uploadMessage: '已取消' });
          return;
        }
        cardBgc = '3_1'; // 使用默认值
      }
      
      // 步骤2：构造 cardMask（50%）
      this.setData({ uploadProgress: 50, uploadMessage: '正在处理卡面...' });
      let cardMask = '';
      try {
        cardMask = await this.buildCardMask();
      } catch (error) {
        console.error('构造cardMask失败:', error);
        const confirmed = await this.showConfirmDialog('卡面图片上传失败，是否跳过并继续创建？');
        if (!confirmed) {
          this.setData({ submitting: false, uploadStatus: 'error', uploadMessage: '已取消' });
          return;
        }
        cardMask = ''; // 使用空值
      }
      
      // 步骤3：构建请求体（80%）
      this.setData({ uploadProgress: 80, uploadMessage: '正在提交...' });
      
      // 根据卡种类型处理预设项目
      const { showPresetRecharge, showPresetCost } = this.data;
      
      const presetRecharge = showPresetRecharge ? presetRechargeList.map(item => ({
        itemName: item.itemName.trim(),
        itemDesc: item.itemDesc || '',
        amount: parseFloat(item.amount)
      })) : [];

      const presetCost = showPresetCost ? presetCostList.map(item => ({
        itemName: item.itemName.trim(),
        itemDesc: item.itemDesc || '',
        amount: parseFloat(item.amount)
      })) : [];

      const requestBody = {
        storeId: storeId,
        cardTypeName: form.cardTypeName.trim(),
        description: form.description || '',
        cardMask: cardMask,
        cardBgc: cardBgc,
        cardTtype: form.cardTtype,
        presetRecharge: JSON.stringify(presetRecharge),
        presetCost: JSON.stringify(presetCost),
        autoNotify: form.autoNotify,
        crossStore: form.crossStore
      };

      const normalToken = await tokenManager.getNormalToken();
      
      if (!normalToken) {
        this.showCustomToast('令牌无效，请重新登录', 'danger');
        this.setData({ submitting: false, uploadStatus: 'error' });
        return;
      }

      // 步骤4：提交到后端（90%）
      this.setData({ uploadProgress: 90, uploadMessage: '正在保存...' });
      
      const res = await post(
        '/v1/member-card-types/create',
        {
          'Content-Type': 'application/json',
          'Authorization': normalToken
        },
        requestBody
      );

      if (res.code === 200) {
        this.setData({ uploadProgress: 100, uploadStatus: 'success', uploadMessage: '创建成功！' });
        this.showCustomToast('卡种创建成功', 'success');
        // 延迟返回上一页
        setTimeout(() => {
          wx.navigateBack();
        }, 1500);
      } else {
        this.setData({ uploadStatus: 'error', uploadMessage: `失败: ${res.message}` });
        this.showCustomToast(`${res.code}: ${res.message}`, 'danger');
      }
    } catch (error) {
      console.error('创建卡种失败:', error);
      this.setData({ uploadStatus: 'error', uploadMessage: '创建失败' });
      this.showCustomToast('创建卡种失败', 'danger');
    } finally {
      this.setData({ submitting: false });
    }
  },

  /**
   * 显示自定义Toast
   */
  showCustomToast(message, type = 'success') {
    const toast = this.selectComponent('#customToast');
    if (toast) {
      toast.showToast(message, type);
    }
  }
});
