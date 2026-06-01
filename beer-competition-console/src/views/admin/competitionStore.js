import { reactive } from 'vue'

export const stages = [
  { label: '草稿', status: 'DRAFT' },
  { label: '报名', status: 'REGISTRATION_OPEN' },
  { label: '收样', status: 'REGISTRATION_CLOSED' },
  { label: '评审准备', status: 'JUDGING_PREP' },
  { label: '评审中', status: 'JUDGING' },
  { label: '结果确认', status: 'RESULT_CONFIRMING' },
  { label: '结果发布', status: 'PUBLISHED' },
]

export const statusMeta = {
  DRAFT: { label: '草稿', tone: 'neutral', next: '完善报名配置' },
  REGISTRATION_OPEN: { label: '报名中', tone: 'success', next: '查看报名酒款' },
  REGISTRATION_CLOSED: { label: '报名截止', tone: 'warning', next: '检查入库情况' },
  JUDGING_PREP: { label: '评审准备', tone: 'gold', next: '检查评审桌' },
  JUDGING: { label: '评审中', tone: 'success', next: '查看现场进度' },
  RESULT_CONFIRMING: { label: '结果确认', tone: 'gold', next: '确认结果发布' },
  PUBLISHED: { label: '已发布', tone: 'neutral', next: '查看归档数据' },
  ARCHIVED: { label: '已归档', tone: 'neutral', next: '查看归档数据' },
}

export const checkItems = [
  { key: 'baseInfo', label: '基础信息' },
  { key: 'categories', label: '投递组别' },
  { key: 'styles', label: '基础风格' },
  { key: 'entryFields', label: '报名字段' },
  { key: 'judgeTables', label: '评审桌' },
  { key: 'scoreForms', label: '评分表' },
  { key: 'storedEntries', label: '酒款入库' },
  { key: 'resultSetup', label: '结果发布' },
]

export const roleLabels = {
  CROSS: '跨界评审',
  PROFESSIONAL: '专业评审',
  CAPTAIN: '桌长',
}

export const fieldTypeLabels = {
  text: '短文本',
  textarea: '长文本',
  number: '数字',
  select: '选项',
  multi_select: '多选',
}

export const competitions = reactive([
  {
    id: 1,
    code: 'BC-2026',
    name: '2026 中国精酿啤酒大赛',
    edition: '第三批次',
    date: '2026-08-20',
    registrationStart: '2026-06-10T10:00:00',
    registrationDeadline: '2026-07-30T18:00:00',
    entryFee: 199,
    status: 'JUDGING_PREP',
    categories: ['IPA', '拉格', '世涛', '酸啤', '特色实验'],
    baseStyles: ['American IPA', 'Double IPA', 'Pilsner', 'Imperial Stout', 'Mixed Fermentation Sour'],
    entryFields: [
      { key: 'specialIngredients', label: '增味原料或特殊工艺', type: 'textarea', required: false, visibleToJudges: true },
      { key: 'packageSpec', label: '包装规格', type: 'text', required: true, visibleToJudges: false },
    ],
    judgeTables: [
      { id: 'A', name: 'A桌', captain: 1, professional: 3, cross: 1, finalized: 68, total: 72 },
      { id: 'B', name: 'B桌', captain: 1, professional: 3, cross: 1, finalized: 61, total: 70 },
      { id: 'C', name: 'C桌', captain: 1, professional: 3, cross: 1, finalized: 54, total: 72 },
      { id: 'D', name: 'D桌', captain: 1, professional: 3, cross: 1, finalized: 55, total: 72 },
    ],
    scoreConfigs: [
      { role: 'CROSS', dimensions: [{ label: '整体感受', maxScore: 20 }, { label: '适饮性', maxScore: 20 }, { label: '记忆点', maxScore: 10 }] },
      { role: 'PROFESSIONAL', dimensions: [{ label: '香气', maxScore: 12 }, { label: '外观', maxScore: 3 }, { label: '味道', maxScore: 20 }, { label: '口感', maxScore: 5 }, { label: '整体印象', maxScore: 10 }] },
      { role: 'CAPTAIN', dimensions: [{ label: '共识评分', maxScore: 50 }] },
    ],
    entriesSummary: { total: 286, pendingPayment: 14, registered: 268, stored: 238, canceled: 4, resultPublished: 0 },
    entries: [
      { uuid: 'BC-2026-IPA-0001', category: 'IPA', style: 'Double IPA', status: 'STORED', payment: '已付款', stored: true },
      { uuid: 'BC-2026-LAG-0042', category: '拉格', style: 'Pilsner', status: 'REGISTERED', payment: '已付款', stored: false },
      { uuid: 'BC-2026-STO-0108', category: '世涛', style: 'Imperial Stout', status: 'PENDING_PAYMENT', payment: '待付款', stored: false },
      { uuid: 'BC-2026-SOU-0149', category: '酸啤', style: 'Mixed Fermentation Sour', status: 'STORED', payment: '已付款', stored: true },
    ],
    progressSummary: { finalized: 238, total: 268, advanced: 32, commentWarnings: 6 },
    resultSetup: { awardsReady: false, published: false, championReady: false },
    checks: {
      baseInfo: 'done',
      categories: 'done',
      styles: 'done',
      entryFields: 'done',
      judgeTables: 'done',
      scoreForms: 'done',
      storedEntries: 'confirm',
      resultSetup: 'pending',
    },
    alerts: [
      { level: 'warning', text: '还有 30 款酒等待入库确认' },
      { level: 'warning', text: '结果奖项尚未设置，评审结束前可先准备' },
    ],
  },
  {
    id: 2,
    code: 'SOUR-SC-2026',
    name: '华南酸啤邀请赛',
    edition: '春季专题',
    date: '2026-06-18',
    registrationStart: '2026-05-01T10:00:00',
    registrationDeadline: '2026-06-01T20:00:00',
    entryFee: 149,
    status: 'REGISTRATION_OPEN',
    categories: ['酸啤', '水果酸啤'],
    baseStyles: ['Berliner Weisse', 'Gose', 'Mixed Fermentation Sour'],
    entryFields: [
      { key: 'fruit', label: '水果或增味说明', type: 'textarea', required: false, visibleToJudges: true },
    ],
    judgeTables: [
      { id: 'A', name: 'A桌', captain: 1, professional: 2, cross: 1, finalized: 0, total: 48 },
      { id: 'B', name: 'B桌', captain: 0, professional: 2, cross: 1, finalized: 0, total: 48 },
    ],
    scoreConfigs: [
      { role: 'CROSS', dimensions: [{ label: '清爽度', maxScore: 20 }, { label: '风味记忆点', maxScore: 20 }, { label: '适饮性', maxScore: 10 }] },
      { role: 'PROFESSIONAL', dimensions: [{ label: '香气', maxScore: 12 }, { label: '外观', maxScore: 3 }, { label: '味道', maxScore: 20 }, { label: '口感', maxScore: 5 }, { label: '整体印象', maxScore: 10 }] },
      { role: 'CAPTAIN', dimensions: [{ label: '共识评分', maxScore: 50 }] },
    ],
    entriesSummary: { total: 96, pendingPayment: 12, registered: 72, stored: 0, canceled: 3, resultPublished: 0 },
    entries: [
      { uuid: 'SOUR-2026-0001', category: '酸啤', style: 'Gose', status: 'REGISTERED', payment: '已付款', stored: false },
      { uuid: 'SOUR-2026-0002', category: '水果酸啤', style: 'Berliner Weisse', status: 'PENDING_PAYMENT', payment: '待付款', stored: false },
    ],
    progressSummary: { finalized: 0, total: 72, advanced: 0, commentWarnings: 0 },
    resultSetup: { awardsReady: false, published: false, championReady: false },
    checks: {
      baseInfo: 'done',
      categories: 'done',
      styles: 'confirm',
      entryFields: 'done',
      judgeTables: 'pending',
      scoreForms: 'confirm',
      storedEntries: 'pending',
      resultSetup: 'pending',
    },
    alerts: [
      { level: 'danger', text: '缺少 1 张评审桌桌长' },
      { level: 'warning', text: '酸啤组尚未确认基础风格展示口径' },
    ],
  },
  {
    id: 3,
    code: 'MAKER-CUP-2026',
    name: '城市小酿造者杯',
    edition: '夏季城市赛',
    date: '2026-07-12',
    registrationStart: '2026-05-20T10:00:00',
    registrationDeadline: '2026-06-28T18:00:00',
    entryFee: 99,
    status: 'DRAFT',
    categories: [],
    baseStyles: [],
    entryFields: [],
    judgeTables: [],
    scoreConfigs: defaultScoreConfigs(),
    entriesSummary: { total: 0, pendingPayment: 0, registered: 0, stored: 0, canceled: 0, resultPublished: 0 },
    entries: [],
    progressSummary: { finalized: 0, total: 0, advanced: 0, commentWarnings: 0 },
    resultSetup: { awardsReady: false, published: false, championReady: false },
    checks: {
      baseInfo: 'done',
      categories: 'pending',
      styles: 'pending',
      entryFields: 'pending',
      judgeTables: 'pending',
      scoreForms: 'pending',
      storedEntries: 'pending',
      resultSetup: 'pending',
    },
    alerts: [
      { level: 'danger', text: '报名组别尚未配置，开放报名会受阻' },
      { level: 'warning', text: '评分表尚未准备' },
    ],
  },
  {
    id: 4,
    code: 'STOUT-WINTER-2025',
    name: '冬季世涛专题赛',
    edition: '年度收官',
    date: '2025-12-16',
    registrationStart: '2025-10-20T10:00:00',
    registrationDeadline: '2025-11-26T18:00:00',
    entryFee: 179,
    status: 'RESULT_CONFIRMING',
    categories: ['世涛', '波特'],
    baseStyles: ['Imperial Stout', 'Barrel Aged Stout', 'Porter'],
    entryFields: [{ key: 'barrel', label: '桶陈信息', type: 'textarea', required: false, visibleToJudges: true }],
    judgeTables: [
      { id: 'A', name: 'A桌', captain: 1, professional: 3, cross: 1, finalized: 40, total: 40 },
      { id: 'B', name: 'B桌', captain: 1, professional: 3, cross: 1, finalized: 40, total: 40 },
      { id: 'C', name: 'C桌', captain: 1, professional: 3, cross: 1, finalized: 41, total: 41 },
    ],
    scoreConfigs: defaultScoreConfigs(),
    entriesSummary: { total: 128, pendingPayment: 0, registered: 121, stored: 121, canceled: 7, resultPublished: 0 },
    entries: [
      { uuid: 'STOUT-2025-0021', category: '世涛', style: 'Imperial Stout', status: 'STORED', payment: '已付款', stored: true },
    ],
    progressSummary: { finalized: 121, total: 121, advanced: 18, commentWarnings: 0 },
    resultSetup: { awardsReady: true, published: false, championReady: false },
    checks: {
      baseInfo: 'done',
      categories: 'done',
      styles: 'done',
      entryFields: 'done',
      judgeTables: 'done',
      scoreForms: 'done',
      storedEntries: 'done',
      resultSetup: 'confirm',
    },
    alerts: [{ level: 'warning', text: '总冠军候选需要主办方最终确认' }],
  },
  {
    id: 5,
    code: 'LAGER-OPEN-2025',
    name: '清爽拉格公开赛',
    edition: '秋季开放赛',
    date: '2025-10-10',
    registrationStart: '2025-08-20T10:00:00',
    registrationDeadline: '2025-09-18T18:00:00',
    entryFee: 129,
    status: 'PUBLISHED',
    categories: ['拉格', '皮尔森'],
    baseStyles: ['Pilsner', 'Helles', 'Vienna Lager'],
    entryFields: [{ key: 'lagering', label: '熟成周期', type: 'text', required: false, visibleToJudges: true }],
    judgeTables: [
      { id: 'A', name: 'A桌', captain: 1, professional: 3, cross: 2, finalized: 54, total: 54 },
      { id: 'B', name: 'B桌', captain: 1, professional: 3, cross: 2, finalized: 53, total: 53 },
      { id: 'C', name: 'C桌', captain: 1, professional: 3, cross: 2, finalized: 53, total: 53 },
    ],
    scoreConfigs: defaultScoreConfigs(),
    entriesSummary: { total: 168, pendingPayment: 0, registered: 160, stored: 160, canceled: 8, resultPublished: 160 },
    entries: [{ uuid: 'LAGER-2025-0031', category: '拉格', style: 'Pilsner', status: 'RESULT_PUBLISHED', payment: '已付款', stored: true }],
    progressSummary: { finalized: 160, total: 160, advanced: 24, commentWarnings: 0 },
    resultSetup: { awardsReady: true, published: true, championReady: true },
    checks: {
      baseInfo: 'done',
      categories: 'done',
      styles: 'done',
      entryFields: 'done',
      judgeTables: 'done',
      scoreForms: 'done',
      storedEntries: 'done',
      resultSetup: 'done',
    },
    alerts: [],
  },
])

export function defaultScoreConfigs() {
  return [
    { role: 'CROSS', dimensions: [{ label: '整体感受', maxScore: 20 }, { label: '适饮性', maxScore: 20 }, { label: '记忆点', maxScore: 10 }] },
    { role: 'PROFESSIONAL', dimensions: [{ label: '香气', maxScore: 12 }, { label: '外观', maxScore: 3 }, { label: '味道', maxScore: 20 }, { label: '口感', maxScore: 5 }, { label: '整体印象', maxScore: 10 }] },
    { role: 'CAPTAIN', dimensions: [{ label: '共识评分', maxScore: 50 }] },
  ]
}

export function getCompetitionById(id) {
  return competitions.find((competition) => String(competition.id) === String(id))
}

export function createCompetitionFromDraft(draft) {
  const competition = {
    id: Date.now(),
    code: draft.code,
    name: draft.name,
    edition: draft.edition,
    date: draft.date,
    registrationStart: draft.registrationStart,
    registrationDeadline: draft.registrationDeadline,
    entryFee: Number(draft.entryFee || 0),
    status: 'DRAFT',
    categories: draft.categories.filter(Boolean),
    baseStyles: draft.baseStyles.filter(Boolean),
    entryFields: draft.entryFields.map((field, index) => ({
      key: field.key || `field_${index + 1}`,
      label: field.label,
      type: field.type,
      required: field.required,
      visibleToJudges: field.visibleToJudges,
    })).filter((field) => field.label),
    judgeTables: draft.judgeTables.map((table, index) => ({
      id: table.id || String.fromCharCode(65 + index),
      name: table.name || `${String.fromCharCode(65 + index)}桌`,
      captain: Number(table.captain || 0),
      professional: Number(table.professional || 0),
      cross: Number(table.cross || 0),
      finalized: 0,
      total: 0,
    })),
    scoreConfigs: draft.scoreConfigs.map((config) => ({
      role: config.role,
      dimensions: config.dimensions.map((dimension) => ({
        label: dimension.label,
        maxScore: Number(dimension.maxScore || 0),
      })).filter((dimension) => dimension.label),
    })),
    entriesSummary: { total: 0, pendingPayment: 0, registered: 0, stored: 0, canceled: 0, resultPublished: 0 },
    entries: [],
    progressSummary: { finalized: 0, total: 0, advanced: 0, commentWarnings: 0 },
    resultSetup: { awardsReady: false, published: false, championReady: false },
    checks: buildChecks(draft),
    alerts: buildDraftAlerts(draft),
  }
  competitions.unshift(competition)
  return competition
}

export function buildChecks(source) {
  const scoreFormsReady = source.scoreConfigs?.length === 3
    && source.scoreConfigs.every((config) => getScoreTotal(config) === 50)
  return {
    baseInfo: source.name && source.code && source.date && source.registrationDeadline && Number(source.entryFee) >= 0 ? 'done' : 'pending',
    categories: source.categories?.filter(Boolean).length ? 'done' : 'pending',
    styles: source.baseStyles?.filter(Boolean).length ? 'done' : 'pending',
    entryFields: source.entryFields?.filter((field) => field.label).length ? 'done' : 'confirm',
    judgeTables: source.judgeTables?.length && source.judgeTables.every((table) => Number(table.captain) === 1) ? 'done' : 'pending',
    scoreForms: scoreFormsReady ? 'done' : 'confirm',
    storedEntries: 'pending',
    resultSetup: 'pending',
  }
}

export function buildDraftAlerts(source) {
  const alerts = []
  if (!source.categories?.filter(Boolean).length) {
    alerts.push({ level: 'danger', text: '投递组别尚未配置，暂不能开放报名' })
  }
  if (!source.baseStyles?.filter(Boolean).length) {
    alerts.push({ level: 'warning', text: '基础风格未配置，评审扫码展示会缺少口径' })
  }
  if (!source.judgeTables?.length) {
    alerts.push({ level: 'warning', text: '评审桌尚未创建，赛前需要补齐' })
  }
  if (!source.scoreConfigs?.every((config) => getScoreTotal(config) === 50)) {
    alerts.push({ level: 'warning', text: '评分表总分需要统一为 50 分' })
  }
  return alerts
}

export function getReadyCount(competition) {
  return Object.values(competition.checks || {}).filter((state) => state === 'done').length
}

export function getScoreTotal(config) {
  return (config.dimensions || []).reduce((sum, dimension) => sum + Number(dimension.maxScore || 0), 0)
}

export function getStageIndex(status) {
  if (status === 'ARCHIVED') {
    return stages.length - 1
  }
  const index = stages.findIndex((stage) => stage.status === status)
  return Math.max(index, 0)
}

export function getDateDistance(value) {
  const today = new Date()
  const target = new Date(`${value}T00:00:00`)
  const distance = Math.ceil((target - today) / 86400000)

  if (distance > 0) {
    return `距比赛 ${distance} 天`
  }
  if (distance === 0) {
    return '比赛日'
  }
  return `已结束 ${Math.abs(distance)} 天`
}

export function formatDate(value) {
  return value ? value.replaceAll('-', '.') : '-'
}

export function formatDateTime(value) {
  return value ? value.replace('T', ' ').slice(0, 16).replaceAll('-', '.') : '-'
}
