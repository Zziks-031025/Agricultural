import request from '@/utils/request'

/** 企业审核列表 */
export function getAuditList(params) {
  return request({
    url: '/api/enterprise/audit/list',
    method: 'get',
    params
  })
}

/** 企业审核详情 */
export function getAuditDetail(id) {
  return request({
    url: `/api/enterprise/audit/detail/${id}`,
    method: 'get'
  })
}

/** 审核通过/驳回 */
export function approveEnterprise(data) {
  return request({
    url: '/api/enterprise/audit/approve',
    method: 'post',
    data
  })
}

/** 企业详情 */
export function getEnterpriseDetail(id) {
  return request({
    url: `/api/enterprise/detail/${id}`,
    method: 'get'
  })
}

/** 更新企业信息 */
export function updateEnterprise(data) {
  return request({
    url: '/api/enterprise/update',
    method: 'put',
    data
  })
}

/** 重新提交企业入驻审核 */
export function reapplyEnterprise(id) {
  return request({
    url: `/api/enterprise/reapply/${id}`,
    method: 'put'
  })
}

/** 删除已驳回的企业入驻申请 */
export function deleteRejectedEnterprise(id) {
  return request({
    url: `/api/enterprise/delete-rejected/${id}`,
    method: 'delete'
  })
}

/** 企业列表(管理员) */
export function getEnterpriseList(params) {
  return request({
    url: '/api/enterprise/list',
    method: 'get',
    params
  })
}

/** 启用/禁用企业 */
export function toggleEnterpriseStatus(id, status) {
  return request({
    url: `/api/enterprise/toggle-status/${id}`,
    method: 'put',
    params: { status }
  })
}

/** 按企业类型查询已审核通过的企业列表 */
export function getEnterpriseListByType(type) {
  return request({
    url: '/api/enterprise/list-by-type',
    method: 'get',
    params: { type }
  })
}

/** 企业注册 */
export function registerEnterprise(data) {
  return request({
    url: '/api/enterprise/register',
    method: 'post',
    data
  })
}

/** 提交图片变更审核 */
export function submitImageAudit(data) {
  return request({
    url: '/api/image-audit/submit',
    method: 'post',
    data
  })
}

/** 查询图片审核列表（管理员） */
export function getImageAuditList(params) {
  return request({
    url: '/api/image-audit/list',
    method: 'get',
    params
  })
}

/** 审核图片变更（管理员） */
export function approveImageAudit(data) {
  return request({
    url: '/api/image-audit/approve',
    method: 'post',
    data
  })
}

/** 查询企业待审核图片数量 */
export function getImageAuditPendingCount(enterpriseId) {
  return request({
    url: '/api/image-audit/pending-count',
    method: 'get',
    params: { enterpriseId }
  })
}

/** 查询资质图片待审核数量 */
export function getQualificationPendingCount() {
  return request({
    url: '/api/image-audit/qualification-pending-count',
    method: 'get'
  })
}

export function getImageAuditByEnterprise(enterpriseId) {
  return request({
    url: '/api/image-audit/by-enterprise',
    method: 'get',
    params: { enterpriseId }
  })
}

/** 文件上传 */
export function uploadFile(file, type) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('type', type || 'common')
  return request({
    url: '/api/file/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
