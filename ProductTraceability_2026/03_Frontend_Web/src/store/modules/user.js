import { login, logout, getInfo } from '@/api/user'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { resetRouter } from '@/router'

/**
 * 根据后端返回的 userType + enterpriseType 确定前端角色
 * userType: 1-平台管理员 2-企业用户 3-普通用户
 * enterpriseType: 1-种植养殖 2-加工宰杀 3-检疫质检
 */
function resolveRoles(userInfo) {
  const userType = userInfo.userType
  if (userType === 1) {
    return ['admin']
  }
  if (userInfo.enterpriseAuditStatus !== undefined &&
      userInfo.enterpriseAuditStatus !== null &&
      userInfo.enterpriseAuditStatus !== 1) {
    return ['enterprise-review']
  }
  if (userType === 2) {
    const enterpriseType = userInfo.enterpriseType
    if (enterpriseType === 1) return ['type1']
    if (enterpriseType === 2) return ['type2']
    if (enterpriseType === 3) return ['type3']
  }
  return ['visitor']
}

const state = {
  token: getToken(),
  name: '',
  userInfo: null,
  roles: []
}

const mutations = {
  SET_TOKEN: (state, token) => {
    state.token = token
  },
  SET_NAME: (state, name) => {
    state.name = name
  },
  SET_USER_INFO: (state, userInfo) => {
    state.userInfo = userInfo
  },
  SET_ROLES: (state, roles) => {
    state.roles = roles
  }
}

const actions = {
  // 登录
  login({ commit }, userInfo) {
    const { username, password } = userInfo
    return new Promise((resolve, reject) => {
      login({ username: username.trim(), password: password }).then(response => {
        const { data } = response
        commit('SET_TOKEN', data.token)
        setToken(data.token)
        resolve()
      }).catch(error => {
        reject(error)
      })
    })
  },

  // 获取用户信息 + 角色
  getInfo({ commit, state }) {
    return new Promise((resolve, reject) => {
      getInfo(state.token).then(response => {
        const { data } = response

        if (!data) {
          reject('验证失败，请重新登录')
        }

        const userInfo = data.userInfo || data
        const roles = resolveRoles(userInfo)

        if (!roles || roles.length <= 0) {
          reject('该账号没有分配角色，请联系管理员')
        }

        commit('SET_USER_INFO', userInfo)
        commit('SET_NAME', userInfo.realName || userInfo.username)
        commit('SET_ROLES', roles)

        resolve({ roles, userInfo })
      }).catch(error => {
        reject(error)
      })
    })
  },

  // 登出
  logout({ commit, state, dispatch }) {
    return new Promise((resolve, reject) => {
      logout(state.token).then(() => {
        commit('SET_TOKEN', '')
        commit('SET_NAME', '')
        commit('SET_USER_INFO', null)
        commit('SET_ROLES', [])
        removeToken()
        resetRouter()

        dispatch('permission/generateRoutes', [], { root: true })

        resolve()
      }).catch(error => {
        reject(error)
      })
    })
  },

  // 重置 Token
  resetToken({ commit }) {
    return new Promise(resolve => {
      commit('SET_TOKEN', '')
      commit('SET_NAME', '')
      commit('SET_USER_INFO', null)
      commit('SET_ROLES', [])
      removeToken()
      resetRouter()
      resolve()
    })
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
