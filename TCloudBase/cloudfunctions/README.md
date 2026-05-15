# 云函数目录说明

## 依赖说明

所有云函数均依赖 `/opt/nodejs/` 下的公共层（Layer），层内已包含：

| 包 | 版本 | 用途 |
|---|---|---|
| `jsonwebtoken` | ^9.0.2 | JWT 签发与验证 |
| `uuid` | ^9.0.1 | UUID v4 生成 |

`@cloudbase/node-sdk` 用到需写入package.json的dependencies中。
`@node-rs/argon2` 用到需写入package.json的dependencies中。
## 各云函数 package.json 无第三方依赖，部署前无需 `npm install`

> 若本地调试需要，可在对应函数目录执行：
> ```bash
> npm install @cloudbase/node-sdk
> ```
> 但**不要将 node_modules 提交或上传**，线上运行时由环境自动注入。

---

## 目录结构

```
cloudfunctions/
├── token/
│   ├── work/          获取商家工作令牌③（需 access 令牌②）
│   ├── privilege/     获取商家特权令牌⑧（需 access 令牌② + 二级密码）
│   ├── memberQrcode/  获取会员二维码令牌⑨（需会员业务令牌⑦）
│   ├── refresh/       刷新 access/work 令牌（剩余≤10分钟才允许）
│   └── logout/        批量令牌注销，写入黑名单（幂等）
└── merchant/
    ├── verifyCode/    发送短信验证码（type=1注册 / type=2找回密码）
    ├── login/         注册(type=1) / 账密登录(type=2) / 自动登录(type=3)
    ├── info/          查询商家信息
    ├── infoUpdate/    修改商家信息
    ├── passwordChange/ 修改密码（需旧密码验证）
    ├── passwordReset/  重置密码（短信验证码验证，无需登录）
    └── sndPswdChange/  修改二级密码（短信验证码验证，需已登录）
```

## 手机号安全规范

- **前端传输**：AES-256-CBC 加密，格式 `Base64(IV + 密文)`
- **云函数存储**：收到后立即 `decryptPhone()` 解密，以 11 位**明文**写入数据库
- **查询**：所有 `where({ phone })` 直接用明文匹配，无随机 IV 问题

## 运行环境

Node.js 18 / 20，腾讯 CloudBase 云函数（HTTP 触发器）
