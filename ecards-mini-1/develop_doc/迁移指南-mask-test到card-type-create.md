# 迁移指南：mask-test 功能到 card-type-create/update

## 一、概述

将 mask-test 页面的背景设计、实时预览、卡面设计功能迁移到卡种创建和修改页面。

### 功能微调
1. ✅ 删除第4个渐变色（brightblack）
2. ✅ 渐变色改为靠左对齐

### 渐变色序号映射
```
1. darkblack (深黑) - 序号1
2. gold (金色) - 序号2
3. winered (酒红) - 序号3
4. originalgold (原金色) - 序号4
5. blue (蓝色) - 序号5
6. red (红色) - 序号6
7. orange (橙色) - 序号7
8. purple (紫色) - 序号8
9. sunset (日落) - 序号9
```

### 渐变方向映射
```
horizontal (水平) - 编号1
vertical (垂直) - 编号2
diagonal (对角) - 编号3
```

---

## 二、cardBgc 和 cardMask 字段规则

### cardBgc 字段
| 用户选择 | 字段值示例 | 说明 |
|---------|-----------|------|
| 渐变色 | `3_1` | `{direct}_{seq}`，如3_1表示对角深黑 |
| 高级图片 | `bg_black.png` | 文件名 |
| 自定义图片 | `/card/xxx.png` | OSS路径 |

### cardMask 字段
| 条件 | 字段值 | 说明 |
|------|--------|------|
| 空文字 + 不显示VIP + 无图案 | `""` | 空字符串 |
| 其他情况 | `/card/xxx.png` | OSS路径 |

---

## 三、实施步骤

### 步骤1：准备工作 ✅

1. 创建辅助工具 `/utils/card-helper.js` ✅
2. 功能微调完成 ✅

### 步骤2：修改 card-type-create.js

#### 2.1 添加数据字段

在 `data` 中添加：

```javascript
// 背景设计
bgType: 'gradient', // gradient, advanced
bgGradient: 'darkblack',
bgGradientDirection: 'diagonal',
bgGradientStyle: '',
bgAdvanced: 'black',
customBgUrl: '',
customBgOssName: '',

// 标题文字设置
titleText: 'VIP会员卡',
titlePosition: 'left-top',
titleColor: '#FFFFFF',
titleColorType: 'gradient',
titleGradient: 'red',
titleFont: 'default',
titleSize: 'medium',

// 字体选项
fontOptions: [],

// 预览控制
showPreview: true,
showBackground: true,

// VIP设置
showVip: true,
vipPosition: 'center-left',
vipColor: '#FFD700',
vipColorType: 'advanced',
vipGradient: 'gold',
vipAdvanced: 'white',
vipFont: 'default',
vipSize: 'xlarge',

// 图案选择
showPattern: true,
selectedPattern: 'dragon',
patternPosition: 'right-bottom',
patternSize: 'large',

// 蒙版图片
maskImageUrl: '',

// Canvas实例
canvas: null,
ctx: null,

// 上传进度
uploadProgress: 0,
uploadStatus: '', // '', uploading, success, error
uploadMessage: ''
```

#### 2.2 添加 onShow 方法

```javascript
async onShow() {
  // 生成上传随机码
  const { generateRandomCode } = require('../../utils/oss');
  this.uploadRandomCode = generateRandomCode();
  
  // 获取STS令牌（用于OSS上传）
  try {
    const { getStsCredentials } = require('../../utils/oss');
    await getStsCredentials('card');
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
}
```

#### 2.3 复制 mask-test 的方法

需要复制以下方法到 card-type-create.js：

1. `initPlatform()` - 初始化平台信息
2. `initCanvas()` - 初始化Canvas
3. `autoGenerateMask()` - 自动生成蒙版
4. `drawPattern()` - 绘制图案
5. `drawTitle()` - 绘制标题
6. `drawVip()` - 绘制VIP
7. `drawVipImage()` - 绘制VIP图片
8. `createGradient()` - 创建渐变色
9. `updateBgGradientStyle()` - 更新背景渐变样式
10. `onAdvancedBgChange()` - 高级背景选择
11. `onUploadCustomBg()` - 上传自定义背景
12. `processImageToRatio()` - 处理图片比例
13. `stretchImage()` - 拉伸图片
14. `cropImageToRatio()` - 裁剪图片
15. `compressImageSmart()` - 智能压缩

#### 2.4 修改 submitForm 方法

```javascript
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
    
    const presetRecharge = presetRechargeList.map(item => ({
      itemName: item.itemName.trim(),
      itemDesc: item.itemDesc || '',
      amount: parseFloat(item.amount)
    }));

    const presetCost = presetCostList.map(item => ({
      itemName: item.itemName.trim(),
      itemDesc: item.itemDesc || '',
      amount: parseFloat(item.amount)
    }));

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
}
```

#### 2.5 添加构造方法

```javascript
/**
 * 构造 cardBgc 字段
 */
async buildCardBgc() {
  const { bgType, bgGradient, bgGradientDirection, bgAdvanced, customBgUrl } = this.data;
  const cardHelper = require('../../utils/card-helper');
  
  let customBgOssPath = '';
  
  // 如果有自定义背景，上传到OSS
  if (customBgUrl) {
    const { uploadCardImage } = require('../../utils/oss');
    customBgOssPath = await uploadCardImage(customBgUrl, 'bkgd', this.uploadRandomCode);
    console.log('[卡种创建] 背景上传成功:', customBgOssPath);
  }
  
  return cardHelper.buildCardBgc({
    bgType,
    bgGradient,
    bgGradientDirection,
    bgAdvanced,
    customBgOssPath
  });
}

/**
 * 构造 cardMask 字段
 */
async buildCardMask() {
  const { titleText, showVip, showPattern, maskImageUrl } = this.data;
  const cardHelper = require('../../utils/card-helper');
  
  // 判断是否为空蒙版
  const isEmpty = (!titleText || titleText.trim() === '') && !showVip && !showPattern;
  
  if (isEmpty) {
    return '';
  }
  
  // 上传蒙版到OSS
  if (maskImageUrl) {
    const { uploadCardImage } = require('../../utils/oss');
    const maskOssPath = await uploadCardImage(maskImageUrl, 'mask', this.uploadRandomCode);
    console.log('[卡种创建] 蒙版上传成功:', maskOssPath);
    return maskOssPath;
  }
  
  return '';
}

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
}
```

### 步骤3：修改 card-type-create.wxml

在基本信息表单后，添加以下模块：

```xml
<!-- 背景设计 -->
<view class="form-card">
  <view class="card-title">
    <text class="card-icon">🖼️</text>
    <text>背景设计</text>
  </view>
  
  <!-- 复制 mask-test.wxml 的背景设计部分 -->
  <!-- ... -->
</view>

<!-- 实时预览 -->
<view class="preview-section">
  <!-- 复制 mask-test.wxml 的实时预览部分 -->
  <!-- ... -->
</view>

<!-- 卡面设计 -->
<view class="form-card">
  <view class="card-title">
    <text class="card-icon">🎨</text>
    <text>卡面设计</text>
  </view>
  
  <!-- 复制 mask-test.wxml 的卡面设计部分 -->
  <!-- ... -->
</view>

<!-- 上传进度条 -->
<view class="upload-progress-modal" wx:if="{{uploadStatus === 'uploading'}}">
  <view class="progress-content">
    <view class="progress-title">正在处理</view>
    <view class="progress-bar-container">
      <view class="progress-bar" style="width: {{uploadProgress}}%;"></view>
    </view>
    <view class="progress-text">{{uploadMessage}} ({{uploadProgress}}%)</view>
  </view>
</view>
```

### 步骤4：修改 card-type-create.wxss

复制 mask-test.wxss 的相关样式，并添加进度条样式：

```css
/* 上传进度条 */
.upload-progress-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.progress-content {
  width: 600rpx;
  background: white;
  border-radius: 16rpx;
  padding: 40rpx;
}

.progress-title {
  font-size: 32rpx;
  font-weight: 600;
  text-align: center;
  margin-bottom: 24rpx;
}

.progress-bar-container {
  width: 100%;
  height: 12rpx;
  background: #f0f0f0;
  border-radius: 6rpx;
  overflow: hidden;
  margin-bottom: 16rpx;
}

.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, #3C8CE7, #5BA3F5);
  transition: width 0.3s;
}

.progress-text {
  font-size: 24rpx;
  color: #666;
  text-align: center;
}
```

### 步骤5：修改 card-type-create.json

添加 custom-toast 组件（如果还没有）：

```json
{
  "navigationBarTitleText": "创建会员卡种",
  "usingComponents": {
    "custom-toast": "/components/custom-toast/custom-toast"
  }
}
```

---

## 四、迁移到 card-type-update

card-type-update 的实现与 card-type-create 基本相同，主要区别：

1. 接口URL改为 `/v1/member-card-types/set`
2. 暂时不实现数据回显（等后续实现）

---

## 五、测试清单

### 功能测试
- [ ] 渐变色选择和预览
- [ ] 渐变方向切换
- [ ] 高级背景选择
- [ ] 自定义背景上传
- [ ] 标题文字设置
- [ ] VIP设置
- [ ] 图案设置
- [ ] 实时预览更新
- [ ] 表单提交
- [ ] 进度条显示
- [ ] OSS上传失败处理
- [ ] 用户确认对话框

### 数据验证
- [ ] cardBgc 渐变色格式：`3_1`
- [ ] cardBgc 高级背景格式：`bg_black.png`
- [ ] cardBgc 自定义格式：`/card/xxx.png`
- [ ] cardMask 空蒙版：`""`
- [ ] cardMask OSS路径：`/card/xxx.png`

---

## 六、注意事项

1. **令牌管理**：确保在 onShow 中获取 STS 令牌和商家普通令牌
2. **错误处理**：OSS 上传失败时，弹窗询问用户是否继续
3. **进度提示**：使用进度条让用户了解上传进度
4. **返回上一页**：提交成功后自动返回
5. **Canvas 初始化**：确保在 DOM 渲染完成后初始化
6. **图片处理**：使用新的裁剪/拉伸逻辑

---

## 七、工具文件

已创建：
- ✅ `/utils/card-helper.js` - cardBgc 和 cardMask 构造工具

---

## 八、预估工作量

- **card-type-create 迁移**：4-6小时
- **card-type-update 迁移**：2-3小时
- **测试验证**：2-3小时
- **总计**：8-12小时

建议分多个会话完成，每次专注一个模块。
