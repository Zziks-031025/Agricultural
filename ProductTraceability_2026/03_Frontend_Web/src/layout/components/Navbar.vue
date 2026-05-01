<template>
  <div class="navbar">
    <div class="left-section">
      <i
        :class="isCollapse ? 'el-icon-s-unfold' : 'el-icon-s-fold'"
        class="hamburger"
        @click="$emit('toggle-sidebar')"
      />
      <el-breadcrumb separator="/" class="breadcrumb">
        <el-breadcrumb-item v-for="(item, index) in breadcrumbs" :key="index">
          <span v-if="index === breadcrumbs.length - 1" class="no-link">{{ item.meta.title }}</span>
          <a v-else @click.prevent="handleLink(item)">{{ item.meta.title }}</a>
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="right-section">
      <el-badge :value="unreadTotal" :max="99" :hidden="unreadTotal === 0" class="message-badge">
        <i class="el-icon-bell message-bell" @click="goMessage" title="我的消息"></i>
      </el-badge>
      <span class="role-tag">{{ roleLabel }}</span>
      <el-dropdown trigger="click" @command="handleCommand">
        <span class="user-dropdown">
          <i class="el-icon-user-solid"></i>
          {{ displayName }}
          <i class="el-icon-arrow-down el-icon--right"></i>
        </span>
        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item command="dashboard">
            <i class="el-icon-s-home"></i> 首页
          </el-dropdown-item>
          <el-dropdown-item divided command="logout">
            <i class="el-icon-switch-button"></i> 退出登录
          </el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>
  </div>
</template>

<script>
import { getUnreadCount } from '@/api/message'

const ROLE_LABELS = {
  admin: '系统管理员',
  type1: '种植养殖企业',
  type2: '加工宰杀企业',
  type3: '检疫质检企业'
}

export default {
  name: 'Navbar',
  props: {
    isCollapse: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      unreadTotal: 0,
      unreadTimer: null
    }
  },
  created() {
    this.fetchUnread()
    this.unreadTimer = setInterval(() => {
      this.fetchUnread()
    }, 30000)
  },
  beforeDestroy() {
    if (this.unreadTimer) {
      clearInterval(this.unreadTimer)
    }
  },
  computed: {
    displayName() {
      return this.$store.getters.name || '用户'
    },
    roleLabel() {
      const roles = this.$store.getters.roles
      if (roles && roles.length > 0) {
        return ROLE_LABELS[roles[0]] || '未知角色'
      }
      return ''
    },
    breadcrumbs() {
      const matched = this.$route.matched.filter(item => item.meta && item.meta.title)
      return matched
    }
  },
  methods: {
    handleCommand(command) {
      if (command === 'logout') {
        this.$confirm('确认退出登录?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          this.$store.dispatch('user/logout').then(() => {
            this.$router.push('/login')
          })
        }).catch(() => {})
      } else if (command === 'dashboard') {
        this.$router.push('/')
      }
    },
    handleLink(item) {
      if (item.redirect) {
        this.$router.push(item.redirect)
      }
    },
    goMessage() {
      this.$router.push('/message')
    },
    async fetchUnread() {
      try {
        const res = await getUnreadCount()
        this.unreadTotal = (res.data && res.data.all) || 0
      } catch (e) {
        // ignore
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.navbar {
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  flex-shrink: 0;

  .left-section {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .hamburger {
    font-size: 20px;
    cursor: pointer;
    color: #1a3a2a;
    padding: 4px;
    transition: color 0.2s;

    &:hover {
      color: #2d8a56;
    }
  }

  .breadcrumb {
    font-size: 13px;

    .no-link {
      color: #97a8be;
      cursor: text;
    }
  }

  .right-section {
    display: flex;
    align-items: center;
    gap: 16px;

    .message-badge {
      margin-right: 4px;
    }

    .message-bell {
      font-size: 20px;
      cursor: pointer;
      color: #606266;
      transition: color 0.2s;

      &:hover {
        color: #2d8a56;
      }
    }

    .role-tag {
      font-size: 12px;
      color: #fff;
      background: linear-gradient(135deg, #2d8a56, #1a6b3a);
      padding: 3px 12px;
      border-radius: 10px;
    }

    .user-dropdown {
      display: flex;
      align-items: center;
      gap: 4px;
      cursor: pointer;
      color: #1a3a2a;
      font-size: 14px;
      transition: color 0.2s;

      &:hover {
        color: #2d8a56;
      }
    }
  }
}
</style>
