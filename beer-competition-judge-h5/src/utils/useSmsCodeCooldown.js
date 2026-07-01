import { computed, onBeforeUnmount, ref } from 'vue'

export function useSmsCodeCooldown(durationSeconds = 60) {
  const sending = ref(false)
  const countdown = ref(0)
  let timer = null

  const sendButtonText = computed(() => {
    if (sending.value) return '发送中...'
    if (countdown.value > 0) return `${countdown.value}秒后重发`
    return '获取验证码'
  })

  function startCountdown() {
    countdown.value = durationSeconds
    window.clearInterval(timer)
    timer = window.setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0) {
        window.clearInterval(timer)
        timer = null
      }
    }, 1000)
  }

  onBeforeUnmount(() => window.clearInterval(timer))

  return {
    countdown,
    sending,
    sendButtonText,
    startCountdown,
  }
}
