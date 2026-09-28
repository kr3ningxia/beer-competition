import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
// ElMessage / ElMessageBox 以函数方式直接 import，unplugin-vue-components 只会为
// 模板中出现的组件注入样式，这里需手动补上这两个服务组件的样式。
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'

function syncVisualViewport() {
  const viewport = window.visualViewport
  const rootStyle = document.documentElement.style
  rootStyle.setProperty('--app-viewport-width', `${viewport?.width || window.innerWidth}px`)
  rootStyle.setProperty('--app-viewport-height', `${viewport?.height || window.innerHeight}px`)
  rootStyle.setProperty('--app-viewport-offset-left', `${viewport?.offsetLeft || 0}px`)
  rootStyle.setProperty('--app-viewport-offset-top', `${viewport?.offsetTop || 0}px`)
}

syncVisualViewport()
window.addEventListener('resize', syncVisualViewport, { passive: true })
window.visualViewport?.addEventListener('resize', syncVisualViewport, { passive: true })
window.visualViewport?.addEventListener('scroll', syncVisualViewport, { passive: true })

createApp(App)
  .use(router)
  .mount('#app')
