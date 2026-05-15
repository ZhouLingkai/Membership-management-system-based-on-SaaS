const { timeUtil } = require('../../utils/time-util.js');
const { validatorUtil } = require('../../utils/validator-util.js');

Page({
	data: {
		// time-util
		tsInput: '',
		dtInput: '',
		tsFormatResult: '',
		strToTsResult: '',
		// validator-util
		phone: '',
		email: '',
		password: '',
		idcard: '',
		validateResult: '',
		// storage-utils
		storeKey: 'demo',
		storeVal: '',
		expireSec: '',
		storageResult: '',
	},

	// 时间工具事件
	onChangeTs(e) {
		this.setData({ tsInput: e.detail.value });
	},
	onChangeStr(e) {
		this.setData({ dtInput: e.detail.value });
	},
	fmtDate() {
		const { tsInput } = this.data;
		const res = timeUtil.formatTimestamp(Number(tsInput), 'date');
		this.setData({ tsFormatResult: res || '无效时间戳' });
	},
	fmtTime() {
		const { tsInput } = this.data;
		const res = timeUtil.formatTimestamp(Number(tsInput), 'time');
		this.setData({ tsFormatResult: res || '无效时间戳' });
	},
	fmtDatetime() {
		const { tsInput } = this.data;
		const res = timeUtil.formatTimestamp(Number(tsInput), 'datetime');
		this.setData({ tsFormatResult: res || '无效时间戳' });
	},
	getNow() {
		const now = timeUtil.nowTimestamp();
		this.setData({ tsInput: String(now), tsFormatResult: String(now) });
	},
	after3Days() {
		const base = this.data.tsInput ? Number(this.data.tsInput) : undefined;
		const ts = timeUtil.timestampAfterDays(3, base);
		this.setData({ tsFormatResult: String(ts) });
	},
	strToTs() {
		const ts = timeUtil.parseToTimestamp(this.data.dtInput);
		this.setData({ strToTsResult: isNaN(ts) ? '解析失败' : String(ts) });
	},

	// 验证工具事件
	onChangePhone(e) { this.setData({ phone: e.detail.value }); },
	onChangeEmail(e) { this.setData({ email: e.detail.value }); },
	onChangePwd(e) { this.setData({ password: e.detail.value }); },
	onChangeId(e) { this.setData({ idcard: e.detail.value }); },
	checkAll() {
		const p = validatorUtil.validatePhone(this.data.phone);
		const e = validatorUtil.validateEmail(this.data.email);
		const w = validatorUtil.validatePassword(this.data.password);
		const i = this.data.idcard ? validatorUtil.validateIdCard(this.data.idcard) : { valid: true, message: '' };
		const msgs = [];
		if (!p.valid) msgs.push(p.message);
		if (!e.valid) msgs.push(e.message);
		if (!w.valid) msgs.push(w.message);
		if (!i.valid) msgs.push(i.message);
		const text = msgs.length ? msgs.join('；') : `通过（密码强度：${w.level}）`;
		this.setData({ validateResult: text });
		if (msgs.length) {
			wx.showToast({ title: '存在不通过项', icon: 'none' });
		} else {
			wx.showToast({ title: '全部通过', icon: 'success' });
		}
	},

	// 存储工具事件
	onChangeStoreKey(e) { this.setData({ storeKey: e.detail.value }); },
	onChangeStoreVal(e) { this.setData({ storeVal: e.detail.value }); },
	onChangeExpire(e) { this.setData({ expireSec: e.detail.value }); },
	async setAsync() {
		const { storeKey, storeVal, expireSec } = this.data;
		const value = this.tryParse(storeVal);
		try {
			const expireTime = Number(expireSec) ? Date.now() + Number(expireSec) * 1000 : undefined;
			const data = expireTime ? { value, expireTime } : value;
			wx.setStorageSync(storeKey, data);
			this.setData({ storageResult: '异步 set 成功' });
		} catch (e) {
			this.setData({ storageResult: '异步 set 失败' });
		}
	},
	async getAsync() {
		const { storeKey } = this.data;
		try {
			const data = wx.getStorageSync(storeKey);
			let val = data;
			if (data && data.expireTime && Date.now() > data.expireTime) {
				wx.removeStorageSync(storeKey);
				val = null;
			} else if (data && data.value !== undefined) {
				val = data.value;
			}
			this.setData({ storageResult: `异步 get：${JSON.stringify(val)}` });
		} catch (e) {
			this.setData({ storageResult: `异步 get：${e.message}` });
		}
	},
	async removeAsync() {
		const { storeKey } = this.data;
		try {
			wx.removeStorageSync(storeKey);
			this.setData({ storageResult: '异步 remove 成功' });
		} catch (e) {
			this.setData({ storageResult: '异步 remove 失败' });
		}
	},
	async clearAsync() {
		try {
			wx.clearStorageSync();
			this.setData({ storageResult: '异步 clear 成功' });
		} catch (e) {
			this.setData({ storageResult: '异步 clear 失败' });
		}
	},
	setSync() {
		const { storeKey, storeVal, expireSec } = this.data;
		const value = this.tryParse(storeVal);
		try {
			const expireTime = Number(expireSec) ? Date.now() + Number(expireSec) * 1000 : undefined;
			const data = expireTime ? { value, expireTime } : value;
			wx.setStorageSync(storeKey, data);
			this.setData({ storageResult: '同步 set 成功' });
		} catch (e) {
			this.setData({ storageResult: '同步 set 失败' });
		}
	},
	getSync() {
		const { storeKey } = this.data;
		try {
			const data = wx.getStorageSync(storeKey);
			let val = data;
			if (data && data.expireTime && Date.now() > data.expireTime) {
				wx.removeStorageSync(storeKey);
				val = null;
			} else if (data && data.value !== undefined) {
				val = data.value;
			}
			this.setData({ storageResult: `同步 get：${JSON.stringify(val)}` });
		} catch (e) {
			this.setData({ storageResult: `同步 get：${e.message}` });
		}
	},
	removeSync() {
		const { storeKey } = this.data;
		try {
			wx.removeStorageSync(storeKey);
			this.setData({ storageResult: '同步 remove 成功' });
		} catch (e) {
			this.setData({ storageResult: '同步 remove 失败' });
		}
	},
	clearSync() {
		try {
			wx.clearStorageSync();
			this.setData({ storageResult: '同步 clear 成功' });
		} catch (e) {
			this.setData({ storageResult: '同步 clear 失败' });
		}
	},

	tryParse(str) {
		if (!str) return '';
		try {
			return JSON.parse(str);
		} catch (_) {
			return str;
		}
	},
});


