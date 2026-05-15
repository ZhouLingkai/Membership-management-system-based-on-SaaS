## 附录B：阿里云OSS前端上传使用指南

> 本指南总结了前端通过STS临时凭证直接上传文件到阿里云OSS的完整实现方案，可供微信小程序或Vue3 Web应用开发参考。

### B.1 技术架构

```
┌──────────────┐    1. 请求STS凭证    ┌──────────────┐    2. 获取临时凭证    ┌──────────────┐
│   前端应用    │ ──────────────────> │   后端服务    │ ──────────────────> │  阿里云STS   │
│  (Web/小程序) │ <────────────────── │   (Java)     │ <────────────────── │              │
└──────────────┘    返回凭证信息      └──────────────┘    返回AccessKey等   └──────────────┘
       │                                                                           
       │ 3. 使用STS凭证直接上传                                                     
       ▼                                                                           
┌──────────────┐                                                                   
│  阿里云OSS   │                                                                   
│   Bucket    │                                                                   
└──────────────┘                                                                   
```

### B.2 后端配置（Spring Boot）

#### B.2.1 STS凭证获取接口

```
GET /api/v1/sts/credentials
Authorization: Bearer {用户令牌}
```

返回数据结构：

```json
{
  "code": 200,
  "data": {
    "accessKeyId": "STS.xxxxxx",
    "accessKeySecret": "xxxxxx",
    "securityToken": "CAISxxxxxx",
    "expiration": "2025-11-27T21:00:00Z",
    "endpoint": "https://oss-cn-hangzhou.aliyuncs.com",
    "region": "oss-cn-hangzhou",
    "bucket": "ecards-test1",
    "pathPrefix": "merchant/userId/"
  }
}
```

### B.3 前端实现（HTML/JavaScript）

#### B.3.1 引入阿里云OSS SDK

```html
<!-- 引入阿里云OSS JavaScript SDK -->
<script src="https://gosspublic.alicdn.com/aliyun-oss-sdk-6.18.0.min.js"></script>
```

#### B.3.2 STS凭证缓存管理

```javascript
// 全局变量缓存STS凭证
let stsCredentials = null;

// 获取STS临时凭证（带缓存，提前5分钟刷新）
async function getStsCredentials() {
    // 检查缓存的凭证是否还有效
    if (stsCredentials && stsCredentials.expirationTime) {
        const now = new Date().getTime();
        const expTime = new Date(stsCredentials.expirationTime).getTime();
        if (expTime - now > 5 * 60 * 1000) { // 提前5分钟刷新
            console.log('使用缓存的STS凭证');
            return stsCredentials;
        }
    }

    // 获取新的STS凭证
    const result = await apiRequest('/api/v1/sts/credentials', 'GET', null, true);
    if (result.code === 200 && result.data) {
        stsCredentials = result.data;
        stsCredentials.expirationTime = result.data.expiration;
        console.log('获取新的STS凭证成功');
        return stsCredentials;
    } else {
        throw new Error('获取STS凭证失败: ' + result.message);
    }
}
```

#### B.3.3 图片压缩函数

```javascript
// 图片压缩（循环压缩直到满足大小要求）
async function compressImage(file, maxSizeMB = 5) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = (e) => {
            const img = new Image();
            img.onload = () => {
                let quality = 0.9;
                let canvas = document.createElement('canvas');
                let ctx = canvas.getContext('2d');
                
                canvas.width = img.width;
                canvas.height = img.height;
                ctx.drawImage(img, 0, 0);
                
                // 循环压缩直到文件大小满足要求
                const compress = () => {
                    canvas.toBlob((blob) => {
                        if (blob.size <= maxSizeMB * 1024 * 1024 || quality <= 0.1) {
                            resolve(blob);
                        } else {
                            quality -= 0.1;
                            compress();
                        }
                    }, file.type, quality);
                };
                compress();
            };
            img.onerror = reject;
            img.src = e.target.result;
        };
        reader.onerror = reject;
        reader.readAsDataURL(file);
    });
}
```

#### B.3.4 上传文件到OSS

```javascript
// 上传文件到OSS（返回Object路径，用于数据库存储）
async function uploadToOss(file, fileName) {
    try {
        // 1. 获取STS凭证
        const credentials = await getStsCredentials();
        
        // 2. 初始化OSS客户端
        const client = new OSS({
            endpoint: credentials.endpoint,
            region: credentials.region,
            accessKeyId: credentials.accessKeyId,
            accessKeySecret: credentials.accessKeySecret,
            stsToken: credentials.securityToken,
            bucket: credentials.bucket
        });
        
        // 3. 生成对象名（路径）
        const objectName = credentials.pathPrefix + fileName;
        
        // 4. 上传文件
        const result = await client.put(objectName, file);
        
        // 5. 返回Object路径（用于数据库存储）
        console.log('文件上传成功，Object路径:', objectName);
        return objectName;
        
    } catch (error) {
        console.error('OSS上传失败:', error);
        throw new Error('文件上传失败: ' + error.message);
    }
}
```

#### B.3.5 生成签名URL（前端计算，用于图片预览）

```javascript
// 使用STS凭证生成签名URL（前端计算，无需后端参与）
async function generateSignedUrlWithSts(objectName) {
    try {
        // 1. 获取STS凭证
        const credentials = await getStsCredentials();
        
        // 2. 创建OSS客户端
        const client = new OSS({
            endpoint: credentials.endpoint,
            region: credentials.region,
            accessKeyId: credentials.accessKeyId,
            accessKeySecret: credentials.accessKeySecret,
            stsToken: credentials.securityToken,
            bucket: credentials.bucket
        });
        
        // 3. 前端直接生成签名URL（30分钟有效期）
        const signedUrl = client.signatureUrl(objectName, {
            expires: 1800  // 30分钟
        });
        
        console.log('前端生成签名URL成功:', objectName);
        return signedUrl;
        
    } catch (error) {
        console.error('生成签名URL失败:', error);
        throw new Error('生成签名URL失败: ' + error.message);
    }
}
```

#### B.3.6 删除OSS文件

```javascript
// 删除 OSS 文件
async function deleteFromOss(objectName) {
    try {
        const credentials = await getStsCredentials();
        const client = new OSS({
            endpoint: credentials.endpoint,
            region: credentials.region,
            accessKeyId: credentials.accessKeyId,
            accessKeySecret: credentials.accessKeySecret,
            stsToken: credentials.securityToken,
            bucket: credentials.bucket
        });
        await client.delete(objectName);
        console.log('文件删除成功:', objectName);
    } catch (error) {
        console.error('文件删除失败:', error);
        throw new Error('文件删除失败: ' + error.message);
    }
}
```

#### B.3.7 完整上传流程示例

```javascript
// 处理文件选择（完整流程）
document.getElementById('fileInput').addEventListener('change', async function(e) {
    const file = e.target.files[0];
    if (!file) return;
    
    const previewDiv = document.getElementById('preview');
    const urlInput = document.getElementById('ossUrl');
    
    // 1. 检查文件类型
    if (!file.type.match('image/(jpeg|jpg|png)')) {
        alert('只支持JPG和PNG格式');
        e.target.value = '';
        return;
    }
    
    try {
        previewDiv.innerHTML = '<p>⏳ 处理中...</p>';
        
        // 2. 删除旧文件（如果存在）
        const oldUrl = urlInput.value;
        if (oldUrl) {
            try {
                await deleteFromOss(oldUrl);
            } catch (error) {
                console.warn('删除旧文件失败:', error.message);
            }
        }
        
        // 3. 压缩图片（如果需要）
        let fileToUpload = file;
        if (file.size > 5 * 1024 * 1024) {
            previewDiv.innerHTML = '<p>⏳ 图片较大，正在压缩...</p>';
            fileToUpload = await compressImage(file);
        }
        
        // 4. 生成文件名
        const timestamp = Date.now();
        const ext = file.name.substring(file.name.lastIndexOf('.'));
        const fileName = `photo_${timestamp}${ext}`;
        
        // 5. 上传到OSS
        previewDiv.innerHTML = '<p>⏳ 正在上传到OSS...</p>';
        const objectName = await uploadToOss(fileToUpload, fileName);
        
        // 6. 保存Object路径（用于提交到后端）
        urlInput.value = objectName;
        
        // 7. 生成签名URL用于预览
        const signedUrl = await generateSignedUrlWithSts(objectName);
        
        // 8. 显示预览
        previewDiv.innerHTML = `
            <img src="${signedUrl}" style="max-width: 200px;">
            <p style="color: green;">✓ 上传成功</p>
        `;
        
    } catch (error) {
        previewDiv.innerHTML = `<p style="color: red;">✗ 上传失败: ${error.message}</p>`;
        e.target.value = '';
        urlInput.value = '';
    }
});
```

### B.4 微信小程序适配

微信小程序无法使用浏览器端的OSS SDK，需要使用以下方式：

```javascript
// 微信小程序上传到OSS
async function uploadToOssWechat(filePath, fileName) {
    // 1. 获取STS凭证
    const credentials = await getStsCredentials();
    
    // 2. 构建上传参数
    const objectName = credentials.pathPrefix + fileName;
    const host = `https://${credentials.bucket}.${credentials.region}.aliyuncs.com`;
    
    // 3. 使用wx.uploadFile上传
    return new Promise((resolve, reject) => {
        wx.uploadFile({
            url: host,
            filePath: filePath,
            name: 'file',
            formData: {
                'key': objectName,
                'OSSAccessKeyId': credentials.accessKeyId,
                'policy': getPolicy(credentials), // 需要自行实现policy生成
                'signature': getSignature(credentials), // 需要自行实现签名
                'x-oss-security-token': credentials.securityToken
            },
            success: (res) => {
                if (res.statusCode === 204 || res.statusCode === 200) {
                    resolve(objectName);
                } else {
                    reject(new Error('上传失败'));
                }
            },
            fail: reject
        });
    });
}
```

### B.5 Vue3 适配要点

```javascript
// Vue3 Composition API 示例
import { ref } from 'vue';

export function useOssUpload() {
    const uploading = ref(false);
    const progress = ref(0);
    
    const upload = async (file) => {
        uploading.value = true;
        try {
            const objectName = await uploadToOss(file, generateFileName(file));
            return objectName;
        } finally {
            uploading.value = false;
        }
    };
    
    const getPreviewUrl = async (objectName) => {
        return await generateSignedUrlWithSts(objectName);
    };
    
    return { upload, getPreviewUrl, uploading, progress };
}
```

### B.6 安全注意事项

1. **永远不要在前端暴露主AccessKey**，只使用STS临时凭证
2. **STS凭证有效期建议30分钟**，前端提前5分钟刷新
3. **限制STS权限范围**：只允许上传到指定路径前缀
4. **服务端验证**：后端接收到Object路径后需验证路径格式
5. **文件类型校验**：前后端都需要校验文件类型和大小