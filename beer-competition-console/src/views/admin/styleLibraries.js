export const fallbackStyleLibraries = [
  {
    value: 'BJCP_2021_CN',
    label: 'BJCP 2021 中文标准库',
    version: '2021',
    language: '中文',
    status: '启用',
    source: 'BJCP',
    categoryCount: 34,
    styleCount: 116,
    updatedAt: '2026.05.18',
    tags: ['报名必填', '支持搜索', '评审可见'],
    categories: ['标准型美国啤酒', '国际拉格', 'IPA', '特色啤酒'],
    styles: ['美式淡拉格', '德式皮尔森', '帝国世涛', '美式 IPA', '赛松', '实验啤酒'],
    styleItems: [
      { categoryName: '1. 标准型美国啤酒', name: '美式淡拉格', styleCode: '1A', status: 1 },
      { categoryName: '5. 浅色苦味型欧洲啤酒', name: '德式皮尔森', styleCode: '5D', status: 1 },
      { categoryName: '20. 美式波特与世涛', name: '帝国世涛', styleCode: '20C', status: 1 },
      { categoryName: '21. IPA', name: '美式 IPA', styleCode: '21A', status: 1 },
      { categoryName: '25. 高酒精度比利时艾尔', name: '赛松', styleCode: '25B', status: 1 },
      { categoryName: '34. 特色啤酒', name: '实验啤酒', styleCode: '34C', status: 1 },
    ],
  },
  {
    value: 'CUSTOM_STANDARD',
    label: '组委会标准风格库',
    version: '2026A',
    language: '中文',
    status: '启用',
    source: '组委会',
    categoryCount: 18,
    styleCount: 64,
    updatedAt: '2026.05.26',
    tags: ['自定义分类', '报名时可搜索', '评审可见'],
    categories: ['拉格专题', '特色增味'],
    styles: ['浅色拉格', '深色拉格', '创意拉格', '水果酸啤', '桶陈世涛', '茶咖啡增味', '实验啤酒'],
    styleItems: [
      { categoryName: '拉格专题', name: '浅色拉格', status: 1 },
      { categoryName: '拉格专题', name: '深色拉格', status: 1 },
      { categoryName: '拉格专题', name: '创意拉格', status: 1 },
      { categoryName: '特色增味', name: '水果酸啤', status: 1 },
      { categoryName: '特色增味', name: '桶陈世涛', status: 1 },
      { categoryName: '特色增味', name: '茶咖啡增味', status: 1 },
      { categoryName: '特色增味', name: '实验啤酒', status: 1 },
    ],
  },
  {
    value: 'BJCP_2021_EN',
    label: 'BJCP 2021 English',
    version: '2021',
    language: 'English',
    status: '备用',
    source: 'BJCP',
    categoryCount: 34,
    styleCount: 116,
    updatedAt: '2026.05.18',
    tags: ['英文展示', '支持搜索', '评审可见'],
    categories: ['IPA', 'Lager'],
    styles: ['American IPA', 'Double IPA', 'German Pils', 'Imperial Stout', 'Mixed-Fermentation Sour Beer', 'Gose'],
    styleItems: [
      { categoryName: 'IPA', name: 'American IPA', styleCode: '21A', status: 1 },
      { categoryName: 'IPA', name: 'Double IPA', styleCode: '22A', status: 1 },
      { categoryName: 'Lager', name: 'German Pils', styleCode: '5D', status: 1 },
      { categoryName: 'IPA', name: 'Imperial Stout', styleCode: '20C', status: 1 },
      { categoryName: 'IPA', name: 'Mixed-Fermentation Sour Beer', styleCode: '28B', status: 1 },
      { categoryName: 'IPA', name: 'Gose', styleCode: '27A', status: 1 },
    ],
  },
]

export const styleLibraries = fallbackStyleLibraries
export const defaultStyleLibraryValue = 'BJCP_2021_CN'

export function normalizeStyleLibrary(item) {
  return {
    ...item,
    value: item.value || item.code,
    label: item.label || item.name,
    statusLabel: item.statusLabel || (item.status === 1 || item.status === '启用' ? '启用' : '停用'),
    tags: item.tags || [],
    categories: item.categories || [],
    styles: item.styles || [],
    styleItems: item.styleItems || [],
    categoryCount: Number(item.categoryCount || 0),
    styleCount: Number(item.styleCount || item.styles?.length || 0),
  }
}

export function normalizeStyleLibraries(items) {
  return (items?.length ? items : fallbackStyleLibraries).map(normalizeStyleLibrary)
}

export function getStyleLibrary(value, libraries = fallbackStyleLibraries) {
  const normalized = normalizeStyleLibraries(libraries)
  return normalized.find((item) => item.value === value) || normalized[0]
}

export function getStyleLibraryLabel(value, libraries = fallbackStyleLibraries) {
  return getStyleLibrary(value, libraries)?.label || value || '未选择'
}
