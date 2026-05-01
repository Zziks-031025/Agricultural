<template>
  <div class="error-page">
    <div class="error-content">
      <h1>404</h1>
      <p>抱歉，您访问的页面不存在</p>
      <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
    </div>
  </div>
</template>

<script>
export default { name: 'Page404' }
</script>

<style lang="scss" scoped>
.error-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;

  .error-content {
    text-align: center;

    h1 {
      font-size: 100px;
      color: #409EFF;
      margin: 0;
    }

    p {
      font-size: 18px;
      color: #909399;
      margin: 16px 0 32px;
    }
  }
}
</style>
