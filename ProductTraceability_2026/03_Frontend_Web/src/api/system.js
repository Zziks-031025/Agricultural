import request from '@/utils/request'

// ==================== 用户管理 ====================

/** 用户列表 */
export function getUserList(params) {
  return request({
    url: '/api/system/user/list',
    method: 'get',
    params
  })
}

/** 新增用户 */
export function addUser(data) {
  return request({
    url: '/api/system/user/add',
    method: 'post',
    data
  })
}

/** 更新用户 */
export function updateUser(data) {
  return request({
    url: '/api/system/user/update',
    method: 'put',
    data
  })
}

/** 删除用户 */
export function deleteUser(id) {
  return request({
    url: `/api/system/user/delete/${id}`,
    method: 'delete'
  })
}

/** 重置密码 */
export function resetPassword(id) {
  return request({
    url: `/api/system/user/reset-password/${id}`,
    method: 'put'
  })
}

/** 启用/禁用用户 */
export function toggleUserStatus(id, status) {
  return request({
    url: `/api/system/user/toggle-status/${id}`,
    method: 'put',
    params: { status }
  })
}

// ==================== 角色管理 ====================

/** 角色列表 */
export function getRoleList(params) {
  return request({
    url: '/api/system/role/list',
    method: 'get',
    params
  })
}

/** 全部角色(下拉用) */
export function getAllRoles() {
  return request({
    url: '/api/system/role/all',
    method: 'get'
  })
}

/** 新增角色 */
export function addRole(data) {
  return request({
    url: '/api/system/role/add',
    method: 'post',
    data
  })
}

/** 更新角色 */
export function updateRole(data) {
  return request({
    url: '/api/system/role/update',
    method: 'put',
    data
  })
}

/** 删除角色 */
export function deleteRole(id) {
  return request({
    url: `/api/system/role/delete/${id}`,
    method: 'delete'
  })
}

/** 获取角色菜单权限 */
export function getRoleMenus(roleId) {
  return request({
    url: `/api/system/role/menus/${roleId}`,
    method: 'get'
  })
}

/** 保存角色菜单权限 */
export function saveRoleMenus(roleId, menuIds) {
  return request({
    url: `/api/system/role/menus/${roleId}`,
    method: 'put',
    data: { menuIds }
  })
}

// ==================== 菜单管理 ====================

/** 菜单树 */
export function getMenuTree() {
  return request({
    url: '/api/system/menu/tree',
    method: 'get'
  })
}

// ==================== 操作日志 ====================

/** 操作日志列表 */
export function getOperationLogs(params) {
  return request({
    url: '/api/system/log/operation',
    method: 'get',
    params
  })
}

/** 登录日志列表 */
export function getLoginLogs(params) {
  return request({
    url: '/api/system/log/login',
    method: 'get',
    params
  })
}

// ==================== 系统配置 ====================

/** 配置列表 */
export function getConfigList(params) {
  return request({
    url: '/api/system/config/list',
    method: 'get',
    params
  })
}

/** 更新配置 */
export function updateConfig(data) {
  return request({
    url: '/api/system/config/update',
    method: 'put',
    data
  })
}

/** 新增配置 */
export function addConfig(data) {
  return request({
    url: '/api/system/config/add',
    method: 'post',
    data
  })
}

/** 删除配置 */
export function deleteConfig(id) {
  return request({
    url: `/api/system/config/delete/${id}`,
    method: 'delete'
  })
}

// ==================== Banner管理 ====================

/** Banner列表(分页) */
export function getBannerList(params) {
  return request({
    url: '/api/banner/list',
    method: 'get',
    params
  })
}

/** 新增Banner */
export function addBanner(data) {
  return request({
    url: '/api/banner/add',
    method: 'post',
    data
  })
}

/** 更新Banner */
export function updateBanner(data) {
  return request({
    url: '/api/banner/update',
    method: 'put',
    data
  })
}

/** 删除Banner */
export function deleteBanner(id) {
  return request({
    url: `/api/banner/delete/${id}`,
    method: 'delete'
  })
}

/** 切换Banner状态 */
export function toggleBannerStatus(id) {
  return request({
    url: `/api/banner/toggle-status/${id}`,
    method: 'put'
  })
}

// ==================== 科普文章管理 ====================

/** 文章列表(分页) */
export function getArticleList(params) {
  return request({
    url: '/api/edu/article/list',
    method: 'get',
    params
  })
}

/** 新增文章 */
export function addArticle(data) {
  return request({
    url: '/api/edu/article/add',
    method: 'post',
    data
  })
}

/** 更新文章 */
export function updateArticle(data) {
  return request({
    url: '/api/edu/article/update',
    method: 'put',
    data
  })
}

/** 删除文章 */
export function deleteArticle(id) {
  return request({
    url: `/api/edu/article/delete/${id}`,
    method: 'delete'
  })
}

/** 切换文章状态 */
export function toggleArticleStatus(id) {
  return request({
    url: `/api/edu/article/toggle-status/${id}`,
    method: 'put'
  })
}
