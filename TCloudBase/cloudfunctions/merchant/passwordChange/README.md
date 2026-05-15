# merchant-passwordChange

修改密码（需登录态 accessToken）。

## argon2 依赖说明

本函数直接依赖 `@node-rs/argon2` 进行密码哈希与验证，**不通过层（Layer）提供**。
原因：`@node-rs/argon2` 包含平台原生二进制，层在 Windows 本地安装时只有 Win32 二进制，
上传到云端 Linux 环境会导致加载失败，因此改为由各云函数在云端独立安装。

## 云端部署

TCB 在部署云函数时会自动读取 `package.json` 并执行 `npm install`，会拉取 Linux x64 的正确二进制。
**无需手动操作**，确保 `package.json` 中已声明：

```json
"dependencies": {
  "@node-rs/argon2": "^2.0.2"
}
```

## 本地开发

本地 Windows 环境**不需要**安装此依赖（无法直接安装 Linux 二进制）。
如需在本地 Linux/WSL 环境测试，在函数目录执行：

```bash
npm install
```
