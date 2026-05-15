# 微信小程序自定义 tabBar 功能说明

## 功能概述

本项目实现了基于用户类型的动态自定义 tabBar 功能，支持以下特性：

- **3种用户类型**：普通用户(1)、商家(2)、员工(3)
- **动态 tabBar**：根据用户类型显示不同的 tab 项
- **实时切换**：支持用户类型切换时 tabBar 立即更新
- **权限控制**：页面访问权限检查
- **升级提示**：用户升级时弹出确认提示

## 用户类型对应的 tabBar 配置

### 普通用户 (userType = 1)
- 会员首页 (`pages/member-home/member-home`)
- 卡管理 (`pages/card-manage/card-manage`)
- 我的 (`pages/profile/profile`)

### 商家 (userType = 2)
- 商家首页 (`pages/merchant-home/merchant-home`)
- 工作台 (`pages/workspace/workspace`)
- 后台管理 (`pages/admin-manage/admin-manage`)
- 我的 (`pages/profile/profile`)

### 员工 (userType = 3)
- 会员首页 (`pages/member-home/member-home`)
- 工作台 (`pages/workspace/workspace`)
- 卡管理 (`pages/card-manage/card-manage`)
- 我的 (`pages/profile/profile`)

## 核心文件说明

### 1. app.js
- 全局状态管理
- tabBar 配置获取
- 用户类型切换逻辑
- 升级提示处理

### 2. app.json
- 自定义 tabBar 配置 (`"custom": true`)
- 所有可能的页面路径

### 3. custom-tab-bar/
- `index.js`: tabBar 组件逻辑
- `index.wxml`: tabBar 组件模板
- `index.wxss`: tabBar 组件样式
- `index.json`: 组件配置

### 4. 页面文件
每个页面都包含：
- tabBar 选中状态同步
- 页面权限检查
- 用户类型相关逻辑

## 使用方法

### 1. 模拟用户升级
在"我的"页面点击"模拟升级为商家（测试）"按钮，会：
- 将 `actualUserType` 设置为 2
- 弹出升级提示："是否进入商家版界面？"
- 用户可选择立即切换或稍后切换

### 2. 手动切换用户类型
- **会员首页**：商家用户可看到"进入商家版"按钮
- **商家首页**：可看到"切换到会员模式"按钮
- **我的页面**：商家用户可在两种模式间切换

### 3. 权限控制
- 非商家用户访问商家页面会被拦截并跳转
- 非商家/员工用户访问工作台会被拦截
- 页面加载时自动检查权限

## 关键变量说明

### app.globalData 中的关键字段：
- `currentUserType`: 当前显示的用户类型（影响 tabBar 显示）
- `actualUserType`: 用户实际类型（用于权限判断）
- `tabBarConfig`: 当前 tabBar 配置数组
- `selectedTabIndex`: 当前选中的 tab 索引

## 开发注意事项

### 1. 图标资源
需要在 `/assets/icons/` 目录下放置对应的图标文件：
- member-home.png / member-home-active.png
- merchant-home.png / merchant-home-active.png
- card-manage.png / card-manage-active.png
- workspace.png / workspace-active.png
- admin-manage.png / admin-manage-active.png
- profile.png / profile-active.png

### 2. 状态同步
- 每个页面的 `onShow` 方法都要调用 `app.updateTabBarSelected()`
- 切换用户类型后会自动更新 tabBar 组件

### 3. 权限检查
- 页面 `onLoad` 时检查访问权限
- 无权限时显示提示并跳转到有权限的页面

### 4. 性能优化
- tabBar 组件使用 `cover-view` 确保层级正确
- 避免频繁的 setData 操作
- 合理使用页面缓存

## 扩展功能

### 添加新的用户类型
1. 在 `app.js` 的 `getTabBarConfig` 方法中添加新配置
2. 更新权限检查逻辑
3. 添加对应的页面文件

### 添加新的 tab 页面
1. 创建页面文件
2. 在 `app.json` 的 tabBar.list 中添加配置
3. 在 `app.js` 的配置中添加到对应用户类型
4. 实现页面的 tabBar 同步逻辑

## 测试建议

1. **基础功能测试**：验证不同用户类型的 tabBar 显示
2. **切换功能测试**：测试用户类型切换的流畅性
3. **权限控制测试**：验证页面访问权限是否正确
4. **升级提示测试**：测试用户升级时的提示逻辑
5. **状态同步测试**：验证 tabBar 选中状态是否正确同步

## 常见问题

### Q: tabBar 不显示或显示异常
A: 检查 `app.json` 中的 `custom: true` 配置，确保 custom-tab-bar 组件正确创建

### Q: 切换用户类型后页面没有跳转
A: 检查 `doSwitchUserType` 方法中的页面跳转逻辑

### Q: 页面权限控制不生效
A: 确保每个页面的 `onLoad` 方法都调用了 `checkUserPermission`

### Q: tabBar 选中状态不同步
A: 确保每个页面的 `onShow` 方法都调用了 `app.updateTabBarSelected`
