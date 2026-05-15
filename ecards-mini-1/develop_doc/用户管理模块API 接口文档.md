# 会员管理系统 API 实际接口文档

---

## 1. 错误码说明

### 1.1 令牌相关错误码 (10xxx)
| 错误码 | 说明 |
|--------|------|
| 10001 | 令牌不存在 |
| 10002 | 令牌已过期 |
| 10003 | 令牌已被加入黑名单 |
| 10004 | 令牌权限不足 |
| 10005 | 令牌无效 |
| 10006 | 设备ID不匹配 |

### 1.2 用户相关错误码 (20xxx)
| 错误码 | 说明 |
|--------|------|
| 20001 | 手机号已注册 |
| 20002 | 验证码无效或已过期 |
| 20003 | 用户信息不存在 |
| 20004 | 密码错误 |
| 20005 | 权限不足 |

### 1.3 商家相关错误码 (30xxx)
| 错误码 | 说明 |
|--------|------|
| 30001 | 非普通用户，无法申请商家 |
| 30002 | 商家信息不存在 |
| 30003 | CDK无效或已使用 |

### 1.4 店铺相关错误码 (40xxx)
| 错误码 | 说明 |
|--------|------|
| 40001 | 非商家用户，无店铺操作权限 |
| 40002 | 店铺不存在 |
| 40003 | 二级密码错误 |

### 1.5 员工相关错误码 (50xxx)
| 错误码 | 说明 |
|--------|------|
| 50001 | 员工已关联该店铺 |
| 50002 | 员工未关联该店铺 |

### 1.6 参数相关错误码 (90xxx)
| 错误码 | 说明 |
|--------|------|
| 90001 | 参数错误 |

### 1.7 系统异常 (99999)
| 错误码 | 说明 |
|--------|------|
| 99999 | 系统异常 |

---

## 3. 用户管理模块

### 3.0 接口列表
| 序号 | 接口名称 | 请求地址 | 请求方法 |
|------|----------|----------|----------|
| 6 | 用户信息查询 | /api/v1/users/info | GET |
| 7 | 用户信息修改 | /api/v1/users/info | PUT |
| 8 | 用户主动退出 | /api/v1/users/logout | DELETE |
| 9 | 密码重置 | /api/v1/users/password/reset | POST |
---

### 3.6 接口6：用户信息查询

#### 3.6.1 基础信息
- **请求地址**：`/api/v1/users/info`
- **请求方法**：`GET`
- **权限说明**：需要携带普通令牌（通过JWT认证过滤器验证）
- **接口简介**：查询用户自身基础信息（昵称、头像、手机号、邀请码等），只能查询自己的信息

#### 3.6.2 请求参数

**请求头（Header）**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| Authorization | String | 是 | Bearer {普通令牌} |


**返回参数**
| 参数名 | 类型 | 说明 |
|--------|------|------|
| userInfo | Object | 用户信息对象 |
| userInfo.userId | String | 用户ID |
| userInfo.nickname | String | 用户昵称 |
| userInfo.userType | Integer | 用户类型：1-普通用户，2-商家，3-员工 |
| userInfo.avatar | String | 用户头像URL |
| userInfo.inviteCode | String | 用户自己的邀请码（供他人注册时填写） |
| userInfo.memberAvatar | String | 会员头像URL |
| userInfo.phone | String | 手机号（AES-256-CBC加密后） |
| userInfo.invitedCode | String | 注册时填写的邀请码（邀请者的邀请码） |
| userInfo.registerTime | String | 注册时间（yyyy-MM-dd HH:mm:ss） |

| userInfo.merchantInfo | Object | 商家信息（仅userType=2时返回） |
| userInfo.merchantInfo.merchantId | String | 商户ID |
| userInfo.merchantInfo.merchantName | String | 商户名称 |
| userInfo.merchantInfo.certification | Integer | 认证状态：1-已认证，2-测试中，3-审核中，4-审核拒绝，5-测试期过，6-认证存疑 |
| userInfo.merchantInfo.merchantLevel | Integer | 商家特权等级：1-普通，2-VIP，3-SVIP |
| userInfo.merchantInfo.testExpireTime | String | 测试期过期时间（certification=2时返回） |

#### 3.4.3 可能的错误码
10001, 10002, 10005, 20003, 20005

---

### 3.5 接口5：用户信息修改

#### 3.5.1 基础信息
- **请求地址**：`/api/v1/users/info`
- **请求方法**：`PUT`
- **权限说明**：需要携带普通令牌（通过JWT认证过滤器验证）
- **接口简介**：修改用户自身非敏感信息（昵称、头像等），只能修改自己的信息

#### 3.5.2 请求参数

**请求头（Header）**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| Authorization | String | 是 | Bearer {普通令牌} |

**请求体（Body）**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | String | 是 | 用户ID（必须与令牌中的userId一致） |
| nickname | String | 否 | 用户昵称（1-50位） |
| avatar | String | 否 | 用户头像URL（最大255字符） |
| memberAvatar | String | 否 | 会员头像URL（最大255字符） |

**返回参数**
| 参数名 | 类型 | 说明 |
|--------|------|------|
| updateTime | String | 信息更新时间（yyyy-MM-dd HH:mm:ss） |

#### 3.5.3 可能的错误码
10001, 10002, 10005, 20003, 20005, 90001

---

### 3.6 接口6：用户主动退出

#### 3.6.1 基础信息
- **请求地址**：`/api/v1/users/logout`
- **请求方法**：`DELETE`
- **权限说明**：需要携带普通令牌（通过JWT认证过滤器验证）
- **接口简介**：注销当前普通令牌与自动登录令牌，支持单设备退出或全设备退出（递增令牌版本号）

#### 3.6.2 请求参数

**请求头（Header）**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| Authorization | String | 是 | Bearer {普通令牌} |
| X-Device-ID | String | 是 | 设备唯一标识 |

**请求体（Body）**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | String | 是 | 用户ID（必须与令牌中的userId一致） |
| autoLoginToken | String | 否 | 自动登录令牌（存在则一并注销） |
| logoutAllDevices | Boolean | 否 | 是否全设备退出（默认false） |
| platform | String | 否 | 平台类型：WEB/MINI_PROGRAM（用于Cookie清理） |

**返回参数**
| 参数名 | 类型 | 说明 |
|--------|------|------|
| revokedJtis | List\<String\> | 已注销令牌的JTI列表 |

**特殊说明**
- Web端（platform=WEB）：自动清除autoLoginToken Cookie
- 小程序端：仅注销服务端令牌记录

#### 3.6.3 可能的错误码
10001, 10002, 10005, 20005

---

### 3.7 接口7：密码重置

#### 3.7.1 基础信息
- **请求地址**：`/api/v1/users/password/reset`
- **请求方法**：`POST`
- **权限说明**：无需认证（公开接口）
- **接口简介**：用户通过手机号+验证码重置登录密码，重置成功后令牌版本号递增，所有旧令牌失效

#### 3.7.2 请求参数

**请求头（Header）**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| X-Device-ID | String | 是 | 设备唯一标识 |

**请求体（Body）**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| phone | String | 是 | 手机号（AES-CBC加密后） |
| verifyCode | String | 是 | 短信验证码（6位数字） |
| newPassword | String | 是 | 新密码（明文，长度≥8位） |
| platform | String | 是 | 平台类型：MINI_PROGRAM/WEB |

**返回参数**
| 参数名 | 类型 | 说明 |
|--------|------|------|
| resetTime | String | 密码重置时间（yyyy-MM-dd HH:mm:ss） |

#### 3.7.3 可能的错误码
20002, 20003, 90001

---

## 4. 通用说明

### 4.1 统一响应格式
所有接口的响应均遵循以下格式：
```json
{
  "code": 0,
  "message": "成功信息或错误信息",
  "data": {...}
}
```
- `code`：0表示成功，非0表示失败（对应错误码）
- `message`：响应消息
- `data`：响应数据（成功时返回）

### 4.2 时间格式
所有时间字段均采用格式：`yyyy-MM-dd HH:mm:ss`

### 4.3 加密说明
- **手机号加密**：前端使用AES-CBC模式加密，密钥长度256位，IV长度16字节
- **密码加密**：后端使用Argon2算法加密存储

### 4.4 令牌版本机制
系统采用令牌版本号机制，当用户执行以下操作时，令牌版本号递增，所有旧令牌自动失效：
- 密码重置
- 全设备退出（logoutAllDevices=true）

### 4.5 令牌传递方式
除公开接口外，需认证的接口均需在请求头中携带令牌：
```
Authorization: Bearer {token}
```

---



