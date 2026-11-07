import test from 'node:test'
import assert from 'node:assert/strict'
import {
  discountPercentIssue,
  discountPercentToRate,
  discountRateToPercent,
  feeTierIssue,
} from './competitionFeeTiers.js'

test('accepts percentages with up to four decimal places', () => {
  assert.equal(discountPercentIssue(80), '')
  assert.equal(discountPercentIssue('86.0204'), '')
})

test('rejects a fifth decimal place', () => {
  assert.equal(discountPercentIssue('86.02045'), '折扣率最多保留 4 位小数')
})

test('tolerates floating point noise produced by number inputs', () => {
  assert.equal(discountPercentIssue(80.00010000000001), '')
  assert.equal(discountPercentIssue(99.99999999999999), '')
})

test('enforces the 1% to 100% range', () => {
  assert.equal(discountPercentIssue(''), '请填写折扣率')
  assert.equal(discountPercentIssue(0.5), '折扣率需在 1% 到 100% 之间')
  assert.equal(discountPercentIssue(100.5), '折扣率需在 1% 到 100% 之间')
})

test('rejects a later tier whose rate exceeds the previous tier', () => {
  assert.equal(
    feeTierIssue({ discountRatePercent: 90 }, 1, [{ discountRatePercent: 80 }]),
    '折扣率不能高于上一档',
  )
})

test('converts percentages to six-decimal rates without float tails', () => {
  assert.equal(discountPercentToRate(86.05), 0.8605)
  assert.equal(discountPercentToRate(86.0204), 0.860204)
  assert.equal(discountRateToPercent(0.8605), 86.05)
  assert.equal(discountRateToPercent(0.860204), 86.0204)
})
