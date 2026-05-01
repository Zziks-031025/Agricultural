<template>
  <div class="page-container">
    <el-card shadow="never" class="log-card">
      <el-tabs v-model="activeTab" @tab-click="handleTabChange">
        <!-- 操作日志 -->
        <el-tab-pane label="操作日志" name="operation">
          <el-form :inline="true" :model="opQuery" size="small" class="filter-form">
            <el-form-item label="操作人">
              <el-input v-model="opQuery.username" placeholder="用户名" clearable style="width: 140px" @keyup.enter.native="fetchOpLogs" />
            </el-form-item>
            <el-form-item label="操作内容">
              <el-input v-model="opQuery.operation" placeholder="操作描述" clearable style="width: 160px" @keyup.enter.native="fetchOpLogs" />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="opQuery.status" placeholder="全部" clearable style="width: 90px">
                <el-option label="成功" :value="1" />
                <el-option label="失败" :value="0" />
              </el-select>
            </el-form-item>
            <el-form-item label="时间">
              <el-date-picker v-model="opQuery.dateRange" type="daterange" range-separator="-" start-placeholder="开始" end-placeholder="结束"
                value-format="yyyy-MM-dd" style="width: 240px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="el-icon-search" @click="fetchOpLogs">查询</el-button>
              <el-button icon="el-icon-refresh" @click="resetOpQuery">重置</el-button>
            </el-form-item>
          </el-form>

          <el-table v-loading="opLoading" :data="opList" border size="small" style="width: 100%">
            <el-table-column prop="id" label="ID" width="60" align="center" />
            <el-table-column prop="username" label="操作人" width="100" />
            <el-table-column prop="operation" label="操作内容" min-width="160" show-overflow-tooltip />
            <el-table-column prop="method" label="请求方法" min-width="200" show-overflow-tooltip />
            <el-table-column prop="ip" label="IP地址" width="130" />
            <el-table-column prop="location" label="操作地点" width="120" show-overflow-tooltip />
            <el-table-column label="状态" width="70" align="center">
              <template slot-scope="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="mini">{{ row.status === 1 ? '成功' : '失败' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="executeTime" label="耗时(ms)" width="85" align="center" />
            <el-table-column prop="createTime" label="操作时间" width="155" />
            <el-table-column label="详情" width="70" align="center">
              <template slot-scope="{ row }">
                <el-button type="text" size="mini" icon="el-icon-view" @click="showOpDetail(row)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination class="pagination" background layout="total, sizes, prev, pager, next, jumper"
            :total="opTotal" :page-sizes="[10, 20, 50, 100]" :page-size.sync="opQuery.size"
            :current-page.sync="opQuery.current" @size-change="fetchOpLogs" @current-change="fetchOpLogs" />
        </el-tab-pane>

        <!-- 登录日志 -->
        <el-tab-pane label="登录日志" name="login">
          <el-form :inline="true" :model="loginQuery" size="small" class="filter-form">
            <el-form-item label="用户名">
              <el-input v-model="loginQuery.username" placeholder="用户名" clearable style="width: 140px" @keyup.enter.native="fetchLoginLogs" />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="loginQuery.status" placeholder="全部" clearable style="width: 90px">
                <el-option label="成功" :value="1" />
                <el-option label="失败" :value="0" />
              </el-select>
            </el-form-item>
            <el-form-item label="时间">
              <el-date-picker v-model="loginQuery.dateRange" type="daterange" range-separator="-" start-placeholder="开始" end-placeholder="结束"
                value-format="yyyy-MM-dd" style="width: 240px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="el-icon-search" @click="fetchLoginLogs">查询</el-button>
              <el-button icon="el-icon-refresh" @click="resetLoginQuery">重置</el-button>
            </el-form-item>
          </el-form>

          <el-table v-loading="loginLoading" :data="loginList" border size="small" style="width: 100%">
            <el-table-column prop="id" label="ID" width="60" align="center" />
            <el-table-column prop="username" label="用户名" width="120" />
            <el-table-column prop="ip" label="IP地址" width="130" />
            <el-table-column prop="location" label="登录地点" width="130" show-overflow-tooltip />
            <el-table-column prop="browser" label="浏览器" width="130" show-overflow-tooltip />
            <el-table-column prop="os" label="操作系统" width="130" show-overflow-tooltip />
            <el-table-column label="状态" width="70" align="center">
              <template slot-scope="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="mini">{{ row.status === 1 ? '成功' : '失败' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="message" label="提示信息" min-width="160" show-overflow-tooltip />
            <el-table-column prop="loginTime" label="登录时间" width="155" />
          </el-table>

          <el-pagination class="pagination" background layout="total, sizes, prev, pager, next, jumper"
            :total="loginTotal" :page-sizes="[10, 20, 50, 100]" :page-size.sync="loginQuery.size"
            :current-page.sync="loginQuery.current" @size-change="fetchLoginLogs" @current-change="fetchLoginLogs" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 操作日志详情弹窗 -->
    <el-dialog title="操作日志详情" :visible.sync="detailVisible" width="620px">
      <el-descriptions v-if="detailData" :column="2" border size="medium">
        <el-descriptions-item label="操作人">{{ detailData.username }}</el-descriptions-item>
        <el-descriptions-item label="操作内容">{{ detailData.operation }}</el-descriptions-item>
        <el-descriptions-item label="请求方法" :span="2">{{ detailData.method }}</el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="2">
          <div class="json-text">{{ detailData.params || '--' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="返回结果" :span="2">
          <div class="json-text">{{ detailData.result || '--' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="错误信息" :span="2" v-if="detailData.status === 0">
          <span class="error-text">{{ detailData.errorMsg || '--' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ detailData.ip }}</el-descriptions-item>
        <el-descriptions-item label="操作地点">{{ detailData.location || '--' }}</el-descriptions-item>
        <el-descriptions-item label="浏览器">{{ detailData.browser || '--' }}</el-descriptions-item>
        <el-descriptions-item label="操作系统">{{ detailData.os || '--' }}</el-descriptions-item>
        <el-descriptions-item label="执行时长">{{ detailData.executeTime || 0 }} ms</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ detailData.createTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script>
import { getOperationLogs, getLoginLogs } from '@/api/system'

export default {
  name: 'SystemLog',
  data() {
    return {
      activeTab: 'operation',
      // 操作日志
      opLoading: false,
      opList: [],
      opTotal: 0,
      opQuery: { current: 1, size: 10, username: '', operation: '', status: null, dateRange: null },
      // 登录日志
      loginLoading: false,
      loginList: [],
      loginTotal: 0,
      loginQuery: { current: 1, size: 10, username: '', status: null, dateRange: null },
      // 详情
      detailVisible: false,
      detailData: null
    }
  },
  created() {
    this.fetchOpLogs()
  },
  methods: {
    handleTabChange(tab) {
      if (tab.name === 'operation' && !this.opList.length) this.fetchOpLogs()
      if (tab.name === 'login' && !this.loginList.length) this.fetchLoginLogs()
    },
    fetchOpLogs() {
      this.opLoading = true
      const params = { ...this.opQuery }
      if (params.dateRange && params.dateRange.length === 2) {
        params.startDate = params.dateRange[0]
        params.endDate = params.dateRange[1]
      }
      delete params.dateRange
      if (!params.username) delete params.username
      if (!params.operation) delete params.operation
      if (params.status === null) delete params.status

      getOperationLogs(params).then(res => {
        const data = res.data
        if (data.records) { this.opList = data.records; this.opTotal = data.total || 0 }
        else if (Array.isArray(data)) { this.opList = data; this.opTotal = data.length }
      }).catch(() => { this.opList = [] }).finally(() => { this.opLoading = false })
    },
    resetOpQuery() {
      this.opQuery = { current: 1, size: 10, username: '', operation: '', status: null, dateRange: null }
      this.fetchOpLogs()
    },
    fetchLoginLogs() {
      this.loginLoading = true
      const params = { ...this.loginQuery }
      if (params.dateRange && params.dateRange.length === 2) {
        params.startDate = params.dateRange[0]
        params.endDate = params.dateRange[1]
      }
      delete params.dateRange
      if (!params.username) delete params.username
      if (params.status === null) delete params.status

      getLoginLogs(params).then(res => {
        const data = res.data
        if (data.records) { this.loginList = data.records; this.loginTotal = data.total || 0 }
        else if (Array.isArray(data)) { this.loginList = data; this.loginTotal = data.length }
      }).catch(() => { this.loginList = [] }).finally(() => { this.loginLoading = false })
    },
    resetLoginQuery() {
      this.loginQuery = { current: 1, size: 10, username: '', status: null, dateRange: null }
      this.fetchLoginLogs()
    },
    showOpDetail(row) {
      this.detailData = row
      this.detailVisible = true
    }
  }
}
</script>

<style lang="scss" scoped>
.page-container { padding: 4px; }
.log-card { border-radius: 8px; }
.filter-form { margin-bottom: 12px; }
.pagination { margin-top: 16px; text-align: right; }
.json-text { font-family: monospace; font-size: 12px; color: #606266; word-break: break-all; max-height: 120px; overflow-y: auto; }
.error-text { color: #f56c6c; }
</style>
