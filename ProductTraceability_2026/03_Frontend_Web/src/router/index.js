import Vue from 'vue'
import VueRouter from 'vue-router'
import Layout from '@/layout'

Vue.use(VueRouter)

// 修复 Vue Router NavigationDuplicated / NavigationRedirected 报错
const originalPush = VueRouter.prototype.push
VueRouter.prototype.push = function push(location) {
  return originalPush.call(this, location).catch(err => {
    if (err && err.name !== 'NavigationDuplicated' &&
        !err.message.includes('Redirected when going from')) {
      throw err
    }
  })
}
const originalReplace = VueRouter.prototype.replace
VueRouter.prototype.replace = function replace(location) {
  return originalReplace.call(this, location).catch(err => {
    if (err && err.name !== 'NavigationDuplicated' &&
        !err.message.includes('Redirected when going from')) {
      throw err
    }
  })
}

/**
 * constantRoutes - 不需要权限的基础路由
 * 所有角色都可以访问
 */
export const constantRoutes = [
  {
    path: '/login',
    component: () => import('@/views/login/index.vue'),
    hidden: true
  },
  {
    path: '/register',
    component: () => import('@/views/login/register.vue'),
    hidden: true
  },
  {
    path: '/404',
    component: () => import('@/views/404.vue'),
    hidden: true
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页', icon: 'el-icon-s-home' }
      }
    ]
  },
  {
    path: '/message',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        name: 'MessageCenter',
        component: () => import('@/views/system/message.vue'),
        meta: { title: '我的消息', icon: 'el-icon-bell' }
      }
    ]
  }
]

/**
 * asyncRoutes - 需要根据角色动态加载的路由
 * meta.roles: 允许访问该路由的角色列表
 *   admin  - 系统管理员
 *   type1  - 种植养殖企业
 *   type2  - 加工宰杀企业
 *   type3  - 检疫质检企业
 *   enterprise-review - 企业审核中或被驳回时的受限角色
 */
export const asyncRoutes = [
  // ==========================================
  // 企业信息 (企业用户通用)
  // ==========================================
  {
    path: '/enterprise-self',
    component: Layout,
    meta: { title: '企业信息', icon: 'el-icon-office-building', roles: ['type1', 'type2', 'type3', 'enterprise-review'] },
    children: [
      {
        path: 'info',
        name: 'EnterpriseSelfInfo',
        component: () => import('@/views/enterprise-self/info.vue'),
        meta: { title: '企业信息', icon: 'el-icon-office-building', roles: ['type1', 'type2', 'type3', 'enterprise-review'] }
      },
      {
        path: 'banner',
        name: 'EnterpriseBanner',
        component: () => import('@/views/system/banner.vue'),
        meta: { title: '轮播图管理', icon: 'el-icon-picture-outline', roles: ['type1', 'type2', 'type3'] }
      },
      {
        path: 'article',
        name: 'EnterpriseArticle',
        component: () => import('@/views/content/article.vue'),
        meta: { title: '科普文章管理', icon: 'el-icon-notebook-2', roles: ['type1', 'type2', 'type3'] }
      }
    ]
  },

  // ==========================================
  // 生产管理 (仅种植养殖企业)
  // ==========================================
  {
    path: '/production',
    component: Layout,
    redirect: '/production/batch-init',
    meta: { title: '生产管理', icon: 'el-icon-s-cooperation', roles: ['type1'] },
    children: [
      {
        path: 'batch-init',
        name: 'BatchInit',
        component: () => import('@/views/production/batch-init.vue'),
        meta: { title: '种植/养殖初始化', icon: 'el-icon-document-add', roles: ['type1'] }
      },
      {
        path: 'process-record',
        name: 'ProcessRecord',
        component: () => import('@/views/production/process-record.vue'),
        meta: { title: '过程信息记录', icon: 'el-icon-edit-outline', roles: ['type1'] }
      }
    ]
  },

  // ==========================================
  // 加工与来源 (仅加工宰杀企业)
  // ==========================================
  {
    path: '/processing',
    component: Layout,
    redirect: '/processing/material',
    meta: { title: '加工与来源', icon: 'el-icon-s-tools', roles: ['type2'] },
    children: [
      {
        path: 'material',
        name: 'ProcessingMaterial',
        component: () => import('@/views/processing/material.vue'),
        meta: { title: '原料接收记录', icon: 'el-icon-box', roles: ['type2'] }
      },
      {
        path: 'product',
        name: 'ProcessingProduct',
        component: () => import('@/views/processing/product.vue'),
        meta: { title: '产品加工记录', icon: 'el-icon-s-operation', roles: ['type2'] }
      }
    ]
  },

  // ==========================================
  // 质检与流通 (根据角色差异化)
  // ==========================================
  {
    path: '/circulation',
    component: Layout,
    redirect: '/circulation/inspection',
    meta: { title: '质检与流通', icon: 'el-icon-s-check', roles: ['type1', 'type2', 'type3'] },
    children: [
      {
        path: 'quarantine-apply',
        name: 'QuarantineApply',
        component: () => import('@/views/circulation/quarantine-apply.vue'),
        meta: { title: '检疫申报', icon: 'el-icon-s-claim', roles: ['type1'] }
      },
      {
        path: 'quarantine-list',
        name: 'QuarantineList',
        component: () => import('@/views/circulation/quarantine-list.vue'),
        meta: { title: '检疫申报列表', icon: 'el-icon-tickets', roles: ['type3'] }
      },
      {
        path: 'inspection',
        name: 'Inspection',
        component: () => import('@/views/circulation/inspection.vue'),
        meta: { title: '检疫质检记录', icon: 'el-icon-document-checked', roles: ['type3'] }
      },
      {
        path: 'storage',
        name: 'Storage',
        component: () => import('@/views/circulation/storage.vue'),
        meta: { title: '仓储信息', icon: 'el-icon-house', roles: ['type1', 'type2'] }
      },
      {
        path: 'transport',
        name: 'Transport',
        component: () => import('@/views/circulation/transport.vue'),
        meta: { title: '运输信息', icon: 'el-icon-truck', roles: ['type1', 'type2'] }
      },
      {
        path: 'sale',
        name: 'Sale',
        component: () => import('@/views/circulation/sale.vue'),
        meta: { title: '销售信息', icon: 'el-icon-sell', roles: ['type2'] }
      }
    ]
  },

  // ==========================================
  // 区块链 (企业用户通用)
  // ==========================================
  {
    path: '/chain',
    component: Layout,
    meta: { title: '区块链', icon: 'el-icon-connection', roles: ['type1', 'type2', 'type3'] },
    children: [
      {
        path: 'upload',
        name: 'ChainUpload',
        component: () => import('@/views/chain/upload.vue'),
        meta: { title: '区块链上链记录', icon: 'el-icon-upload2', roles: ['type1', 'type2', 'type3'] }
      }
    ]
  },

  // ==========================================
  // 数据统计 (企业用户通用)
  // ==========================================
  {
    path: '/analysis',
    component: Layout,
    meta: { title: '数据统计', icon: 'el-icon-data-analysis', roles: ['type1', 'type2', 'type3'] },
    children: [
      {
        path: 'statistics',
        name: 'AnalysisStatistics',
        component: () => import('@/views/analysis/statistics.vue'),
        meta: { title: '数据统计分析', icon: 'el-icon-pie-chart', roles: ['type1', 'type2', 'type3'] }
      }
    ]
  },

  // ==========================================
  // 溯源查询 (企业用户通用)
  // ==========================================
  {
    path: '/trace-query',
    component: Layout,
    meta: { title: '溯源查询', icon: 'el-icon-search', roles: ['type1', 'type2', 'type3'] },
    children: [
      {
        path: 'index',
        name: 'TraceQuery',
        component: () => import('@/views/supervision/trace.vue'),
        meta: { title: '溯源查询', icon: 'el-icon-search', roles: ['type1', 'type2', 'type3'] }
      }
    ]
  },

  // ==========================================
  // 以下为管理员专属路由
  // ==========================================

  // 企业管理 (仅管理员)
  {
    path: '/enterprise',
    component: Layout,
    redirect: '/enterprise/audit',
    meta: { title: '企业管理', icon: 'el-icon-office-building', roles: ['admin'] },
    children: [
      {
        path: 'audit',
        name: 'EnterpriseAudit',
        component: () => import('@/views/enterprise/audit.vue'),
        meta: { title: '企业入驻审核', icon: 'el-icon-s-claim', roles: ['admin'] }
      },
      {
        path: 'list',
        name: 'EnterpriseList',
        component: () => import('@/views/enterprise/list.vue'),
        meta: { title: '企业信息管理', icon: 'el-icon-notebook-2', roles: ['admin'] }
      },
      {
        path: 'image-audit',
        name: 'ImageAudit',
        component: () => import('@/views/enterprise/image-audit.vue'),
        meta: { title: '图片审核管理', icon: 'el-icon-picture-outline', roles: ['admin'] }
      }
    ]
  },

  // 系统管理 (仅管理员)
  {
    path: '/system',
    component: Layout,
    redirect: '/system/user',
    meta: { title: '系统管理', icon: 'el-icon-setting', roles: ['admin'] },
    children: [
      {
        path: 'user',
        name: 'SystemUser',
        component: () => import('@/views/system/user.vue'),
        meta: { title: '用户管理', icon: 'el-icon-user', roles: ['admin'] }
      },
      {
        path: 'banner',
        name: 'SystemBanner',
        component: () => import('@/views/system/banner.vue'),
        meta: { title: '轮播图管理', icon: 'el-icon-picture-outline', roles: ['admin'] }
      },
      {
        path: 'article',
        name: 'SystemArticle',
        component: () => import('@/views/content/article.vue'),
        meta: { title: '科普文章管理', icon: 'el-icon-notebook-2', roles: ['admin'] }
      },
      {
        path: 'config',
        name: 'SystemConfig',
        component: () => import('@/views/system/config.vue'),
        meta: { title: '系统参数配置', icon: 'el-icon-s-tools', roles: ['admin'] }
      },
      {
        path: 'log',
        name: 'SystemLog',
        component: () => import('@/views/system/log.vue'),
        meta: { title: '操作日志', icon: 'el-icon-document', roles: ['admin'] }
      }
    ]
  },

  // 模版管理 (仅管理员)
  {
    path: '/template',
    component: Layout,
    redirect: '/template/list',
    meta: { title: '模版管理', icon: 'el-icon-files', roles: ['admin'] },
    children: [
      {
        path: 'list',
        name: 'TemplateList',
        component: () => import('@/views/template/list.vue'),
        meta: { title: '溯源模版配置', icon: 'el-icon-document-copy', roles: ['admin'] }
      }
    ]
  },

  // 区块链管理 (仅管理员)
  {
    path: '/blockchain',
    component: Layout,
    redirect: '/blockchain/data',
    meta: { title: '区块链管理', icon: 'el-icon-connection', roles: ['admin'] },
    children: [
      {
        path: 'data',
        name: 'BlockchainData',
        component: () => import('@/views/blockchain/data.vue'),
        meta: { title: '上链数据统计', icon: 'el-icon-s-data', roles: ['admin'] }
      },
      {
        path: 'contract',
        name: 'BlockchainContract',
        component: () => import('@/views/blockchain/contract.vue'),
        meta: { title: '智能合约管理', icon: 'el-icon-document', roles: ['admin'] }
      },
      {
        path: 'node',
        name: 'BlockchainNode',
        component: () => import('@/views/blockchain/node.vue'),
        meta: { title: '节点监控', icon: 'el-icon-monitor', roles: ['admin'] }
      }
    ]
  },

  // 数据监管 (仅管理员)
  {
    path: '/supervision',
    component: Layout,
    redirect: '/supervision/trace',
    meta: { title: '数据监管', icon: 'el-icon-view', roles: ['admin'] },
    children: [
      {
        path: 'trace',
        name: 'SupervisionTrace',
        component: () => import('@/views/supervision/trace.vue'),
        meta: { title: '溯源数据查询', icon: 'el-icon-search', roles: ['admin'] }
      },
      {
        path: 'integrity',
        name: 'SupervisionIntegrity',
        component: () => import('@/views/supervision/integrity.vue'),
        meta: { title: '数据完整性校验', icon: 'el-icon-circle-check', roles: ['admin'] }
      },
      {
        path: 'chain-upload',
        name: 'SupervisionChainUpload',
        component: () => import('@/views/chain/upload.vue'),
        meta: { title: '区块链上链记录', icon: 'el-icon-upload2', roles: ['admin'] }
      },
      {
        path: 'statistics',
        name: 'SupervisionStatistics',
        component: () => import('@/views/analysis/statistics.vue'),
        meta: { title: '数据统计分析', icon: 'el-icon-pie-chart', roles: ['admin'] }
      }
    ]
  },

  // 统计分析 (仅管理员)
  {
    path: '/statistics',
    component: Layout,
    redirect: '/statistics/dashboard',
    meta: { title: '统计分析', icon: 'el-icon-data-line', roles: ['admin'] },
    children: [
      {
        path: 'dashboard',
        name: 'StatisticsDashboard',
        component: () => import('@/views/statistics/dashboard.vue'),
        meta: { title: '数据统计大屏', icon: 'el-icon-monitor', roles: ['admin'] }
      },
      {
        path: 'scan',
        name: 'ScanStats',
        component: () => import('@/views/statistics/scan-stats.vue'),
        meta: { title: '溯源查询统计', icon: 'el-icon-search', roles: ['admin'] }
      }
    ]
  },

  // 404 兜底 - 必须放在最后
  { path: '*', redirect: '/404', hidden: true }
]

const createRouter = () => new VueRouter({
  mode: 'hash',
  scrollBehavior: () => ({ y: 0 }),
  routes: constantRoutes
})

const router = createRouter()

// 重置路由（用于注销后清除动态路由）
export function resetRouter() {
  const newRouter = createRouter()
  router.matcher = newRouter.matcher
}

export default router
