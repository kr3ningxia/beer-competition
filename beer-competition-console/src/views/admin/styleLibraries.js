export const fallbackStyleLibraries = [
  {
    value: 'BJCP_2021_CN',
    label: 'BJCP 2021 中文标准库',
    version: '2021',
    language: '中文',
    status: '启用',
    source: 'BJCP',
    categoryCount: 34,
    styleCount: 120,
    updatedAt: '2026.05.18',
    tags: ['报名必填', '支持搜索', '评审可见'],
    categories: ['IPA', '拉格', '世涛与酸啤'],
    styles: ['American IPA', 'Double IPA', 'Pilsner', 'Imperial Stout', 'Mixed Fermentation Sour', 'Gose', 'Helles', 'Saison'],
    styleItems: [
      { categoryName: 'IPA', name: 'American IPA', styleCode: '21A', status: 1 },
      { categoryName: 'IPA', name: 'Double IPA', styleCode: '22A', status: 1 },
      { categoryName: '拉格', name: 'Pilsner', styleCode: '5D', status: 1 },
      { categoryName: '世涛与酸啤', name: 'Imperial Stout', styleCode: '20C', status: 1 },
      { categoryName: '世涛与酸啤', name: 'Mixed Fermentation Sour', styleCode: '28B', status: 1 },
      { categoryName: '世涛与酸啤', name: 'Gose', styleCode: '27A', status: 1 },
      { categoryName: '拉格', name: 'Helles', styleCode: '4A', status: 1 },
      { categoryName: 'IPA', name: 'Saison', styleCode: '25B', status: 1 },
    ],
  },
  {
    value: 'CUSTOM_STANDARD',
    label: '主办方标准风格库',
    version: '2026A',
    language: '中文',
    status: '启用',
    source: '主办方',
    categoryCount: 18,
    styleCount: 64,
    updatedAt: '2026.05.26',
    tags: ['自定义分类', '报名可搜', '评审可见'],
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
    styleCount: 120,
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
