### 通用工具类的描述

**1. /utils/encode.js**

包含 AES 加密/解密工具，用于加密解密手机号。例如 API 文档中在请求参数中提到手机号需要加密后传输，或者从后端收到的手机号是密文后，需要调用该工具加密解密（encryptAES/decryptAES）。

---

**2. /utils/request.js**

HTTP 请求封装工具，基于 wx.request，提供统一的请求入口（直接封装与后端的联系）。自动补全请求URL（BASE_URL = 'http://localhost:8087/api'，从v1）自动补充公共请求头（Device-ID、X-Request-ID），内置响应拦截器（401 跳转登录、403 权限提示）。主要方法包括 request.get、request.post、request.put、request.patch、request.delete，以及辅助方法 generateDeviceId、generateUUID、md5。
使用示例（登录接口）：
	    // 加密手机号
            const encryptedPhone = encryptAES(phone);
            // 调用登录接口
            const res = await post(
                '/v1/users/login',  // 从v1开始
                { 'Content-Type': 'application/json' },
                {   // 请求体
                    phone: encryptedPhone,
                    password: password,
                    rememberMe: rememberMe,
                    platform: 'MINI_PROGRAM'
                }
            );
            console.log(res)  // 每次调用建议附带一句打印
	    if(res.code != 200) {
		   this.showCustomToast(`${res.code}: ${res.message}`, 'danger');  // 自定义组件-/custom-toast
		   return;
	    }
	    res.data（响应参数）
---

**3. /utils/storage-utils.js**

本地存储工具，封装 wx.Storage API，支持自动序列化/反序列化、支持设置过期时间（默认 30 天）、统一 key 前缀。提供异步方法（set、get、remove、clear）和同步方法（setSync、getSync、removeSync、clearSync）。使用时可设置过期时间，例如 set(key, value, {expire: 7200}) 表示 2 小时后过期。

---

**4. /utils/time-util.js**

时间戳与日期处理工具，用于时间格式转换。支持秒/毫秒时间戳自动识别，兼容多种日期字符串格式。主要方法：formatTimestamp 将时间戳转为字符串（支持 date/time/datetime 模式）；parseToTimestamp 将字符串转为时间戳；getCurrentTime 获取当前时间（返回多种格式）；timestampAfterDays 计算 X 天后的时间戳。

---

**5. /utils/token.js**

令牌管理工具，管理多种令牌类型（普通令牌、特权令牌、工作令牌、管理令牌、自动登录令牌）。支持令牌获取、存储（内存 + 本地）、读取、过期检查、自动刷新（过期前 10 分钟）。主要方法包括：getNormalToken/saveNormalToken 用于普通令牌；getWorkToken/saveWorkToken 用于工作令牌（需传入 storeId）；getPrivilegeToken/savePrivilegeToken 用于特权令牌；fetchNormalToken/fetchWorkToken 从后端获取令牌；logout 登出清除所有令牌。

特别重要：后续开发的接口都需要令牌，需要请求前调用get***Token来获取令牌。

---

**6. /utils/validator-util.js**
一般用不到。

表单验证工具（部分），使用正则表达式进行高性能验证，返回 {valid, message} 格式结果。主要方法：validatePhone 验证手机号（中国大陆 11 位）；validateEmail 验证邮箱格式；validatePassword 验证密码强度（返回 weak/medium/strong 等级）；validateIdCard 验证身份证号（18 位基础格式）。验证失败时会在 message 中返回友好提示。