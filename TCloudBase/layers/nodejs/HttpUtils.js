'use strict';

/**
 * 解析云函数 event，统一返回请求三要素
 * 腾讯云开发云函数通过 event 接收 HTTP 触发器请求，无 Express req/res
 * @param {Object} event - 云函数 event 对象
 * @returns {{ method: string, headers: Object, params: Object }}
 */
function parseRequest(event) {
    const method = (event.httpMethod || 'UNKNOWN').toUpperCase();
    const headers = event.headers || {};
    let params = {};

    try {
        if (method === 'GET') {
            params = event.queryString || {};
        } else if (method === 'POST') {
            if (!event.body) {
                params = {};
            } else if (typeof event.body === 'object') {
                params = event.body;
            } else {
                params = JSON.parse(event.body);
            }
        }
    } catch (e) {
        params = {};
    }

    return { method, headers, params };
}

module.exports = { parseRequest };
