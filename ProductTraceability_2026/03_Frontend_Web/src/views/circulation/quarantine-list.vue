<template>
  <div class="page-container">
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input v-model="queryParams.batchCode" placeholder="搜索批次号" prefix-icon="el-icon-search"
            size="small" clearable style="width: 220px" @keyup.enter.native="handleSearch" />
          <el-select v-model="queryParams.status" placeholder="申报状态" clearable size="small" style="width: 140px" @change="handleSearch">
            <el-option label="待检疫" value="pending" />
            <el-option label="已完成" value="completed" />
          </el-select>
          <el-button type="primary" size="small" icon="el-icon-search" @click="handleSearch">搜索</el-button>
          <el-button size="small" icon="el-icon-refresh" @click="resetQuery">重置</el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="table-card">
      <div slot="header" class="card-header">
        <span>检疫申报列表</span>
      </div>

      <el-table v-loading="loading" :data="tableData" border size="small" style="width: 100%" :header-cell-style="{ whiteSpace: 'nowrap' }">
        <el-table-column label="状态" width="70" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="statusTagType(row.checkResult)" size="mini">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="batchCode" label="批次号" min-width="160" show-overflow-tooltip />
        <el-table-column prop="enterpriseName" label="申报企业" min-width="120" show-overflow-tooltip />
        <el-table-column prop="productName" label="产品名称" min-width="100" show-overflow-tooltip />
        <el-table-column label="数量" width="90">
          <template slot-scope="{ row }">{{ row.quantity || '--' }} {{ row.unit || '' }}</template>
        </el-table-column>
        <el-table-column prop="applyTime" label="申报时间" width="150" show-overflow-tooltip />
        <el-table-column prop="inspector" label="检疫员" width="90">
          <template slot-scope="{ row }">{{ row.inspector || '--' }}</template>
        </el-table-column>
        <el-table-column prop="certNo" label="证书编号" min-width="120" show-overflow-tooltip>
          <template slot-scope="{ row }">{{ row.certNo || '--' }}</template>
        </el-table-column>
        <el-table-column label="上链" width="50" align="center">
          <template slot-scope="{ row }">
            <el-tooltip v-if="row.txHash" :content="row.txHash" placement="top">
              <i class="el-icon-link" style="color: #2d8a56; cursor: pointer" @click="copyHash(row.txHash)"></i>
            </el-tooltip>
            <span v-else class="no-data">--</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="200" align="center" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" icon="el-icon-view" @click="handleDetail(row)">详情</el-button>
            <el-button v-if="row.checkResult === null" type="text" size="mini" icon="el-icon-edit"
              @click="handleInspection(row)">检疫录入</el-button>
            <el-button v-if="row.checkResult !== null && !row.certNo" type="text" size="mini" icon="el-icon-upload"
              @click="handleUploadReport(row)">上传报告</el-button>
            <el-button type="text" size="mini" icon="el-icon-delete"
              style="color: #F56C6C" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="tableData.length === 0 && !loading" class="empty-text">暂无检疫申报记录</div>

      <el-pagination class="pagination" background layout="total, prev, pager, next"
        :total="total" :page-size="queryParams.size" :current-page.sync="queryParams.current"
        @current-change="fetchList" />
    </el-card>

    <el-dialog title="申报详情" :visible.sync="detailVisible" width="700px">
      <el-descriptions v-if="currentRow" :column="2" size="small" border>
        <el-descriptions-item label="批次号">{{ currentRow.batchCode }}</el-descriptions-item>
        <el-descriptions-item label="申报企业">{{ currentRow.enterpriseName }}</el-descriptions-item>
        <el-descriptions-item label="产品名称">{{ currentRow.productName }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ currentRow.quantity }} {{ currentRow.unit }}</el-descriptions-item>
        <el-descriptions-item label="申报时间">{{ currentRow.applyTime }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ currentRow.statusText }}</el-descriptions-item>
        <el-descriptions-item label="检疫员">{{ currentRow.inspector || '--' }}</el-descriptions-item>
        <el-descriptions-item label="证书编号">{{ currentRow.certNo || '--' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentRow.remark || '--' }}</el-descriptions-item>
      </el-descriptions>
      
      <div v-if="currentRow" style="margin-top: 20px;">
        <div style="font-weight: 500; margin-bottom: 10px; color: #606266;">检疫证书</div>
        <div v-if="currentRow.images && parseImages(currentRow.images).length > 0" style="display: flex; flex-wrap: wrap; gap: 10px;">
          <el-image
            v-for="(img, index) in parseImages(currentRow.images)"
            :key="index"
            :src="getImageUrl(img)"
            :preview-src-list="parseImages(currentRow.images).map(i => getImageUrl(i))"
            fit="cover"
            style="width: 120px; height: 120px; border-radius: 4px; cursor: pointer; border: 1px solid #dcdfe6;"
          >
            <div slot="error" style="display: flex; justify-content: center; align-items: center; height: 100%; background: #f5f7fa; color: #909399;">
              <i class="el-icon-picture-outline" style="font-size: 24px;"></i>
            </div>
          </el-image>
        </div>
        <div v-else style="color: #909399; font-size: 14px;">暂无证书图片</div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'QuarantineList',
  data() {
    return {
      loading: false,
      tableData: [],
      total: 0,
      queryParams: {
        current: 1,
        size: 20,
        batchCode: '',
        status: ''
      },
      detailVisible: false,
      currentRow: null
    }
  },
  mounted() {
    this.fetchList()
  },
  methods: {
    fetchList() {
      this.loading = true
      const userInfo = this.$store.getters.userInfo || {}
      const inspectionEnterpriseId = userInfo.enterpriseId

      const params = {
        batchCode: this.queryParams.batchCode || undefined,
        status: this.queryParams.status || undefined
      }
      if (inspectionEnterpriseId) {
        params.inspectionEnterpriseId = inspectionEnterpriseId
      }

      request({ url: '/api/quarantine/apply/list-for-inspector', method: 'get', params }).then(res => {
        this.tableData = res.data || []
        this.total = this.tableData.length
        this.loading = false
      }).catch(err => {
        this.$message.error(err.message || '加载失败')
        this.loading = false
      })
    },
    handleSearch() {
      this.queryParams.current = 1
      this.fetchList()
    },
    resetQuery() {
      this.queryParams = {
        current: 1,
        size: 20,
        batchCode: '',
        status: ''
      }
      this.fetchList()
    },
    statusTagType(checkResult) {
      if (checkResult === null || checkResult === undefined) return 'warning'
      return checkResult === 1 ? 'success' : 'danger'
    },
    handleDetail(row) {
      this.currentRow = row
      this.detailVisible = true
    },
    handleInspection(row) {
      this.$router.push({
        path: '/circulation/inspection',
        query: { batchCode: row.batchCode }
      })
    },
    handleUploadReport(row) {
      this.$router.push({
        path: '/circulation/inspection',
        query: { batchCode: row.batchCode, mode: 'upload' }
      })
    },
    handleDelete(row) {
      this.$confirm('确认删除该检疫申报记录？删除后批次状态将恢复。', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        request({ url: `/api/quarantine/apply/delete/${row.id}`, method: 'delete' }).then(() => {
          this.$message.success('删除成功')
          this.fetchList()
        }).catch(err => {
          this.$message.error(err.message || '删除失败')
        })
      }).catch(() => {})
    },
    copyHash(hash) {
      const input = document.createElement('input')
      input.value = hash
      document.body.appendChild(input)
      input.select()
      document.execCommand('copy')
      document.body.removeChild(input)
      this.$message.success('交易哈希已复制')
    },
    parseImages(images) {
      if (!images) return []
      if (typeof images === 'string') {
        try {
          const parsed = JSON.parse(images)
          return Array.isArray(parsed) ? parsed : images.split(',').filter(i => i)
        } catch {
          return images.split(',').filter(i => i)
        }
      }
      return Array.isArray(images) ? images : []
    },
    getImageUrl(img) {
      if (!img) return ''
      if (img.startsWith('http://') || img.startsWith('https://')) return img
      // Use relative path so devServer proxy handles /uploads correctly
      return img.startsWith('/') ? img : '/' + img
    }
  }
}
</script>

<style scoped>
.page-container {
  padding: 20px;
}
.toolbar-card {
  margin-bottom: 16px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.toolbar-left {
  display: flex;
  gap: 12px;
  align-items: center;
}
.table-card {
  min-height: 500px;
  overflow-x: auto;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.empty-text {
  text-align: center;
  padding: 40px 0;
  color: #909399;
}
.pagination {
  margin-top: 16px;
  text-align: right;
}
.no-data {
  color: #c0c4cc;
}
</style>
