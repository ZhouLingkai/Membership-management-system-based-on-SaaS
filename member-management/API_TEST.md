# 新版登录系统 API 测试指南

## 🚀 **开发完成状态**

✅ **所有核心功能已实现：**

### **1. 新增接口（2个）**
- ✅ `POST /api/v1/users/autologin-wx` - 小程序自动登录
- ✅ `POST /api/v1/users/autologin-web` - Web自动登录

### **2. 修改接口（2个）**
- ✅ `POST /api/v1/users/login` - 支持rememberMe时长选择 + Web端Cookie设置
- ✅ `DELETE /api/v1/users/logout` - 支持Web端Cookie清理

### **3. 核心功能**
- ✅ **7天/8小时令牌时长选择**：`rememberMe=true`→7天，`rememberMe=false`→8小时
- ✅ **Web端Cookie自动处理**：登录设置、轮换更新、退出清理
- ✅ **令牌轮换机制**：7天令牌第3次使用轮换，8小时令牌第3次使用注销
- ✅ **设备绑定验证**：所有令牌都绑定Device-Id
- ✅ **安全Cookie设置**：HttpOnly、SameSite=Lax、Secure（生产环境）

---

## 🧪 **API 测试用例**

### **测试1：Web端登录（7天令牌）**
```bash
POST /api/v1/users/login
Headers:
  Device-Id: web-device-123
  Content-Type: application/json
Body:
{
  "phone": "encrypted_phone",
  "password": "password123",
  "rememberMe": true,
  "platform": "WEB"
}

期望结果：
- 返回normalToken和autoLoginToken
- 响应头包含Set-Cookie（7天过期）
- autoExpireTime为7天后
```

### **测试2：Web端登录（8小时令牌）**
```bash
POST /api/v1/users/login
Headers:
  Device-Id: web-device-123
  Content-Type: application/json
Body:
{
  "phone": "encrypted_phone", 
  "password": "password123",
  "rememberMe": false,
  "platform": "WEB"
}

期望结果：
- 返回normalToken和autoLoginToken
- 响应头包含Set-Cookie（8小时过期）
- autoExpireTime为8小时后
```

### **测试3：Web端自动登录**
```bash
POST /api/v1/users/autologin-web
Headers:
  Device-Id: web-device-123
  Cookie: autoLoginToken=Bearer_xxx

期望结果：
- 返回新的normalToken
- 第3次使用时：7天令牌轮换Cookie，8小时令牌清除Cookie
```

### **测试4：小程序自动登录**
```bash
POST /api/v1/users/autologin-wx
Headers:
  Device-Id: wx-device-456
  Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...

期望结果：
- 返回新的normalToken
- 第3次使用时：7天令牌返回newAutoLoginToken，8小时令牌不返回
```

### **测试5：Web端退出登录**
```bash
DELETE /api/v1/users/logout
Headers:
  Device-Id: web-device-123
  Authorization: Bearer normalToken
Body:
{
  "userId": "user-uuid",
  "platform": "WEB",
  "logoutAllDevices": false
}

期望结果：
- 响应头包含清除Cookie指令
- 返回revokedJtis列表
```

---

## 🔧 **配置验证**

### **application.properties 新增配置**
```properties
# 8小时自动登录令牌
jwt.expiration.auto-login-short=28800000

# Cookie配置
cookie.domain=
cookie.secure=false
cookie.same-site=Lax
```

---

## 🚨 **已知问题**

1. **AdminTokenService警告**：JWT签名方法已弃用，但不影响功能
2. **向后兼容**：现有客户端需要更新以支持新的自动登录接口

---

## 📋 **部署检查清单**

- [ ] 确认配置文件中的Cookie设置适合环境
- [ ] 生产环境设置`cookie.secure=true`
- [ ] 验证Redis连接正常
- [ ] 测试跨域Cookie传输
- [ ] 验证Device-Id头传输
- [ ] 测试令牌轮换机制
- [ ] 验证8小时令牌注销逻辑

---

## 🎯 **下一步建议**

1. **前端集成**：更新Web端和小程序端的登录逻辑
2. **监控告警**：添加令牌使用统计和异常监控
3. **性能优化**：考虑Redis连接池优化
4. **安全加固**：添加IP白名单和频率限制
