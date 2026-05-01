import request from '@/utils/request'

/** 溯源查询统计概览 */
export function getScanStatsOverview() {
  return request({
    url: '/api/scan-stats/overview',
    method: 'get'
  })
}

/** 查询趋势 */
export function getScanStatsTrend(params) {
  return request({
    url: '/api/scan-stats/trend',
    method: 'get',
    params
  })
}

/** 查询日志列表 */
export function getScanStatsLogs(params) {
  return request({
    url: '/api/scan-stats/logs',
    method: 'get',
    params
  })
}
