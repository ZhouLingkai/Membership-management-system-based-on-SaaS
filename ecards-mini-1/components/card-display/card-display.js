// components/card-display/card-display.js
const { generateSignedUrl } = require('../../utils/oss');

Component({
  /**
   * 组件的属性列表
   */
  properties: {
    // 卡片宽度（rpx）
    width: {
      type: Number,
      value: 720  // 默认96%屏幕宽度
    },
    // 背景字段
    cardBgc: {
      type: String,
      value: ''
    },
    // 蒙版字段
    cardMask: {
      type: String,
      value: ''
    }
  },

  /**
   * 组件的初始数据
   */
  data: {
    height: 454,  // 根据width计算得出
    bgType: 'default',  // 背景类型：oss, local, gradient, default
    bgUrl: '',  // 背景图片URL
    bgGradientStyle: '',  // 渐变样式
    maskUrl: ''  // 蒙版图片URL
  },

  /**
   * 组件的方法列表
   */
  methods: {
    /**
     * 处理cardBgc字段
     */
    async processCardBgc(cardBgc) {
      if (!cardBgc) {
        // 空值，使用默认背景
        this.setData({
          bgType: 'default',
          bgUrl: '',
          bgGradientStyle: ''
        });
        return;
      }

      const firstChar = cardBgc.charAt(0);

      // 1. OSS图片（以 / 开头）
      if (firstChar === '/') {
        try {
          const signedUrl = await generateSignedUrl(cardBgc.substring(1));  // 去掉开头的 /
          this.setData({
            bgType: 'oss',
            bgUrl: signedUrl,
            bgGradientStyle: ''
          });
        } catch (error) {
          console.error('[card-display] OSS背景图片加载失败:', error);
          this.setData({ bgType: 'default' });
        }
        return;
      }

      // 2. 本地高级图片（以 bg 开头）
      if (cardBgc.startsWith('bg')) {
        this.setData({
          bgType: 'local',
          bgUrl: `/assets/bkgd/${cardBgc}`,
          bgGradientStyle: ''
        });
        return;
      }

      // 3. 渐变色（数字开头，格式：{direct}_{seq}）
      if (/^\d/.test(firstChar)) {
        const gradientStyle = this.parseGradient(cardBgc);
        this.setData({
          bgType: 'gradient',
          bgUrl: '',
          bgGradientStyle: gradientStyle
        });
        return;
      }

      // 4. 其他情况，使用默认背景
      this.setData({
        bgType: 'default',
        bgUrl: '',
        bgGradientStyle: ''
      });
    },

    /**
     * 解析渐变色配置
     * @param {string} gradientConfig - 格式：{direct}_{seq}
     * @returns {string} CSS渐变样式
     */
    parseGradient(gradientConfig) {
      const parts = gradientConfig.split('_');
      if (parts.length !== 2) {
        return 'background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);';
      }

      const direct = parseInt(parts[0]);
      const seq = parseInt(parts[1]);

      // 渐变方向映射
      const directionMap = {
        1: 'to right',      // 水平
        2: 'to bottom',     // 垂直
        3: '135deg'         // 对角
      };
      const direction = directionMap[direct] || '135deg';

      // 渐变色方案映射（与mask-test保持一致）
      const gradientMap = {
        1: ['#111111', '#222222', '#111111'],  // 暗黑
        2: ['#B8860B', '#D4AF37'],             // 金色
        3: ['#FF6B6B', '#FFE66D'],             // 日落
        4: ['#4FACFE', '#00F2FE'],             // 海洋
        5: ['#43E97B', '#38F9D7'],             // 薄荷
        6: ['#FA709A', '#FEE140'],             // 粉橙
        7: ['#30CFD0', '#330867'],             // 紫青
        8: ['#A8EDEA', '#FED6E3'],             // 梦幻
        9: ['#FF9A9E', '#FAD0C4', '#FBC2EB']   // 彩虹
      };

      const colors = gradientMap[seq] || ['#667eea', '#764ba2'];

      // 生成CSS渐变样式
      if (colors.length === 2) {
        return `background: linear-gradient(${direction}, ${colors[0]}, ${colors[1]});`;
      } else if (colors.length === 3) {
        return `background: linear-gradient(${direction}, ${colors[0]}, ${colors[1]}, ${colors[2]});`;
      }

      return 'background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);';
    },

    /**
     * 处理cardMask字段
     */
    async processCardMask(cardMask) {
      console.log('[card-display] processCardMask called, cardMask:', cardMask);
      
      if (!cardMask || cardMask === '') {
        // 空字符串，不显示蒙版
        console.log('[card-display] cardMask为空，不显示蒙版');
        this.setData({ maskUrl: '' });
        return;
      }
      let objectName = ""
      // OSS图片（以 / 开头 或不含 /）
      if (cardMask.charAt(0) === '/') {
        objectName = cardMask.substring(1);  // 去掉开头的 /
      } else {
        objectName = cardMask
      }
      try {
        console.log('[card-display] 准备获取OSS签名URL, objectName:', objectName);
        const signedUrl = await generateSignedUrl(objectName);
        console.log('[card-display] 获取签名URL成功:', signedUrl);
        this.setData({ maskUrl: signedUrl });
      } catch (error) {
        console.error('[card-display] OSS蒙版图片加载失败:', error);
        this.setData({ maskUrl: '' });
      }
      return;

      // 其他情况不显示蒙版
      console.log('[card-display] cardMask格式不识别:', cardMask);
      this.setData({ maskUrl: '' });
    },

    /**
     * 计算卡片高度（保持1.585:1比例）
     */
    calculateHeight() {
      const height = Math.round(this.data.width / 1.585);
      this.setData({ height });
    }
  },

  /**
   * 组件生命周期
   */
  lifetimes: {
    attached() {
      // 计算高度
      this.calculateHeight();
      
      // 处理背景和蒙版
      this.processCardBgc(this.data.cardBgc);
      this.processCardMask(this.data.cardMask);
    }
  },

  /**
   * 监听属性变化
   */
  observers: {
    'width': function(newWidth) {
      this.calculateHeight();
    },
    'cardBgc': function(newCardBgc) {
      this.processCardBgc(newCardBgc);
    },
    'cardMask': function(newCardMask) {
      this.processCardMask(newCardMask);
    }
  }
})
