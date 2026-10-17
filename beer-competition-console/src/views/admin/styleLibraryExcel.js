let xlsxPromise
function loadXlsx() {
  if (!xlsxPromise) xlsxPromise = import('xlsx')
  return xlsxPromise
}

const COLUMN_DEFS = [
  { key: 'categoryName', label: '分类', aliases: ['分类', '类别', '分组', '组别', '品类', 'category', 'categoryname'] },
  { key: 'name', label: '风格名称', aliases: ['风格名称', '风格', '名称', '酒款风格', 'style', 'stylename', 'name'] },
  { key: 'styleCode', label: '编号', aliases: ['编号', '风格编号', '代号', '编码', 'code', 'stylecode'] },
  { key: 'description', label: '评审可见说明', aliases: ['评审可见说明', '评审说明', '说明', '描述', '备注', 'description'] },
]

const HEADERS = ['分类', '风格名称', '编号', '评审可见说明']

const TEMPLATE_ROWS = [
  HEADERS,
  ['经典美式啤酒', '美式淡拉格', '1A', '干净清爽，突出麦芽与轻酒花'],
  ['IPA', '美式IPA', '21A', '啤酒花香气突出，苦味干净'],
]

const TEMPLATE_FILE_NAME = '风格库导入模板.xlsx'
const COLUMN_WIDTHS = [{ wch: 22 }, { wch: 26 }, { wch: 10 }, { wch: 46 }]

function normalizeHeader(value) {
  return String(value ?? '')
    .trim()
    .toLowerCase()
    .replace(/[\s:：*＊()-]/g, '')
}

function resolveColumns(headerRow) {
  const normalized = headerRow.map(normalizeHeader)
  const mapping = {}
  COLUMN_DEFS.forEach((def) => {
    const index = normalized.findIndex((header) => header && def.aliases.includes(header))
    if (index >= 0) mapping[def.key] = index
  })
  return mapping
}

function cellText(row, index) {
  if (index == null || index < 0) return ''
  const value = row[index]
  if (value == null) return ''
  return String(value).trim()
}

function toRows(items) {
  return (items || []).map((item) => [
    item.categoryName || '',
    item.name || '',
    item.styleCode || '',
    item.description || '',
  ])
}

export function buildStyleLibraryRows(items) {
  return [HEADERS, ...toRows(items)]
}

function safeFilePart(value) {
  return String(value || '').replace(/[\\/:*?"<>|]/g, '_').trim() || '风格库'
}

function writeSheet(XLSX, rows, fileName) {
  const sheet = XLSX.utils.aoa_to_sheet(rows)
  sheet['!cols'] = COLUMN_WIDTHS
  const workbook = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(workbook, sheet, '风格库')
  XLSX.writeFile(workbook, fileName)
}

export async function downloadStyleImportTemplate() {
  const XLSX = await loadXlsx()
  writeSheet(XLSX, TEMPLATE_ROWS, TEMPLATE_FILE_NAME)
}

export async function exportStyleLibrary({ name, items }) {
  const XLSX = await loadXlsx()
  writeSheet(XLSX, buildStyleLibraryRows(items), `${safeFilePart(name)}-风格库.xlsx`)
}

export async function parseStyleImportFile(file) {
  const XLSX = await loadXlsx()

  let rows
  try {
    const buffer = await file.arrayBuffer()
    const workbook = XLSX.read(buffer, { type: 'array' })
    const sheetName = workbook.SheetNames[0]
    if (!sheetName) throw new Error('empty')
    rows = XLSX.utils.sheet_to_json(workbook.Sheets[sheetName], { header: 1, blankrows: false, defval: '' })
  } catch {
    throw new Error('无法读取该文件，请使用下载的 Excel 模板（.xlsx）')
  }

  if (!rows.length) throw new Error('表格为空，没有可导入的内容')

  const mapping = resolveColumns(rows[0])
  const hasHeader = Object.keys(mapping).length > 0
  const indexOf = (key, fallback) => (hasHeader ? mapping[key] : fallback)

  const categoryIndex = indexOf('categoryName', 0)
  const nameIndex = indexOf('name', 1)
  const codeIndex = indexOf('styleCode', 2)
  const descriptionIndex = indexOf('description', 3)

  const items = []
  for (let i = hasHeader ? 1 : 0; i < rows.length; i += 1) {
    const row = rows[i]
    if (!row || !row.length) continue
    const name = cellText(row, nameIndex)
    if (!name) continue
    items.push({
      categoryName: cellText(row, categoryIndex),
      name,
      styleCode: cellText(row, codeIndex),
      description: cellText(row, descriptionIndex),
    })
  }

  if (!items.length) {
    throw new Error('未解析到有效的风格数据，请确认已填写「风格名称」列')
  }

  return items
}
