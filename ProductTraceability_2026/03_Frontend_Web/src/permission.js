import router from './router'
import store from './store'
import { Message } from 'element-ui'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'

NProgress.configure({ showSpinner: false })

const whiteList = ['/login', '/register']

router.beforeEach(async(to, from, next) => {
  NProgress.start()

  const hasToken = getToken()

  if (hasToken) {
    if (to.path === '/login') {
      // 已登录, 跳转首页
      next({ path: '/' })
      NProgress.done()
    } else {
      // 判断是否已获取用户角色
      const hasRoles = store.getters.roles && store.getters.roles.length > 0
      if (hasRoles) {
        next()
      } else {
        try {
          // 获取用户信息(包含角色)
          const { roles } = await store.dispatch('user/getInfo')

          // 根据角色生成可访问的路由表
          const accessRoutes = await store.dispatch('permission/generateRoutes', roles)

          // 动态添加路由
          router.addRoutes(accessRoutes)

          // hack: 确保 addRoutes 完成
          // set replace: true 防止导航留下历史记录
          next({ ...to, replace: true })
        } catch (error) {
          // 获取信息失败, 清除 token 并跳转登录
          await store.dispatch('user/resetToken')
          Message.error(error || '权限验证失败，请重新登录')
          next(`/login?redirect=${to.path}`)
          NProgress.done()
        }
      }
    }
  } else {
    // 未登录
    if (whiteList.indexOf(to.path) !== -1) {
      next()
    } else {
      next(`/login?redirect=${to.path}`)
      NProgress.done()
    }
  }
})

router.afterEach(() => {
  NProgress.done()
})
