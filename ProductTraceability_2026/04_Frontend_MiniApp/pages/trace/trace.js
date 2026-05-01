const app = getApp()
const request = require('../../utils/request.js')
const { proxyImage, proxyImages } = require('../../utils/util.js')

Page({
  data: {
    batchId: '',
    loading: true,
    loadError: false,
    errorMsg: '',

    productInfo: {},
    timelineList: [],
    enterpriseInfo: null,

    verifying: false,
    showVerifyResult: false,
    verifySuccess: false,
    verifyData: null
  },

  onLoad(options) {
    const batchId = options.batchId || options.id || options.code
    if (!batchId) {
      this.setData({
        loading: false,
        loadError: true,
        errorMsg: '未获取到批次号'
      })
      return
    }

    this.setData({ batchId })
    this.loadData()
  },

  // ==================== 数据加载 ====================

  async loadData() {
    this.setData({ loading: true, loadError: false })

    try {
      const res = await request.get('/api/trace/detail', {
        batchId: this.data.batchId
      })

      if (res && res.code === 200 && res.data) {
        const data = res.data
        const apiBase = (app.globalData && app.globalData.apiBaseUrl) || ''
        const productInfo = data.batchInfo || {}
        const enterpriseInfo = data.enterpriseInfo || null

        // 拼接产品图片URL
        if (productInfo.imageUrl && !productInfo.imageUrl.startsWith('http')) {
          productInfo.imageUrl = apiBase + productInfo.imageUrl
        }
        // 如果没有产品图片，从溯源节点中提取第一张现场照片作为背景
        if (!productInfo.imageUrl) {
          const timeline = data.timeline || []
          for (let i = 0; i < timeline.length; i++) {
            let imgs = timeline[i].images || []
            if (imgs.length > 0) {
              const firstImg = imgs[0]
              productInfo.imageUrl = (firstImg && !firstImg.startsWith('http')) ? apiBase + firstImg : firstImg
              break
            }
          }
        }
        // 拼接企业logo URL
        if (enterpriseInfo && enterpriseInfo.logo && !enterpriseInfo.logo.startsWith('http') && !enterpriseInfo.logo.startsWith('/images')) {
          enterpriseInfo.logo = apiBase + enterpriseInfo.logo
        }

        const timelineList = this.processTimeline(data.timeline || [], apiBase)

        // 代理下载所有图片后再setData
        this._proxyAndSetData(productInfo, timelineList, enterpriseInfo)
      } else {
        throw new Error(res?.message || '获取数据失败')
      }
    } catch (error) {
      console.error('Load trace data error:', error)
      this.setData({
        loading: false,
        loadError: true,
        errorMsg: error.message || '加载溯源数据失败'
      })
    }
  },

  /**
   * 处理时间轴数据，为每个节点补充前端展示所需字段
   */
  processTimeline(timeline, apiBase) {
    return timeline.map((node, index) => {
      // 拼接图片URL
      let images = node.images || []
      images = images.map(img => {
        if (!img) return ''
        if (img.startsWith('http')) return img
        return apiBase + img
      })
      
      // 检疫证书URL（如果有）
      let certUrl = ''
      if (node.stage === 'inspection' && images.length > 0) {
        certUrl = images[0]
      }

      // 处理details对象，转换为数组格式便于遍历显示
      const details = node.details || {}
      const detailsArray = Object.keys(details)
        .filter(key => details[key] !== null && details[key] !== undefined && details[key] !== '')
        .map(key => ({
          label: key,
          value: details[key]
        }))
      
      // 提取特定字段用于兼容旧版显示逻辑
      const operatorName = node.operator || details['负责人'] || details['检疫员'] || details['操作人'] || ''
      const location = details['作业地点'] || details['仓库位置'] || details['产地'] || details['出发地'] || ''
      const remark = details['操作说明'] || details['备注'] || ''
      
      // 检疫专属字段
      const inspectionResult = details['检疫结果'] || ''
      const certificateNo = details['证书编号'] || ''
      const resultTagClass = inspectionResult === '合格' ? 'tag-pass' : 'tag-fail'
      const resultTagText = inspectionResult || '待检'
      
      return {
        id: node.id || index,
        title: node.title || '',
        time: node.time || '',
        operator: node.operator || '',
        stage: node.stage || '',
        details: details,
        detailsArray: detailsArray,
        txHash: node.txHash || '',
        blockNumber: node.blockNumber || '',
        images: images,
        showChainInfo: false,
        txHashShort: node.txHash ? this.shortenHash(node.txHash) : '',
        txHashDisplay: node.txHash ? this.formatTxHashDisplay(node.txHash) : '',
        isQuarantine: node.stage === 'inspection',
        isOfficial: node.official === true,
        nodeTypeLabel: this.getStageLabel(node.stage),
        nodeTypeClass: this.getStageClass(node.stage),
        hasImages: images.length > 0,
        certificateUrl: certUrl,
        hasCertificate: !!certUrl,
        // 兼容字段
        operatorName: operatorName,
        location: location,
        remark: remark,
        // 检疫专属
        inspectionResult: inspectionResult,
        certificateNo: certificateNo,
        resultTagClass: resultTagClass,
        resultTagText: resultTagText
      }
    })
  },

  getStageLabel(stage) {
    const map = {
      'batch_init': '养殖端',
      'growth': '养殖端',
      'inspection': '检疫端',
      'processing': '加工端',
      'storage': '仓储端',
      'transport': '物流端',
      'sale': '销售端'
    }
    return map[stage] || ''
  },

  getStageClass(stage) {
    const map = {
      'batch_init': 'tag-breeding',
      'growth': 'tag-breeding',
      'inspection': 'tag-quarantine',
      'processing': 'tag-processing',
      'storage': 'tag-storage',
      'transport': 'tag-transport',
      'sale': 'tag-sales'
    }
    return map[stage] || ''
  },

  // ==================== 哈希工具方法 ====================

  shortenHash(hash) {
    if (!hash || hash.length < 20) return hash
    return hash.substring(0, 10) + '...' + hash.substring(hash.length - 8)
  },

  formatTxHashDisplay(hash) {
    if (!hash || hash.length < 20) return hash
    return hash.substring(0, 8) + '...' + hash.substring(hash.length - 6)
  },

  // ==================== 交互事件 ====================

  toggleChainInfo(e) {
    const index = e.currentTarget.dataset.index
    const key = `timelineList[${index}].showChainInfo`
    const currentValue = this.data.timelineList[index].showChainInfo
    this.setData({
      [key]: !currentValue
    })
  },

  copyText(e) {
    const text = e.currentTarget.dataset.text
    if (!text) return

    wx.setClipboardData({
      data: text,
      success: () => {
        wx.showToast({ title: '已复制', icon: 'success', duration: 1500 })
      }
    })
  },

  previewImage(e) {
    const { urls, current } = e.currentTarget.dataset
    if (urls && urls.length > 0) {
      wx.previewImage({
        urls: urls,
        current: current || urls[0]
      })
    }
  },

  // 点击检疫证书缩略图，放大查看
  previewCertificate(e) {
    const url = e.currentTarget.dataset.url
    if (url) {
      wx.previewImage({
        urls: [url],
        current: url
      })
    }
  },

  // 跳转企业公示详情页
  goToEnterpriseDetail() {
    const id = this.data.enterpriseInfo && this.data.enterpriseInfo.id
    if (id) {
      wx.navigateTo({
        url: `/pages/index/enterprise-detail?id=${id}`
      })
    } else {
      wx.showToast({ title: '企业信息不完整', icon: 'none' })
    }
  },

  // ==================== 区块链验真 ====================

  startVerify() {
    this.setData({ verifying: true })
    // 显示 1.5s Loading 动画后调用后端
    setTimeout(() => {
      this.doVerify()
    }, 1500)
  },

  async doVerify() {
    try {
      const res = await request.post('/api/blockchain/verify', {
        batchId: this.data.batchId
      })

      this.setData({ verifying: false })

      if (res && res.code === 200 && res.data) {
        this.setData({
          showVerifyResult: true,
          verifySuccess: res.data.verified === true,
          verifyData: {
            verifyTime: this.formatCurrentDateTime(),
            blockNumber: res.data.blockNumber || '--',
            totalNodes: res.data.totalNodes || 0,
            verifiedNodes: res.data.verifiedNodes || 0,
            nodeResults: res.data.nodeResults || []
          }
        })
      } else {
        this.setData({
          showVerifyResult: true,
          verifySuccess: false,
          verifyData: null
        })
      }
    } catch (error) {
      console.error('Verify error:', error)
      this.setData({
        verifying: false,
        showVerifyResult: true,
        verifySuccess: false,
        verifyData: null
      })
    }
  },

  formatCurrentDateTime() {
    const d = new Date()
    const pad = n => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  },

  closeVerifyResult() {
    this.setData({ showVerifyResult: false })
  },

  stopPropagation() {},

  /**
   * 代理下载所有图片后再统一setData
   */
  _proxyAndSetData(productInfo, timelineList, enterpriseInfo) {
    // 收集所有需要代理的单张图片
    const singleImageTasks = []
    // productInfo.imageUrl
    singleImageTasks.push(productInfo.imageUrl ? proxyImage(productInfo.imageUrl) : Promise.resolve(''))
    // enterpriseInfo.logo
    singleImageTasks.push(enterpriseInfo && enterpriseInfo.logo ? proxyImage(enterpriseInfo.logo) : Promise.resolve(''))

    // 收集所有时间轴节点的图片
    const timelineTasks = timelineList.map(node => {
      const tasks = []
      // 节点图片列表
      if (node.images && node.images.length > 0) {
        tasks.push(proxyImages(node.images))
      } else {
        tasks.push(Promise.resolve([]))
      }
      // 检疫证书
      if (node.certificateUrl) {
        tasks.push(proxyImage(node.certificateUrl))
      } else {
        tasks.push(Promise.resolve(''))
      }
      return Promise.all(tasks)
    })

    Promise.all([Promise.all(singleImageTasks), Promise.all(timelineTasks)]).then(([singleResults, timelineResults]) => {
      // 更新单张图片
      if (singleResults[0]) productInfo.imageUrl = singleResults[0]
      if (singleResults[1] && enterpriseInfo) enterpriseInfo.logo = singleResults[1]

      // 更新时间轴节点图片
      const finalTimeline = timelineList.map((node, idx) => {
        const [localImages, localCert] = timelineResults[idx]
        return {
          ...node,
          images: localImages.length > 0 ? localImages : node.images,
          certificateUrl: localCert || node.certificateUrl
        }
      })

      this.setData({
        loading: false,
        productInfo: productInfo,
        timelineList: finalTimeline,
        enterpriseInfo: enterpriseInfo
      })
    }).catch(() => {
      this.setData({
        loading: false,
        productInfo: productInfo,
        timelineList: timelineList,
        enterpriseInfo: enterpriseInfo
      })
    })
  }
})
