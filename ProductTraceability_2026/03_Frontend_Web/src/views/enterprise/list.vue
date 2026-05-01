<template>
  <div class="page-container">
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="queryParams" size="small">
        <el-form-item label="企业类型">
          <el-select v-model="queryParams.enterpriseType" placeholder="全部" clearable style="width: 140px">
            <el-option label="种植养殖" :value="1" />
            <el-option label="加工屠宰" :value="2" />
            <el-option label="检疫质检" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="审核状态">
          <el-select v-model="queryParams.auditStatus" placeholder="全部" clearable style="width: 120px">
            <el-option label="待审核" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已驳回" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 100px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="queryParams.keyword"
            placeholder="企业名称/编号/联系人"
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

    <el-row :gutter="16" class="stat-row">
      <el-col :span="6" v-for="(card, idx) in statCards" :key="idx">
        <el-card shadow="hover" class="stat-card" :class="card.cls">
          <div class="stat-body">
            <div class="stat-icon"><i :class="card.icon"></i></div>
            <div class="stat-info">
              <p class="stat-label">{{ card.label }}</p>
              <h3 class="stat-value">{{ card.value }}</h3>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="table-card">
      <div slot="header" class="card-header">
        <span>企业列表</span>
        <el-tag size="small" type="info">共 {{ total }} 家企业</el-tag>
      </div>

      <el-table v-loading="loading" :data="tableData" border size="small" style="width: 100%">
        <el-table-column prop="enterpriseCode" label="企业编号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="enterpriseName" label="企业名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="企业类型" width="110" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="typeTagColor(row.enterpriseType)" size="mini">{{ typeLabel(row.enterpriseType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="legalPerson" label="法人代表" width="100" />
        <el-table-column prop="contactPerson" label="联系人" width="90" />
        <el-table-column prop="contactPhone" label="联系电话" width="120" />
        <el-table-column label="地区" min-width="130" show-overflow-tooltip>
          <template slot-scope="{ row }">{{ formatRegion(row) }}</template>
        </el-table-column>
        <el-table-column label="审核状态" width="90" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="auditTagType(row.auditStatus)" size="mini">{{ auditLabel(row.auditStatus) }}</el-tag>
          </template>
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
        <el-table-column prop="createTime" label="入驻时间" width="150" />
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" icon="el-icon-view" @click="handleDetail(row)">详情</el-button>
            <el-button type="text" size="mini" icon="el-icon-edit" @click="handleEdit(row)">编辑</el-button>
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

    <el-dialog title="企业详情" :visible.sync="detailVisible" width="700px" top="6vh">
      <el-descriptions v-if="detailData" :column="2" border size="medium">
        <el-descriptions-item label="企业编号">{{ detailData.enterpriseCode }}</el-descriptions-item>
        <el-descriptions-item label="企业名称">{{ detailData.enterpriseName }}</el-descriptions-item>
        <el-descriptions-item label="企业类型">
          <el-tag :type="typeTagColor(detailData.enterpriseType)" size="mini">{{ typeLabel(detailData.enterpriseType) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="审核状态">
          <el-tag :type="auditTagType(detailData.auditStatus)" size="mini">{{ auditLabel(detailData.auditStatus) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="法人代表">{{ detailData.legalPerson || '--' }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ detailData.contactPerson || '--' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detailData.contactPhone || '--' }}</el-descriptions-item>
        <el-descriptions-item label="联系邮箱">{{ detailData.contactEmail || '--' }}</el-descriptions-item>
        <el-descriptions-item label="所在地区" :span="2">{{ formatRegion(detailData) }}</el-descriptions-item>
        <el-descriptions-item label="详细地址" :span="2">{{ detailData.address || '--' }}</el-descriptions-item>
        <el-descriptions-item label="企业简介" :span="2">{{ detailData.introduction || '--' }}</el-descriptions-item>
        <el-descriptions-item label="入驻时间">{{ detailData.createTime || '--' }}</el-descriptions-item>
        <el-descriptions-item label="审核时间">{{ detailData.auditTime || '--' }}</el-descriptions-item>
        <el-descriptions-item label="审核备注" :span="2">{{ detailData.auditRemark || '--' }}</el-descriptions-item>
      </el-descriptions>

      <div class="cert-section" v-if="detailData">
        <h4>资质证书</h4>
        <div class="cert-images">
          <div class="cert-item" v-if="detailData.businessLicense">
            <p>营业执照</p>
            <el-image
              :src="resolveUrl(detailData.businessLicense)"
              fit="contain"
              :preview-src-list="[resolveUrl(detailData.businessLicense)]"
              style="width: 160px; height: 120px"
            />
          </div>
          <div class="cert-item" v-if="detailData.productionLicense">
            <p>行业许可证</p>
            <el-image
              :src="resolveUrl(detailData.productionLicense)"
              fit="contain"
              :preview-src-list="[resolveUrl(detailData.productionLicense)]"
              style="width: 160px; height: 120px"
            />
          </div>
          <div class="cert-item" v-if="!detailData.businessLicense && !detailData.productionLicense">
            <el-empty description="暂无资质证书" :image-size="60" />
          </div>
        </div>
      </div>
    </el-dialog>

    <el-dialog
      title="编辑企业信息"
      :visible.sync="editVisible"
      width="880px"
      top="4vh"
      :close-on-click-modal="false"
      @close="resetEditState"
    >
      <div v-loading="editLoading">
        <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="110px" size="small">
          <el-divider content-position="left">基本信息</el-divider>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="企业编号">
                <el-input v-model="editForm.enterpriseCode" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="企业类型">
                <el-input :value="typeLabel(editForm.enterpriseType)" disabled />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="企业名称" prop="enterpriseName">
                <el-input v-model="editForm.enterpriseName" placeholder="请输入企业名称" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="法定代表人" prop="legalPerson">
                <el-input v-model="editForm.legalPerson" placeholder="请输入法定代表人" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="联系人" prop="contactPerson">
                <el-input v-model="editForm.contactPerson" placeholder="请输入联系人" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="联系电话" prop="contactPhone">
                <el-input v-model="editForm.contactPhone" maxlength="11" placeholder="请输入联系电话" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="联系邮箱">
                <el-input v-model="editForm.contactEmail" placeholder="请输入联系邮箱" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-divider content-position="left">地址信息</el-divider>

          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="省份">
                <el-input v-model="editForm.province" placeholder="省份" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="城市">
                <el-input v-model="editForm.city" placeholder="城市" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="区/县">
                <el-input v-model="editForm.district" placeholder="区/县" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="详细地址" prop="address">
            <el-input v-model="editForm.address" placeholder="请输入详细地址" />
          </el-form-item>

          <el-divider content-position="left">企业简介</el-divider>

          <el-form-item label="企业简介">
            <el-input
              v-model="editForm.introduction"
              type="textarea"
              :rows="3"
              placeholder="请输入企业简介"
            />
          </el-form-item>

          <el-divider content-position="left">企业形象</el-divider>

          <el-form-item label="企业 Logo">
            <div class="cert-upload-area">
              <el-upload
                action="#"
                list-type="picture-card"
                :auto-upload="false"
                :file-list="editLogoList"
                :on-change="(file, fileList) => handleEditFileChange(file, fileList, 'logo')"
                :on-remove="(file, fileList) => handleEditFileRemove(fileList, 'logo')"
                :on-preview="handleEditPreview"
                accept="image/*"
                :limit="1"
                :class="{ 'hide-upload-btn': editLogoList.length >= 1 }"
              >
                <div class="upload-trigger">
                  <i class="el-icon-camera"></i>
                  <span>上传 Logo</span>
                </div>
              </el-upload>
              <div class="upload-tip">建议尺寸 200x200，将显示在企业端首页与详情页</div>
            </div>
          </el-form-item>

          <el-form-item label="企业背景图">
            <div class="cert-upload-area">
              <el-upload
                action="#"
                list-type="picture-card"
                :auto-upload="false"
                :file-list="editCoverImageList"
                :on-change="(file, fileList) => handleEditFileChange(file, fileList, 'coverImage')"
                :on-remove="(file, fileList) => handleEditFileRemove(fileList, 'coverImage')"
                :on-preview="handleEditPreview"
                accept="image/*"
                :limit="1"
                :class="{ 'hide-upload-btn': editCoverImageList.length >= 1 }"
              >
                <div class="upload-trigger">
                  <i class="el-icon-camera"></i>
                  <span>上传背景图</span>
                </div>
              </el-upload>
              <div class="upload-tip">建议尺寸 750x400，将显示在小程序企业详情顶部</div>
            </div>
          </el-form-item>

          <el-divider content-position="left">资质管理</el-divider>

          <el-form-item label="营业执照">
            <div class="cert-upload-area">
              <el-upload
                action="#"
                list-type="picture-card"
                :auto-upload="false"
                :file-list="editBusinessLicenseList"
                :on-change="(file, fileList) => handleEditFileChange(file, fileList, 'businessLicense')"
                :on-remove="(file, fileList) => handleEditFileRemove(fileList, 'businessLicense')"
                :on-preview="handleEditPreview"
                accept="image/*"
                :limit="1"
                :class="{ 'hide-upload-btn': editBusinessLicenseList.length >= 1 }"
              >
                <div class="upload-trigger">
                  <i class="el-icon-camera"></i>
                  <span>上传执照</span>
                </div>
              </el-upload>
            </div>
          </el-form-item>

          <el-form-item label="行业许可证">
            <div class="cert-upload-area">
              <el-upload
                action="#"
                list-type="picture-card"
                :auto-upload="false"
                :file-list="editProductionLicenseList"
                :on-change="(file, fileList) => handleEditFileChange(file, fileList, 'productionLicense')"
                :on-remove="(file, fileList) => handleEditFileRemove(fileList, 'productionLicense')"
                :on-preview="handleEditPreview"
                accept="image/*"
                :limit="1"
                :class="{ 'hide-upload-btn': editProductionLicenseList.length >= 1 }"
              >
                <div class="upload-trigger">
                  <i class="el-icon-camera"></i>
                  <span>上传证书</span>
                </div>
              </el-upload>
            </div>
          </el-form-item>
        </el-form>
      </div>

      <div slot="footer" class="dialog-footer">
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSaving" @click="handleEditSave">保存</el-button>
      </div>
    </el-dialog>

    <el-dialog :visible.sync="editPreviewVisible" width="auto" append-to-body>
      <img :src="editPreviewUrl" style="max-width: 100%; max-height: 80vh" />
    </el-dialog>
  </div>
</template>

<script>
import { getEnterpriseList, getEnterpriseDetail, toggleEnterpriseStatus, updateEnterprise, uploadFile } from '@/api/enterprise'

const TYPE_MAP = { 1: '种植养殖', 2: '加工屠宰', 3: '检疫质检' }
const MOBILE_PHONE_PATTERN = '^1[3-9]\\d{9}$'
const AUDIT_MAP = { 0: '待审核', 1: '已通过', 2: '已驳回' }
const IMAGE_FIELD_CONFIG = {
  logo: { uploadType: 'logo', label: '企业 Logo' },
  coverImage: { uploadType: 'cover', label: '企业背景图' },
  businessLicense: { uploadType: 'license', label: '营业执照' },
  productionLicense: { uploadType: 'license', label: '行业许可证' }
}

function createEmptyEditForm() {
  return {
    id: null,
    enterpriseCode: '',
    enterpriseName: '',
    enterpriseType: null,
    legalPerson: '',
    contactPerson: '',
    contactPhone: '',
    contactEmail: '',
    province: '',
    city: '',
    district: '',
    address: '',
    introduction: '',
    logo: '',
    coverImage: '',
    businessLicense: '',
    productionLicense: ''
  }
}

export default {
  name: 'EnterpriseList',
  data() {
    return {
      loading: false,
      tableData: [],
      total: 0,
      queryParams: {
        current: 1,
        size: 10,
        enterpriseType: null,
        auditStatus: null,
        status: null,
        keyword: ''
      },
      statCards: [
        { label: '企业总数', value: 0, icon: 'el-icon-office-building', cls: 'stat-total' },
        { label: '种植养殖', value: 0, icon: 'el-icon-s-cooperation', cls: 'stat-type1' },
        { label: '加工屠宰', value: 0, icon: 'el-icon-s-tools', cls: 'stat-type2' },
        { label: '检疫质检', value: 0, icon: 'el-icon-s-check', cls: 'stat-type3' }
      ],
      detailVisible: false,
      detailData: null,
      editVisible: false,
      editLoading: false,
      editSaving: false,
      editForm: createEmptyEditForm(),
      editRules: {
        enterpriseName: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
        legalPerson: [{ required: true, message: '请输入法定代表人', trigger: 'blur' }],
        contactPerson: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
        contactPhone: [
          { required: true, message: '请输入联系电话', trigger: 'blur' },
          { pattern: new RegExp(MOBILE_PHONE_PATTERN), message: '请输入11位手机号', trigger: 'blur' }
        ],
        address: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
      },
      editLogoList: [],
      editCoverImageList: [],
      editBusinessLicenseList: [],
      editProductionLicenseList: [],
      editPendingUploads: {
        logo: null,
        coverImage: null,
        businessLicense: null,
        productionLicense: null
      },
      editPreviewVisible: false,
      editPreviewUrl: ''
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    fetchList() {
      this.loading = true
      const params = { ...this.queryParams }
      if (!params.keyword) delete params.keyword
      if (params.enterpriseType === null) delete params.enterpriseType
      if (params.auditStatus === null) delete params.auditStatus
      if (params.status === null) delete params.status

      getEnterpriseList(params).then(res => {
        const data = res.data
        if (data.records) {
          this.tableData = data.records
          this.total = data.total || 0
        } else if (Array.isArray(data)) {
          this.tableData = data
          this.total = data.length
        }
        this.updateStats()
      }).catch(() => {
        this.tableData = []
        this.total = 0
      }).finally(() => {
        this.loading = false
      })
    },
    updateStats() {
      this.statCards[0].value = this.total
      this.statCards[1].value = this.tableData.filter(item => item.enterpriseType === 1).length
      this.statCards[2].value = this.tableData.filter(item => item.enterpriseType === 2).length
      this.statCards[3].value = this.tableData.filter(item => item.enterpriseType === 3).length
    },
    handleQuery() {
      this.queryParams.current = 1
      this.fetchList()
    },
    resetQuery() {
      this.queryParams = { current: 1, size: 10, enterpriseType: null, auditStatus: null, status: null, keyword: '' }
      this.fetchList()
    },
    formatRegion(row) {
      return [row.province, row.city, row.district].filter(Boolean).join('/') || '--'
    },
    handleDetail(row) {
      getEnterpriseDetail(row.id).then(res => {
        this.detailData = res.data
        this.detailVisible = true
      })
    },
    async handleEdit(row) {
      this.editVisible = true
      this.editLoading = true
      try {
        const res = await getEnterpriseDetail(row.id)
        this.applyEditDetailToForm(res.data)
      } catch (error) {
        this.editVisible = false
      } finally {
        this.editLoading = false
      }
    },
    applyEditDetailToForm(detail) {
      const form = createEmptyEditForm()
      Object.keys(form).forEach(key => {
        if (detail[key] !== undefined && detail[key] !== null) {
          form[key] = detail[key]
        }
      })
      this.editForm = form
      this.editLogoList = this.createUploadList('企业 Logo', detail.logo)
      this.editCoverImageList = this.createUploadList('企业背景图', detail.coverImage)
      this.editBusinessLicenseList = this.createUploadList('营业执照', detail.businessLicense)
      this.editProductionLicenseList = this.createUploadList('行业许可证', detail.productionLicense)
      this.editPendingUploads = {
        logo: null,
        coverImage: null,
        businessLicense: null,
        productionLicense: null
      }
    },
    createUploadList(name, value) {
      return value ? [{ name, url: this.resolveUrl(value) }] : []
    },
    handleToggleStatus(row) {
      const newStatus = row.status === 1 ? 0 : 1
      const actionText = newStatus === 0 ? '禁用' : '启用'
      this.$confirm(`确定要${actionText}企业“${row.enterpriseName}”吗？`, '提示', {
        type: 'warning'
      }).then(() => {
        toggleEnterpriseStatus(row.id, newStatus).then(() => {
          this.$message.success(`${actionText}成功`)
          this.fetchList()
        })
      }).catch(() => {})
    },
    setEditFileList(field, fileList) {
      if (field === 'logo') {
        this.editLogoList = fileList
      } else if (field === 'coverImage') {
        this.editCoverImageList = fileList
      } else if (field === 'businessLicense') {
        this.editBusinessLicenseList = fileList
      } else if (field === 'productionLicense') {
        this.editProductionLicenseList = fileList
      }
    },
    handleEditFileChange(file, fileList, field) {
      if (file.raw && file.raw.size > 10 * 1024 * 1024) {
        this.$message.error('图片大小不能超过 10MB')
        fileList.pop()
        return
      }
      this.editPendingUploads[field] = file.raw || null
      this.setEditFileList(field, fileList.slice(-1))
    },
    handleEditFileRemove(fileList, field) {
      this.editPendingUploads[field] = null
      this.editForm[field] = ''
      this.setEditFileList(field, fileList)
    },
    handleEditPreview(file) {
      this.editPreviewUrl = file.url || ''
      this.editPreviewVisible = true
    },
    async handleEditSave() {
      const valid = await this.$refs.editFormRef.validate().catch(() => false)
      if (!valid) return

      this.editSaving = true
      try {
        const payload = { ...this.editForm }
        for (const [field, config] of Object.entries(IMAGE_FIELD_CONFIG)) {
          if (this.editPendingUploads[field]) {
            const uploadRes = await uploadFile(this.editPendingUploads[field], config.uploadType)
            payload[field] = uploadRes.data.url
          }
        }
        await updateEnterprise(payload)
        this.$message.success('企业信息保存成功')
        if (this.detailData && this.detailData.id === payload.id) {
          this.detailData = { ...this.detailData, ...payload }
        }
        this.editVisible = false
        this.fetchList()
      } catch (error) {
        // response interceptor already handles feedback
      } finally {
        this.editSaving = false
      }
    },
    resetEditState() {
      this.editLoading = false
      this.editSaving = false
      this.editForm = createEmptyEditForm()
      this.editLogoList = []
      this.editCoverImageList = []
      this.editBusinessLicenseList = []
      this.editProductionLicenseList = []
      this.editPendingUploads = {
        logo: null,
        coverImage: null,
        businessLicense: null,
        productionLicense: null
      }
      this.editPreviewVisible = false
      this.editPreviewUrl = ''
    },
    typeLabel(type) { return TYPE_MAP[type] || '未知' },
    typeTagColor(type) { return { 1: 'success', 2: 'warning', 3: '' }[type] || 'info' },
    auditLabel(status) { return AUDIT_MAP[status] || '未知' },
    auditTagType(status) { return { 0: 'warning', 1: 'success', 2: 'danger' }[status] || 'info' },
    resolveUrl(path) {
      if (!path) return ''
      if (path.startsWith('http://tmp')) return ''
      if (path.startsWith('http')) return path
      return process.env.VUE_APP_BASE_API + path.replace(/^\/api/, '')
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: #2d8a56;

.page-container { padding: 4px; }

.filter-card { border-radius: 8px; margin-bottom: 16px; }

.stat-row { margin-bottom: 16px; }

.stat-card {
  border-radius: 8px;
  border: none;

  .stat-body {
    display: flex;
    align-items: center;
    gap: 14px;
  }

  .stat-icon {
    width: 48px;
    height: 48px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;

    i {
      font-size: 24px;
      color: #fff;
    }
  }

  .stat-label {
    font-size: 13px;
    color: #909399;
    margin: 0 0 4px;
  }

  .stat-value {
    font-size: 24px;
    font-weight: 600;
    color: #303133;
    margin: 0;
  }

  &.stat-total .stat-icon { background: $primary; }
  &.stat-type1 .stat-icon { background: #67c23a; }
  &.stat-type2 .stat-icon { background: #e6a23c; }
  &.stat-type3 .stat-icon { background: #409eff; }
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

.cert-section {
  margin-top: 20px;

  h4 {
    font-size: 14px;
    color: #303133;
    margin: 0 0 12px;
  }

  .cert-images {
    display: flex;
    gap: 20px;
    flex-wrap: wrap;
  }

  .cert-item {
    text-align: center;

    p {
      font-size: 12px;
      color: #909399;
      margin: 0 0 6px;
    }
  }
}

.cert-upload-area {
  min-height: 152px;

  ::v-deep .el-upload-list--picture-card {
    display: inline-flex;
    align-items: flex-start;

    .el-upload-list__item {
      margin: 0 8px 0 0;
      transition: none;
    }
  }

  ::v-deep .el-upload--picture-card {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    line-height: normal;
    margin: 0;
  }
}

.hide-upload-btn {
  ::v-deep .el-upload--picture-card {
    display: none !important;
  }
}

.upload-trigger {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;

  i {
    font-size: 28px;
    color: $primary;
  }

  span {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
  }
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
}
</style>
