/**
 * 小程序统一环境配置文件
 * 
 * 切换环境时只需修改此文件，无需逐页面更改
 * 
 * 使用方式: const config = require('../../config.js')  // 根据文件层级调整路径
 *          或 const config = require('/config.js')     // 绝对路径(需小程序支持)
 */

// ============ 环境切换 ============
// 'dev'  = 开发环境 (局域网IP)
// 'prod' = 生产环境 (线上域名)
const ENV = 'dev'

// ============ 环境配置 ============
const ENV_CONFIG = {
  dev: {
    // 后端API基础地址 (开发环境: 局域网IP + 后端端口)
    // 每次切换网络后在这里更新IP即可 (cmd运行 ipconfig 查看)
    API_BASE_URL: 'http://192.168.31.70:8888',
  },
  prod: {
    // 后端API基础地址 (生产环境: 线上域名，需HTTPS)
    API_BASE_URL: 'https://your-domain.com',
  }
}

// ============ 导出当前环境配置 ============
const currentConfig = ENV_CONFIG[ENV] || ENV_CONFIG.dev

module.exports = {
  // 当前环境标识
  ENV: ENV,

  // 后端API基础地址
  API_BASE_URL: currentConfig.API_BASE_URL,

  // 文件上传相关
  UPLOAD_URL: currentConfig.API_BASE_URL + '/api/file/upload',

  // 图片访问前缀 (用于拼接 /uploads/... 相对路径)
  IMAGE_BASE_URL: currentConfig.API_BASE_URL,

  // Mock开关
  USE_MOCK: false,
}
