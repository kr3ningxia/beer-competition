const ROLE_LABELS = {
  CROSS: '跨界评审',
  PROFESSIONAL: '专业评审',
  CAPTAIN: '桌长',
}

const people = {
  '13800000011': {
    id: 11,
    displayName: '林晓峰',
    phone: '13800000011',
    wechat: 'lin-beer',
    qualification: 'BJCP 认证评审，6 年精酿评审经验',
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
    qualification: '国家级啤酒评审，担任 A 桌桌长',
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
    shortCode: 'IPA001',
    flightId: 'F1',
    categoryName: 'IPA 与淡色艾尔',
    style: 'American IPA',
    abv: '6.4%',
    extraFields: [
      { key: 'hop', label: '主要酒花', value: 'Citra / Mosaic / Simcoe' },
      { key: 'process', label: '特殊工艺', value: '双倍干投' },
    ],
  },
  {
    uuid: 'BC-2026-IPA-0002',
    shortCode: 'IPA002',
    flightId: 'F1',
    categoryName: 'IPA 与淡色艾尔',
    style: 'Hazy IPA',
    abv: '6.8%',
    extraFields: [
      { key: 'hop', label: '主要酒花', value: 'Nelson Sauvin / Galaxy' },
      { key: 'process', label: '特殊工艺', value: '燕麦与小麦提升酒体' },
    ],
  },
  {
    uuid: 'BC-2026-STO-0003',
    shortCode: 'STO003',
    flightId: 'F1',
    categoryName: '深色啤酒',
    style: 'Imperial Stout',
    abv: '9.2%',
    extraFields: [
      { key: 'adjunct', label: '增味原料', value: '可可豆、冷萃咖啡' },
    ],
  },
  {
    uuid: 'BC-2026-SOU-0004',
    shortCode: 'SOU004',
    flightId: 'F1',
    categoryName: '酸啤与水果啤酒',
    style: 'Fruited Sour',
    abv: '4.7%',
    extraFields: [
      { key: 'fruit', label: '水果添加', value: '覆盆子、酸樱桃' },
    ],
  },
  {
    uuid: 'BC-2026-LAG-0005',
    shortCode: 'LAG005',
    flightId: 'F1',
    categoryName: '拉格与小麦',
    style: 'German Pils',
    abv: '5.0%',
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
      {
        key: 'appearance',
        label: '第一印象',
        maxScore: 10,
        description: '香气、外观和入口第一感受是否有吸引力。',
        notePlaceholder: '例如香气是否讨喜、第一口是否愿意继续喝。',
      },
      {
        key: 'drinkability',
        label: '易饮度',
        maxScore: 20,
        description: '入口顺畅度、平衡感和复饮意愿。',
        notePlaceholder: '例如苦度、酸度、甜感是否容易接受。',
      },
      {
        key: 'overall',
        label: '整体喜好',
        maxScore: 20,
        description: '作为消费者是否愿意推荐或再次购买。',
        notePlaceholder: '例如适合的饮用场景和推荐理由。',
      },
    ],
  },
  PROFESSIONAL: {
    roleLabel: ROLE_LABELS.PROFESSIONAL,
    commentMinLength: 30,
    dimensions: [
      {
        key: 'aroma',
        label: '香气',
        maxScore: 12,
        description: '麦芽、酒花、酵母和发酵特征的清晰度与协调性。',
        notePlaceholder: '记录主要香气、强度、缺陷或风格贴合度。',
      },
      {
        key: 'appearance',
        label: '外观',
        maxScore: 3,
        description: '颜色、澄清度、泡沫状态与风格符合度。',
        notePlaceholder: '记录颜色、泡持、浑浊度等观察。',
      },
      {
        key: 'flavor',
        label: '味道',
        maxScore: 20,
        description: '入口到收口的风味层次、平衡和缺陷表现。',
        notePlaceholder: '记录甜苦酸、酒花/麦芽表现、余味和缺陷。',
      },
      {
        key: 'mouthfeel',
        label: '口感',
        maxScore: 5,
        description: '酒体、杀口感、涩感、温热感与顺滑度。',
        notePlaceholder: '记录酒体厚薄、碳酸、涩感或酒精感。',
      },
      {
        key: 'overall',
        label: '整体印象',
        maxScore: 10,
        description: '完成度、风格准确性和整体评审反馈。',
        notePlaceholder: '给参赛方留下综合反馈和改进方向。',
      },
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
    roleLabel: '专业评审',
    dimensions: [
      { key: 'aroma', label: '香气', score: 11, maxScore: 12, note: '热带水果和松针感清晰，香气集中。' },
      { key: 'appearance', label: '外观', score: 3, maxScore: 3, note: '酒体清亮，泡沫状态稳定。' },
      { key: 'flavor', label: '味道', score: 17, maxScore: 20, note: '苦味收束干净，中段层次稍短。' },
      { key: 'mouthfeel', label: '口感', score: 4, maxScore: 5, note: '酒体中等，杀口感合适。' },
      { key: 'overall', label: '整体印象', score: 8, maxScore: 10, note: '整体完成度较高，风格辨识度清楚。' },
    ],
    totalScore: 43,
    comments: '香气：热带水果和松针感清晰，香气集中。\n外观：酒体清亮，泡沫状态稳定。\n味道：苦味收束干净，中段层次稍短。\n口感：酒体中等，杀口感合适。\n整体印象：整体完成度较高，风格辨识度清楚。',
    submittedAt: '14:12',
    locked: false,
    finalFlag: false,
  },
  {
    id: 'seed-2',
    beerUuid: 'BC-2026-IPA-0001',
    judgeName: '陈可',
    judgeRoleType: 'CROSS',
    roleLabel: '跨界评审',
    dimensions: [
      { key: 'appearance', label: '第一印象', score: 9, maxScore: 10, note: '香气很讨喜，入口第一感觉轻松。' },
      { key: 'drinkability', label: '易饮度', score: 17, maxScore: 20, note: '苦味不会突兀，喝起来比较顺。' },
      { key: 'overall', label: '整体喜好', score: 18, maxScore: 20, note: '适合推荐给喜欢清爽果香型 IPA 的消费者。' },
    ],
    totalScore: 44,
    comments: '第一印象：香气很讨喜，入口第一感觉轻松。\n易饮度：苦味不会突兀，喝起来比较顺。\n整体喜好：适合推荐给喜欢清爽果香型 IPA 的消费者。',
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
