// components/custom-toast/custom-toast.js
Component({
  /**
   * 组件的属性列表
   */
  properties: {
    // 消息内容
    message: {
      type: String,
      value: ''
    },
    // 消息类型：success（安全消息，绿色）或 danger（危险消息，红色）
    type: {
      type: String,
      value: 'success' // 默认 success
    },
    // 动画类型：slide（从顶部滑下）或 fade（从中间淡入）
    animationType: {
      type: String,
      value: 'slide' // 默认 slide
    },
    // 是否显示
    show: {
      type: Boolean,
      value: false
    },
    // 显示时长（毫秒），默认2000ms
    duration: {
      type: Number,
      value: 2000
    }
  },

  /**
   * 组件的初始数据
   */
  data: {
  },

  /**
   * 组件的方法列表
   */
  methods: {
    /**
     * 显示消息（通过方法调用方式）
     * @param {string} message - 消息内容
     * @param {string} type - 消息类型：success 或 danger
     * @param {string} animationType - 动画类型：slide 或 fade
     * @param {number} duration - 显示时长（毫秒）
     */
    showToast(message, type = 'success', animationType = 'slide', duration = 2000) {
      this.setData({
        message,
        type,
        animationType,
        duration,
        show: true
      });

      // 定时隐藏
      if (this._timer) {
        clearTimeout(this._timer);
      }
      this._timer = setTimeout(() => {
        this.hideToast();
      }, duration);
    },

    /**
     * 隐藏消息
     */
    hideToast() {
      this.setData({
        show: false
      });
      if (this._timer) {
        clearTimeout(this._timer);
        this._timer = null;
      }
    }
  },

  /**
   * 组件生命周期
   */
  lifetimes: {
    attached() {
      // 组件挂载时初始化
    },
    detached() {
      // 组件卸载时清理定时器
      if (this._timer) {
        clearTimeout(this._timer);
        this._timer = null;
      }
    }
  },

  /**
   * 属性观察器
   */
  observers: {
    'show'(show) {
      // 当外部通过属性控制显示时，自动处理隐藏逻辑
      if (show && this.data.duration) {
        if (this._timer) {
          clearTimeout(this._timer);
        }
        this._timer = setTimeout(() => {
          this.setData({ show: false });
          this._timer = null;
        }, this.data.duration);
      } else if (!show && this._timer) {
        clearTimeout(this._timer);
        this._timer = null;
      }
    }
  }
});

