import request from '@/utils/request'

/** 养殖企业首页统计 */
export function getFarmerStats(params) {
  return request({
    url: '/api/dashboard/farmer-stats',
    method: 'get',
    params
  })
}

/** 数据统计分析页 */
export function getAnalysisStats(params) {
  return request({
    url: '/api/dashboard/analysis-stats',
    method: 'get',
    params
  })
}

/** 加工宰杀企业首页统计 */
export function getProcessorStats(params) {
  return request({
    url: '/api/dashboard/processor-stats',
    method: 'get',
    params
  })
}

/** 检疫质检企业首页统计 */
export function getQuarantineStats(params) {
  return request({
    url: '/api/dashboard/quarantine-stats',
    method: 'get',
    params
  })
}

/** 管理员端统计大屏 */
export function getDashboardStats(role) {
  return request({
    url: '/api/dashboard/admin-stats',
    method: 'get',
    params: { role }
  })
}
