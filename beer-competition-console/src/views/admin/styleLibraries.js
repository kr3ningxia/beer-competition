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
    styles: ['American IPA', 'Double IPA', 'Pilsner', 'Imperial Stout', 'Mixed Fermentation Sour', 'Gose', 'Helles', 'Saison'],
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
    styles: ['浅色拉格', '深色拉格', '创意拉格', '水果酸啤', '桶陈世涛', '茶咖啡增味', '实验啤酒'],
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
    styles: ['American IPA', 'Double IPA', 'German Pils', 'Imperial Stout', 'Mixed-Fermentation Sour Beer', 'Gose'],
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
    styles: item.styles || [],
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
