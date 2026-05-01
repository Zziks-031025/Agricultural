/**
 * 表单验证工具函数
 */

/**
 * 验证是否为有效数字
 * @param {string|number} value - 待验证的值
 * @returns {boolean}
 */
function isNumber(value) {
  if (value === '' || value === null || value === undefined) {
    return false
  }
  return !isNaN(parseFloat(value)) && isFinite(value)
}

/**
 * 验证是否为正数
 * @param {string|number} value - 待验证的值
 * @returns {boolean}
 */
function isPositiveNumber(value) {
  return isNumber(value) && parseFloat(value) > 0
}

/**
 * 验证是否为非负数
 * @param {string|number} value - 待验证的值
 * @returns {boolean}
 */
function isNonNegativeNumber(value) {
  return isNumber(value) && parseFloat(value) >= 0
}

/**
 * 验证手机号格式
 * @param {string} phone - 手机号
 * @returns {boolean}
 */
function isValidPhone(phone) {
  if (!phone) return false
  const phoneStr = String(phone).trim()
  return /^1[3-9]\d{9}$/.test(phoneStr)
}

/**
 * 验证温度范围（-50到100摄氏度）
 * @param {string|number} temp - 温度值
 * @returns {boolean}
 */
function isValidTemperature(temp) {
  if (!isNumber(temp)) return false
  const value = parseFloat(temp)
  return value >= -50 && value <= 100
}

/**
 * 验证湿度范围（0-100%）
 * @param {string|number} humidity - 湿度值
 * @returns {boolean}
 */
function isValidHumidity(humidity) {
  if (!isNumber(humidity)) return false
  const value = parseFloat(humidity)
  return value >= 0 && value <= 100
}

/**
 * 显示验证错误提示
 * @param {string} message - 错误信息
 */
function showValidationError(message) {
  wx.showToast({
    title: message,
    icon: 'none',
    duration: 2000
  })
}

module.exports = {
  isNumber,
  isPositiveNumber,
  isNonNegativeNumber,
  isValidPhone,
  isValidTemperature,
  isValidHumidity,
  showValidationError
}
