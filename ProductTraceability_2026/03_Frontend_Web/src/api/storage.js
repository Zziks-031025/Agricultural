import request from '@/utils/request'

/** 仓储记录列表 */
export function getStorageList(params) {
  return request({
    url: '/api/storage/list',
    method: 'get',
    params
  })
}

/** 新增仓储记录 */
export function createStorage(data) {
  return request({
    url: '/api/storage/create',
    method: 'post',
    data
  })
}

/** 删除仓储记录 */
export function deleteStorage(id) {
  return request({
    url: `/api/storage/delete/${id}`,
    method: 'delete'
  })
}
