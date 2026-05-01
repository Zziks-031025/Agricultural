<template>
  <div class="register-container">
    <div class="register-bg-decoration">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>

    <div class="register-content">
      <!-- 左侧品牌区 -->
      <div class="register-brand">
        <div class="brand-inner">
          <div class="brand-icon">
            <i class="el-icon-office-building"></i>
          </div>
          <h1>企业入驻申请</h1>
          <p>填写企业信息，提交审核后即可使用系统</p>
          <div class="brand-steps">
            <div class="step-item" :class="{ active: currentStep >= 1 }">
              <div class="step-num">1</div>
              <span>基本信息</span>
            </div>
            <div class="step-item" :class="{ active: currentStep >= 2 }">
              <div class="step-num">2</div>
              <span>证照上传</span>
            </div>
            <div class="step-item" :class="{ active: currentStep >= 3 }">
              <div class="step-num">3</div>
              <span>账号设置</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧表单 -->
      <div class="register-form-wrapper">
        <el-form ref="registerForm" :model="form" :rules="rules" label-width="130px" size="small" class="register-form">
          <div class="form-header">
            <h2>{{ stepTitle }}</h2>
            <p>{{ stepDesc }}</p>
          </div>

          <!-- Step 1: 基本信息 -->
          <div v-show="currentStep === 1">
            <el-form-item label="企业名称" prop="enterpriseName">
              <el-input v-model="form.enterpriseName" placeholder="请输入企业全称" />
            </el-form-item>
            <el-form-item label="统一社会信用代码" prop="enterpriseCode">
              <el-input v-model="form.enterpriseCode" placeholder="18位统一社会信用代码" maxlength="18" />
            </el-form-item>
            <el-form-item label="企业类型" prop="enterpriseType">
              <el-select v-model="form.enterpriseType" placeholder="请选择企业类型" style="width: 100%">
                <el-option :value="1" label="种植养殖企业" />
                <el-option :value="2" label="加工宰杀企业" />
                <el-option :value="3" label="检疫质检机构" />
              </el-select>
            </el-form-item>
            <el-form-item label="法人代表" prop="legalPerson">
              <el-input v-model="form.legalPerson" placeholder="法人代表姓名" />
            </el-form-item>
            <el-form-item label="联系人" prop="contactPerson">
              <el-input v-model="form.contactPerson" placeholder="联系人姓名" />
            </el-form-item>
            <el-form-item label="联系电话" prop="contactPhone">
              <el-input v-model="form.contactPhone" placeholder="11位手机号" maxlength="11" />
            </el-form-item>
            <el-form-item label="联系邮箱">
              <el-input v-model="form.contactEmail" placeholder="企业邮箱（选填）" />
            </el-form-item>
            <el-form-item label="所在省份">
              <el-input v-model="form.province" placeholder="如：山东省" />
            </el-form-item>
            <el-form-item label="所在城市">
              <el-input v-model="form.city" placeholder="如：济南市" />
            </el-form-item>
            <el-form-item label="所在区县">
              <el-input v-model="form.district" placeholder="如：历下区" />
            </el-form-item>
            <el-form-item label="详细地址" prop="address">
              <el-input v-model="form.address" placeholder="详细街道地址" />
            </el-form-item>
            <el-form-item label="企业简介">
              <el-input v-model="form.introduction" type="textarea" :rows="3" placeholder="企业简介（选填）" maxlength="500" show-word-limit />
            </el-form-item>
          </div>

          <!-- Step 2: 证照上传 -->
          <div v-show="currentStep === 2">
            <el-form-item label="企业Logo" prop="logo">
              <div class="upload-area">
                <el-upload
                  action="#"
                  :auto-upload="false"
                  :show-file-list="false"
                  accept="image/*"
                  :on-change="(f) => handleImageChange(f, 'logo')"
                >
                  <img v-if="form.logo" :src="form.logo" class="upload-preview" />
                  <div v-else class="upload-placeholder">
                    <i class="el-icon-picture-outline"></i>
                    <span>上传Logo</span>
                  </div>
                </el-upload>
                <span class="upload-hint">建议尺寸 200x200，支持 JPG/PNG</span>
              </div>
            </el-form-item>

            <el-form-item label="营业执照" prop="businessLicense">
              <div class="upload-area">
                <el-upload
                  action="#"
                  :auto-upload="false"
                  :show-file-list="false"
                  accept="image/*"
                  :on-change="(f) => handleImageChange(f, 'businessLicense')"
                >
                  <img v-if="form.businessLicense" :src="form.businessLicense" class="upload-preview license-preview" />
                  <div v-else class="upload-placeholder license-placeholder">
                    <i class="el-icon-upload2"></i>
                    <span>上传营业执照</span>
                  </div>
                </el-upload>
                <span class="upload-hint">请上传清晰的营业执照照片</span>
              </div>
            </el-form-item>

            <el-form-item label="行业许可证" prop="productionLicense">
              <div class="upload-area">
                <el-upload
                  action="#"
                  :auto-upload="false"
                  :show-file-list="false"
                  accept="image/*"
                  :on-change="(f) => handleImageChange(f, 'productionLicense')"
                >
                  <img v-if="form.productionLicense" :src="form.productionLicense" class="upload-preview license-preview" />
                  <div v-else class="upload-placeholder license-placeholder">
                    <i class="el-icon-upload2"></i>
                    <span>上传行业许可证</span>
                  </div>
                </el-upload>
                <span class="upload-hint">食品经营许可证/动物防疫条件合格证等</span>
              </div>
            </el-form-item>

            <el-form-item label="企业背景图">
              <div class="upload-area">
                <el-upload
                  action="#"
                  :auto-upload="false"
                  :show-file-list="false"
                  accept="image/*"
                  :on-change="(f) => handleImageChange(f, 'coverImage')"
                >
                  <img v-if="form.coverImage" :src="form.coverImage" class="upload-preview cover-preview" />
                  <div v-else class="upload-placeholder cover-placeholder">
                    <i class="el-icon-picture"></i>
                    <span>上传企业背景图（选填）</span>
                  </div>
                </el-upload>
                <span class="upload-hint">建议尺寸 1200x400，用于企业主页展示</span>
              </div>
            </el-form-item>
          </div>

          <!-- Step 3: 账号设置 -->
          <div v-show="currentStep === 3">
            <el-form-item label="管理员账号" prop="username">
              <el-input v-model="form.username" placeholder="用于登录系统的管理员账号" />
            </el-form-item>
            <el-form-item label="登录密码" prop="password">
              <el-input v-model="form.password" type="password" placeholder="不少于6位" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="form.confirmPassword" type="password" placeholder="再次输入密码" show-password />
            </el-form-item>

            <div class="submit-summary">
              <h4>请确认以下信息</h4>
              <el-descriptions :column="1" border size="mini">
                <el-descriptions-item label="企业名称">{{ form.enterpriseName }}</el-descriptions-item>
                <el-descriptions-item label="信用代码">{{ form.enterpriseCode }}</el-descriptions-item>
                <el-descriptions-item label="企业类型">{{ enterpriseTypeLabel }}</el-descriptions-item>
                <el-descriptions-item label="联系人">{{ form.contactPerson }} / {{ form.contactPhone }}</el-descriptions-item>
                <el-descriptions-item label="地址">{{ form.province }}{{ form.city }}{{ form.district }} {{ form.address }}</el-descriptions-item>
                <el-descriptions-item label="证照">
                  <el-tag v-if="form.businessLicense" type="success" size="mini">营业执照 ✓</el-tag>
                  <el-tag v-else type="info" size="mini">营业执照 未上传</el-tag>
                  <el-tag v-if="form.productionLicense" type="success" size="mini" style="margin-left: 6px">许可证 ✓</el-tag>
                  <el-tag v-else type="info" size="mini" style="margin-left: 6px">许可证 未上传</el-tag>
                  <el-tag v-if="form.logo" type="success" size="mini" style="margin-left: 6px">Logo ✓</el-tag>
                </el-descriptions-item>
              </el-descriptions>
            </div>
          </div>

          <!-- 底部操作栏 -->
          <div class="form-actions">
            <el-button v-if="currentStep > 1" size="small" @click="prevStep">上一步</el-button>
            <el-button v-if="currentStep < 3" type="primary" size="small" @click="nextStep">下一步</el-button>
            <el-button v-if="currentStep === 3" type="primary" size="small" :loading="submitting" @click="handleSubmit">
              {{ submitting ? '提交中...' : '提交注册申请' }}
            </el-button>
            <el-button type="text" size="small" @click="goLogin" style="margin-left: 16px">已有账号？去登录</el-button>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script>
import { registerEnterprise, uploadFile } from '@/api/enterprise'

export default {
  name: 'Register',
  data() {
    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== this.form.password) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }
    return {
      currentStep: 1,
      submitting: false,
      form: {
        enterpriseName: '',
        enterpriseCode: '',
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
        businessLicense: '',
        productionLicense: '',
        coverImage: '',
        username: '',
        password: '',
        confirmPassword: ''
      },
      // 存储待上传的文件对象
      pendingFiles: {
        logo: null,
        businessLicense: null,
        productionLicense: null,
        coverImage: null
      },
      rules: {
        enterpriseName: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
        enterpriseCode: [
          { required: true, message: '请输入统一社会信用代码', trigger: 'blur' },
          { len: 18, message: '统一社会信用代码为18位', trigger: 'blur' }
        ],
        enterpriseType: [{ required: true, message: '请选择企业类型', trigger: 'change' }],
        legalPerson: [{ required: true, message: '请输入法人代表', trigger: 'blur' }],
        contactPerson: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
        contactPhone: [
          { required: true, message: '请输入联系电话', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
        ],
        address: [{ required: true, message: '请输入详细地址', trigger: 'blur' }],
        logo: [{ required: true, message: '请上传企业Logo', trigger: 'change' }],
        businessLicense: [{ required: true, message: '请上传营业执照', trigger: 'change' }],
        productionLicense: [{ required: true, message: '请上传行业许可证', trigger: 'change' }],
        username: [
          { required: true, message: '请输入管理员账号', trigger: 'blur' },
          { min: 3, max: 20, message: '账号长度为3-20位', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请确认密码', trigger: 'blur' },
          { validator: validateConfirmPassword, trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    stepTitle() {
      const titles = { 1: '填写企业基本信息', 2: '上传企业证照', 3: '设置管理员账号' }
      return titles[this.currentStep]
    },
    stepDesc() {
      const descs = {
        1: '请填写真实的企业信息，便于管理员审核',
        2: '上传营业执照、许可证等资质文件',
        3: '设置企业管理员的登录账号和密码'
      }
      return descs[this.currentStep]
    },
    enterpriseTypeLabel() {
      const map = { 1: '种植养殖企业', 2: '加工宰杀企业', 3: '检疫质检机构' }
      return map[this.form.enterpriseType] || '未选择'
    }
  },
  methods: {
    async handleImageChange(file, field) {
      if (file.raw.size > 10 * 1024 * 1024) {
        this.$message.error('图片大小不能超过10MB')
        return
      }
      // 本地预览
      this.form[field] = URL.createObjectURL(file.raw)
      this.pendingFiles[field] = file.raw
      // 触发表单验证
      this.$refs.registerForm.validateField(field)
    },

    nextStep() {
      if (this.currentStep === 1) {
        const step1Fields = ['enterpriseName', 'enterpriseCode', 'enterpriseType', 'legalPerson', 'contactPerson', 'contactPhone', 'address']
        let allValid = true
        let pending = step1Fields.length
        step1Fields.forEach(f => {
          this.$refs.registerForm.validateField(f, err => {
            if (err) allValid = false
            pending--
            if (pending === 0 && allValid) {
              this.currentStep = 2
            }
          })
        })
      } else if (this.currentStep === 2) {
        const step2Fields = ['logo', 'businessLicense', 'productionLicense']
        let allValid = true
        let pending = step2Fields.length
        step2Fields.forEach(f => {
          this.$refs.registerForm.validateField(f, err => {
            if (err) allValid = false
            pending--
            if (pending === 0 && allValid) {
              this.currentStep = 3
            }
          })
        })
      }
    },

    prevStep() {
      if (this.currentStep > 1) this.currentStep--
    },

    async handleSubmit() {
      // 验证第三步字段
      const step3Fields = ['username', 'password', 'confirmPassword']
      const valid = await new Promise(resolve => {
        let allValid = true
        let pending = step3Fields.length
        step3Fields.forEach(f => {
          this.$refs.registerForm.validateField(f, err => {
            if (err) allValid = false
            pending--
            if (pending === 0) resolve(allValid)
          })
        })
      })
      if (!valid) return

      this.submitting = true
      try {
        // 1. 先上传图片
        const uploadFields = ['logo', 'businessLicense', 'productionLicense', 'coverImage']
        for (const field of uploadFields) {
          if (this.pendingFiles[field]) {
            const res = await uploadFile(this.pendingFiles[field], 'enterprise')
            this.form[field] = res.data.url
          }
        }

        // 2. 提交注册
        const submitData = {
          enterpriseName: this.form.enterpriseName,
          enterpriseCode: this.form.enterpriseCode,
          enterpriseType: this.form.enterpriseType,
          legalPerson: this.form.legalPerson,
          contactPerson: this.form.contactPerson,
          contactPhone: this.form.contactPhone,
          contactEmail: this.form.contactEmail || null,
          province: this.form.province || null,
          city: this.form.city || null,
          district: this.form.district || null,
          address: this.form.address,
          introduction: this.form.introduction || null,
          logo: this.form.logo || null,
          businessLicense: this.form.businessLicense || null,
          productionLicense: this.form.productionLicense || null,
          coverImage: this.form.coverImage || null,
          username: this.form.username,
          password: this.form.password
        }

        await registerEnterprise(submitData)
        this.$alert('企业入驻申请已提交成功！管理员将在1-3个工作日内完成审核，审核结果将通过系统消息通知您。', '注册成功', {
          confirmButtonText: '去登录',
          type: 'success',
          callback: () => {
            this.$router.push('/login')
          }
        })
      } catch (e) {
        // error handled by interceptor
      } finally {
        this.submitting = false
      }
    },

    goLogin() {
      this.$router.push('/login')
    }
  }
}
</script>

<style lang="scss" scoped>
.register-container {
  width: 100%;
  min-height: 100vh;
  background: linear-gradient(135deg, #1a3a2a 0%, #2d5a3f 50%, #1a4a35 100%);
  position: relative;
  overflow: auto;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.register-bg-decoration {
  position: fixed;
  width: 100%;
  height: 100%;
  pointer-events: none;
  .circle {
    position: absolute;
    border-radius: 50%;
    opacity: 0.06;
    background: #fff;
  }
  .circle-1 { width: 600px; height: 600px; top: -200px; right: -100px; }
  .circle-2 { width: 400px; height: 400px; bottom: -100px; left: -100px; }
  .circle-3 { width: 200px; height: 200px; top: 50%; left: 50%; }
}

.register-content {
  display: flex;
  width: 1000px;
  min-height: 680px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  z-index: 1;
}

.register-brand {
  width: 320px;
  background: linear-gradient(160deg, #2d8a56 0%, #1a6b3a 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 30px;
  flex-shrink: 0;

  .brand-inner { text-align: center; }

  .brand-icon {
    width: 70px;
    height: 70px;
    margin: 0 auto 20px;
    background: rgba(255, 255, 255, 0.15);
    border-radius: 18px;
    display: flex;
    align-items: center;
    justify-content: center;
    i { font-size: 36px; color: #fff; }
  }

  h1 { font-size: 22px; font-weight: 600; margin-bottom: 10px; }
  p { font-size: 13px; opacity: 0.8; margin-bottom: 40px; line-height: 1.6; }

  .brand-steps {
    display: flex;
    flex-direction: column;
    gap: 20px;
    text-align: left;

    .step-item {
      display: flex;
      align-items: center;
      gap: 12px;
      font-size: 14px;
      opacity: 0.5;
      transition: opacity 0.3s;

      &.active { opacity: 1; }

      .step-num {
        width: 30px;
        height: 30px;
        border-radius: 50%;
        border: 2px solid rgba(255, 255, 255, 0.5);
        display: flex;
        align-items: center;
        justify-content: center;
        font-weight: 600;
        font-size: 14px;
      }

      &.active .step-num {
        background: rgba(255, 255, 255, 0.2);
        border-color: #fff;
      }
    }
  }
}

.register-form-wrapper {
  flex: 1;
  background: #fff;
  padding: 30px 35px;
  overflow-y: auto;
  max-height: 90vh;
}

.register-form {
  .form-header {
    margin-bottom: 24px;
    h2 { font-size: 20px; color: #303133; margin: 0 0 6px 0; }
    p { font-size: 13px; color: #909399; margin: 0; }
  }
}

.form-actions {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #eee;
  display: flex;
  align-items: center;
}

// Upload
.upload-area {
  display: flex;
  align-items: flex-start;
  gap: 12px;

  .upload-hint {
    font-size: 12px;
    color: #909399;
    margin-top: 8px;
    display: block;
  }
}

.upload-placeholder, .upload-preview {
  border: 1px dashed #d9d9d9;
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.3s;
  &:hover { border-color: #2d8a56; }
}

.upload-placeholder {
  width: 120px;
  height: 120px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #fafafa;
  i { font-size: 28px; color: #c0c4cc; }
  span { font-size: 12px; color: #909399; margin-top: 6px; }
}

.license-placeholder {
  width: 200px;
  height: 140px;
}

.cover-placeholder {
  width: 280px;
  height: 100px;
}

.upload-preview {
  width: 120px;
  height: 120px;
  object-fit: cover;
}

.license-preview {
  width: 200px;
  height: 140px;
}

.cover-preview {
  width: 280px;
  height: 100px;
}

.submit-summary {
  margin-top: 16px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;

  h4 {
    font-size: 14px;
    color: #303133;
    margin: 0 0 12px 0;
  }
}
</style>
