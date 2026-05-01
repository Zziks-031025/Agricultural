<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="queryParams" size="small">
        <el-form-item label="参数类型">
          <el-select v-model="queryParams.configType" placeholder="全部" clearable style="width: 140px">
            <el-option label="区块链" value="blockchain" />
            <el-option label="文件上传" value="upload" />
            <el-option label="系统基础" value="system" />
            <el-option label="邮件配置" value="mail" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="queryParams.keyword" placeholder="参数键/描述" clearable style="width: 200px" @keyup.enter.native="handleQuery" />
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
        <span>系统参数配置</span>
        <el-button type="primary" size="small" icon="el-icon-plus" @click="handleAdd">新增配置</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border size="small" style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column prop="configKey" label="参数键" min-width="200" show-overflow-tooltip>
          <template slot-scope="{ row }">
            <span class="config-key">{{ row.configKey }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="configValue" label="参数值" min-width="240" show-overflow-tooltip>
          <template slot-scope="{ row }">
            <span class="config-value">{{ row.configValue }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="configType" label="参数类型" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag size="mini" :type="typeTagColor(row.configType)">{{ typeNameMap(row.configType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
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
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="540px" @close="resetForm">
      <el-form ref="configForm" :model="form" :rules="rules" label-width="90px" size="small">
        <el-form-item label="参数键" prop="configKey">
          <el-input v-model="form.configKey" placeholder="如: blockchain.rpc.url" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="参数值" prop="configValue">
          <el-input v-model="form.configValue" type="textarea" :rows="3" placeholder="参数值" />
        </el-form-item>
        <el-form-item label="参数类型">
          <el-select v-model="form.configType" placeholder="请选择" style="width: 100%" clearable>
            <el-option label="区块链" value="blockchain" />
            <el-option label="文件上传" value="upload" />
            <el-option label="系统基础" value="system" />
            <el-option label="邮件配置" value="mail" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="参数用途描述" />
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
import { getConfigList, addConfig, updateConfig, deleteConfig } from '@/api/system'

export default {
  name: 'SystemConfig',
  data() {
    return {
      loading: false,
      submitting: false,
      tableData: [],
      total: 0,
      queryParams: { current: 1, size: 20, configType: '', keyword: '' },
      dialogVisible: false,
      dialogTitle: '',
      form: { id: null, configKey: '', configValue: '', configType: '', description: '' },
      rules: {
        configKey: [{ required: true, message: '请输入参数键', trigger: 'blur' }],
        configValue: [{ required: true, message: '请输入参数值', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    fetchList() {
      this.loading = true
      const params = { ...this.queryParams }
      if (!params.configType) delete params.configType
      if (!params.keyword) delete params.keyword

      getConfigList(params).then(res => {
        const data = res.data
        if (data.records) { this.tableData = data.records; this.total = data.total || 0 }
        else if (Array.isArray(data)) { this.tableData = data; this.total = data.length }
      }).catch(() => { this.tableData = [] }).finally(() => { this.loading = false })
    },
    handleQuery() { this.queryParams.current = 1; this.fetchList() },
    resetQuery() { this.queryParams = { current: 1, size: 20, configType: '', keyword: '' }; this.fetchList() },
    handleAdd() {
      this.dialogTitle = '新增配置'
      this.form = { id: null, configKey: '', configValue: '', configType: '', description: '' }
      this.dialogVisible = true
    },
    handleEdit(row) {
      this.dialogTitle = '编辑配置'
      this.form = { id: row.id, configKey: row.configKey, configValue: row.configValue || '', configType: row.configType || '', description: row.description || '' }
      this.dialogVisible = true
    },
    handleSubmit() {
      this.$refs.configForm.validate(valid => {
        if (!valid) return
        this.submitting = true
        const api = this.form.id ? updateConfig : addConfig
        api(this.form).then(() => {
          this.$message.success(this.form.id ? '更新成功' : '新增成功')
          this.dialogVisible = false
          this.fetchList()
        }).finally(() => { this.submitting = false })
      })
    },
    handleDelete(row) {
      this.$confirm(`确定删除配置「${row.configKey}」吗？`, '提示', { type: 'warning' }).then(() => {
        deleteConfig(row.id).then(() => {
          this.$message.success('删除成功')
          this.fetchList()
        })
      }).catch(() => {})
    },
    resetForm() { this.$refs.configForm && this.$refs.configForm.resetFields() },
    typeTagColor(type) { return { blockchain: '', upload: 'success', system: 'warning', mail: 'info' }[type] || 'info' },
    typeNameMap(type) { return { blockchain: '区块链', upload: '文件上传', system: '系统基础', mail: '邮件配置' }[type] || type || '--' }
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
.config-key { font-family: monospace; font-size: 12px; color: #409eff; }
.config-value { font-family: monospace; font-size: 12px; color: #606266; }
.danger-btn { color: #f56c6c !important; }
.pagination { margin-top: 16px; text-align: right; }
</style>
