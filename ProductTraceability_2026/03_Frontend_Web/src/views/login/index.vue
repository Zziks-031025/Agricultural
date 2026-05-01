<template>
  <div class="login-container">
    <!-- 背景装饰 -->
    <div class="login-bg-decoration">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>

    <div class="login-content">
      <!-- 左侧品牌区 -->
      <div class="login-brand">
        <div class="brand-inner">
          <div class="brand-icon">
            <i class="el-icon-connection"></i>
          </div>
          <h1>农产品溯源管理系统</h1>
          <p>基于区块链技术的全链路农产品质量追溯平台</p>
          <div class="brand-features">
            <div class="feature-item">
              <i class="el-icon-lock"></i>
              <span>数据不可篡改</span>
            </div>
            <div class="feature-item">
              <i class="el-icon-view"></i>
              <span>全程可追溯</span>
            </div>
            <div class="feature-item">
              <i class="el-icon-s-check"></i>
              <span>质量有保障</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧登录表单 -->
      <div class="login-form-wrapper">
        <el-form
          ref="loginForm"
          :model="loginForm"
          :rules="loginRules"
          class="login-form"
          autocomplete="on"
        >
          <div class="form-header">
            <h2>欢迎登录</h2>
            <p>请输入您的账号和密码</p>
          </div>

          <el-form-item prop="username">
            <el-input
              ref="username"
              v-model="loginForm.username"
              placeholder="请输入用户名"
              prefix-icon="el-icon-user"
              tabindex="1"
              autocomplete="on"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              ref="password"
              v-model="loginForm.password"
              :type="passwordVisible ? 'text' : 'password'"
              placeholder="请输入密码"
              prefix-icon="el-icon-lock"
              tabindex="2"
              autocomplete="on"
              @keyup.enter.native="handleLogin"
            >
              <i
                slot="suffix"
                :class="passwordVisible ? 'el-icon-view' : 'el-icon-hide'"
                class="password-toggle"
                @click="passwordVisible = !passwordVisible"
              />
            </el-input>
          </el-form-item>

          <el-form-item prop="captcha">
            <div class="captcha-row">
              <el-input
                ref="captcha"
                v-model="loginForm.captcha"
                placeholder="请输入验证码"
                prefix-icon="el-icon-key"
                tabindex="3"
                @keyup.enter.native="handleLogin"
              />
              <canvas
                ref="captchaCanvas"
                class="captcha-canvas"
                width="120"
                height="40"
                @click="refreshCaptcha"
              />
            </div>
          </el-form-item>

          <el-button
            :loading="loading"
            type="primary"
            class="login-btn"
            tabindex="4"
            @click.native.prevent="handleLogin"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>

          <div class="login-tip">
            <span>提示: 不同账号登录后看到的菜单不同</span>
          </div>
          <div class="register-link">
            <span>还没有账号？</span>
            <router-link to="/register">企业入驻申请</router-link>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Login',
  data() {
    const validateCaptcha = (rule, value, callback) => {
      if (!value) {
        callback(new Error('请输入验证码'))
      } else if (value.toLowerCase() !== this.captchaText.toLowerCase()) {
        callback(new Error('验证码不正确'))
      } else {
        callback()
      }
    }
    return {
      loginForm: {
        username: '',
        password: '',
        captcha: ''
      },
      loginRules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 5, message: '密码长度不能少于5位', trigger: 'blur' }
        ],
        captcha: [{ required: true, validator: validateCaptcha, trigger: 'blur' }]
      },
      loading: false,
      passwordVisible: false,
      captchaText: '',
      redirect: undefined
    }
  },
  watch: {
    $route: {
      handler(route) {
        this.redirect = route.query && route.query.redirect
      },
      immediate: true
    }
  },
  mounted() {
    this.refreshCaptcha()
    if (this.loginForm.username === '') {
      this.$refs.username.focus()
    }
  },
  methods: {
    /** 生成随机验证码并绘制到 canvas */
    refreshCaptcha() {
      const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789'
      let text = ''
      for (let i = 0; i < 4; i++) {
        text += chars.charAt(Math.floor(Math.random() * chars.length))
      }
      this.captchaText = text
      this.loginForm.captcha = ''

      this.$nextTick(() => {
        const canvas = this.$refs.captchaCanvas
        if (!canvas) return
        const ctx = canvas.getContext('2d')
        const width = canvas.width
        const height = canvas.height

        // 背景
        ctx.fillStyle = '#f0f0f0'
        ctx.fillRect(0, 0, width, height)

        // 干扰线
        for (let i = 0; i < 4; i++) {
          ctx.strokeStyle = this.randomColor(100, 200)
          ctx.beginPath()
          ctx.moveTo(Math.random() * width, Math.random() * height)
          ctx.lineTo(Math.random() * width, Math.random() * height)
          ctx.stroke()
        }

        // 干扰点
        for (let i = 0; i < 30; i++) {
          ctx.fillStyle = this.randomColor(0, 255)
          ctx.beginPath()
          ctx.arc(Math.random() * width, Math.random() * height, 1, 0, 2 * Math.PI)
          ctx.fill()
        }

        // 文字
        const fontSize = 24
        ctx.font = `bold ${fontSize}px Arial`
        ctx.textBaseline = 'middle'
        for (let i = 0; i < text.length; i++) {
          ctx.fillStyle = this.randomColor(30, 120)
          ctx.save()
          ctx.translate(18 + i * 25, height / 2)
          ctx.rotate((Math.random() - 0.5) * 0.4)
          ctx.fillText(text[i], 0, 0)
          ctx.restore()
        }
      })
    },

    randomColor(min, max) {
      const r = Math.floor(Math.random() * (max - min) + min)
      const g = Math.floor(Math.random() * (max - min) + min)
      const b = Math.floor(Math.random() * (max - min) + min)
      return `rgb(${r},${g},${b})`
    },

    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          this.loading = true
          this.$store.dispatch('user/login', this.loginForm)
            .then(() => {
              this.$router.push({ path: this.redirect || '/' }).catch(() => {})
              this.loading = false
            })
            .catch(() => {
              this.loading = false
              this.refreshCaptcha()
            })
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.login-container {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #1a3a2a 0%, #2d5a3f 50%, #1a4a35 100%);
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-bg-decoration {
  position: absolute;
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

.login-content {
  display: flex;
  width: 900px;
  min-height: 520px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  z-index: 1;
}

.login-brand {
  width: 400px;
  background: linear-gradient(160deg, #2d8a56 0%, #1a6b3a 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;

  .brand-inner {
    text-align: center;
  }

  .brand-icon {
    width: 80px;
    height: 80px;
    margin: 0 auto 24px;
    background: rgba(255, 255, 255, 0.15);
    border-radius: 20px;
    display: flex;
    align-items: center;
    justify-content: center;

    i {
      font-size: 40px;
      color: #fff;
    }
  }

  h1 {
    font-size: 24px;
    font-weight: 600;
    margin-bottom: 12px;
  }

  p {
    font-size: 14px;
    opacity: 0.8;
    margin-bottom: 40px;
    line-height: 1.6;
  }

  .brand-features {
    display: flex;
    flex-direction: column;
    gap: 16px;

    .feature-item {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 14px;
      opacity: 0.9;

      i {
        font-size: 18px;
      }
    }
  }
}

.login-form-wrapper {
  flex: 1;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.login-form {
  width: 100%;
  max-width: 360px;

  .form-header {
    margin-bottom: 32px;

    h2 {
      font-size: 24px;
      color: #303133;
      margin: 0 0 8px 0;
    }

    p {
      font-size: 14px;
      color: #909399;
      margin: 0;
    }
  }

  .el-form-item {
    margin-bottom: 22px;
  }
}

.captcha-row {
  display: flex;
  gap: 12px;

  .el-input {
    flex: 1;
  }

  .captcha-canvas {
    cursor: pointer;
    border-radius: 4px;
    border: 1px solid #DCDFE6;
    flex-shrink: 0;

    &:hover {
      border-color: #2d8a56;
    }
  }
}

.password-toggle {
  cursor: pointer;
  color: #C0C4CC;
  line-height: 40px;
  padding-right: 4px;

  &:hover {
    color: #2d8a56;
  }
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  border-radius: 4px;
  margin-top: 4px;
}

.login-tip {
  text-align: center;
  margin-top: 20px;
  font-size: 12px;
  color: #C0C4CC;
}

.register-link {
  text-align: center;
  margin-top: 12px;
  font-size: 13px;
  color: #909399;

  a {
    color: #2d8a56;
    text-decoration: none;
    font-weight: 500;
    margin-left: 4px;

    &:hover {
      text-decoration: underline;
    }
  }
}
</style>
