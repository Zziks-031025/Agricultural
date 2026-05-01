<template>
  <div class="page-container">
    <el-alert
      v-if="form.auditStatus === 2"
      :title="'企业审核未通过：' + (form.auditRemark || '请根据提示修改资料后重新提交审核')"
      type="error"
      show-icon
      :closable="false"
      style="margin-bottom: 12px; border-radius: 8px"
    />

    <el-alert
      v-else-if="form.auditStatus === 0"
      title="企业资料正在审核中，审核通过后将自动开放完整企业工作台。"
      type="info"
      show-icon
      :closable="false"
      style="margin-bottom: 12px; border-radius: 8px"
    />

    <el-card shadow="never" class="profile-card" v-loading="loading">
      <div slot="header" class="card-header">
        <span>企业信息</span>
        <el-tag v-if="form.auditStatus === 1" type="success" size="small">已认证</el-tag>
        <el-tag v-else-if="form.auditStatus === 0" type="warning" size="small">待审核</el-tag>
        <el-tag v-else-if="form.auditStatus === 2" type="danger" size="small">审核驳回</el-tag>
      </div>

      <el-form ref="profileForm" :model="form" :rules="rules" label-width="120px" size="small" style="max-width: 860px">
        <el-divider content-position="left">基本信息</el-divider>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="企业编号">
              <el-input v-model="form.enterpriseCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="企业类型">
              <el-input :value="enterpriseTypeLabel" disabled />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="企业名称" prop="enterpriseName">
              <el-input v-model="form.enterpriseName" placeholder="请输入企业名称" :disabled="!editing" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="法定代表人" prop="legalPerson">
              <el-input v-model="form.legalPerson" placeholder="请输入法定代表人" :disabled="!editing" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="联系人" prop="contactPerson">
              <el-input v-model="form.contactPerson" placeholder="联系人姓名" :disabled="!editing" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="contactPhone">
              <el-input v-model="form.contactPhone" maxlength="11" placeholder="联系电话" :disabled="!editing" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="联系邮箱">
              <el-input v-model="form.contactEmail" placeholder="联系邮箱" :disabled="!editing" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">地址信息</el-divider>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="省份">
              <el-input v-model="form.province" placeholder="省份" :disabled="!editing" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="城市">
              <el-input v-model="form.city" placeholder="城市" :disabled="!editing" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="区/县">
              <el-input v-model="form.district" placeholder="区/县" :disabled="!editing" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="详细地址" prop="address">
          <el-input v-model="form.address" placeholder="详细地址" :disabled="!editing" />
        </el-form-item>

        <el-divider content-position="left">企业简介</el-divider>

        <el-form-item label="企业简介">
          <el-input
            v-model="form.introduction"
            type="textarea"
            :rows="3"
            placeholder="请输入企业简介"
            :disabled="!editing"
          />
        </el-form-item>

        <el-divider content-position="left">企业形象</el-divider>

        <el-form-item label="企业 Logo">
          <div class="cert-upload-area">
            <el-upload
              action="#"
              list-type="picture-card"
              :auto-upload="false"
              :file-list="logoList"
              :on-change="(f, fl) => handleCertChange(f, fl, 'logo')"
              :on-remove="(f, fl) => handleCertRemove(fl, 'logo')"
              :on-preview="handlePreview"
              accept="image/*"
              :limit="1"
              :disabled="!editing"
              :class="{ 'hide-upload-btn': logoList.length >= 1 }"
            >
              <div class="upload-trigger">
                <i class="el-icon-camera"></i>
                <span>上传 Logo</span>
              </div>
            </el-upload>
            <div class="upload-tip">建议尺寸 200×200，会显示在企业端和小程序端的企业形象位置。</div>
          </div>
        </el-form-item>

        <el-form-item label="企业背景图">
          <div class="cert-upload-area">
            <el-upload
              action="#"
              list-type="picture-card"
              :auto-upload="false"
              :file-list="coverImageList"
              :on-change="(f, fl) => handleCertChange(f, fl, 'coverImage')"
              :on-remove="(f, fl) => handleCertRemove(fl, 'coverImage')"
              :on-preview="handlePreview"
              accept="image/*"
              :limit="1"
              :disabled="!editing"
              :class="{ 'hide-upload-btn': coverImageList.length >= 1 }"
            >
              <div class="upload-trigger">
                <i class="el-icon-camera"></i>
                <span>上传背景图</span>
              </div>
            </el-upload>
            <div class="upload-tip">建议尺寸 750×400，会显示在企业详情头图位置。</div>
          </div>
        </el-form-item>

        <el-divider content-position="left">资质管理</el-divider>

        <el-form-item label="营业执照">
          <div class="cert-upload-area">
            <el-upload
              action="#"
              list-type="picture-card"
              :auto-upload="false"
              :file-list="businessLicenseList"
              :on-change="(f, fl) => handleCertChange(f, fl, 'businessLicense')"
              :on-remove="(f, fl) => handleCertRemove(fl, 'businessLicense')"
              :on-preview="handlePreview"
              accept="image/*"
              :limit="1"
              :disabled="!editing"
              :class="{ 'hide-upload-btn': businessLicenseList.length >= 1 }"
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
              :file-list="productionLicenseList"
              :on-change="(f, fl) => handleCertChange(f, fl, 'productionLicense')"
              :on-remove="(f, fl) => handleCertRemove(fl, 'productionLicense')"
              :on-preview="handlePreview"
              accept="image/*"
              :limit="1"
              :disabled="!editing"
              :class="{ 'hide-upload-btn': productionLicenseList.length >= 1 }"
            >
              <div class="upload-trigger">
                <i class="el-icon-camera"></i>
                <span>上传证书</span>
              </div>
            </el-upload>
          </div>
        </el-form-item>

        <el-form-item>
          <template v-if="!editing">
            <el-button type="primary" icon="el-icon-edit" @click="editing = true">编辑信息</el-button>
            <el-button v-if="form.auditStatus === 2" plain @click="startReapply">修改后重新提交</el-button>
          </template>
          <template v-else>
            <el-button
              type="primary"
              icon="el-icon-check"
              :loading="saving"
              @click="form.auditStatus === 2 ? handleReapply() : handleSave()"
            >
              {{ form.auditStatus === 2 ? '保存并重新提交审核' : '保存' }}
            </el-button>
            <el-button icon="el-icon-close" @click="handleCancel">取消</el-button>
          </template>
        </el-form-item>
      </el-form>
    </el-card>

    <el-dialog :visible.sync="previewVisible" width="auto" append-to-body>
      <img :src="previewUrl" style="max-width: 100%; max-height: 80vh" />
    </el-dialog>
  </div>
</template>

<script>
import {
  getEnterpriseDetail,
  updateEnterprise,
  uploadFile,
  submitImageAudit,
  reapplyEnterprise
} from '@/api/enterprise'

const TYPE_MAP = { 1: '种植养殖企业', 2: '加工屠宰企业', 3: '检疫质检企业' }
const MOBILE_PHONE_PATTERN = '^1[3-9]\\d{9}$'

function validateMobilePhone(rule, value, callback) {
  if (!value) {
    callback(new Error('请输入联系电话'))
    return
  }
  if (!new RegExp(MOBILE_PHONE_PATTERN).test(value)) {
    callback(new Error('请输入11位手机号'))
    return
  }
  callback()
}

function buildFileList(name, path, resolveUrl) {
  return path ? [{ name, url: resolveUrl(path) }] : []
}

export default {
  name: 'EnterpriseSelfInfo',
  data() {
    return {
      loading: false,
      saving: false,
      editing: false,
      form: {
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
        businessLicense: '',
        productionLicense: '',
        logo: '',
        coverImage: '',
        auditStatus: null,
        auditRemark: ''
      },
      formBackup: null,
      rules: {
        enterpriseName: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
        legalPerson: [{ required: true, message: '请输入法定代表人', trigger: 'blur' }],
        contactPhone: [{ validator: validateMobilePhone, trigger: 'blur' }],
        address: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
      },
      logoList: [],
      businessLicenseList: [],
      productionLicenseList: [],
      coverImageList: [],
      logoListBackup: [],
      businessLicenseListBackup: [],
      productionLicenseListBackup: [],
      coverImageListBackup: [],
      pendingUploads: { logo: null, businessLicense: null, productionLicense: null, coverImage: null },
      previewVisible: false,
      previewUrl: ''
    }
  },
  computed: {
    enterpriseTypeLabel() {
      return TYPE_MAP[this.form.enterpriseType] || '未知'
    }
  },
  created() {
    this.fetchProfile()
  },
  methods: {
    fetchProfile() {
      const userInfo = this.$store.getters.userInfo
      if (!userInfo || !userInfo.enterpriseId) {
        this.$message.warning('未关联企业信息')
        return
      }
      this.loading = true
      getEnterpriseDetail(userInfo.enterpriseId).then(res => {
        this.applyProfileData(res.data || {})
      }).catch(() => {}).finally(() => {
        this.loading = false
      })
    },
    refreshProfileSilently() {
      const userInfo = this.$store.getters.userInfo
      if (!userInfo || !userInfo.enterpriseId) return Promise.resolve()
      return getEnterpriseDetail(userInfo.enterpriseId).then(res => {
        this.applyProfileData(res.data || {})
      }).catch(() => {})
    },
    applyProfileData(data) {
      Object.keys(this.form).forEach(key => {
        this.form[key] = data[key] !== undefined && data[key] !== null ? data[key] : this.form[key]
      })
      this.logoList = buildFileList('企业 Logo', data.logo, this.resolveUrl)
      this.businessLicenseList = buildFileList('营业执照', data.businessLicense, this.resolveUrl)
      this.productionLicenseList = buildFileList('行业许可证', data.productionLicense, this.resolveUrl)
      this.coverImageList = buildFileList('企业背景图', data.coverImage, this.resolveUrl)
      this.formBackup = JSON.parse(JSON.stringify(this.form))
      this.logoListBackup = JSON.parse(JSON.stringify(this.logoList))
      this.businessLicenseListBackup = JSON.parse(JSON.stringify(this.businessLicenseList))
      this.productionLicenseListBackup = JSON.parse(JSON.stringify(this.productionLicenseList))
      this.coverImageListBackup = JSON.parse(JSON.stringify(this.coverImageList))
    },
    resolveUrl(path) {
      if (!path) return ''
      if (path.startsWith('http://tmp')) return ''
      if (path.startsWith('http')) return path
      return process.env.VUE_APP_BASE_API + path.replace(/^\/api/, '')
    },
    handleCertChange(file, fileList, field) {
      if (file.raw && file.raw.size > 10 * 1024 * 1024) {
        this.$message.error('图片大小不能超过10MB')
        fileList.pop()
        return
      }
      this.pendingUploads[field] = file.raw
      const latest = fileList.slice(-1)
      if (field === 'logo') this.logoList = latest
      if (field === 'businessLicense') this.businessLicenseList = latest
      if (field === 'productionLicense') this.productionLicenseList = latest
      if (field === 'coverImage') this.coverImageList = latest
    },
    handleCertRemove(fileList, field) {
      this.pendingUploads[field] = null
      if (field === 'logo') {
        this.logoList = fileList
        this.form.logo = ''
      }
      if (field === 'businessLicense') {
        this.businessLicenseList = fileList
        this.form.businessLicense = ''
      }
      if (field === 'productionLicense') {
        this.productionLicenseList = fileList
        this.form.productionLicense = ''
      }
      if (field === 'coverImage') {
        this.coverImageList = fileList
        this.form.coverImage = ''
      }
    },
    handlePreview(file) {
      this.previewUrl = file.url
      this.previewVisible = true
    },
    startReapply() {
      this.editing = true
    },
    handleSave() {
      return this.saveProfile(false)
    },
    handleReapply() {
      return this.saveProfile(true)
    },
    async saveProfile(needReapply) {
      const valid = await this.$refs.profileForm.validate().catch(() => false)
      if (!valid) return

      this.saving = true
      try {
        const userInfo = this.$store.getters.userInfo
        const auditFields = ['logo', 'businessLicense', 'productionLicense', 'coverImage']
        const auditFieldDbNames = {
          logo: 'logo',
          businessLicense: 'business_license',
          productionLicense: 'production_license',
          coverImage: 'cover_image'
        }
        let auditMessages = 0

        for (const field of auditFields) {
          if (!this.pendingUploads[field]) continue
          const uploadType = field === 'logo' ? 'logo' : (field === 'coverImage' ? 'cover' : 'license')
          const uploadRes = await uploadFile(this.pendingUploads[field], uploadType)
          const newUrl = uploadRes.data.url
          const oldUrl = this.formBackup ? this.formBackup[field] : ''
          await submitImageAudit({
            enterpriseId: this.form.id,
            userId: userInfo ? userInfo.id : null,
            fieldName: auditFieldDbNames[field],
            oldValue: oldUrl || '',
            newValue: newUrl
          })
          this.form[field] = this.formBackup ? this.formBackup[field] : ''
          this.pendingUploads[field] = null
          auditMessages += 1
        }

        await updateEnterprise({ ...this.form })

        if (needReapply && this.form.auditStatus === 2) {
          await reapplyEnterprise(this.form.id)
        }

        if (needReapply && this.form.auditStatus === 2) {
          this.$message.success('企业信息已保存，并重新提交审核')
        } else if (auditMessages > 0) {
          this.$message.success(`企业信息已保存，${auditMessages} 项图片变更已提交审核`)
        } else {
          this.$message.success('企业信息保存成功')
        }

        this.editing = false
        await this.refreshProfileSilently()
      } catch (e) {
        // error handled by interceptor
      } finally {
        this.saving = false
      }
    },
    handleCancel() {
      this.editing = false
      this.pendingUploads = { logo: null, businessLicense: null, productionLicense: null, coverImage: null }
      if (this.formBackup) {
        this.form = JSON.parse(JSON.stringify(this.formBackup))
        this.logoList = JSON.parse(JSON.stringify(this.logoListBackup))
        this.businessLicenseList = JSON.parse(JSON.stringify(this.businessLicenseListBackup))
        this.productionLicenseList = JSON.parse(JSON.stringify(this.productionLicenseListBackup))
        this.coverImageList = JSON.parse(JSON.stringify(this.coverImageListBackup))
      }
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: #2d8a56;

.page-container { padding: 4px; }

.profile-card {
  border-radius: 8px;

  .card-header {
    display: flex;
    align-items: center;
    gap: 12px;
    font-weight: 600;
    font-size: 16px;
    color: #1a3a2a;
  }
}

.cert-upload-area {
  height: 152px;
  overflow: hidden;

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

  .upload-trigger {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;

    i { font-size: 28px; color: $primary; }
    span { font-size: 12px; color: #909399; margin-top: 4px; }
  }
}

.hide-upload-btn {
  ::v-deep .el-upload--picture-card {
    display: none !important;
  }
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
