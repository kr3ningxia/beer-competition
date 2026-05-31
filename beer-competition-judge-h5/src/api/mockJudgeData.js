const ROLE_LABELS = {
  CROSS: '大众评委',
  PROFESSIONAL: '专业评委',
  CAPTAIN: '桌长',
}

const people = {
  '13800000011': {
    id: 11,
    displayName: '林晓峰',
    phone: '13800000011',
    wechat: 'lin-beer',
    qualification: 'BJCP 认证评委，6 年精酿评审经验',
    role: 'PROFESSIONAL',
    tableName: 'A 桌',
  },
  '13800000012': {
    id: 12,
    displayName: '陈可',
    phone: '13800000012',
    wechat: 'chen-taste',
    qualification: '餐饮媒体评审，长期关注本地酒吧文化',
    role: 'CROSS',
    tableName: 'A 桌',
  },
  '13800000013': {
    id: 13,
    displayName: '周明',
    phone: '13800000013',
    wechat: 'captain-zhou',
    qualification: '国家级啤酒评委，担任 A 桌桌长',
    role: 'CAPTAIN',
    tableName: 'A 桌',
  },
}

export const competition = {
  id: 1,
  name: '2026 大湾区精酿啤酒大赛',
  competitionDate: '2026-08-18',
  status: '现场评审中',
  tableName: 'A 桌',
  flightName: '第 1 轮',
}

export const entries = [
  {
    uuid: 'BC-2026-IPA-0001',
    flightId: 'F1',
    categoryName: 'IPA 与淡色艾尔',
    style: 'American IPA',
    abv: '6.4%',
    description: '酒体清亮，突出柑橘、松针与热带水果香气，收口干净，苦度中高。',
    extraFields: [
      { key: 'hop', label: '主要酒花', value: 'Citra / Mosaic / Simcoe' },
      { key: 'process', label: '特殊工艺', value: '双倍干投' },
    ],
  },
  {
    uuid: 'BC-2026-IPA-0002',
    flightId: 'F1',
    categoryName: 'IPA 与淡色艾尔',
    style: 'Hazy IPA',
    abv: '6.8%',
    description: '浑浊外观，香气以芒果、百香果和白葡萄为主，口感饱满，苦度柔和。',
    extraFields: [
      { key: 'hop', label: '主要酒花', value: 'Nelson Sauvin / Galaxy' },
      { key: 'process', label: '特殊工艺', value: '燕麦与小麦提升酒体' },
    ],
  },
  {
    uuid: 'BC-2026-STO-0003',
    flightId: 'F1',
    categoryName: '深色啤酒',
    style: 'Imperial Stout',
    abv: '9.2%',
    description: '深棕近黑色酒体，带有咖啡、黑巧克力和烘烤麦芽香气，尾段有轻微温热感。',
    extraFields: [
      { key: 'adjunct', label: '增味原料', value: '可可豆、冷萃咖啡' },
    ],
  },
  {
    uuid: 'BC-2026-SOU-0004',
    flightId: 'F1',
    categoryName: '酸啤与水果啤酒',
    style: 'Fruited Sour',
    abv: '4.7%',
    description: '浅粉色酒体，酸度清爽，带覆盆子、樱桃和乳酸香气，整体平衡轻盈。',
    extraFields: [
      { key: 'fruit', label: '水果添加', value: '覆盆子、酸樱桃' },
    ],
  },
  {
    uuid: 'BC-2026-LAG-0005',
    flightId: 'F1',
    categoryName: '拉格与小麦',
    style: 'German Pils',
    abv: '5.0%',
    description: '金黄色清澈酒体，麦芽洁净，草本酒花香气明显，收口爽脆。',
    extraFields: [
      { key: 'process', label: '特殊工艺', value: '低温长时间熟成' },
    ],
  },
]

export const scoreConfigs = {
  CROSS: {
    roleLabel: ROLE_LABELS.CROSS,
    commentMinLength: 50,
    dimensions: [
      { key: 'appearance', label: '第一印象', maxScore: 10 },
      { key: 'drinkability', label: '易饮度', maxScore: 20 },
      { key: 'overall', label: '整体喜好', maxScore: 20 },
    ],
  },
  PROFESSIONAL: {
    roleLabel: ROLE_LABELS.PROFESSIONAL,
    commentMinLength: 30,
    dimensions: [
      { key: 'aroma', label: '香气', maxScore: 12 },
      { key: 'appearance', label: '外观', maxScore: 3 },
      { key: 'flavor', label: '味道', maxScore: 20 },
      { key: 'mouthfeel', label: '口感', maxScore: 5 },
      { key: 'overall', label: '整体印象', maxScore: 10 },
    ],
  },
  CAPTAIN: {
    roleLabel: ROLE_LABELS.CAPTAIN,
    commentMinLength: 20,
    dimensions: [
      { key: 'consensus', label: '共识分', maxScore: 50 },
    ],
  },
}

const tableScores = [
  {
    id: 'seed-1',
    beerUuid: 'BC-2026-IPA-0001',
    judgeName: '林晓峰',
    judgeRoleType: 'PROFESSIONAL',
    roleLabel: '专业评委',
    dimensions: [
      { key: 'aroma', label: '香气', score: 10.5, maxScore: 12 },
      { key: 'appearance', label: '外观', score: 2.5, maxScore: 3 },
      { key: 'flavor', label: '味道', score: 17, maxScore: 20 },
      { key: 'mouthfeel', label: '口感', score: 4, maxScore: 5 },
      { key: 'overall', label: '整体印象', score: 8, maxScore: 10 },
    ],
    totalScore: 42,
    comments: '香气表现集中，热带水果和松针感清晰，苦味收束干净。中段层次稍短，但整体完成度较高。',
    submittedAt: '14:12',
    locked: false,
    finalFlag: false,
  },
  {
    id: 'seed-2',
    beerUuid: 'BC-2026-IPA-0001',
    judgeName: '陈可',
    judgeRoleType: 'CROSS',
    roleLabel: '大众评委',
    dimensions: [
      { key: 'appearance', label: '第一印象', score: 8.5, maxScore: 10 },
      { key: 'drinkability', label: '易饮度', score: 17, maxScore: 20 },
      { key: 'overall', label: '整体喜好', score: 18, maxScore: 20 },
    ],
    totalScore: 43.5,
    comments: '香气很讨喜，入口也比较顺，苦味不会让人觉得突兀。整体适合推荐给喜欢清爽果香型 IPA 的消费者。',
    submittedAt: '14:16',
    locked: false,
    finalFlag: false,
  },
  {
    id: 'seed-final-3',
    beerUuid: 'BC-2026-STO-0003',
    judgeName: '周明',
    judgeRoleType: 'CAPTAIN',
    roleLabel: '桌长',
    dimensions: [{ key: 'consensus', label: '共识分', score: 44, maxScore: 50 }],
    totalScore: 44,
    comments: '本桌认为风味完整度高，烘烤、咖啡和巧克力层次清楚，酒精感控制较好。',
    submittedAt: '14:32',
    locked: true,
    finalFlag: true,
    advanced: true,
  },
]

export function getRoleLabel(role) {
  return ROLE_LABELS[role] || role
}

export function getPersonByPhone(phone) {
  return people[phone] || null
}

export function getFallbackPerson(phone) {
  return {
    id: Date.now(),
    displayName: '现场评审',
    phone,
    wechat: '',
    qualification: '现场临时账号',
    role: 'CROSS',
    tableName: 'A 桌',
  }
}

export function seedScores() {
  return tableScores
}
