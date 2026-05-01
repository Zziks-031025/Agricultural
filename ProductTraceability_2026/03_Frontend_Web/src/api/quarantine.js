import request from '@/utils/request'

/** 提交检疫结果 */
export function submitQuarantine(data) {
  return request({
    url: '/api/quarantine/submit',
    method: 'post',
    data
  })
}

/** 养殖企业提交检疫申报 */
export function applyQuarantine(data) {
  return request({
    url: '/api/quarantine/apply',
    method: 'post',
    data
  })
}

/** 查询检疫申报列表 */
export function getQuarantineApplyList(params) {
  return request({
    url: '/api/quarantine/apply/list',
    method: 'get',
    params
  })
}

/** 删除检疫申报（仅待受理状态可删除） */
export function deleteQuarantineApply(id) {
  return request({
    url: `/api/quarantine/apply/delete/${id}`,
    method: 'delete'
  })
}
