import request from './request'

export function fetchBeerCoinOverview() {
  return request.get('/api/admin/beer-coins', { authScope: 'admin' })
}

export function fetchBeerCoinWallet() {
  return request.get('/api/admin/beer-coins/wallet', { authScope: 'admin' })
}

export function fetchActiveBeerCoinPricing() {
  return request.get('/api/admin/beer-coins/pricing', { authScope: 'admin' })
}

export function saveBeerCoinPricing(payload) {
  return request.post('/api/admin/beer-coins/pricing', payload, { authScope: 'admin' })
}

export function createBeerCoinPurchaseOrder(payload) {
  return request.post('/api/admin/beer-coins/purchase-orders', payload, { authScope: 'admin' })
}

export function fetchBeerCoinPurchaseOrders() {
  return request.get('/api/admin/beer-coins/purchase-orders', { authScope: 'admin' })
}

export function fetchBeerCoinPurchaseOrder(id) {
  return request.get(`/api/admin/beer-coins/purchase-orders/${id}`, { authScope: 'admin' })
}

export function createBeerCoinNativePayment(id) {
  return request.post(`/api/admin/beer-coins/purchase-orders/${id}/wechat/native`, {}, { authScope: 'admin' })
}

export function createBeerCoinJsapiPayment(id, code) {
  return request.post(`/api/admin/beer-coins/purchase-orders/${id}/wechat/jsapi`, { code }, { authScope: 'admin' })
}

export function fetchBeerCoinWechatClientConfig() {
  return request.get('/api/admin/beer-coins/wechat/client-config', { authScope: 'admin' })
}

export function simulateBeerCoinPayment(id) {
  return request.post(`/api/admin/beer-coins/purchase-orders/${id}/simulate`, {}, { authScope: 'admin' })
}

export function fetchBeerCoinPurchaseOrderStatus(id) {
  return request.get(`/api/admin/beer-coins/purchase-orders/${id}/status`, { authScope: 'admin' })
}

export function fetchBeerCoinLedger() {
  return request.get('/api/admin/beer-coins/ledger', { authScope: 'admin' })
}

export function fetchBeerCoinLots() {
  return request.get('/api/admin/beer-coins/lots', { authScope: 'admin' })
}

export function adjustBeerCoin(payload) {
  return request.post('/api/admin/beer-coins/adjustments', payload, { authScope: 'admin' })
}
