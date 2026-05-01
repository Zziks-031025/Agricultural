import request from '@/utils/request'

/** 运输记录列表 */
export function getTransportList(params) {
  return request({
    url: '/api/transport/list',
    method: 'get',
    params
  })
}

/** 新增运输记录 */
export function createTransport(data) {
  return request({
    url: '/api/transport/create',
    method: 'post',
    data
  })
}

/** 删除运输记录 */
export function deleteTransport(id) {
  return request({
    url: `/api/transport/delete/${id}`,
    method: 'delete'
  })
}
