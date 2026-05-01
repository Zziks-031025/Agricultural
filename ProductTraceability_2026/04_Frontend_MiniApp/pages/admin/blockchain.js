// 区块链管理页面 - 毕设技术亮点展示
const app = getApp()
const request = require('../../utils/request.js')

Page({
  data: {
    refreshing: false,
    
    // 节点状态
    nodeStatus: {
      connected: true,
      networkName: 'Ganache Local',
      rpcUrl: 'http://127.0.0.1:7545',
      blockNumber: 0,
      avgBlockTime: 0,
      lastSyncTime: ''
    },
    
    // 数据统计
    statistics: {
      totalTransactions: 0,
      todayHashes: 0,
      totalBatches: 0,
      activeEnterprises: 0
    },
    
    // Gas 费用信息
    gasInfo: {
      totalGasUsed: '0',
      estimatedEth: '0.0000',
      currentGasPrice: '20',
      avgGasPerTx: '0',
      gasLimit: '300000'
    },
    
    // 智能合约信息
    contractInfo: {
      name: 'Traceability',
      version: '^0.8.0',
      address: '0x0000000000000000000000000000000000000000',
      deployTime: '-',
      deployBlock: '0'
    }
  },

  onLoad() {
    this.loadBlockchainData()
  },

  onShow() {
    // 每次显示时刷新数据
    this.loadBlockchainData()
  },

  // 加载区块链数据
  loadBlockchainData() {
    this.loadNodeStatus()
    this.loadStatistics()
    this.loadGasInfo()
    this.loadContractInfo()
  },

  // 刷新节点状态
  refreshNodeStatus() {
    if (this.data.refreshing) return
    
    this.setData({ refreshing: true })
    
    this.loadNodeStatus()
    this.loadStatistics()
    this.loadContractInfo()
    this.setData({ refreshing: false })
    wx.showToast({ title: '刷新成功', icon: 'success' })
  },

  // 加载节点状态
  loadNodeStatus() {
    request.get('/api/blockchain/contract/info').then(res => {
      if (res.code === 200 && res.data) {
        const d = res.data
        this.setData({
          nodeStatus: {
            connected: true,
            networkName: d.network || 'Ganache Local',
            rpcUrl: 'http://127.0.0.1:7545',
            blockNumber: d.blockNumber || 0,
            avgBlockTime: 0,
            lastSyncTime: this.formatTime(new Date())
          }
        })
      }
    }).catch(() => {
      this.setData({ 'nodeStatus.connected': false })
    })
  },

  // 加载统计数据
  loadStatistics() {
    request.get('/api/blockchain/stats').then(res => {
      if (res.code === 200 && res.data) {
        this.setData({
          statistics: {
            totalTransactions: res.data.totalCount || 0,
            todayHashes: res.data.todayCount || 0,
            totalBatches: res.data.totalBatches || 0,
            activeEnterprises: res.data.activeEnterprises || 0
          }
        })
      }
    }).catch(() => {})
  },

  // 加载 Gas 费用信息（从区块链统计数据推算）
  loadGasInfo() {
    const txCount = this.data.statistics.totalTransactions || 1
    const estimatedGasPerTx = 45000
    const totalGas = txCount * estimatedGasPerTx
    const gasPrice = 20
    const estimatedEth = (totalGas * gasPrice / 1e9).toFixed(4)
    
    this.setData({
      gasInfo: {
        totalGasUsed: this.formatNumber(totalGas),
        estimatedEth: estimatedEth,
        currentGasPrice: gasPrice.toString(),
        avgGasPerTx: this.formatNumber(estimatedGasPerTx),
        gasLimit: '300000'
      }
    })
  },

  // 加载合约信息
  loadContractInfo() {
    request.get('/api/blockchain/contract/info').then(res => {
      if (res.code === 200 && res.data) {
        const d = res.data
        this.setData({
          contractInfo: {
            name: d.contractName || 'Traceability',
            version: '^0.8.0',
            address: d.contractAddress || '0x0000000000000000000000000000000000000000',
            deployTime: d.deployTime || '-',
            deployBlock: d.blockNumber ? String(d.blockNumber) : '0'
          }
        })
      }
    }).catch(() => {})
  },

  // 复制合约地址
  copyContractAddress() {
    const address = this.data.contractInfo.address
    wx.setClipboardData({
      data: address,
      success: () => {
        wx.showToast({
          title: '已复制到剪贴板',
          icon: 'success'
        })
      }
    })
  },

  // 格式化时间
  formatTime(date) {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hour = String(date.getHours()).padStart(2, '0')
    const minute = String(date.getMinutes()).padStart(2, '0')
    const second = String(date.getSeconds()).padStart(2, '0')
    return `${year}-${month}-${day} ${hour}:${minute}:${second}`
  },

  // 格式化数字（添加千分位）
  formatNumber(num) {
    return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  },

  // 页面分享
  onShareAppMessage() {
    return {
      title: '区块链溯源系统 - 节点监控',
      path: '/pages/admin/blockchain'
    }
  }
})
