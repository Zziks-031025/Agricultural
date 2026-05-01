import request from '@/utils/request'

/**
 * 获取消息列表（分页）
 * @param {Object} params - { current, size, type }
 */
export function getMessageList(params) {
  return request({
    url: '/api/message/list',
    method: 'get',
    params
  })
}

/**
 * 获取消息详情
 * @param {Number} id - 消息ID
 */
export function getMessageDetail(id) {
  return request({
    url: `/api/message/detail/${id}`,
    method: 'get'
  })
}

/**
 * 获取未读消息数量
 */
export function getUnreadCount() {
  return request({
    url: '/api/message/unread-count',
    method: 'get'
  })
}

/**
 * 批量标记已读
 * @param {Array} ids - 消息ID数组
 */
export function markAsRead(ids) {
  return request({
    url: '/api/message/mark-read',
    method: 'post',
    data: { ids }
  })
}

/**
 * 批量删除消息
 * @param {Array} ids - 消息ID数组
 */
export function deleteMessages(ids) {
  return request({
    url: '/api/message/delete',
    method: 'post',
    data: { ids }
  })
}
