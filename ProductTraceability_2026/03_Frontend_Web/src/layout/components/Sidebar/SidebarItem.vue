<template>
  <div v-if="!item.hidden">
    <!-- 只有一个子路由或无子路由: 显示为普通菜单项 -->
    <template v-if="hasOneShowingChild(item.children, item)">
      <el-menu-item
        v-if="onlyOneChild.meta"
        :index="resolvePath(onlyOneChild.path)"
        @click="handleClick(resolvePath(onlyOneChild.path))"
      >
        <i :class="onlyOneChild.meta.icon || (item.meta && item.meta.icon) || 'el-icon-menu'" />
        <span slot="title">{{ onlyOneChild.meta.title }}</span>
      </el-menu-item>
    </template>

    <!-- 多个子路由: 显示为子菜单 -->
    <el-submenu v-else :index="resolvePath(item.path)">
      <template slot="title">
        <i :class="(item.meta && item.meta.icon) || 'el-icon-menu'" />
        <span>{{ item.meta && item.meta.title }}</span>
      </template>
      <sidebar-item
        v-for="child in item.children"
        :key="child.path"
        :item="child"
        :base-path="resolvePath(child.path)"
      />
    </el-submenu>
  </div>
</template>

<script>
export default {
  name: 'SidebarItem',
  props: {
    item: {
      type: Object,
      required: true
    },
    basePath: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      onlyOneChild: null
    }
  },
  methods: {
    hasOneShowingChild(children = [], parent) {
      const showingChildren = children.filter(item => {
        if (item.hidden) {
          return false
        }
        this.onlyOneChild = item
        return true
      })

      if (showingChildren.length === 1) {
        return true
      }

      if (showingChildren.length === 0) {
        this.onlyOneChild = { ...parent, path: '', noShowingChildren: true }
        return true
      }

      return false
    },

    resolvePath(routePath) {
      if (this.isExternal(routePath)) {
        return routePath
      }
      if (this.isExternal(this.basePath)) {
        return this.basePath
      }
      // 简易路径拼接, 替代 Node.js path.resolve
      const base = this.basePath.replace(/\/+$/, '')
      const child = routePath.replace(/^\/+/, '')
      if (!child) return base || '/'
      if (routePath.startsWith('/')) return routePath
      return base + '/' + child
    },

    isExternal(p) {
      return /^(https?:|mailto:|tel:)/.test(p)
    },

    handleClick(p) {
      if (this.$route.path !== p) {
        this.$router.push(p)
      }
    }
  }
}
</script>
