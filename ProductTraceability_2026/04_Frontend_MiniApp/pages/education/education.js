const app = getApp()
const request = require('../../utils/request.js')
const { proxyImage, proxyImages } = require('../../utils/util.js')

/**
 * 科普教育页面
 * 根据用户身份显示不同内容：
 * - 养殖企业(Type 1): 养殖技术、防疫知识
 * - 加工企业(Type 2): 加工规范、卫生标准
 * - 消费者/普通用户: 区块链溯源、食品安全
 */
Page({
  data: {
    // 用户信息
    userType: 3,         // 1-管理员 2-企业用户 3-普通用户
    enterpriseType: 0,   // 1-养殖 2-加工 3-检疫
    enterpriseId: null,  // 企业ID
    category: '',        // 页面传入的分类参数

    // 页面状态
    showList: true,      // true-显示列表 false-显示详情

    // 文章数据
    articleList: [],     // 当前显示的文章列表
    currentArticle: {},  // 当前查看的文章详情

    // 分类标题
    categoryTitle: '科普教育'
  },

  onLoad(options) {
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    const userType = userInfo.userType || 3
    const enterpriseType = userInfo.enterpriseType || 0
    const enterpriseId = userInfo.enterpriseId || null
    const category = options.category || ''

    this.setData({ userType, enterpriseType, enterpriseId, category })

    if (options.id) {
      this.loadArticleDetail(options.id)
    } else {
      this.loadArticleList()
    }
  },

  /**
   * 获取分类标题
   */
  getCategoryTitle(category, enterpriseType) {
    if (category === 'process-standard') return '加工规范'
    if (category === 'hygiene-spec') return '卫生规范'
    if (category === 'farming') return '养殖技术'
    if (category === 'quarantine-standard') return '检疫规范'
    return '科普教育'
  },

  /**
   * 获取API查询的category参数
   */
  getApiCategory(category) {
    const map = {
      'process-standard': 'process',
      'hygiene-spec': 'hygiene',
      'farming': 'farming',
      'quarantine-standard': 'quarantine'
    }
    return map[category] || ''
  },

  /**
   * 加载文章列表
   */
  loadArticleList() {
    const { userType, enterpriseType, enterpriseId, category } = this.data
    const categoryTitle = this.getCategoryTitle(category, enterpriseType)

    this.setData({ categoryTitle, showList: true })
    wx.setNavigationBarTitle({ title: categoryTitle })

    const params = {}
    if (enterpriseId) {
      params.enterpriseId = enterpriseId
    }
    const apiCategory = this.getApiCategory(category)
    if (apiCategory) {
      params.category = apiCategory
    }

    const baseUrl = (app && app.globalData ? app.globalData.apiBaseUrl : '') || ''

    request.get('/api/edu/article/published', params).then(res => {
      if (res.code === 200 && res.data && res.data.length > 0) {
        const articleList = res.data.map(item => {
          let contentArr = []
          try { contentArr = JSON.parse(item.content || '[]') } catch(e) { contentArr = [] }
          return {
            id: String(item.id),
            category: item.category || '',
            title: item.title || '',
            author: item.author || '',
            publishTime: (item.publishTime || '').substring(0, 10),
            viewCount: item.viewCount || 0,
            cover: item.coverUrl && !item.coverUrl.startsWith('http') ? baseUrl + item.coverUrl : (item.coverUrl || ''),
            summary: item.summary || '',
            content: contentArr
          }
        })
        const coverUrls = articleList.map(a => a.cover)
        proxyImages(coverUrls).then(localCovers => {
          articleList.forEach((a, i) => { if (localCovers[i]) a.cover = localCovers[i] })
          this.setData({ articleList })
        }).catch(() => {
          this.setData({ articleList })
        })
      } else {
        this.setData({ articleList: [] })
      }
    }).catch(() => {
      this.setData({ articleList: [] })
    })
  },

  /**
   * 加载文章详情
   */
  loadArticleDetail(id) {
    const baseUrl = (app && app.globalData ? app.globalData.apiBaseUrl : '') || ''

    if (!/^\d+$/.test(id)) {
      this.setData({ showList: true })
      return
    }

    request.get('/api/edu/article/detail/' + id).then(res => {
      if (res.code === 200 && res.data) {
        const item = res.data
        let contentArr = []
        try { contentArr = JSON.parse(item.content || '[]') } catch(e) { contentArr = [] }
        contentArr = contentArr.map(block => {
          if (block.type === 'image' && block.value && !block.value.startsWith('http')) {
            return { ...block, value: baseUrl + block.value }
          }
          return block
        })
        const article = {
          id: String(item.id),
          category: item.category || '',
          title: item.title || '',
          author: item.author || '',
          publishTime: (item.publishTime || '').substring(0, 10),
          viewCount: item.viewCount || 0,
          cover: item.coverUrl && !item.coverUrl.startsWith('http') ? baseUrl + item.coverUrl : (item.coverUrl || ''),
          summary: item.summary || '',
          content: contentArr
        }
        const allImgUrls = [article.cover, ...contentArr.filter(b => b.type === 'image').map(b => b.value)]
        proxyImages(allImgUrls).then(localUrls => {
          if (localUrls[0]) article.cover = localUrls[0]
          let imgIdx = 1
          article.content = contentArr.map(block => {
            if (block.type === 'image') {
              return { ...block, value: localUrls[imgIdx++] || block.value }
            }
            return block
          })
          this.setData({ currentArticle: article, showList: false })
        }).catch(() => {
          this.setData({ currentArticle: article, showList: false })
        })
        wx.setNavigationBarTitle({ title: article.title })
        request.put('/api/edu/article/view/' + id).catch(() => {})
      } else {
        wx.showToast({ title: '文章不存在', icon: 'none' })
        this.setData({ showList: true })
      }
    }).catch(() => {
      wx.showToast({ title: '加载失败', icon: 'none' })
      this.setData({ showList: true })
    })
  },

  /**
   * 点击文章进入详情（使用页面跳转，支持系统返回按钮）
   */
  goToDetail(e) {
    const id = e.currentTarget.dataset.id
    const { category } = this.data

    let url = `/pages/education/education?id=${id}`
    if (category) {
      url += `&category=${category}`
    }

    wx.navigateTo({ url })
  },

  /**
   * 页面内返回按钮
   */
  backToList() {
    wx.navigateBack({
      fail: () => {
        this.loadArticleList()
      }
    })
  },

  updateViewCount(id) {
    // view count is reported via API in loadArticleDetail
  },

  previewImage(e) {
    const url = e.currentTarget.dataset.url
    wx.previewImage({
      current: url,
      urls: [url]
    })
  },

  onShareAppMessage() {
    const article = this.data.currentArticle || {}
    return {
      title: article.title || '科普教育',
      path: `/pages/education/education?id=${article.id || ''}`,
      imageUrl: article.cover || '/images/agricultural.png'
    }
  }
})
