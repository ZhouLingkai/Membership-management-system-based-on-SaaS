/**
 * utils/validator-util.js
 * 高性能正则表达式表单验证工具（适用于微信小程序会员系统，JavaScript 版）
 *
 * 功能：
 * - 手机号验证（中国大陆：11位，以 13/14/15/17/18/19 开头）
 * - 邮箱验证（基础邮箱格式）
 * - 密码强度验证（≥8 位，需包含字母和数字；提供 weak/medium/strong 强度等级）
 * - 身份证号验证（18 位，最后一位可为 X/x，仅做基础格式校验）
 *
 * 返回：
 * - 手机/邮箱/身份证：{ valid: boolean, message: string }
 * - 密码：{ valid: boolean, level: 'weak' | 'medium' | 'strong', message: string }
 */

/**
 * 手机号验证（中国大陆）
 * 规则：
 * - 必填（空值提示“请输入手机号”）
 * - 格式：11 位数字，且以 13/14/15/17/18/19 开头
 *   正则：^1(?:3\\d|4\\d|5\\d|7\\d|8\\d|9\\d)\\d{8}$
 * @param {string} input
 * @returns {{ valid: boolean, message: string }}
 */
function validatePhone(input) {
	const value = (input || '').trim();
	if (!value) {
		return { valid: false, message: '请输入手机号' };
	}
	const pattern = /^1(?:3\d|4\d|5\d|7\d|8\d|9\d)\d{8}$/;
	const valid = pattern.test(value);
	
	return { valid, message: valid ? '' : '请输入正确的手机号' };
}

/**
 * 邮箱验证
 * 规则：
 * - 必填（空值提示“请输入邮箱地址”）
 * - 基础格式：本地部分@域名，域名至少一段点分后缀（长度≥2）
 *   正则：^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$
 *   为性能与兼容性折中，未覆盖所有 RFC 边界情况
 * @param {string} input
 * @returns {{ valid: boolean, message: string }}
 */
function validateEmail(input) {
	const value = (input || '').trim();
	if (!value) {
		return { valid: false, message: '请输入邮箱地址' };
	}
	const pattern = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/i;
	const valid = pattern.test(value);
	return { valid, message: valid ? '' : '请输入正确的邮箱地址' };
}

/**
 * 密码强度验证
 * 规则：
 * - 必填（空值提示"请输入密码"）
 * - 强度等级：
 *   - wrong：不满足weak（长度 < 8 或 不包含字母 或 不包含数字），valid=false
 *   - weak：长度 ≥ 8，同时包含字母 + 数字，且不满足medium，valid=true
 *   - medium：长度 ≥ 8，包含大写/小写/数字/特殊字符中的3种，且不满足strong，valid=true
 *   - strong：长度 ≥ 10，包含大写 + 小写 + 数字 + 特殊字符（4种都要有），valid=true
 * @param {string} input
 * @returns {{ valid: boolean, level: 'weak'|'medium'|'strong', message: string }}
 */
function validatePassword(input) {
	const value = (input || '').trim();
	if (!value) {
		return { valid: false, level: 'danger', message: '请输入密码' };
	}

	const length = value.length;
	const hasLower = /[a-z]/.test(value);
	const hasUpper = /[A-Z]/.test(value);
	const hasLetter = /[a-zA-Z]/.test(value);
	const hasDigit = /\d/.test(value);
	const hasSpecial = /[^a-zA-Z0-9]/.test(value);

	// wrong：不满足weak（长度 < 8 或 不包含字母 或 不包含数字）
	if (length < 8 || !hasLetter || !hasDigit) {
		return {
			valid: false,
			level: 'danger',
			message: '密码至少8位，需包含字母和数字',
		};
	}

	// 统计包含的字符类型数量（大写、小写、数字、特殊字符）
	let typeCount = 0;
	if (hasUpper) typeCount++;
	if (hasLower) typeCount++;
	if (hasDigit) typeCount++;
	if (hasSpecial) typeCount++;

	// strong：长度 ≥ 10，且包含大写 + 小写 + 数字 + 特殊字符（4种都要有）
	if (length >= 10 && hasUpper && hasLower && hasDigit && hasSpecial) {
		return { valid: true, level: 'strong', message: '密码强度高' };
	}

	// medium：长度 ≥ 8，包含大写/小写/数字/特殊字符中的3种，且不满足strong
	if (typeCount >= 3) {
		return { valid: true, level: 'medium', message: '密码强度合适' };
	}

	// weak：长度 ≥ 8，同时包含字母 + 数字，且不满足medium
	return { valid: true, level: 'weak', message: '密码强度低，建议包含大写/小写/数字/特殊字符中的3种' };
}

/**
 * 身份证号验证（基础格式）
 * 规则：
 * - 必填（空值提示“请输入身份证号”）
 * - 格式：18 位；前 17 位为数字；最后一位为数字或 X/x
 *   正则：^\\d{17}[\\dXx]$
 * - 仅做基础位数与末位 X 校验；不校验地址码与校验位算法
 * @param {string} input
 * @returns {{ valid: boolean, message: string }}
 */
function validateIdCard(input) {
	const value = (input || '').trim();
	if (!value) {
		return { valid: false, message: '请输入身份证号' };
	}
	const pattern = /^\d{17}[\dXx]$/;
	const valid = pattern.test(value);
	return { valid, message: valid ? '' : '请输入正确的身份证号' };
}

const validatorUtil = {
	validatePhone,
	validateEmail,
	validatePassword,
	validateIdCard,
};

module.exports = {
	validatorUtil,
	// 也支持按需导出函数
	validatePhone,
	validateEmail,
	validatePassword,
	validateIdCard,
};

/**
 * 使用示例（在页面或组件 JS 中）：
 *
 * const { validatorUtil } = require('../../utils/validator-util.js');
 *
 * const phoneRes = validatorUtil.validatePhone('13800138000');
 * if (!phoneRes.valid) {
 *   wx.showToast({ title: phoneRes.message, icon: 'none' });
 * }
 *
 * const emailRes = validatorUtil.validateEmail('user@example.com');
 * if (!emailRes.valid) {
 *   wx.showToast({ title: emailRes.message, icon: 'none' });
 * }
 *
 * const pwdRes = validatorUtil.validatePassword('Aa123456!');
 * if (!pwdRes.valid) {
 *   wx.showToast({ title: pwdRes.message, icon: 'none' });
 * } else {
 *   console.log('密码强度：', pwdRes.level); // 'medium' | 'strong'
 * }
 *
 * const idRes = validatorUtil.validateIdCard('11010119900307827X');
 * if (!idRes.valid) {
 *   wx.showToast({ title: idRes.message, icon: 'none' });
 * }
 */


