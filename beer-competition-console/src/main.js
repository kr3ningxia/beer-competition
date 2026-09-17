import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

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
