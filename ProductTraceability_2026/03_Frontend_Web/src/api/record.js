import request from '@/utils/request'

/** 生长记录列表 */
export function getRecordList(params) {
  return request({
    url: '/api/record/list',
    method: 'get',
    params
  })
}

/** 新增生长记录 */
export function createRecord(data) {
  return request({
    url: '/api/record/create',
    method: 'post',
    data
  })
}

/** 删除生长记录 */
export function deleteRecord(id) {
  return request({
    url: `/api/record/delete/${id}`,
    method: 'delete'
  })
}
