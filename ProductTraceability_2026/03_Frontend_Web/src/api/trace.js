import request from '@/utils/request'

/** 溯源详情(聚合时间轴) */
export function getTraceDetail(params) {
  return request({
    url: '/api/trace/detail',
    method: 'get',
    params
  })
}

/** 区块链验证 */
export function verifyBlockchain(data) {
  return request({
    url: '/api/blockchain/verify',
    method: 'post',
    data
  })
}

/** 区块链统计(管理员) */
export function getBlockchainStats() {
  return request({
    url: '/api/blockchain/stats',
    method: 'get'
  })
}

/** 区块链节点状态(管理员) */
export function getBlockchainNodeStatus() {
  return request({
    url: '/api/blockchain/node/status',
    method: 'get'
  })
}

/** 区块链上链日志列表(企业端) */
export function getBlockchainLogs(params) {
  return request({
    url: '/api/blockchain/logs',
    method: 'get',
    params
  })
}

/** 失败记录重新上链 */
export function retryChain(id) {
  return request({
    url: '/api/blockchain/retry/' + id,
    method: 'post'
  })
}
