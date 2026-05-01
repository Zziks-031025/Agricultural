const app = getApp()
const util = require('../../utils/util.js')
const request = require('../../utils/request.js')

// 数据库状态码配置（严格对齐数据库定义）
const DB_STATUS_CONFIG = {
  1: { text: '初始化', color: '#4CAF50' },   // 绿色
  2: { text: '生长中', color: '#8BC34A' },   // 浅绿
  3: { text: '已收获', color: '#FF9800' },   // 橙色（待检疫）
  4: { text: '加工中', color: '#2196F3' },   // 蓝色
  5: { text: '已检疫', color: '#9C27B0' },   // 紫色
  6: { text: '已入库', color: '#00BCD4' },   // 青色
  7: { text: '运输中', color: '#3F51B5' },   // 靛蓝
  8: { text: '已销售', color: '#757575' },   // 灰色
  9: { text: '加工完成', color: '#00897B' }  // 深青
}

// ========== Per-role Tab configurations ==========
// key:    unique identifier used in switchTab
// label:  display text
// statuses: array of DB status codes to filter, null = all
// dotKey: if non-empty, this tab can show a red dot; value maps to a count field
// dotWarning: true = orange dot, false = red dot
const ROLE_TABS = {
  // Type 1: 养殖户
  1: [
    { key: 'all',       label: '全部',   statuses: null,              dotKey: '',         dotWarning: false },
    { key: 'breeding',  label: '养殖中', statuses: [1, 2],           dotKey: 'breeding', dotWarning: false },
    { key: 'pending',   label: '待检疫', statuses: [3],              dotKey: 'pending',  dotWarning: true },
    { key: 'completed', label: '已出栏', statuses: [4, 5, 6, 7, 8, 9], dotKey: '',         dotWarning: false }
  ],
  // Type 2: 加工厂
  2: [
    { key: 'all',            label: '全部',   statuses: null,              dotKey: '',              dotWarning: false },
    { key: 'pendingReceive', label: '待接收', statuses: [5, 7],           dotKey: 'pendingReceive', dotWarning: true },
    { key: 'processing',     label: '加工中', statuses: [4, 9],           dotKey: '',              dotWarning: false },
    { key: 'finished',       label: '已完结', statuses: [6, 8],           dotKey: '',              dotWarning: false }
  ],
  // Type 3: 检疫质检机构
  3: [
    { key: 'all',       label: '全部',   statuses: null,             dotKey: '',        dotWarning: false },
    { key: 'pending',   label: '待处理', statuses: [3],              dotKey: 'pending', dotWarning: true },
    { key: 'passed',    label: '已合格', statuses: [5, 6, 7, 8, 9],    dotKey: '',        dotWarning: false },
    { key: 'failed',    label: '不合格', statuses: null,             dotKey: '',        dotWarning: false }
  ],
  // Admin / default
  0: [
    { key: 'all',       label: '全部',   statuses: null,        dotKey: '', dotWarning: false },
    { key: 'farmPhase', label: '养殖期', statuses: [1, 2, 3],  dotKey: '', dotWarning: false },
    { key: 'flowPhase', label: '流通期', statuses: [4, 5, 6, 7, 9], dotKey: '', dotWarning: false },
    { key: 'sold',      label: '已销售', statuses: [8],         dotKey: '', dotWarning: false }
  ]
}

Page({
  data: {
    activeTab: 'all',
    activeTabLabel: '全部',
    tabList: [],                // Dynamic tabs - populated in onLoad
    searchKeyword: '',
    enterpriseId: null,
    enterpriseName: '',
    enterpriseType: 0,
    userType: 3,
    action: '',                 // add-record: 添加生长记录模式
    isInspector: false,
    filterByCurrentUser: false,
    inspectionEnterpriseId: null,
    batchList: [],
    page: 1,
    pageSize: 10,
    loading: false,
    noMore: false,
    // 二维码弹窗
    showQrcode: false,
    currentQrcode: '',
    currentBatchCode: ''
  },

  onLoad(options) {
    // 获取当前登录用户信息
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    const userType = userInfo.userType || 3
    const enterpriseType = userInfo.enterpriseType || app.globalData.enterpriseType || 0
    const currentEnterpriseId = userInfo.enterpriseId || null
    const currentEnterpriseName = userInfo.enterpriseName || ''

    // Build dynamic tab list based on role
    const roleKey = (userType === 1) ? 0 : (enterpriseType || 0) // admin -> 0
    const tabSource = ROLE_TABS[roleKey] || ROLE_TABS[0]
    // Deep copy so we can mutate showDot later
    const tabList = tabSource.map(t => Object.assign({}, t, { showDot: false }))

    // 检疫员默认 Tab 为'待处理'，其他角色为'全部'
    const isInspector = (userType === 2 && enterpriseType === 3)
    const defaultTab = 'all'
    const defaultTabConfig = tabList.find(t => t.key === defaultTab)

    this.setData({
      userType,
      enterpriseType,
      isInspector,
      tabList,
      activeTab: defaultTab,
      activeTabLabel: defaultTabConfig ? defaultTabConfig.label : '全部'
    })

    // 检疫员设置页面标题
    if (isInspector) {
      wx.setNavigationBarTitle({ title: '检疫批次列表' })
    }
    
    // 接收从企业管理页面传递的参数（管理员查看指定企业的批次）
    if (options.enterpriseId) {
      this.setData({ 
        enterpriseId: options.enterpriseId,
        filterByCurrentUser: true
      })
    }
    
    // 如果有企业名称参数，自动填入搜索框
    if (options.enterpriseName) {
      const enterpriseName = decodeURIComponent(options.enterpriseName)
      this.setData({ searchKeyword: enterpriseName })
    }
    
    // 接收操作模式参数
    if (options.action) {
      this.setData({ action: options.action })
      
      // 如果是添加记录模式，设置标题
      if (options.action === 'add-record') {
        wx.setNavigationBarTitle({ title: '选择批次' })
      }
    }
    
    // 企业用户（userType=2）：自动按当前企业筛选批次
    if (userType === 2 && currentEnterpriseId && !options.enterpriseId) {
      if (isInspector) {
        // 检疫企业：按 inspectionEnterpriseId 过滤，只显示分配给本机构的批次
        this.setData({
          filterByCurrentUser: false,
          enterpriseId: null,
          enterpriseName: currentEnterpriseName,
          enterpriseType: enterpriseType,
          inspectionEnterpriseId: currentEnterpriseId
        })
      } else {
        // 养殖/加工企业：按批次所属企业ID过滤
        this.setData({
          filterByCurrentUser: true,
          enterpriseId: currentEnterpriseId,
          enterpriseName: currentEnterpriseName,
          enterpriseType: enterpriseType
        })
        wx.showToast({
          title: '已筛选本企业批次',
          icon: 'none',
          duration: 1500
        })
      }
    }
    
    // loadBatchList 由 onShow 统一触发，避免 onLoad+onShow 并发加载冲突
  },

  onShow() {
    this.setData({
      page: 1,
      batchList: [],
      noMore: false,
      loading: false
    })
    this.loadBatchList()
    this.loadStatusCount()
  },

  // 加载各状态批次数量 & update tab dot visibility
  loadStatusCount() {
    const params = {}
    if (this.data.enterpriseId) {
      params.enterpriseId = this.data.enterpriseId
    }
    if (this.data.enterpriseType) {
      params.enterpriseType = this.data.enterpriseType
    }
    request.get('/api/batch/status-count', params).then(res => {
      if (res.code === 200 && res.data) {
        const counts = res.data
        const dotCounts = {
          breeding: counts.breeding || (counts['1'] || 0) + (counts['2'] || 0),
          pending: counts.pending || counts['3'] || 0,
          pendingReceive: counts.pendingReceive || (counts['5'] || 0) + (counts['7'] || 0)
        }
        // 读取已读记录：只有当数量比上次查看时增多才显示红点
        const readRecord = wx.getStorageSync('batch_tab_read') || {}
        this._dotCounts = dotCounts
        const tabList = this.data.tabList.map(tab => {
          const copy = Object.assign({}, tab)
          if (copy.dotKey && dotCounts[copy.dotKey] !== undefined) {
            const current = dotCounts[copy.dotKey]
            const lastRead = readRecord[copy.dotKey]
            // 有记录且当前数量 <= 已读数量，不显示红点
            copy.showDot = (lastRead === undefined) ? current > 0 : current > lastRead
          } else {
            copy.showDot = false
          }
          return copy
        })
        this.setData({ tabList })
      }
    }).catch(() => {})
  },

  switchTab(e) {
    const tab = e.currentTarget.dataset.tab
    const tabConfig = this.data.tabList.find(t => t.key === tab)

    // 点击Tab后清除该Tab的红点，并持久化已读数量
    const tabList = this.data.tabList.map(t => {
      if (t.key === tab && t.showDot) {
        return Object.assign({}, t, { showDot: false })
      }
      return t
    })

    // 持久化：记录该tab当前批次数量为已读
    if (tabConfig && tabConfig.dotKey && this._dotCounts) {
      const readRecord = wx.getStorageSync('batch_tab_read') || {}
      readRecord[tabConfig.dotKey] = this._dotCounts[tabConfig.dotKey] || 0
      wx.setStorageSync('batch_tab_read', readRecord)
    }

    this.setData({
      activeTab: tab,
      activeTabLabel: tabConfig ? tabConfig.label : '全部',
      tabList,
      page: 1,
      batchList: [],
      noMore: false
    })
    this.loadBatchList()
  },

  onSearchInput(e) {
    this.setData({
      searchKeyword: e.detail.value
    })
    
    clearTimeout(this.searchTimer)
    this.searchTimer = setTimeout(() => {
      this.setData({
        page: 1,
        batchList: [],
        noMore: false
      })
      this.loadBatchList()
    }, 500)
  },

  clearSearch() {
    this.setData({
      searchKeyword: '',
      page: 1,
      batchList: [],
      noMore: false
    })
    this.loadBatchList()
  },

  loadBatchList() {
    if (this.data.loading || this.data.noMore) {
      return
    }

    this.setData({ loading: true })

    const params = {
      current: this.data.page,
      size: this.data.pageSize
    }

    if (this.data.searchKeyword) {
      params.keyword = this.data.searchKeyword
    }

    // 按企业筛选
    if (this.data.filterByCurrentUser && this.data.enterpriseId) {
      params.enterpriseId = this.data.enterpriseId
      params.enterpriseType = this.data.enterpriseType
    }

    // 检疫企业：按分配给本机构的批次过滤
    if (this.data.inspectionEnterpriseId) {
      params.inspectionEnterpriseId = this.data.inspectionEnterpriseId
    }

    // 按Tab状态筛选
    const activeTabConfig = this.data.tabList.find(t => t.key === this.data.activeTab)
    if (activeTabConfig && activeTabConfig.statuses) {
      params.statuses = activeTabConfig.statuses.join(',')
    }

    request.get('/api/batch/list', params).then(res => {
      if (res.code === 200 && res.data) {
        let records = (res.data.records || []).map(item => {
          const batchStatus = item.status || item.batchStatus
          const statusConfig = DB_STATUS_CONFIG[batchStatus] || { text: '未知', color: '#999' }
          const txHash = item.txHash || null
          const txHashDisplay = txHash ? `${txHash.substring(0, 8)}...${txHash.substring(txHash.length - 6)}` : ''
          return {
            id: item.id,
            batchCode: item.batchCode,
            productName: item.productName || '--',
            productIcon: '/images/agricultural.png',
            quantity: item.quantity || item.currentQuantity || item.initQuantity || 0,
            unit: item.unit || '只',
            enterpriseId: item.enterpriseId,
            enterpriseType: item.enterpriseType,
            enterpriseName: item.enterpriseName || '--',
            status: batchStatus,
            statusText: statusConfig.text,
            statusColor: statusConfig.color,
            inspectionResult: item.inspectionResult != null ? item.inspectionResult : null,
            createTime: item.createTime || '--',
            createDate: item.createDate || (item.createTime ? item.createTime.substring(0, 10) : '--'),
            txHash: txHash,
            txHashDisplay: txHashDisplay
          }
        })

        // 检疫企业的"不合格"tab：只显示检疫结果为0的批次
        if (this.data.activeTab === 'failed') {
          records = records.filter(item => item.inspectionResult === 0)
        }

        const newList = this.data.page === 1 ? records : [...this.data.batchList, ...records]
        this.setData({
          batchList: newList,
          loading: false,
          noMore: records.length < this.data.pageSize
        })
      } else {
        this.setData({ loading: false })
      }
    }).catch(err => {
      console.error('[batch] loadBatchList error:', err)
      this.setData({ loading: false })
    })
  },

  loadMore() {
    if (!this.data.loading && !this.data.noMore) {
      this.setData({
        page: this.data.page + 1
      })
      this.loadBatchList()
    }
  },

  goToDetail(e) {
    const id = e.currentTarget.dataset.id
    
    // 如果是添加记录模式，跳转到生长记录列表页
    if (this.data.action === 'add-record') {
      wx.navigateTo({
        url: `/pages/batch/record-list?batchId=${id}`
      })
    } else {
      // 所有角色（养殖户、加工厂、检疫员）统一跳转内部详情页
      wx.navigateTo({
        url: `/pages/batch/detail?id=${id}`
      })
    }
  },

  // 复制哈希值
  copyHash(e) {
    const hash = e.currentTarget.dataset.hash
    if (!hash) return
    
    wx.setClipboardData({
      data: hash,
      success: () => {
        wx.showToast({
          title: '哈希已复制',
          icon: 'success'
        })
      }
    })
  },

  // 显示批次二维码
  showBatchCode(e) {
    const item = e.currentTarget.dataset.item
    
    // TODO: 替换为真实的二维码生成API
    const qrcodeUrl = `https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=${encodeURIComponent('https://trace.example.com/batch/' + item.batchCode)}`
    
    this.setData({
      showQrcode: true,
      currentQrcode: qrcodeUrl,
      currentBatchCode: item.batchCode
    })
  },

  // 隐藏二维码弹窗
  hideQrcode() {
    this.setData({
      showQrcode: false
    })
  },

  // 保存二维码到相册
  saveQrcode() {
    wx.showLoading({ title: '保存中...' })
    
    wx.downloadFile({
      url: this.data.currentQrcode,
      success: (res) => {
        if (res.statusCode === 200) {
          wx.saveImageToPhotosAlbum({
            filePath: res.tempFilePath,
            success: () => {
              wx.hideLoading()
              wx.showToast({
                title: '已保存到相册',
                icon: 'success'
              })
            },
            fail: (err) => {
              wx.hideLoading()
              if (err.errMsg.includes('auth deny')) {
                wx.showModal({
                  title: '提示',
                  content: '请授权保存图片到相册',
                  confirmText: '去设置',
                  success: (res) => {
                    if (res.confirm) {
                      wx.openSetting()
                    }
                  }
                })
              } else {
                wx.showToast({
                  title: '保存失败',
                  icon: 'none'
                })
              }
            }
          })
        }
      },
      fail: () => {
        wx.hideLoading()
        wx.showToast({
          title: '下载失败',
          icon: 'none'
        })
      }
    })
  },

  // 阻止冒泡
  stopPropagation() {},

  onPullDownRefresh() {
    this.setData({
      page: 1,
      batchList: [],
      noMore: false
    })
    this.loadBatchList()
    this.loadStatusCount()
    
    setTimeout(() => {
      wx.stopPullDownRefresh()
    }, 1000)
  }
})
