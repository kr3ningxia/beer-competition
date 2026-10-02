import test from 'node:test'
import assert from 'node:assert/strict'
import { formatRecruitmentSchedule } from './recruitmentSchedule.js'

test('uses the configured judging time on the competition date', () => {
  assert.equal(formatRecruitmentSchedule({ competitionDate: '2026-10-09', judgingStartTime: '09:00:00' }), '2026/10/09 09:00')
})

test('does not invent 08:00 for a legacy date-only recruitment', () => {
  assert.equal(formatRecruitmentSchedule({ competitionDate: '2026-10-09' }), '2026/10/09 时间待定')
})

test('does not substitute the registration start for a missing competition date', () => {
  assert.equal(formatRecruitmentSchedule({ recruitmentStart: '2026-10-01T08:00:00', judgingStartTime: '09:00:00' }), '日期待定')
})

test('keeps local wall time even when the browser timezone changes', () => {
  const originalTimezone = process.env.TZ
  try {
    for (const timezone of ['Asia/Shanghai', 'UTC', 'America/New_York']) {
      process.env.TZ = timezone
      assert.equal(formatRecruitmentSchedule({ competitionDate: '2026-10-09', judgingStartTime: '09:30:00' }), '2026/10/09 09:30')
    }
  } finally {
    if (originalTimezone === undefined) delete process.env.TZ
    else process.env.TZ = originalTimezone
  }
})
