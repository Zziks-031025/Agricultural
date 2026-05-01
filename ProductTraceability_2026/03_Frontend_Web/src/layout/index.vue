<template>
  <div class="app-wrapper" :class="{ 'sidebar-collapsed': isCollapse }">
    <div class="sidebar-container">
      <sidebar :collapse="isCollapse" />
    </div>
    <div class="main-container">
      <navbar :is-collapse="isCollapse" @toggle-sidebar="toggleSidebar" />
      <app-main />
    </div>
  </div>
</template>

<script>
import Sidebar from './components/Sidebar/index.vue'
import Navbar from './components/Navbar'
import AppMain from './components/AppMain'

export default {
  name: 'Layout',
  components: {
    Sidebar,
    Navbar,
    AppMain
  },
  data() {
    return {
      isCollapse: false
    }
  },
  methods: {
    toggleSidebar() {
      this.isCollapse = !this.isCollapse
    }
  }
}
</script>

<style lang="scss" scoped>
.app-wrapper {
  display: flex;
  width: 100%;
  height: 100%;

  .sidebar-container {
    width: 210px;
    height: 100%;
    transition: width 0.3s;
    flex-shrink: 0;
  }

  .main-container {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-width: 0;
    height: 100%;
    overflow: hidden;
  }

  &.sidebar-collapsed .sidebar-container {
    width: 64px;
  }
}
</style>
