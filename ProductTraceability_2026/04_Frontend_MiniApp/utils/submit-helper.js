/**
 * submit-helper.js
 * Standardized form submission utilities
 * - Auto-inject common fields (enterprise_id, operator, dates)
 * - Enum value conversion
 * - Field name mapping to match database schema
 */

const util = require('./util.js')

/**
 * Get current user info from storage
 * @returns {Object} userInfo or empty object
 */
function getUserInfo() {
  const app = getApp()
  return (app && app.globalData && app.globalData.userInfo) || wx.getStorageSync('userInfo') || {}
}

/**
 * Get enterprise ID from current user
 * @returns {Number|null}
 */
function getEnterpriseId() {
  const userInfo = getUserInfo()
  return userInfo.enterpriseId || null
}

/**
 * Get operator name from current user
 * @returns {String}
 */
function getOperatorName() {
  const userInfo = getUserInfo()
  return userInfo.realName || userInfo.nickName || userInfo.username || ''
}

/**
 * Get current date string: YYYY-MM-DD
 * @returns {String}
 */
function getCurrentDate() {
  return util.formatDate(new Date())
}

/**
 * Get current datetime string: YYYY-MM-DD HH:mm:ss
 * @returns {String}
 */
function getCurrentDateTime() {
  return util.formatTime(new Date())
}

/**
 * Inject common hidden fields into submit data
 * Automatically adds: enterpriseId, operator (if missing), date defaults
 * @param {Object} data - The form data to augment
 * @param {Object} options - Optional overrides
 * @param {String} options.dateField - Name of the date field to auto-fill (e.g. 'storageDate')
 * @param {String} options.operatorField - Name of the operator field (default 'operator')
 * @returns {Object} Augmented data
 */
function injectCommonFields(data, options = {}) {
  const result = Object.assign({}, data)

  // 1. Auto-inject enterpriseId
  if (!result.enterpriseId) {
    const eid = getEnterpriseId()
    if (eid) {
      result.enterpriseId = eid
    }
  }

  // 2. Auto-inject operator name
  const operatorField = options.operatorField || 'operator'
  if (!result[operatorField]) {
    result[operatorField] = getOperatorName()
  }

  // 3. Auto-fill date field with current date if not provided
  if (options.dateField) {
    const fields = Array.isArray(options.dateField) ? options.dateField : [options.dateField]
    fields.forEach(field => {
      if (!result[field]) {
        result[field] = getCurrentDate()
      }
    })
  }

  // 4. Auto-fill datetime field with current datetime if not provided
  if (options.dateTimeField) {
    const fields = Array.isArray(options.dateTimeField) ? options.dateTimeField : [options.dateTimeField]
    fields.forEach(field => {
      if (!result[field]) {
        result[field] = getCurrentDateTime()
      }
    })
  }

  return result
}

// ==================== Enum Mappings ====================

/**
 * Inspection result: text -> database value
 * Front-end: '合格' / '不合格'
 * Database: 1 (合格) / 0 (不合格)
 */
const INSPECTION_RESULT_MAP = {
  '合格': 1,
  '不合格': 0
}

function mapInspectionResult(text) {
  if (text === null || text === undefined) return null
  if (typeof text === 'number') return text
  return INSPECTION_RESULT_MAP[text] !== undefined ? INSPECTION_RESULT_MAP[text] : null
}

/**
 * Storage type: text -> database value
 * Front-end: '入库' / '出库' / '库存盘点'
 * Database: 1 / 2 / 3
 */
const STORAGE_TYPE_MAP = {
  '入库': 1,
  '原材料入库': 1,
  '成品入库': 1,
  '出库': 2,
  '库存盘点': 3
}

function mapStorageType(text) {
  if (text === null || text === undefined) return null
  if (typeof text === 'number') return text
  return STORAGE_TYPE_MAP[text] !== undefined ? STORAGE_TYPE_MAP[text] : 1
}

/**
 * Product type: template label -> database value
 */
const PRODUCT_TYPE_MAP = {
  '肉鸡养殖': 1,
  '肉鸡': 1,
  '西红柿种植': 2,
  '西红柿': 2
}

function mapProductType(val) {
  if (typeof val === 'number') return val
  return PRODUCT_TYPE_MAP[val] || parseInt(val) || 1
}

/**
 * Get default product_name based on product_type
 * @param {Number} productType 1-肉鸡 2-西红柿
 * @returns {String}
 */
function getDefaultProductName(productType) {
  const map = { 1: '肉鸡', 2: '西红柿' }
  return map[productType] || '肉鸡'
}

/**
 * Get default unit based on product_type
 * @param {Number} productType 1-肉鸡 2-西红柿
 * @returns {String}
 */
function getDefaultUnit(productType) {
  const map = { 1: '只', 2: '株' }
  return map[productType] || '只'
}

// ==================== Field Name Mapping ====================

/**
 * Rename front-end field names to match database column names
 * @param {Object} data - The form data
 * @param {Object} fieldMap - Mapping of { frontendKey: databaseKey }
 * @returns {Object} Renamed data
 */
function renameFields(data, fieldMap) {
  const result = Object.assign({}, data)
  Object.keys(fieldMap).forEach(frontKey => {
    const dbKey = fieldMap[frontKey]
    if (result[frontKey] !== undefined && frontKey !== dbKey) {
      result[dbKey] = result[frontKey]
      delete result[frontKey]
    }
  })
  return result
}

// ==================== Image Upload ====================

/**
 * Upload a single image file to server
 * @param {String} tempFilePath - wx temp file path
 * @param {String} type - file type category (certificate/record/storage/sale etc.)
 * @returns {Promise<String>} server URL of uploaded file
 */
function uploadImage(tempFilePath, type) {
  type = type || 'common'
  return new Promise((resolve, reject) => {
    const app = getApp()
    const baseUrl = (app && app.globalData ? app.globalData.apiBaseUrl : '') || ''
    const token = (app && app.globalData ? app.globalData.token : '') || wx.getStorageSync('token') || ''

    wx.uploadFile({
      url: baseUrl + '/api/file/upload',
      filePath: tempFilePath,
      name: 'file',
      formData: { type: type },
      header: {
        'Authorization': token ? ('Bearer ' + token) : ''
      },
      success: (res) => {
        try {
          const data = JSON.parse(res.data)
          if (data.code === 200 && data.data && data.data.url) {
            resolve(data.data.url)
          } else {
            reject(new Error(data.message || 'upload failed'))
          }
        } catch (e) {
          reject(new Error('parse upload response failed'))
        }
      },
      fail: (err) => {
        reject(err)
      }
    })
  })
}

/**
 * Upload multiple images in sequence
 * @param {Array<String>} tempFilePaths - array of wx temp file paths
 * @param {String} type - file type category
 * @returns {Promise<Array<String>>} array of server URLs
 */
function uploadImages(tempFilePaths, type) {
  if (!tempFilePaths || tempFilePaths.length === 0) {
    return Promise.resolve([])
  }
  const tasks = tempFilePaths.map(path => {
    // Skip already-uploaded URLs (start with /uploads/ or real http URL with domain)
    // Note: wx temp files look like "http://tmp/xxx" - these must NOT be skipped
    if (path.startsWith('/uploads/') || (path.startsWith('http') && !path.startsWith('http://tmp'))) {
      return Promise.resolve(path)
    }
    return uploadImage(path, type)
  })
  return Promise.all(tasks)
}

module.exports = {
  getUserInfo,
  getEnterpriseId,
  getOperatorName,
  getCurrentDate,
  getCurrentDateTime,
  injectCommonFields,
  mapInspectionResult,
  mapStorageType,
  mapProductType,
  getDefaultProductName,
  getDefaultUnit,
  renameFields,
  uploadImage,
  uploadImages,
  INSPECTION_RESULT_MAP,
  STORAGE_TYPE_MAP,
  PRODUCT_TYPE_MAP
}
