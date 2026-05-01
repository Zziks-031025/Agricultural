import request from '@/utils/request'

/** 模版列表 */
export function getTemplateList(params) {
  return request({
    url: '/api/template/list',
    method: 'get',
    params
  })
}

/** 模版详情 */
export function getTemplateDetail(id) {
  return request({
    url: `/api/template/detail/${id}`,
    method: 'get'
  })
}

/** 系统字段目录 */
export function getSystemFieldCatalog() {
  return request({
    url: '/api/template/system-field-catalog',
    method: 'get'
  })
}

/** 新增模版 */
export function addTemplate(data) {
  return request({
    url: '/api/template/add',
    method: 'post',
    data
  })
}

/** 更新模版 */
export function updateTemplate(data) {
  return request({
    url: '/api/template/update',
    method: 'put',
    data
  })
}

/** 删除模版 */
export function deleteTemplate(id) {
  return request({
    url: `/api/template/delete/${id}`,
    method: 'delete'
  })
}

/** 启用/禁用模版 */
export function toggleTemplateStatus(id, status) {
  return request({
    url: `/api/template/toggle-status/${id}`,
    method: 'put',
    params: { status }
  })
}

// ==================== 溯源环节 ====================

/** 获取模版下的环节列表 */
export function getStageList(templateId) {
  return request({
    url: `/api/template/stage/list/${templateId}`,
    method: 'get'
  })
}

/** 新增环节 */
export function addStage(data) {
  return request({
    url: '/api/template/stage/add',
    method: 'post',
    data
  })
}

/** 更新环节 */
export function updateStage(data) {
  return request({
    url: '/api/template/stage/update',
    method: 'put',
    data
  })
}

/** 删除环节 */
export function deleteStage(id) {
  return request({
    url: `/api/template/stage/delete/${id}`,
    method: 'delete'
  })
}

// ==================== 溯源字段 ====================

/** 获取环节下的字段列表 */
export function getFieldList(stageId) {
  return request({
    url: `/api/template/field/list/${stageId}`,
    method: 'get'
  })
}

/** 新增字段 */
export function addField(data) {
  return request({
    url: '/api/template/field/add',
    method: 'post',
    data
  })
}

/** 更新字段 */
export function updateField(data) {
  return request({
    url: '/api/template/field/update',
    method: 'put',
    data
  })
}

/** 删除字段 */
export function deleteField(id) {
  return request({
    url: `/api/template/field/delete/${id}`,
    method: 'delete'
  })
}
