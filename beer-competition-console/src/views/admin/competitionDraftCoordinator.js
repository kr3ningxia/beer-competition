export function createSerialTaskQueue(onPendingChange = () => {}) {
  let chain = Promise.resolve(true)
  let pendingCount = 0

  function updatePending(delta) {
    pendingCount = Math.max(0, pendingCount + delta)
    onPendingChange(pendingCount)
  }

  return {
    enqueue(task) {
      updatePending(1)
      const result = chain.catch(() => true).then(task)
      const trackedResult = result.finally(() => updatePending(-1))
      chain = trackedResult.then(() => true, () => true)
      return trackedResult
    },
    async wait() {
      try {
        return await chain
      } catch {
        return false
      }
    },
    getPendingCount() {
      return pendingCount
    },
  }
}

export function matchesCompetitionRequest(request, current) {
  return Number(request.generation) === Number(current.generation)
    && String(request.competitionId || '') === String(current.competitionId || '')
}

export async function drainDirtyTask(isDirty, task) {
  let result = true
  while (isDirty()) {
    result = await task()
    if (result === false) return false
  }
  return result
}
