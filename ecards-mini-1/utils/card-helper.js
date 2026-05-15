/**
 * 会员卡种辅助工具
 * 用于构造cardBgc和cardMask字段
 */

/**
 * 获取渐变色序号
 * @param {string} gradientType - 渐变色类型
 * @returns {number} 序号 1-9
 */
function getGradientSeq(gradientType) {
  const seqMap = {
    'darkblack': 1,
    'gold': 2,
    'winered': 3,
    'originalgold': 4,
    'blue': 5,
    'red': 6,
    'orange': 7,
    'purple': 8,
    'sunset': 9
  };
  return seqMap[gradientType] || 1;
}

/**
 * 获取渐变方向编号
 * @param {string} direction - horizontal, vertical, diagonal
 * @returns {number} 1-水平, 2-垂直, 3-对角
 */
function getDirectionCode(direction) {
  const dirMap = {
    'horizontal': 1,
    'vertical': 2,
    'diagonal': 3
  };
  return dirMap[direction] || 3;
}

/**
 * 构造cardBgc字段
 * @param {object} params
 * @param {string} params.bgType - gradient, advanced, custom
 * @param {string} params.bgGradient - 渐变色类型
 * @param {string} params.bgGradientDirection - 渐变方向
 * @param {string} params.bgAdvanced - 高级背景文件名
 * @param {string} params.customBgOssPath - 自定义背景OSS路径
 * @returns {string} cardBgc字段值
 */
function buildCardBgc(params) {
  const { bgType, bgGradient, bgGradientDirection, bgAdvanced, customBgOssPath } = params;
  
  if (customBgOssPath) {
    // 自定义图片优先
    return customBgOssPath;
  }
  
  if (bgType === 'gradient') {
    // 渐变色：{direct}_{seq}
    const direct = getDirectionCode(bgGradientDirection);
    const seq = getGradientSeq(bgGradient);
    return `${direct}_${seq}`;
  }
  
  if (bgType === 'advanced') {
    // 高级背景：文件名
    return `bg_${bgAdvanced}.png`;
  }
  
  // 默认
  return '3_1';
}

/**
 * 构造cardMask字段
 * @param {object} params
 * @param {string} params.titleText - 标题文字
 * @param {boolean} params.showVip - 是否显示VIP
 * @param {boolean} params.showPattern - 是否显示图案
 * @param {string} params.maskOssPath - 蒙版OSS路径
 * @returns {string} cardMask字段值
 */
function buildCardMask(params) {
  const { titleText, showVip, showPattern, maskOssPath } = params;
  
  // 判断是否为空蒙版
  const isEmpty = (!titleText || titleText.trim() === '') && !showVip && !showPattern;
  
  if (isEmpty) {
    return '';
  }
  
  // 返回OSS路径
  return maskOssPath || '';
}

/**
 * 从cardBgc解析背景配置（用于回显）
 * @param {string} cardBgc
 * @returns {object} { bgType, bgGradient, bgGradientDirection, bgAdvanced, customBgOssPath }
 */
function parseCardBgc(cardBgc) {
  if (!cardBgc) {
    return { bgType: 'gradient', bgGradient: 'darkblack', bgGradientDirection: 'diagonal' };
  }
  
  // 判断是否为OSS路径
  if (cardBgc.startsWith('/card/')) {
    return { bgType: 'custom', customBgOssPath: cardBgc };
  }
  
  // 判断是否为高级背景
  if (cardBgc.startsWith('bg_')) {
    const advanced = cardBgc.replace('bg_', '').replace('.png', '');
    return { bgType: 'advanced', bgAdvanced: advanced };
  }
  
  // 解析渐变色：{direct}_{seq}
  const parts = cardBgc.split('_');
  if (parts.length === 2) {
    const direct = parseInt(parts[0]);
    const seq = parseInt(parts[1]);
    
    const directionMap = { 1: 'horizontal', 2: 'vertical', 3: 'diagonal' };
    const gradientMap = {
      1: 'darkblack', 2: 'gold', 3: 'winered', 4: 'originalgold',
      5: 'blue', 6: 'red', 7: 'orange', 8: 'purple', 9: 'sunset'
    };
    
    return {
      bgType: 'gradient',
      bgGradient: gradientMap[seq] || 'darkblack',
      bgGradientDirection: directionMap[direct] || 'diagonal'
    };
  }
  
  // 默认
  return { bgType: 'gradient', bgGradient: 'darkblack', bgGradientDirection: 'diagonal' };
}

module.exports = {
  buildCardBgc,
  buildCardMask,
  parseCardBgc,
  getGradientSeq,
  getDirectionCode
};
