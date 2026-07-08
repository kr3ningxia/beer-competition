export function isWechatBrowser() {
  if (typeof navigator === 'undefined') return false
  return /MicroMessenger/i.test(navigator.userAgent || '')
}

export function currentUrlWithoutWechatCode() {
  const url = new URL(window.location.href)
  url.searchParams.delete('code')
  url.searchParams.delete('state')
  return url.toString()
}

export function buildWechatOauthUrl(appId) {
  const redirectUri = encodeURIComponent(currentUrlWithoutWechatCode())
  const state = `pay_${Date.now()}`
  return `https://open.weixin.qq.com/connect/oauth2/authorize?appid=${encodeURIComponent(appId)}&redirect_uri=${redirectUri}&response_type=code&scope=snsapi_base&state=${state}#wechat_redirect`
}

export function invokeWechatPay(params) {
  return new Promise((resolve, reject) => {
    const payload = {
      appId: params.appId,
      timeStamp: params.timeStamp,
      nonceStr: params.nonceStr,
      package: params.packageValue,
      signType: params.signType,
      paySign: params.paySign,
    }
    const invoke = () => {
      window.WeixinJSBridge.invoke('getBrandWCPayRequest', payload, (result) => {
        if (result?.err_msg === 'get_brand_wcpay_request:ok') {
          resolve(result)
          return
        }
        reject(new Error(resolveWechatPayError(result?.err_msg)))
      })
    }
    if (typeof window.WeixinJSBridge === 'undefined') {
      document.addEventListener('WeixinJSBridgeReady', invoke, { once: true })
      return
    }
    invoke()
  })
}

function resolveWechatPayError(message = '') {
  if (message.includes('cancel')) return '已取消微信支付'
  if (message.includes('fail')) return '微信支付未完成，请稍后重试'
  return '微信支付未完成'
}
