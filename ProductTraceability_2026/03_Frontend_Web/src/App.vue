<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script>
export default {
  name: 'App'
}
</script>

<style lang="scss">
/* ===== Base Reset ===== */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  height: 100%;
  overflow: hidden;
}

#app {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  height: 100%;
}

/* ===== Element UI Green Theme Overrides ===== */
$green-primary: #2d8a56;
$green-light: #3ca66b;
$green-lighter: #e8f5ee;
$green-dark: #1a6b3a;

/* Primary buttons */
.el-button--primary {
  background-color: $green-primary;
  border-color: $green-primary;

  &:hover, &:focus {
    background-color: $green-light;
    border-color: $green-light;
  }

  &:active {
    background-color: $green-dark;
    border-color: $green-dark;
  }

  &.is-plain {
    color: $green-primary;
    background: $green-lighter;
    border-color: #b3e0c6;

    &:hover, &:focus {
      color: #fff;
      background: $green-primary;
      border-color: $green-primary;
    }
  }
}

/* Links */
.el-link--primary {
  color: $green-primary;

  &:hover {
    color: $green-light;
  }
}

/* Checkbox/Radio */
.el-checkbox__input.is-checked .el-checkbox__inner,
.el-radio__input.is-checked .el-radio__inner {
  background-color: $green-primary;
  border-color: $green-primary;
}

.el-checkbox__input.is-checked + .el-checkbox__label,
.el-radio__input.is-checked + .el-radio__label {
  color: $green-primary;
}

/* Switch */
.el-switch.is-checked .el-switch__core {
  border-color: $green-primary;
  background-color: $green-primary;
}

/* Pagination */
.el-pagination.is-background .el-pager li:not(.disabled).active {
  background-color: $green-primary;
}

/* Tabs */
.el-tabs__item.is-active {
  color: $green-primary;
}

.el-tabs__active-bar {
  background-color: $green-primary;
}

/* Dropdown active item */
.el-dropdown-menu__item:focus,
.el-dropdown-menu__item:not(.is-disabled):hover {
  background-color: $green-lighter;
  color: $green-primary;
}

/* Message box confirm button */
.el-message-box__btns .el-button--primary {
  background-color: $green-primary;
  border-color: $green-primary;
}

/* Breadcrumb link color */
.el-breadcrumb__inner a:hover,
.el-breadcrumb__inner.is-link:hover {
  color: $green-primary;
}

/* Card subtle border */
.el-card {
  border-color: #e8ede9;
}

/* Descriptions label bg */
.el-descriptions .el-descriptions-item__label {
  background-color: #f5faf7;
}

/* Scrollbar */
.el-scrollbar__thumb {
  background-color: rgba(45, 138, 86, 0.3);

  &:hover {
    background-color: rgba(45, 138, 86, 0.5);
  }
}

/* Tag primary */
.el-tag--primary {
  background-color: $green-lighter;
  border-color: #b3e0c6;
  color: $green-primary;
}

/* Input focus border */
.el-input__inner:focus,
.el-textarea__inner:focus {
  border-color: $green-primary;
}
</style>
