<template>
  <div class="page-container">
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="queryParams" size="small">
        <el-form-item label="用户类型">
          <el-select v-model="queryParams.userType" placeholder="全部" clearable style="width: 130px">
            <el-option label="平台管理员" :value="1" />
            <el-option label="企业用户" :value="2" />
            <el-option label="普通用户" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 100px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="queryParams.keyword"
            placeholder="用户名/姓名/手机号"
            clearable
            style="width: 200px"
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleQuery">查询</el-button>
          <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <div slot="header" class="card-header">
        <span>用户列表</span>
        <el-button type="primary" size="small" icon="el-icon-plus" @click="handleAdd">新增用户</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border size="small" style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column prop="username" label="用户名" min-width="110" />
        <el-table-column prop="realName" label="真实姓名" min-width="100" />
        <el-table-column label="用户类型" width="110" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="userTypeTag(row.userType)" size="mini">{{ userTypeLabel(row.userType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="120" />
        <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
        <el-table-column label="关联企业" min-width="180" show-overflow-tooltip>
          <template slot-scope="{ row }">{{ row.enterpriseName || '--' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template slot-scope="{ row }">
            <el-switch
              :value="row.status === 1"
              active-color="#2d8a56"
              inactive-color="#dcdfe6"
              @change="handleToggleStatus(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="最近登录" width="150">
          <template slot-scope="{ row }">{{ row.lastLoginTime || '--' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" icon="el-icon-edit" @click="handleEdit(row)">编辑</el-button>
            <el-button type="text" size="mini" icon="el-icon-key" @click="handleResetPwd(row)">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :page-sizes="[10, 20, 50]"
        :page-size.sync="queryParams.size"
        :current-page.sync="queryParams.current"
        @size-change="fetchList"
        @current-change="fetchList"
      />
    </el-card>

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="560px" @close="resetForm">
      <el-form ref="userForm" :model="form" :rules="rules" label-width="90px" size="small">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码，至少6位" show-password />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="用户类型" prop="userType">
          <el-select v-model="form.userType" placeholder="请选择" style="width: 100%" @change="onUserTypeChange">
            <el-option label="平台管理员" :value="1" />
            <el-option label="企业用户" :value="2" />
            <el-option label="普通用户" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.userType === 2" label="关联企业" prop="enterpriseId">
          <el-select
            v-model="form.enterpriseId"
            filterable
            clearable
            placeholder="请选择企业"
            style="width: 100%"
          >
            <el-option
              v-for="enterprise in enterpriseOptions"
              :key="enterprise.id"
              :label="enterprise.enterpriseName"
              :value="enterprise.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注信息" />
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
import { getUserList, addUser, updateUser, resetPassword, toggleUserStatus } from '@/api/system'
import { getEnterpriseList } from '@/api/enterprise'

const USER_TYPE_MAP = { 1: '平台管理员', 2: '企业用户', 3: '普通用户' }

function createEmptyForm() {
  return {
    id: null,
    username: '',
    realName: '',
    password: '',
    phone: '',
    email: '',
    userType: 2,
    enterpriseId: null,
    remark: ''
  }
}

export default {
  name: 'SystemUser',
  data() {
    return {
      loading: false,
      submitting: false,
      tableData: [],
      total: 0,
      enterpriseOptions: [],
      queryParams: { current: 1, size: 10, userType: null, status: null, keyword: '' },
      dialogVisible: false,
      dialogTitle: '',
      form: createEmptyForm(),
      rules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, message: '密码至少6位', trigger: 'blur' }
        ],
        userType: [{ required: true, message: '请选择用户类型', trigger: 'change' }],
        enterpriseId: [{
          validator: (rule, value, callback) => {
            if (this.form.userType === 2 && !value) {
              callback(new Error('请选择关联企业'))
              return
            }
            callback()
          },
          trigger: 'change'
        }]
      }
    }
  },
  created() {
    this.fetchList()
    this.fetchEnterpriseOptions()
  },
  methods: {
    fetchList() {
      this.loading = true
      const params = { ...this.queryParams }
      if (!params.keyword) delete params.keyword
      if (params.userType === null) delete params.userType
      if (params.status === null) delete params.status

      getUserList(params).then(res => {
        const data = res.data
        if (data.records) {
          this.tableData = data.records
          this.total = data.total || 0
        } else if (Array.isArray(data)) {
          this.tableData = data
          this.total = data.length
        }
      }).catch(() => {
        this.tableData = []
        this.total = 0
      }).finally(() => {
        this.loading = false
      })
    },
    fetchEnterpriseOptions() {
      getEnterpriseList({ current: 1, size: 500, status: 1 }).then(res => {
        const records = res.data.records || res.data || []
        this.enterpriseOptions = records
      }).catch(() => {
        this.enterpriseOptions = []
      })
    },
    handleQuery() {
      this.queryParams.current = 1
      this.fetchList()
    },
    resetQuery() {
      this.queryParams = { current: 1, size: 10, userType: null, status: null, keyword: '' }
      this.fetchList()
    },
    handleAdd() {
      this.dialogTitle = '新增用户'
      this.form = createEmptyForm()
      this.dialogVisible = true
    },
    handleEdit(row) {
      this.dialogTitle = '编辑用户'
      this.form = {
        id: row.id,
        username: row.username,
        realName: row.realName || '',
        password: '',
        phone: row.phone || '',
        email: row.email || '',
        userType: row.userType,
        enterpriseId: row.enterpriseId || null,
        remark: row.remark || ''
      }
      this.dialogVisible = true
    },
    handleSubmit() {
      this.$refs.userForm.validate(valid => {
        if (!valid) return
        this.submitting = true
        const api = this.form.id ? updateUser : addUser
        const payload = {
          ...this.form,
          enterpriseId: this.form.userType === 2 ? this.form.enterpriseId : null
        }
        api(payload).then(() => {
          this.$message.success(this.form.id ? '更新成功' : '新增成功')
          this.dialogVisible = false
          this.fetchList()
        }).finally(() => {
          this.submitting = false
        })
      })
    },
    handleResetPwd(row) {
      this.$confirm(`确定要重置用户「${row.username}」的密码吗？`, '重置密码', { type: 'warning' }).then(() => {
        resetPassword(row.id).then((res) => {
          const nextPassword = (res && res.data) || '123456'
          this.$message.success(`密码已重置，默认密码为：${nextPassword}`)
        })
      }).catch(() => {})
    },
    handleToggleStatus(row) {
      const newStatus = row.status === 1 ? 0 : 1
      const text = newStatus === 0 ? '禁用' : '启用'
      this.$confirm(`确定要${text}用户「${row.username}」吗？`, '提示', { type: 'warning' }).then(() => {
        toggleUserStatus(row.id, newStatus).then(() => {
          this.$message.success(`${text}成功`)
          this.fetchList()
        })
      }).catch(() => {})
    },
    onUserTypeChange(val) {
      if (val !== 2) {
        this.form.enterpriseId = null
      }
      this.$nextTick(() => {
        this.$refs.userForm && this.$refs.userForm.clearValidate('enterpriseId')
      })
    },
    resetForm() {
      this.$refs.userForm && this.$refs.userForm.resetFields()
      this.form = createEmptyForm()
    },
    userTypeLabel(type) {
      return USER_TYPE_MAP[type] || '未知'
    },
    userTypeTag(type) {
      return { 1: 'danger', 2: 'success', 3: 'info' }[type] || 'info'
    }
  }
}
</script>

<style lang="scss" scoped>
.page-container { padding: 4px; }

.filter-card {
  border-radius: 8px;
  margin-bottom: 16px;
}

.table-card {
  border-radius: 8px;

  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-weight: 600;
    color: #1a3a2a;
  }
}

.pagination { margin-top: 16px; text-align: right; }
</style>
