let xlsxPromise
function loadXlsx() {
  if (!xlsxPromise) xlsxPromise = import('xlsx')
  return xlsxPromise
}

const SUMMARY_HEADERS = ['序号', '投递组别', '风格范围', '允许填报风格数', '备注']
const DETAIL_HEADERS = ['投递组别', '分类', '风格名称', '编号', '评审可见说明']
const SUMMARY_WIDTHS = [{ wch: 6 }, { wch: 24 }, { wch: 12 }, { wch: 14 }, { wch: 46 }]
const DETAIL_WIDTHS = [{ wch: 24 }, { wch: 22 }, { wch: 26 }, { wch: 10 }, { wch: 46 }]

function normalizeStyles(styles) {
  return (styles || []).filter((style) => style && style.name && style.status !== 0)
}

function buildCategoryRows({ categories, styles }) {
  const activeStyles = normalizeStyles(styles)
  const activeByName = new Map(activeStyles.map((style) => [style.name, style]))

  const summaryRows = [SUMMARY_HEADERS]
  const detailRows = [DETAIL_HEADERS]

  ;(categories || []).forEach((category, index) => {
    const groupName = String(category?.name || '').trim() || `未命名组别 ${index + 1}`
    const sequence = String(index + 1)

    if (!Array.isArray(category?.styleNames)) {
      summaryRows.push([
        sequence,
        groupName,
        '不限风格',
        String(activeStyles.length),
        '',
      ])
      detailRows.push([groupName, '全部风格', `不限风格（全部 ${activeStyles.length} 个风格均可选）`, '', ''])
      return
    }

    const selectedNames = new Set(category.styleNames)
    const matchedStyles = activeStyles.filter((style) => selectedNames.has(style.name))
    const unmatchedNames = category.styleNames.filter((name) => name && !activeByName.has(name))
    const remarks = []
    if (!matchedStyles.length) {
      remarks.push('未选择任何风格，厂商报名时该组别没有可选风格')
    }
    if (unmatchedNames.length) {
      remarks.push(`${unmatchedNames.length} 个已选风格不在当前风格库：${unmatchedNames.join('、')}`)
    }

    summaryRows.push([
      sequence,
      groupName,
      '指定风格',
      String(matchedStyles.length),
      remarks.join('；'),
    ])

    matchedStyles.forEach((style) => {
      detailRows.push([
        groupName,
        style.categoryName || '未归类',
        style.name,
        style.styleCode || '',
        style.description || '',
      ])
    })
    unmatchedNames.forEach((name) => {
      detailRows.push([groupName, '', name, '', '当前风格库已不存在'])
    })
    if (!matchedStyles.length && !unmatchedNames.length) {
      detailRows.push([groupName, '', '（未选择任何风格）', '', '该组别暂无允许填报的风格'])
    }
  })

  return { summaryRows, detailRows }
}

function safeFilePart(value) {
  return String(value || '').replace(/[\\/:*?"<>|]/g, '_').trim() || '比赛'
}

export function buildCategoryStyleRows(payload) {
  return buildCategoryRows(payload)
}

export async function exportCategoryStyleScope({ competitionName, categories, styles }) {
  const XLSX = await loadXlsx()
  const { summaryRows, detailRows } = buildCategoryRows({ categories, styles })

  const summarySheet = XLSX.utils.aoa_to_sheet(summaryRows)
  summarySheet['!cols'] = SUMMARY_WIDTHS
  const detailSheet = XLSX.utils.aoa_to_sheet(detailRows)
  detailSheet['!cols'] = DETAIL_WIDTHS

  const workbook = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(workbook, summarySheet, '投递组别')
  XLSX.utils.book_append_sheet(workbook, detailSheet, '允许填报风格')
  XLSX.writeFile(workbook, `${safeFilePart(competitionName)}-投递组别与风格范围.xlsx`)
}
