<template>
  <div class="page-container">
    <el-card shadow="never" class="form-card">
      <div slot="header"><span>检疫质检录入</span></div>

      <el-form ref="inspectionForm" :model="form" :rules="rules" label-width="120px" size="small" style="max-width: 800px">
        <!-- Batch Query -->
        <el-form-item label="批次号" prop="batchCode">
          <div style="display: flex; gap: 8px">
            <el-input v-model="form.batchCode" placeholder="输入批次号查询" style="flex: 1" />
            <el-button type="primary" plain :loading="querying" @click="queryBatch">查询批次</el-button>
          </div>
        </el-form-item>

        <!-- Batch Info Display -->
        <el-form-item v-if="batchInfo" label="批次信息">
          <el-descriptions :column="3" size="mini" border>
            <el-descriptions-item label="产品名称">{{ batchInfo.productName }}</el-descriptions-item>
            <el-descriptions-item label="数量">{{ batchInfo.quantity }} {{ batchInfo.unit }}</el-descriptions-item>
            <el-descriptions-item label="来源企业">{{ batchInfo.enterpriseName || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-form-item>

        <el-divider content-position="left">检疫信息</el-divider>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="检疫日期" prop="inspectionDate">
              <el-date-picker
                v-model="form.inspectionDate"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="检疫结果" prop="checkResult">
              <el-radio-group v-model="form.checkResult">
                <el-radio-button :label="1">
                  <i class="el-icon-circle-check"></i> 合格
                </el-radio-button>
                <el-radio-button :label="0">
                  <i class="el-icon-circle-close"></i> 不合格
                </el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="检疫项目" prop="inspectionItems">
          <el-input
            v-model="form.inspectionItems"
            type="textarea"
            :rows="3"
            placeholder="请填写具体检疫项目，如：外观检查、微生物检测、药残检测..."
          />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="证书编号" prop="certNo">
              <el-input v-model="form.certNo" placeholder="检疫证书编号" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">检疫员信息</el-divider>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="检疫员姓名" prop="inspector">
              <el-input v-model="form.inspector" placeholder="检疫员姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="检疫员编号">
              <el-input v-model="form.inspectorCode" placeholder="检疫员工号/编号" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">检疫证书图片</el-divider>

        <el-form-item label="证书照片">
          <el-upload
            action="#"
            list-type="picture-card"
            :auto-upload="false"
            :file-list="certFileList"
            :on-change="handleCertChange"
            :on-remove="handleCertRemove"
            :on-preview="handlePreview"
            accept="image/*"
            :limit="5"
          >
            <div class="upload-trigger">
              <i class="el-icon-camera"></i>
              <span>上传证书</span>
            </div>
            <div slot="tip" class="upload-tip">
              支持上传高分辨率图片, 最多5张, 单张不超过10MB
            </div>
          </el-upload>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" :disabled="!batchInfo" @click="handleSubmit">
            提交检疫结果
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Image Preview -->
    <el-dialog :visible.sync="previewVisible" width="auto" append-to-body>
      <img :src="previewUrl" style="max-width: 100%; max-height: 80vh" />
    </el-dialog>
  </div>
</template>

<script>
import { getBatchDetail } from '@/api/batch'
import { submitQuarantine } from '@/api/quarantine'

export default {
  name: 'Inspection',
  data() {
    return {
      querying: false,
      submitting: false,
      batchInfo: null,
      form: {
        batchCode: '',
        batchId: null,
        inspectionDate: '',
        checkResult: 1,
        inspectionItems: '',
        certNo: '',
        inspector: '',
        inspectorCode: ''
      },
      rules: {
        batchCode: [{ required: true, message: '请输入批次号', trigger: 'blur' }],
        inspectionDate: [{ required: true, message: '请选择检疫日期', trigger: 'change' }],
        checkResult: [{ required: true, message: '请选择检疫结果', trigger: 'change' }],
        inspectionItems: [{ required: true, message: '请填写检疫项目', trigger: 'blur' }],
        certNo: [{ required: true, message: '请输入证书编号', trigger: 'blur' }],
        inspector: [{ required: true, message: '请输入检疫员姓名', trigger: 'blur' }]
      },
      certFileList: [],
      previewVisible: false,
      previewUrl: ''
    }
  },
  mounted() {
    // 根据当前登录账号自动填充检疫员信息（用户可修改）
    const userInfo = this.$store.getters.userInfo || {}
    this.form.inspector = userInfo.realName || userInfo.username || ''
    this.form.inspectorCode = userInfo.certificateNo || userInfo.employeeCode || ('QY-' + (userInfo.id || '0001'))

    if (this.$route.query.batchCode) {
      this.form.batchCode = this.$route.query.batchCode
      this.queryBatch()
    }
  },
  methods: {
    queryBatch() {
      if (!this.form.batchCode.trim()) {
        this.$message.warning('请输入批次号')
        return
      }
      this.querying = true
      getBatchDetail({ batchCode: this.form.batchCode.trim() }).then(res => {
        this.batchInfo = res.data
        this.form.batchId = res.data.id
        this.$message.success('批次查询成功')
      }).catch(() => {
        this.batchInfo = null
        this.form.batchId = null
      }).finally(() => {
        this.querying = false
      })
    },
    handleCertChange(file, fileList) {
      // Validate file size (10MB)
      if (file.raw && file.raw.size > 10 * 1024 * 1024) {
        this.$message.error('单张图片不超过10MB')
        fileList.pop()
        return
      }
      this.certFileList = fileList
    },
    handleCertRemove(file, fileList) {
      this.certFileList = fileList
    },
    handlePreview(file) {
      this.previewUrl = file.url
      this.previewVisible = true
    },
    handleSubmit() {
      this.$refs.inspectionForm.validate(async valid => {
        if (!valid) return
        
        this.submitting = true
        
        try {
          let certImageUrls = []
          if (this.certFileList.length > 0) {
            this.$message.info('正在上传证书图片...')
            const { uploadFile } = await import('@/api/enterprise')
            const uploadPromises = this.certFileList.map(fileItem => {
              return uploadFile(fileItem.raw, 'certificate')
            })
            const results = await Promise.all(uploadPromises)
            certImageUrls = results.map(res => res.data.url)
          }
          
          const data = {
            ...this.form,
            imagePath: certImageUrls.join(',')
          }
          
          const res = await submitQuarantine(data)
          if (res.data && res.data.txHash) {
            this.$message.success('检疫结果提交成功，数据已上链存证')
          } else {
            this.$message.success('检疫结果提交成功')
          }
          this.handleReset()
        } catch (error) {
          console.error('提交失败:', error)
        } finally {
          this.submitting = false
        }
      })
    },
    handleReset() {
      this.$refs.inspectionForm.resetFields()
      this.batchInfo = null
      this.certFileList = []
    }
  }
}
</script>

<style lang="scss" scoped>
.page-container { padding: 4px; }
.form-card { border-radius: 8px; }
::v-deep .el-upload--picture-card {
  display: flex; align-items: center; justify-content: center;
  line-height: normal;
}
.upload-trigger {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  i { font-size: 28px; color: #2d8a56; }
  span { font-size: 12px; color: #909399; margin-top: 4px; }
}
.upload-tip { font-size: 12px; color: #909399; margin-top: 4px; }
</style>
