export const brewTypeOptions = [
  { label: '商酿比赛', value: 'COMMERCIAL' },
  { label: '家酿比赛', value: 'HOMEBREW' },
  { label: '商酿家酿混合赛', value: 'MIXED' },
]

export function brewTypeLabel(value) {
  return brewTypeOptions.find((item) => item.value === value)?.label || ''
}
