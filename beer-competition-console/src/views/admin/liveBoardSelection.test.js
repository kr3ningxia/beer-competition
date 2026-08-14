import test from 'node:test'
import assert from 'node:assert/strict'
import { selectDefaultLiveBoardCompetition } from './liveBoardSelection.js'

const NOW = new Date(2026, 7, 14, 12)

test('selects a judging competition before considering dates', () => {
  const selected = selectDefaultLiveBoardCompetition([
    { id: 1, status: 'REGISTRATION_CLOSED', competitionDate: '2026-08-14' },
    { id: 2, status: 'JUDGING', competitionDate: '2026-09-20' },
  ], NOW)

  assert.equal(selected.id, 2)
})

test('selects judging preparation when no competition is judging', () => {
  const selected = selectDefaultLiveBoardCompetition([
    { id: 1, status: 'REGISTRATION_CLOSED', competitionDate: '2026-08-14' },
    { id: 2, status: 'JUDGING_PREP', competitionDate: '2026-09-20' },
  ], NOW)

  assert.equal(selected.id, 2)
})

test('selects the competition nearest to today when none is active', () => {
  const selected = selectDefaultLiveBoardCompetition([
    { id: 1, status: 'REGISTRATION_OPEN', competitionDate: '2026-09-20' },
    { id: 2, status: 'REGISTRATION_CLOSED', competitionDate: '2026-08-12' },
    { id: 3, status: 'DRAFT', competitionDate: '2026-08-30' },
  ], NOW)

  assert.equal(selected.id, 2)
})

test('prefers the upcoming competition when dates are equally close', () => {
  const selected = selectDefaultLiveBoardCompetition([
    { id: 1, status: 'REGISTRATION_CLOSED', competitionDate: '2026-08-12' },
    { id: 2, status: 'REGISTRATION_OPEN', competitionDate: '2026-08-16' },
  ], NOW)

  assert.equal(selected.id, 2)
})
