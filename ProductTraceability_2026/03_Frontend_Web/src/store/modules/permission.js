import { constantRoutes, asyncRoutes } from '@/router'

/**
 * 判断用户是否有权限访问该路由
 * @param {Array} roles 用户角色列表
 * @param {Object} route 路由对象
 */
function hasPermission(roles, route) {
  if (route.meta && route.meta.roles) {
    return roles.some(role => route.meta.roles.includes(role))
  }
  // 没有设置 roles 表示不需要权限
  return true
}

/**
 * 递归过滤异步路由表
 * @param {Array} routes asyncRoutes
 * @param {Array} roles 用户角色
 */
export function filterAsyncRoutes(routes, roles) {
  const res = []

  routes.forEach(route => {
    const tmp = { ...route }

    if (hasPermission(roles, tmp)) {
      if (tmp.children) {
        tmp.children = filterAsyncRoutes(tmp.children, roles)
        // 如果过滤后没有子路由，跳过该父路由
        if (tmp.children.length === 0) {
          return
        }
      }
      res.push(tmp)
    }
  })

  return res
}

const state = {
  routes: [],
  addRoutes: []
}

const mutations = {
  SET_ROUTES: (state, routes) => {
    state.addRoutes = routes
    state.routes = constantRoutes.concat(routes)
  }
}

const actions = {
  generateRoutes({ commit }, roles) {
    return new Promise(resolve => {
      let accessedRoutes

      if (roles.includes('admin')) {
        // 管理员拥有全部路由，但排除仅企业用户使用的路由
        accessedRoutes = asyncRoutes.filter(route => {
          return !(route.path === '/enterprise-self')
        })
      } else {
        // 企业用户按角色过滤路由
        accessedRoutes = filterAsyncRoutes(asyncRoutes, roles)
      }

      commit('SET_ROUTES', accessedRoutes)
      resolve(accessedRoutes)
    })
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
