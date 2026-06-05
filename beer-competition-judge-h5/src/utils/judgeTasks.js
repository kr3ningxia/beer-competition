const RANKING_TASK_TYPES = new Set(['RANKING_ROUND', 'RANKING_PARTICIPANT'])

const TASK_PRIORITY = {
  RANKING_ROUND: 3,
  RANKING_PARTICIPANT: 3,
  CAPTAIN_FINALIZE: 2,
  SCORE_ENTRY: 1,
}

export function isRankingTaskType(taskType) {
  return RANKING_TASK_TYPES.has(taskType)
}

export function selectCurrentTask(tasks) {
  if (!tasks?.length) return null
  return [...tasks].sort((left, right) => (
    Number(right.roundId || 0) - Number(left.roundId || 0)
      || Number(TASK_PRIORITY[right.taskType] || 0) - Number(TASK_PRIORITY[left.taskType] || 0)
      || Number(right.roundTableId || 0) - Number(left.roundTableId || 0)
  ))[0]
}
