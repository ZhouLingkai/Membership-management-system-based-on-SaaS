/**
 * 会员卡Canvas绘制Mixin
 * 包含所有Canvas绘制相关方法
 */

const cardCanvasMixin = {
  /**
   * 自动生成蒙版
   */
  async autoGenerateMask() {
    const { canvas, ctx, titleText, showVip, showPattern } = this.data;
    
    if (!canvas || !ctx) {
      console.warn('[Canvas] Canvas未初始化，跳过生成');
      return;
    }

    try {
      // 清空画布
      ctx.clearRect(0, 0, 856, 540);

      // 绘制图案
      if (showPattern) {
        await this.drawPattern(ctx);
      }

      // 绘制标题
      if (titleText && titleText.trim() !== '') {
        this.drawTitle(ctx);
      }

      // 绘制VIP
      if (showVip) {
        await this.drawVip(ctx);
      }

      // 导出为图片
      const tempFilePath = await new Promise((resolve, reject) => {
        wx.canvasToTempFilePath({
          canvas: canvas,
          success: (res) => resolve(res.tempFilePath),
          fail: reject
        }, this);
      });

      this.setData({
        maskImageUrl: tempFilePath
      });

      console.log('[Canvas] 蒙版生成成功');
    } catch (error) {
      console.error('[Canvas] 蒙版生成失败:', error);
    }
  },

  /**
   * 绘制图案
   */
  async drawPattern(ctx) {
    return new Promise((resolve, reject) => {
      const { canvas, selectedPattern, patternPosition, patternSize } = this.data;
      
      // 图案路径映射
      const patternMap = {
        'dragon': '/packageCardType/assets/mask/dragon.png',
        'diamond': '/packageCardType/assets/mask/diamond.png',
        'round': '/packageCardType/assets/mask/round.png'
      };
      const imagePath = patternMap[selectedPattern] || patternMap['dragon'];
      
      // 创建图片对象
      const img = canvas.createImage();
      img.onload = () => {
        // 卡片尺寸
        const cardHeight = 540;
        const cardWidth = 856;
        
        // 图案高度占卡片高度的比例
        const sizeMap = {
          'small': 0.35,   // 35%
          'medium': 0.45,  // 45%
          'large': 0.55,   // 55%
          'xlarge': 0.70   // 70%
        };
        const heightRatio = sizeMap[patternSize] || 0.45;
        
        // 计算图案高度
        const imgHeight = cardHeight * heightRatio;
        
        // 根据原图比例计算宽度（保持原图宽高比）
        const imgWidth = (img.width / img.height) * imgHeight;
        
        console.log(`[图案绘制] 原图尺寸: ${img.width}x${img.height}, 绘制尺寸: ${imgWidth.toFixed(2)}x${imgHeight.toFixed(2)}`);
        
        // 距离边缘5px（注意：这里的坐标是逻辑坐标，已经通过ctx.scale处理）
        const padding = 5;
        let x, y;
        
        if (patternPosition === 'left-top') {
          // 左上：左边距5px，上边距5px
          x = padding;
          y = padding;
        } else if (patternPosition === 'center') {
          // 正中：水平垂直居中
          x = (cardWidth - imgWidth) / 2;
          y = (cardHeight - imgHeight) / 2;
        } else if (patternPosition === 'left-bottom') {
          // 左下：左边距5px，下边距5px
          x = padding;
          y = cardHeight - imgHeight - padding;
        } else if (patternPosition === 'right-bottom') {
          // 右下：右边距5px，下边距5px
          x = cardWidth - imgWidth - padding;
          y = cardHeight - imgHeight - padding;
        } else if (patternPosition === 'right-top') {
          // 右上：右边距5px，上边距5px
          x = cardWidth - imgWidth - padding;
          y = padding;
        } else {
          // 默认右下
          x = cardWidth - imgWidth - padding;
          y = cardHeight - imgHeight - padding;
        }
        
        console.log(`[图案绘制] 位置: ${patternPosition}, 坐标: (${x.toFixed(2)}, ${y.toFixed(2)}), 卡片尺寸: ${cardWidth}x${cardHeight}`);
        
        // 设置透明度
        ctx.globalAlpha = 0.3;
        ctx.drawImage(img, x, y, imgWidth, imgHeight);
        ctx.globalAlpha = 1.0;
        
        resolve();
      };
      
      img.onerror = (err) => {
        console.error('图案加载失败:', err);
        resolve(); // 即使失败也继续
      };
      
      img.src = imagePath;
    });
  },

  /**
   * 绘制标题
   */
  drawTitle(ctx) {
    const { titleText, titlePosition, titleColor, titleColorType, titleGradient, titleFont, titleSize } = this.data;
    
    // 字体大小映射
    const sizeMap = {
      'small': 40,
      'medium': 50,
      'large': 60,
      'xlarge': 80
    };
    const fontSize = sizeMap[titleSize] || 50;

    // 字体映射
    const fontMap = {
      'default': 'system-ui, -apple-system, sans-serif',
      'songti': 'Songti SC, STSong, SimSun, serif',
      'heiti': 'Heiti SC, STHeiti, SimHei, sans-serif',
      'hiragino': 'Hiragino Sans GB, sans-serif',
      'pingfang': 'PingFang SC, sans-serif',
      'times': 'Times New Roman, serif',
      'stheiti': 'STHeiti, sans-serif',
      'yahei': 'Microsoft YaHei, sans-serif',
      'roboto': 'Roboto, sans-serif'
    };
    const fontFamily = fontMap[titleFont] || fontMap['default'];

    // 设置字体
    ctx.font = `bold ${fontSize}px ${fontFamily}`;
    ctx.textBaseline = 'top';

    // 位置计算
    const padding = 60;
    const y = padding;
    let x;
    
    if (titlePosition === 'left-top') {
      ctx.textAlign = 'left';
      x = padding;
    } else if (titlePosition === 'right-top') {
      ctx.textAlign = 'right';
      x = 856 - padding;
    } else if (titlePosition === 'center-top') {
      ctx.textAlign = 'center';
      x = 856 / 2;
    } else {
      ctx.textAlign = 'left';
      x = padding;
    }

    // 设置阴影
    ctx.shadowColor = 'rgba(0, 0, 0, 0.1)';
    ctx.shadowBlur = 10;
    ctx.shadowOffsetX = 2;
    ctx.shadowOffsetY = 2;

    // 设置颜色
    if (titleColorType === 'gradient' && titleGradient) {
      const gradient = this.createGradient(ctx, titleGradient, x, y, 200);
      ctx.fillStyle = gradient;
    } else {
      ctx.fillStyle = titleColor;
    }

    // 添加描边
    ctx.strokeStyle = 'rgba(0, 0, 0, 0.3)';
    ctx.lineWidth = 3;
    ctx.strokeText(titleText, x, y);
    ctx.fillText(titleText, x, y);

    // 清除阴影
    ctx.shadowColor = 'transparent';
    ctx.shadowBlur = 0;
    ctx.shadowOffsetX = 0;
    ctx.shadowOffsetY = 0;
  },

  /**
   * 绘制VIP
   */
  async drawVip(ctx) {
    const { vipColorType } = this.data;
    
    if (vipColorType === 'advanced') {
      return this.drawVipImage(ctx);
    }
    
    // 绘制文字VIP
    const { vipPosition, vipColor, vipGradient, vipFont, vipSize } = this.data;
    
    const sizeMap = {
      'small': 80,
      'medium': 120,
      'large': 160,
      'xlarge': 200
    };
    const fontSize = sizeMap[vipSize];

    const fontMap = {
      'default': 'system-ui, -apple-system, sans-serif',
      'songti': 'Songti SC, STSong, SimSun, serif',
      'heiti': 'Heiti SC, STHeiti, SimHei, sans-serif',
      'hiragino': 'Hiragino Sans GB, sans-serif',
      'pingfang': 'PingFang SC, sans-serif',
      'times': 'Times New Roman, serif',
      'stheiti': 'STHeiti, sans-serif',
      'yahei': 'Microsoft YaHei, sans-serif',
      'roboto': 'Roboto, sans-serif'
    };
    const fontFamily = fontMap[vipFont] || fontMap['default'];

    ctx.font = `italic bold ${fontSize}px ${fontFamily}`;
    ctx.textBaseline = 'middle';

    const y = 540 / 2;
    let x;
    
    if (vipPosition === 'center-left') {
      ctx.textAlign = 'left';
      x = 60;
    } else if (vipPosition === 'center') {
      ctx.textAlign = 'center';
      x = 856 / 2;
    } else {
      ctx.textAlign = 'right';
      x = 856 - 60;
    }

    ctx.shadowColor = 'rgba(0, 0, 0, 0.5)';
    ctx.shadowBlur = 10;
    ctx.shadowOffsetX = 2;
    ctx.shadowOffsetY = 2;

    if (vipGradient) {
      const gradient = this.createGradient(ctx, vipGradient, x, y, 300);
      ctx.fillStyle = gradient;
    } else {
      ctx.fillStyle = vipColor;
    }

    ctx.strokeStyle = 'rgba(0, 0, 0, 0.5)';
    ctx.lineWidth = 4;
    ctx.strokeText('VIP', x, y);
    ctx.fillText('VIP', x, y);

    ctx.shadowColor = 'transparent';
    ctx.shadowBlur = 0;
    ctx.shadowOffsetX = 0;
    ctx.shadowOffsetY = 0;
  },

  /**
   * 绘制VIP图片
   */
  async drawVipImage(ctx) {
    return new Promise((resolve, reject) => {
      const { canvas, vipAdvanced, vipPosition, vipSize } = this.data;
      
      const vipImageMap = {
        '3D': '/packageCardType/assets/vip/VIP-3D.png',
        'black': '/packageCardType/assets/vip/VIP-black.png',
        'blackgold': '/packageCardType/assets/vip/VIP-blackgold.png',
        'cola': '/packageCardType/assets/vip/VIP-cola.png',
        'gold': '/packageCardType/assets/vip/VIP-gold.png',
        'pinkgold': '/packageCardType/assets/vip/VIP-pinkgold.png',
        'pinkorange': '/packageCardType/assets/vip/VIP-pinkorange.png',
        'white': '/packageCardType/assets/vip/VIP-white.png'
      };
      const imagePath = vipImageMap[vipAdvanced] || vipImageMap['3D'];
      
      const img = canvas.createImage();
      img.onload = () => {
        const imgRatio = 16 / 9;
        
        const sizeMap = {
          'small': 100,
          'medium': 150,
          'large': 190,
          'xlarge': 230
        };
        const imgHeight = sizeMap[vipSize] || 150;
        const imgWidth = imgHeight * imgRatio;
        
        const y = (540 - imgHeight) / 2;
        
        let x;
        if (vipPosition === 'center-left') {
          x = 40;
        } else if (vipPosition === 'center') {
          x = (856 - imgWidth) / 2;
        } else {
          x = 856 - imgWidth - 40;
        }
        
        ctx.drawImage(img, x, y, imgWidth, imgHeight);
        resolve();
      };
      
      img.onerror = (err) => {
        console.error('VIP图片加载失败:', err);
        reject(err);
      };
      
      img.src = imagePath;
    });
  },

  /**
   * 创建渐变色
   */
  createGradient(ctx, gradientType, x, y, width, direction = 'diagonal') {
    const gradientMap = {
      'darkblack': ['#111111', '#222222', '#111111'],
      'gold': ['#B8860B', '#D4AF37'],
      'winered': ['#8B0000', '#9E2B25'],
      'originalgold': ['#FDEB71', '#F8D800'],
      'blue': ['#ABDCFF', '#0396FF'],
      'red': ['#FEB692', '#EA5455'],
      'orange': ['#FCCF31', '#F55555'],
      'purple': ['#F761A1', '#8C1BAB'],
      'sunset': ['#FFA8A8', '#FCFF00']
    };

    const colors = gradientMap[gradientType] || ['#FFFFFF', '#FFFFFF'];

    let gradient;
    if (direction === 'horizontal') {
      gradient = ctx.createLinearGradient(x, y, x + width, y);
    } else if (direction === 'vertical') {
      gradient = ctx.createLinearGradient(x, y, x, y + width);
    } else {
      gradient = ctx.createLinearGradient(x, y, x + width, y + width);
    }

    if (colors.length === 2) {
      gradient.addColorStop(0, colors[0]);
      gradient.addColorStop(1, colors[1]);
    } else if (colors.length === 3) {
      gradient.addColorStop(0, colors[0]);
      gradient.addColorStop(0.5, colors[1]);
      gradient.addColorStop(1, colors[2]);
    }

    return gradient;
  },

  /**
   * 处理图片到目标比例
   */
  async processImageToRatio(imagePath, width, height) {
    const targetRatio = 1.585;
    const currentRatio = width / height;
    
    console.log(`[图片处理] 原始尺寸: ${width}x${height}, 比例: ${currentRatio.toFixed(3)}`);
    
    // 区间1: [1.485, 1.685] - 直接拉伸
    if (currentRatio >= 1.485 && currentRatio <= 1.685) {
      console.log('[图片处理] 区间1: 直接拉伸');
      return await this.stretchImage(imagePath, width, height, targetRatio);
    }
    
    // 区间2: [1.33, 1.78] - 先裁剪再拉伸
    if (currentRatio >= 1.33 && currentRatio <= 1.78) {
      console.log('[图片处理] 区间2: 先裁剪再拉伸');
      let cropRatio;
      if (currentRatio < 1.585) {
        cropRatio = 1.485;
      } else {
        cropRatio = 1.685;
      }
      const croppedPath = await this.cropImageToRatio(imagePath, width, height, cropRatio);
      const croppedInfo = await new Promise((resolve, reject) => {
        wx.getImageInfo({
          src: croppedPath,
          success: resolve,
          fail: reject
        });
      });
      return await this.stretchImage(croppedPath, croppedInfo.width, croppedInfo.height, targetRatio);
    }
    
    // 其他情况: 按1.585裁剪
    console.log('[图片处理] 其他情况: 按1.585裁剪');
    return await this.cropImageToRatio(imagePath, width, height, targetRatio);
  },

  /**
   * 拉伸图片
   */
  async stretchImage(imagePath, width, height, targetRatio) {
    return new Promise((resolve, reject) => {
      const query = wx.createSelectorQuery().in(this);
      query.select('#tempCanvas')
        .fields({ node: true, size: true })
        .exec(async (res) => {
          if (!res || !res[0]) {
            reject(new Error('临时Canvas节点查询失败'));
            return;
          }

          const canvas = res[0].node;
          const ctx = canvas.getContext('2d');
          
          const targetWidth = Math.round(height * targetRatio);
          canvas.width = targetWidth;
          canvas.height = height;
          
          const img = canvas.createImage();
          img.onload = async () => {
            ctx.drawImage(img, 0, 0, targetWidth, height);
            
            try {
              const tempFilePath = await new Promise((resolve, reject) => {
                wx.canvasToTempFilePath({
                  canvas: canvas,
                  success: (res) => resolve(res.tempFilePath),
                  fail: reject
                }, this);
              });
              
              console.log(`[拉伸] 完成: ${width}x${height} -> ${targetWidth}x${height}`);
              resolve(tempFilePath);
            } catch (error) {
              reject(error);
            }
          };
          
          img.onerror = reject;
          img.src = imagePath;
        });
    });
  },

  /**
   * 裁剪图片
   */
  async cropImageToRatio(imagePath, width, height, targetRatio) {
    return new Promise((resolve, reject) => {
      const query = wx.createSelectorQuery().in(this);
      query.select('#tempCanvas')
        .fields({ node: true, size: true })
        .exec(async (res) => {
          if (!res || !res[0]) {
            reject(new Error('临时Canvas节点查询失败'));
            return;
          }

          const canvas = res[0].node;
          const ctx = canvas.getContext('2d');
          
          const currentRatio = width / height;
          let cropWidth, cropHeight, cropX, cropY;
          
          if (currentRatio > targetRatio) {
            cropHeight = height;
            cropWidth = Math.round(height * targetRatio);
            cropX = Math.round((width - cropWidth) / 2);
            cropY = 0;
          } else {
            cropWidth = width;
            cropHeight = Math.round(width / targetRatio);
            cropX = 0;
            cropY = Math.round((height - cropHeight) / 2);
          }
          
          canvas.width = cropWidth;
          canvas.height = cropHeight;
          
          const img = canvas.createImage();
          img.onload = async () => {
            ctx.drawImage(img, cropX, cropY, cropWidth, cropHeight, 0, 0, cropWidth, cropHeight);
            
            try {
              const tempFilePath = await new Promise((resolve, reject) => {
                wx.canvasToTempFilePath({
                  canvas: canvas,
                  success: (res) => resolve(res.tempFilePath),
                  fail: reject
                }, this);
              });
              
              console.log(`[裁剪] 完成: ${width}x${height} -> ${cropWidth}x${cropHeight}`);
              resolve(tempFilePath);
            } catch (error) {
              reject(error);
            }
          };
          
          img.onerror = reject;
          img.src = imagePath;
        });
    });
  },

  /**
   * 智能压缩图片
   */
  async compressImageSmart(imagePath) {
    try {
      const fileInfo = await new Promise((resolve, reject) => {
        wx.getFileInfo({
          filePath: imagePath,
          success: resolve,
          fail: reject
        });
      });

      const fileSizeKB = fileInfo.size / 1024;
      console.log(`[压缩] 原始大小: ${fileSizeKB.toFixed(2)}KB`);

      let quality = 90;
      if (fileSizeKB > 500) {
        quality = 70;
      } else if (fileSizeKB > 300) {
        quality = 80;
      }

      const compressedPath = await new Promise((resolve, reject) => {
        wx.compressImage({
          src: imagePath,
          quality: quality,
          success: (res) => resolve(res.tempFilePath),
          fail: reject
        });
      });

      const compressedInfo = await new Promise((resolve, reject) => {
        wx.getFileInfo({
          filePath: compressedPath,
          success: resolve,
          fail: reject
        });
      });

      const compressedSizeKB = compressedInfo.size / 1024;
      console.log(`[压缩] 压缩后大小: ${compressedSizeKB.toFixed(2)}KB, 质量: ${quality}`);

      return compressedPath;
    } catch (error) {
      console.error('[压缩] 压缩失败，使用原图:', error);
      return imagePath;
    }
  }
};

module.exports = cardCanvasMixin;
