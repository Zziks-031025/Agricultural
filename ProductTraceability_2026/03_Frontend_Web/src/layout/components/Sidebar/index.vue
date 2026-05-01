<template>
  <div class="sidebar-wrapper">
    <!-- Logo -->
    <div class="sidebar-logo" :class="{ 'is-collapse': collapse }">
      <i class="el-icon-connection logo-icon"></i>
      <span v-show="!collapse" class="logo-text">农产品溯源</span>
    </div>

    <!-- Menu -->
    <el-scrollbar wrap-class="scrollbar-wrapper">
      <el-menu
        :default-active="activeMenu"
        :collapse="collapse"
        :background-color="'#1a3a2a'"
        :text-color="'#b3d4c4'"
        :active-text-color="'#5ee89d'"
        :unique-opened="true"
        :collapse-transition="false"
        mode="vertical"
      >
        <sidebar-item
          v-for="route in routes"
          :key="route.path"
          :item="route"
          :base-path="route.path"
        />
      </el-menu>
    </el-scrollbar>
  </div>
</template>

<script>
import SidebarItem from './SidebarItem.vue'

export default {
  name: 'Sidebar',
  components: { SidebarItem },
  props: {
    collapse: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    routes() {
      return this.$store.getters.permissionRoutes.filter(r => !r.hidden)
    },
    activeMenu() {
      const { meta, path } = this.$route
      if (meta && meta.activeMenu) {
        return meta.activeMenu
      }
      return path
    }
  }
}
</script>

<style lang="scss" scoped>
.sidebar-wrapper {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #1a3a2a;
}

.sidebar-logo {
  height: 50px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  background-color: #142e22;
  overflow: hidden;
  white-space: nowrap;

  .logo-icon {
    font-size: 24px;
    color: #5ee89d;
    flex-shrink: 0;
  }

  .logo-text {
    font-size: 16px;
    font-weight: 600;
    color: #fff;
    margin-left: 10px;
  }

  &.is-collapse {
    justify-content: center;
    padding: 0;
  }
}

.el-scrollbar {
  flex: 1;
  overflow: hidden;
}

::v-deep .scrollbar-wrapper {
  overflow-x: hidden !important;
}

::v-deep .el-menu {
  border-right: none;
}

/* Submenu title hover */
::v-deep .el-submenu__title:hover {
  background-color: #245038 !important;
}

/* Menu item hover */
::v-deep .el-menu-item:hover {
  background-color: #245038 !important;
}

/* Active menu item */
::v-deep .el-menu-item.is-active {
  background-color: #2d8a56 !important;
  color: #fff !important;
}
</style>
