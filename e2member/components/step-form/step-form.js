// components/step-form/step-form.js
Component({
  /**
   * 组件的属性列表
   */
  properties: {
    // 步骤标题数组
    steps: {
      type: Array,
      value: []
    },
    // 当前步骤索引
    currentStep: {
      type: Number,
      value: 0
    }
  },

  /**
   * 组件的初始数据
   */
  data: {
    // 最大已完成步骤（用于控制跳转）-1表示没有步骤完成
    maxCompletedStep: -1
  },

  /**
   * 组件的方法列表
   */
  methods: {
    /**
     * 点击步骤圆圈跳转
     */
    onStepClick(e) {
      const targetStep = e.currentTarget.dataset.index;
      const { currentStep, maxCompletedStep } = this.data;
      
      // 只能跳转到已完成的步骤（包括正在进行的步骤）
      // targetStep <= maxCompletedStep 表示该步骤已完成或正在进行
      if (targetStep <= maxCompletedStep && targetStep !== currentStep) {
        this.setData({
          currentStep: targetStep
        });
        
        // 触发步骤变化事件
        this.triggerEvent('stepchange', { 
          currentStep: targetStep,
          direction: targetStep > currentStep ? 'next' : 'prev'
        });
      }
    },

    /**
     * 下一步
     */
    nextStep() {
      const { currentStep, maxCompletedStep } = this.data;
      const totalSteps = this.properties.steps.length;
      
      if (currentStep < totalSteps - 1) {
        const nextStep = currentStep + 1;
        
        // 更新最大已完成步骤
        const newMaxCompleted = Math.max(maxCompletedStep, currentStep);
        
        this.setData({
          currentStep: nextStep,
          maxCompletedStep: newMaxCompleted
        });
        
        // 触发步骤变化事件
        this.triggerEvent('stepchange', { 
          currentStep: nextStep,
          direction: 'next'
        });
        
        return true;
      }
      return false;
    },

    /**
     * 上一步
     */
    prevStep() {
      const { currentStep } = this.data;
      
      if (currentStep > 0) {
        const prevStep = currentStep - 1;
        
        this.setData({
          currentStep: prevStep
        });
        
        // 触发步骤变化事件
        this.triggerEvent('stepchange', { 
          currentStep: prevStep,
          direction: 'prev'
        });
        
        return true;
      }
      return false;
    },

    /**
     * 跳转到指定步骤
     */
    goToStep(step) {
      const { currentStep, maxCompletedStep } = this.data;
      const totalSteps = this.properties.steps.length;
      
      // 验证步骤范围
      if (step < 0 || step >= totalSteps) {
        return false;
      }
      
      // 只能跳转到已完成或正在进行的步骤
      if (step <= maxCompletedStep) {
        this.setData({
          currentStep: step
        });
        
        // 触发步骤变化事件
        this.triggerEvent('stepchange', { 
          currentStep: step,
          direction: step > currentStep ? 'next' : 'prev'
        });
        
        return true;
      }
      
      return false;
    },

    /**
     * 更新当前步骤的完成状态
     * 如果当前步骤未完成，则降低最大完成步骤
     */
    updateStepStatus(completed) {
      const { currentStep, maxCompletedStep } = this.data;
      
      if (completed) {
        // 标记当前步骤为已完成
        const newMaxCompleted = Math.max(maxCompletedStep, currentStep);
        this.setData({
          maxCompletedStep: newMaxCompleted
        });
      } else {
        // 如果当前步骤变为未完成，且是最大完成步骤，则降低最大完成步骤
        if (currentStep === maxCompletedStep) {
          this.setData({
            maxCompletedStep: Math.max(0, currentStep - 1)
          });
        }
      }
    },

    /**
     * 重置组件状态
     */
    reset() {
      this.setData({
        currentStep: 0,
        maxCompletedStep: -1
      });
    },

    /**
     * 获取当前步骤信息
     */
    getCurrentStepInfo() {
      const { currentStep, maxCompletedStep } = this.data;
      return {
        currentStep,
        maxCompletedStep,
        totalSteps: this.properties.steps.length,
        canGoNext: currentStep < this.properties.steps.length - 1,
        canGoPrev: currentStep > 0
      };
    }
  },

  /**
   * 组件生命周期
   */
  lifetimes: {
    attached() {
      // 初始化时，没有任何步骤完成
      this.setData({
        maxCompletedStep: -1
      });
    }
  }
});
