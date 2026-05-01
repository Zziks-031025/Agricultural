<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="queryParams" size="small">
        <el-form-item v-if="isAdmin" label="所属企业">
          <el-select v-model="queryParams.enterpriseId" placeholder="全部企业" clearable style="width: 200px">
            <el-option label="平台通用" :value="-1" />
            <el-option v-for="e in enterpriseOptions" :key="e.id" :label="e.enterpriseName" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="queryParams.category" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="c in categoryOptions" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 100px">
            <el-option label="已发布" :value="1" />
            <el-option label="草稿" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="queryParams.keyword" placeholder="标题/摘要" clearable style="width: 200px" @keyup.enter.native="handleQuery" />
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
        <span>{{ isAdmin ? '科普文章管理（所有企业）' : '科普文章管理' }}</span>
        <el-button type="primary" size="small" icon="el-icon-plus" @click="handleAdd">新增文章</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border size="small" style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column label="封面" width="100" align="center">
          <template slot-scope="{ row }">
            <el-image v-if="row.coverUrl" :src="resolveUrl(row.coverUrl)" style="width: 70px; height: 50px; border-radius: 4px" fit="cover">
              <div slot="error" class="image-error"><i class="el-icon-picture-outline"></i></div>
            </el-image>
            <span v-else class="no-cover">无封面</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column label="分类" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag size="mini" :type="categoryTagType(row.category)">{{ categoryName(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="isAdmin" label="所属企业" width="150" align="center">
          <template slot-scope="{ row }">
            <span v-if="!row.enterpriseId" class="platform-tag">平台通用</span>
            <span v-else>{{ getEnterpriseName(row.enterpriseId) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="author" label="作者" width="90" align="center" />
        <el-table-column prop="viewCount" label="阅读量" width="80" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template slot-scope="{ row }">
            <el-switch :value="row.status === 1" active-color="#2d8a56" @change="handleToggleStatus(row)" />
          </template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" width="155" />
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
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="800px" top="5vh" @close="resetForm">
      <el-form ref="articleForm" :model="form" :rules="rules" label-width="90px" size="small">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item v-if="isAdmin" label="所属企业">
              <el-select v-model="form.enterpriseId" placeholder="请选择企业" style="width: 100%">
                <el-option label="平台通用" :value="null" />
                <el-option v-for="e in enterpriseOptions" :key="e.id" :label="e.enterpriseName" :value="e.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类" prop="category">
              <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%">
                <el-option v-for="c in categoryOptions" :key="c.value" :label="c.label" :value="c.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="文章标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="作者">
          <el-input v-model="form.author" placeholder="作者名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="form.summary" type="textarea" :rows="2" placeholder="文章摘要(可选)" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="封面图">
          <div class="cover-upload-area">
            <el-upload action="#" :auto-upload="false" :show-file-list="false" :on-change="handleCoverChange" accept="image/*">
              <div v-if="form.coverUrl || coverPreview" class="cover-preview">
                <img :src="coverPreview || resolveUrl(form.coverUrl)" />
                <div class="cover-mask"><i class="el-icon-edit"></i></div>
              </div>
              <div v-else class="cover-placeholder">
                <i class="el-icon-plus"></i>
                <span>上传封面</span>
              </div>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="正文内容" prop="contentText">
          <el-input v-model="form.contentText" type="textarea" :rows="12" placeholder="请输入文章正文内容。支持分段，每段之间用空行分隔。以##开头的行会作为小标题显示。" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="排序">
              <el-input-number v-model="form.sortOrder" :min="0" :max="9999" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="草稿" :value="0" />
                <el-option label="发布" :value="1" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer">
        <el-button size="small" @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" size="small" :loading="submitting" @click="handleSubmit">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getArticleList, addArticle, updateArticle, deleteArticle, toggleArticleStatus } from '@/api/system'
import { getEnterpriseList } from '@/api/enterprise'
import request from '@/utils/request'
import { mapState } from 'vuex'

const CATEGORY_OPTIONS = [
  { label: '养殖技术', value: 'farming' },
  { label: '防疫知识', value: 'vaccine' },
  { label: '加工规范', value: 'process' },
  { label: '卫生标准', value: 'hygiene' },
  { label: '检疫规范', value: 'quarantine' },
  { label: '区块链科普', value: 'blockchain' },
  { label: '食品安全', value: 'safety' }
]

export default {
  name: 'EduArticle',
  data() {
    return {
      loading: false,
      submitting: false,
      tableData: [],
      total: 0,
      enterpriseOptions: [],
      categoryOptions: CATEGORY_OPTIONS,
      queryParams: { current: 1, size: 20, enterpriseId: null, category: '', status: null, keyword: '' },
      dialogVisible: false,
      dialogTitle: '',
      form: {
        id: null,
        enterpriseId: null,
        category: '',
        title: '',
        author: '',
        summary: '',
        coverUrl: '',
        contentText: '',
        sortOrder: 0,
        status: 0
      },
      pendingCover: null,
      coverPreview: '',
      rules: {
        title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
        category: [{ required: true, message: '请选择分类', trigger: 'change' }],
        contentText: [{ required: true, message: '请输入正文内容', trigger: 'blur' }]
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

      if (!this.isAdmin && this.currentEnterpriseId) {
        params.enterpriseId = this.currentEnterpriseId
      }
      if (this.isAdmin && params.enterpriseId === -1) {
        delete params.enterpriseId
      }
      if (params.enterpriseId === null || params.enterpriseId === '') delete params.enterpriseId
      if (!params.category) delete params.category
      if (params.status === null || params.status === '') delete params.status
      if (!params.keyword) delete params.keyword

      getArticleList(params).then(res => {
        const data = res.data
        if (data.records) { this.tableData = data.records; this.total = data.total || 0 }
        else if (Array.isArray(data)) { this.tableData = data; this.total = data.length }
      }).catch(() => { this.tableData = [] }).finally(() => { this.loading = false })
    },

    handleQuery() { this.queryParams.current = 1; this.fetchList() },

    resetQuery() {
      this.queryParams = { current: 1, size: 20, enterpriseId: null, category: '', status: null, keyword: '' }
      this.fetchList()
    },

    handleAdd() {
      this.dialogTitle = '新增文章'
      this.form = {
        id: null,
        enterpriseId: this.isAdmin ? null : this.currentEnterpriseId,
        category: '', title: '', author: '', summary: '', coverUrl: '',
        contentText: '', sortOrder: 0, status: 0
      }
      this.pendingCover = null
      this.coverPreview = ''
      this.dialogVisible = true
    },

    handleEdit(row) {
      this.dialogTitle = '编辑文章'
      this.form = {
        id: row.id,
        enterpriseId: row.enterpriseId || null,
        category: row.category || '',
        title: row.title || '',
        author: row.author || '',
        summary: row.summary || '',
        coverUrl: row.coverUrl || '',
        contentText: this.contentJsonToText(row.content),
        sortOrder: row.sortOrder || 0,
        status: row.status != null ? row.status : 0
      }
      this.pendingCover = null
      this.coverPreview = ''
      this.dialogVisible = true
    },

    handleCoverChange(file) {
      this.pendingCover = file.raw
      this.coverPreview = URL.createObjectURL(file.raw)
      this.form.coverUrl = '__pending__'
    },

    async handleSubmit() {
      this.$refs.articleForm.validate(async valid => {
        if (!valid) return
        this.submitting = true

        try {
          if (this.pendingCover) {
            const formData = new FormData()
            formData.append('file', this.pendingCover)
            formData.append('type', 'article')
            const uploadRes = await request({
              url: '/api/file/upload',
              method: 'post',
              data: formData,
              headers: { 'Content-Type': 'multipart/form-data' }
            })
            if (uploadRes.data && uploadRes.data.url) {
              this.form.coverUrl = uploadRes.data.url
            } else {
              this.$message.error('封面上传失败')
              this.submitting = false
              return
            }
          }

          const contentJson = this.textToContentJson(this.form.contentText)

          const submitData = {
            id: this.form.id,
            enterpriseId: this.isAdmin ? this.form.enterpriseId : this.currentEnterpriseId,
            category: this.form.category,
            title: this.form.title,
            author: this.form.author,
            summary: this.form.summary,
            coverUrl: this.form.coverUrl === '__pending__' ? '' : this.form.coverUrl,
            content: contentJson,
            sortOrder: this.form.sortOrder,
            status: this.form.status
          }

          const api = this.form.id ? updateArticle : addArticle
          await api(submitData)
          this.$message.success(this.form.id ? '更新成功' : '新增成功')
          this.dialogVisible = false
          this.fetchList()
        } catch (e) {
          // handled by interceptor
        } finally {
          this.submitting = false
        }
      })
    },

    handleDelete(row) {
      this.$confirm(`确定删除文章「${row.title}」吗？`, '提示', { type: 'warning' }).then(() => {
        deleteArticle(row.id).then(() => {
          this.$message.success('删除成功')
          this.fetchList()
        })
      }).catch(() => {})
    },

    handleToggleStatus(row) {
      toggleArticleStatus(row.id).then(() => {
        this.$message.success(row.status === 1 ? '已转为草稿' : '已发布')
        this.fetchList()
      })
    },

    resetForm() {
      this.pendingCover = null
      this.coverPreview = ''
      this.$refs.articleForm && this.$refs.articleForm.resetFields()
    },

    /** 将JSON content转为纯文本编辑格式 */
    contentJsonToText(contentStr) {
      if (!contentStr) return ''
      try {
        const arr = JSON.parse(contentStr)
        if (!Array.isArray(arr)) return contentStr
        return arr.map(item => {
          if (item.type === 'subtitle') return '## ' + item.value
          return item.value || ''
        }).join('\n\n')
      } catch (e) {
        return contentStr
      }
    },

    /** 将纯文本转为JSON content格式 */
    textToContentJson(text) {
      if (!text) return '[]'
      const paragraphs = text.split(/\n\s*\n/).filter(p => p.trim())
      const arr = paragraphs.map(p => {
        const trimmed = p.trim()
        if (trimmed.startsWith('## ')) {
          return { type: 'subtitle', value: trimmed.substring(3).trim() }
        }
        return { type: 'text', value: trimmed }
      })
      return JSON.stringify(arr)
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
    },

    categoryName(cat) {
      const found = CATEGORY_OPTIONS.find(c => c.value === cat)
      return found ? found.label : cat
    },

    categoryTagType(cat) {
      const map = { farming: 'success', vaccine: 'success', process: 'warning', hygiene: 'warning', quarantine: '', blockchain: 'info', safety: 'danger' }
      return map[cat] || 'info'
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
.platform-tag { color: #909399; font-size: 12px; }
.no-cover { color: #c0c4cc; font-size: 12px; }

.image-error {
  display: flex; align-items: center; justify-content: center;
  width: 70px; height: 50px; background: #f5f7fa;
  i { font-size: 18px; color: #c0c4cc; }
}

.cover-upload-area {
  ::v-deep .el-upload {
    border: 1px dashed #d9d9d9;
    border-radius: 6px;
    cursor: pointer;
    transition: border-color 0.3s;
    &:hover { border-color: #2d8a56; }
  }
}

.cover-preview {
  position: relative;
  width: 200px; height: 120px;
  img { width: 100%; height: 100%; object-fit: cover; border-radius: 6px; }
}

.cover-mask {
  position: absolute; top: 0; left: 0;
  width: 100%; height: 100%;
  background: rgba(0,0,0,0.4);
  display: flex; align-items: center; justify-content: center;
  opacity: 0; transition: opacity 0.3s; border-radius: 6px;
  i { font-size: 24px; color: #fff; }
  &:hover { opacity: 1; }
}

.cover-placeholder {
  width: 200px; height: 120px;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  background: #fafafa; border-radius: 6px;
  i { font-size: 28px; color: #c0c4cc; }
  span { font-size: 12px; color: #909399; margin-top: 4px; }
}
</style>
