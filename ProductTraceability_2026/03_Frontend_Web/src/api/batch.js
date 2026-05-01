import request from '@/utils/request'

/** 批次列表 */
export function getBatchList(params) {
  return request({
    url: '/api/batch/list',
    method: 'get',
    params
  })
}

/** 批次详情 */
export function getBatchDetail(params) {
  return request({
    url: '/api/batch/detail',
    method: 'get',
    params
  })
}

/** 批次状态统计 */
export function getStatusCount() {
  return request({
    url: '/api/batch/status-count',
    method: 'get'
  })
}

/** 创建批次 */
export function createBatch(data) {
  return request({
    url: '/api/batch/create',
    method: 'post',
    data
  })
}

/** 新增批次 (别名) */
export function addBatch(data) {
  return request({
    url: '/api/batch/add',
    method: 'post',
    data
  })
}

/** 接收批次(加工企业) */
export function receiveBatch(data) {
  return request({
    url: '/api/batch/receive',
    method: 'post',
    data
  })
}

/** 删除批次 */
export function deleteBatch(id) {
  return request({
    url: `/api/batch/delete/${id}`,
    method: 'delete'
  })
}

/** 检疫状态查询 */
export function quarantineCheck(batchCode) {
  return request({
    url: `/api/batch/quarantine-check/${batchCode}`,
    method: 'get'
  })
}

/** 拒绝接收批次(加工企业) */
export function rejectBatch(batchId, data) {
  return request({
    url: `/api/batch/reject/${batchId}`,
    method: 'post',
    data
  })
}
