const { defineConfig } = require('@vue/cli-service')

const backendUrl = process.env.VUE_APP_BACKEND_URL || 'http://localhost:8888'

module.exports = defineConfig({
  transpileDependencies: true,
  lintOnSave: false,
  devServer: {
    port: 8080,
    open: true,
    proxy: {
      '/api': {
        target: backendUrl,
        changeOrigin: true
      },
      '/auth': {
        target: backendUrl,
        changeOrigin: true
      },
      '/uploads': {
        target: backendUrl,
        changeOrigin: true
      }
    }
  }
})
