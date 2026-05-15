const { getTime } = require('/opt/nodejs/timeUtils');

exports.main = async (event, context) => {
    // ===================== 1. 获取 HTTP 请求核心信息 =====================
    // 请求方式：GET / POST / PUT 等
    const requestMethod = event.httpMethod || 'UNKNOWN';

    // 请求头（全部获取）
    const requestHeaders = event.headers || {};

    // ===================== 2. 解析请求参数（自动适配 GET/POST） =====================
    let requestParams = {};       // 业务参数
    let requestBody = null;       // POST 请求体（GET 为 null）

    try {
        if (requestMethod === 'GET') {
            // GET 请求：参数在 queryString
            requestParams = event.queryString || {};
        }
        else if (requestMethod === 'POST') {
            // POST 请求：解析 body  JSON 字符串
            requestBody = event.body ? JSON.parse(event.body) : {};
            requestParams = requestBody;
        }
    } catch (e) {
        console.error('参数解析失败：', e);
        requestParams = {};
    }

    // ===================== 3. 原有业务逻辑（获取时间） =====================
    // 从请求参数中获取 timezone，兼容 GET/POST
    const timezone = requestParams.timezone || 'Asia/Shanghai';
    const result = getTime(timezone);

    // ===================== 4. 组装返回结果 =====================
    const response = {
        code: 0,
        message: 'success',
        // 核心返回：请求信息
        requestInfo: {
            requestMethod: requestMethod,    // 请求方式
            requestHeaders: requestHeaders   // 请求头
        },
        data: result
    };

    // GET 请求不返回 requestBody，POST 才返回
    if (requestMethod === 'POST') {
        response.requestInfo.requestBody = requestBody;
    }

    console.log('[getTime] 调用成功:', JSON.stringify(response));
    return response;
};