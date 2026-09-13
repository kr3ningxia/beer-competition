import assert from 'node:assert/strict'
import test from 'node:test'
import { createSerialTaskQueue, drainDirtyTask, matchesCompetitionRequest } from './competitionDraftCoordinator.js'

test('serial task queue waits for the active save before starting the next save', async () => {
  const events = []
  let releaseFirst
  const firstGate = new Promise((resolve) => {
    releaseFirst = resolve
  })
  const queue = createSerialTaskQueue()
  const first = queue.enqueue(async () => {
    events.push('first:start')
    await firstGate
    events.push('first:end')
    return true
  })
  const second = queue.enqueue(async () => {
    events.push('second:start')
    return true
  })

  await new Promise((resolve) => setImmediate(resolve))
  assert.deepEqual(events, ['first:start'])
  releaseFirst()
  assert.equal(await first, true)
  assert.equal(await second, true)
  assert.deepEqual(events, ['first:start', 'first:end', 'second:start'])
  assert.equal(queue.getPendingCount(), 0)
})

test('request context rejects responses from an old competition or generation', () => {
  const current = { competitionId: 42, generation: 8 }

  assert.equal(matchesCompetitionRequest({ competitionId: '42', generation: 8 }, current), true)
  assert.equal(matchesCompetitionRequest({ competitionId: 41, generation: 8 }, current), false)
  assert.equal(matchesCompetitionRequest({ competitionId: 42, generation: 7 }, current), false)
})

test('failed save does not poison later saves', async () => {
  const queue = createSerialTaskQueue()

  await assert.rejects(queue.enqueue(async () => {
    throw new Error('save failed')
  }), /save failed/)
  assert.equal(await queue.wait(), true)
  assert.equal(await queue.enqueue(async () => 'saved'), 'saved')
})

test('dirty task keeps saving when edits happen during an active save', async () => {
  let dirtyVersion = 1
  const savedVersions = []

  const result = await drainDirtyTask(
    () => dirtyVersion > 0,
    async () => {
      const savingVersion = dirtyVersion
      savedVersions.push(savingVersion)
      if (savingVersion === 1) dirtyVersion = 2
      if (dirtyVersion === savingVersion) dirtyVersion = 0
      return true
    },
  )

  assert.equal(result, true)
  assert.deepEqual(savedVersions, [1, 2])
})
