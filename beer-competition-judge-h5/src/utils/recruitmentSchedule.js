// 比赛日期和评审时刻均为赛事当地时间，直接组合，避免日期经 UTC 转换后凭空显示 08:00。
export function formatRecruitmentSchedule(item) {
  const date = item?.competitionDate
  if (!date) return '日期待定'
  const dateLabel = String(date).slice(0, 10).replaceAll('-', '/')
  const time = item.judgingStartTime
  return `${dateLabel} ${time ? String(time).slice(0, 5) : '时间待定'}`
}
