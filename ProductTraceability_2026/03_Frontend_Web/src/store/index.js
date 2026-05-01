import Vue from 'vue'
import Vuex from 'vuex'
import user from './modules/user'
import permission from './modules/permission'

Vue.use(Vuex)

const getters = {
  token: state => state.user.token,
  name: state => state.user.name,
  roles: state => state.user.roles,
  userInfo: state => state.user.userInfo,
  permissionRoutes: state => state.permission.routes,
  addRoutes: state => state.permission.addRoutes
}

export default new Vuex.Store({
  modules: {
    user,
    permission
  },
  getters
})
