import request from '@/utils/request'

/** 销售记录列表 */
export function getSaleList(params) {
  return request({
    url: '/api/sale/list',
    method: 'get',
    params
  })
}

/** 新增销售记录 */
export function createSale(data) {
  return request({
    url: '/api/sale/create',
    method: 'post',
    data
  })
}
