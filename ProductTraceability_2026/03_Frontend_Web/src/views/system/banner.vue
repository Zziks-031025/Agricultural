<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="queryParams" size="small">
        <el-form-item v-if="isAdmin" label="所属企业">
          <el-select v-model="queryParams.enterpriseId" placeholder="全部企业" clearable style="width: 200px">
            <el-option label="平台通用(游客)" :value="-1" />
            <el-option v-for="e in enterpriseOptions" :key="e.id" :label="e.enterpriseName" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 100px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="queryParams.keyword" placeholder="标题/描述" clearable style="width: 200px" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleQuery">查询</el-button>
          <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" class="table-card">
      <div slot="header" class="card-header">
        <span>{{ isAdmin ? '轮播图管理（所有企业）' : '轮播图管理' }}</span>
        <el-button type="primary" size="small" icon="el-icon-plus" @click="handleAdd">新增Banner</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border size="small" style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column label="图片" width="160" align="center">
          <template slot-scope="{ row }">
            <el-image
              :src="resolveUrl(row.imageUrl)"
              style="width: 130px; height: 70px; border-radius: 4px"
              fit="cover"
              :preview-src-list="[resolveUrl(row.imageUrl)]"
            >
              <div slot="error" class="image-error">
                <i class="el-icon-picture-outline"></i>
              </div>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="140" show-overflow-tooltip />
        <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
        <el-table-column v-if="isAdmin" label="所属企业" width="150" align="center">
          <template slot-scope="{ row }">
            <span v-if="!row.enterpriseId" class="platform-tag">平台通用</span>
            <span v-else>{{ getEnterpriseName(row.enterpriseId) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="70" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template slot-scope="{ row }">
            <el-switch :value="row.status === 1" active-color="#2d8a56" @change="handleToggleStatus(row)" />
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="155" />
        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" icon="el-icon-edit" @click="handleEdit(row)">编辑</el-button>
            <el-button type="text" size="mini" icon="el-icon-delete" class="danger-btn" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pagination" background layout="total, prev, pager, next"
        :total="total" :page-size="queryParams.size" :current-page.sync="queryParams.current"
        @current-change="fetchList" />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="600px" @close="resetForm">
      <el-form ref="bannerForm" :model="form" :rules="rules" label-width="90px" size="small">
        <el-form-item v-if="isAdmin" label="所属企业" prop="enterpriseId">
          <el-select v-model="form.enterpriseId" placeholder="请选择企业" style="width: 100%" @change="handleFormEnterpriseChange">
            <el-option label="平台通用(游客首页)" :value="null" />
            <el-option v-for="e in enterpriseOptions" :key="e.id" :label="e.enterpriseName" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="轮播图标题" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" placeholder="轮播图描述文案(可选)" maxlength="255" show-word-limit />
        </el-form-item>
        <el-form-item label="图片" prop="imageUrl">
          <div class="banner-upload-area">
            <el-upload
              action="#"
              :auto-upload="false"
              :show-file-list="false"
              :on-change="handleImageChange"
              accept="image/*"
            >
              <div v-if="form.imageUrl || imagePreview" class="banner-preview">
                <img :src="imagePreview || resolveUrl(form.imageUrl)" />
                <div class="banner-preview-mask">
                  <i class="el-icon-edit"></i>
                  <span>更换图片</span>
                </div>
              </div>
              <div v-else class="banner-upload-placeholder">
                <i class="el-icon-plus"></i>
                <span>上传Banner图片</span>
                <span class="upload-hint">建议尺寸 750×400</span>
              </div>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="跳转链接">
          <el-input v-model="form.linkUrl" placeholder="点击跳转URL(可选)" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" />
          <span class="form-hint">数值越小越靠前</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.statusBool" active-text="启用" inactive-text="禁用" active-color="#2d8a56" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button size="small" @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" size="small" :loading="submitting" @click="handleSubmit">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getBannerList, addBanner, updateBanner, deleteBanner, toggleBannerStatus } from '@/api/system'
import { getEnterpriseList } from '@/api/enterprise'
import request from '@/utils/request'
import { mapState } from 'vuex'

export default {
  name: 'SystemBanner',
  data() {
    return {
      loading: false,
      submitting: false,
      tableData: [],
      total: 0,
      enterpriseOptions: [],
      queryParams: { current: 1, size: 20, enterpriseId: null, status: null, keyword: '' },
      dialogVisible: false,
      dialogTitle: '',
      form: {
        id: null,
        enterpriseId: null,
        title: '',
        description: '',
        imageUrl: '',
        linkUrl: '',
        sortOrder: 1,
        statusBool: true
      },
      pendingFile: null,
      imagePreview: '',
      rules: {
        title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
        imageUrl: [{ required: true, message: '请上传图片', trigger: 'change' }]
      }
    }
  },
  computed: {
    ...mapState('user', ['userInfo']),
    isAdmin() {
      return this.userInfo && this.userInfo.userType === 1
    },
    currentEnterpriseId() {
      return this.userInfo ? this.userInfo.enterpriseId : null
    }
  },
  created() {
    if (this.isAdmin) {
      this.loadEnterpriseOptions()
    }
    this.fetchList()
  },
  methods: {
    loadEnterpriseOptions() {
      getEnterpriseList({ current: 1, size: 200 }).then(res => {
        const data = res.data
        this.enterpriseOptions = data.records || data || []
      }).catch(() => { this.enterpriseOptions = [] })
    },

    fetchList() {
      this.loading = true
      const params = { ...this.queryParams }
      delete params.platformOnly

      // 企业用户强制只查自己的Banner
      if (!this.isAdmin && this.currentEnterpriseId) {
        params.enterpriseId = this.currentEnterpriseId
      }
      // 管理员：-1代表查平台通用(enterpriseId=null的)，null代表查全部
      if (this.isAdmin && params.enterpriseId === -1) {
        // 后端暂不支持查null的enterpriseId，先不传，全部查
        params.platformOnly = true
        delete params.enterpriseId
      }
      if (params.enterpriseId === null || params.enterpriseId === '') delete params.enterpriseId
      if (params.platformOnly !== true) delete params.platformOnly
      if (params.status === null || params.status === '') delete params.status
      if (!params.keyword) delete params.keyword

      getBannerList(params).then(res => {
        const data = res.data
        if (data.records) { this.tableData = data.records; this.total = data.total || 0 }
        else if (Array.isArray(data)) { this.tableData = data; this.total = data.length }
      }).catch(() => { this.tableData = [] }).finally(() => { this.loading = false })
    },

    handleQuery() { this.queryParams.current = 1; this.fetchList() },

    resetQuery() {
      this.queryParams = { current: 1, size: 20, enterpriseId: null, status: null, keyword: '' }
      this.fetchList()
    },

    handleAdd() {
      const defaultEnterpriseId = this.getDefaultScopeEnterpriseId()
      this.dialogTitle = '新增Banner'
      this.form = {
        id: null,
        enterpriseId: defaultEnterpriseId,
        title: '', description: '', imageUrl: '', linkUrl: '',
        sortOrder: this.getNextSortOrderForScope(defaultEnterpriseId), statusBool: true
      }
      this.pendingFile = null
      this.imagePreview = ''
      this.dialogVisible = true
    },

    handleEdit(row) {
      this.dialogTitle = '编辑Banner'
      this.form = {
        id: row.id,
        enterpriseId: row.enterpriseId || null,
        title: row.title || '',
        description: row.description || '',
        imageUrl: row.imageUrl || '',
        linkUrl: row.linkUrl || '',
        sortOrder: row.sortOrder || 0,
        statusBool: row.status === 1
      }
      this.pendingFile = null
      this.imagePreview = ''
      this.dialogVisible = true
    },

    handleImageChange(file) {
      this.pendingFile = file.raw
      this.imagePreview = URL.createObjectURL(file.raw)
      this.form.imageUrl = '__pending__'
    },

    async handleSubmit() {
      this.$refs.bannerForm.validate(async valid => {
        if (!valid) return
        this.submitting = true

        try {
          if (this.pendingFile) {
            const formData = new FormData()
            formData.append('file', this.pendingFile)
            formData.append('type', 'banner')
            const uploadRes = await request({
              url: '/api/file/upload',
              method: 'post',
              data: formData,
              headers: { 'Content-Type': 'multipart/form-data' }
            })
            if (uploadRes.data && uploadRes.data.url) {
              this.form.imageUrl = uploadRes.data.url
            } else {
              this.$message.error('图片上传失败')
              this.submitting = false
              return
            }
          }

          const submitData = {
            id: this.form.id,
            enterpriseId: this.isAdmin ? this.form.enterpriseId : this.currentEnterpriseId,
            title: this.form.title,
            description: this.form.description,
            imageUrl: this.form.imageUrl,
            linkUrl: this.form.linkUrl,
            sortOrder: this.form.sortOrder,
            status: this.form.statusBool ? 1 : 0
          }

          const api = this.form.id ? updateBanner : addBanner
          await api(submitData)
          this.$message.success(this.form.id ? '更新成功' : '新增成功')
          this.dialogVisible = false
          this.fetchList()
        } catch (e) {
          // error handled by interceptor
        } finally {
          this.submitting = false
        }
      })
    },

    handleDelete(row) {
      this.$confirm(`确定删除Banner「${row.title}」吗？`, '提示', { type: 'warning' }).then(() => {
        deleteBanner(row.id).then(() => {
          this.$message.success('删除成功')
          this.fetchList()
        })
      }).catch(() => {})
    },

    handleToggleStatus(row) {
      toggleBannerStatus(row.id).then(() => {
        this.$message.success(row.status === 1 ? '已禁用' : '已启用')
        this.fetchList()
      })
    },

    handleFormEnterpriseChange(value) {
      if (!this.form.id) {
        this.form.sortOrder = this.getNextSortOrderForScope(value)
      }
    },

    getDefaultScopeEnterpriseId() {
      if (!this.isAdmin) {
        return this.currentEnterpriseId
      }
      if (this.queryParams.enterpriseId === -1) {
        return null
      }
      if (this.queryParams.enterpriseId !== null && this.queryParams.enterpriseId !== '') {
        return this.queryParams.enterpriseId
      }
      return null
    },

    getNextSortOrderForScope(scopeEnterpriseId) {
      const normalizedScope = this.normalizeEnterpriseId(scopeEnterpriseId)
      const scopedBanners = this.tableData.filter(row => this.normalizeEnterpriseId(row.enterpriseId) === normalizedScope)
      const maxSortOrder = scopedBanners.reduce((max, row) => Math.max(max, Number(row.sortOrder) || 0), 0)
      return maxSortOrder + 1
    },

    normalizeEnterpriseId(value) {
      return value === null || value === undefined || value === '' ? null : value
    },

    resetForm() {
      this.pendingFile = null
      this.imagePreview = ''
      this.$refs.bannerForm && this.$refs.bannerForm.resetFields()
    },

    resolveUrl(path) {
      if (!path || path === '__pending__') return ''
      if (path.startsWith('http://tmp')) return ''
      if (path.startsWith('http')) return path
      return path
    },

    getEnterpriseName(enterpriseId) {
      const e = this.enterpriseOptions.find(item => item.id === enterpriseId)
      return e ? e.enterpriseName : ('企业#' + enterpriseId)
    }
  }
}
</script>

<style lang="scss" scoped>
.page-container { padding: 4px; }
.filter-card { border-radius: 8px; margin-bottom: 16px; }
.table-card {
  border-radius: 8px;
  .card-header {
    display: flex; align-items: center; justify-content: space-between;
    font-weight: 600; color: #1a3a2a;
  }
}
.danger-btn { color: #f56c6c !important; }
.pagination { margin-top: 16px; text-align: right; }

.image-error {
  display: flex; align-items: center; justify-content: center;
  width: 130px; height: 70px; background: #f5f7fa;
  i { font-size: 24px; color: #c0c4cc; }
}

.banner-upload-area {
  ::v-deep .el-upload {
    border: 1px dashed #d9d9d9;
    border-radius: 6px;
    cursor: pointer;
    transition: border-color 0.3s;
    &:hover { border-color: #2d8a56; }
  }
}

.banner-preview {
  position: relative;
  width: 375px;
  height: 200px;
  img {
    width: 100%; height: 100%;
    object-fit: cover;
    border-radius: 6px;
  }
}

.banner-preview-mask {
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 100%;
  background: rgba(0, 0, 0, 0.4);
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
  border-radius: 6px;
  i { font-size: 28px; color: #fff; }
  span { font-size: 13px; color: #fff; margin-top: 4px; }
  &:hover { opacity: 1; }
}

.banner-upload-placeholder {
  width: 375px;
  height: 200px;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  background: #fafafa;
  border-radius: 6px;
  i { font-size: 32px; color: #c0c4cc; }
  span { font-size: 13px; color: #909399; margin-top: 4px; }
  .upload-hint { font-size: 11px; color: #c0c4cc; margin-top: 2px; }
}

.form-hint {
  margin-left: 12px;
  font-size: 12px;
  color: #909399;
}
</style>
