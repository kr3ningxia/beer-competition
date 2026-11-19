import test from 'node:test'
import assert from 'node:assert/strict'
import {
  feeTierDisplayIssue,
  feeTierIssue,
  tierPriceIssue,
  tierPriceToZhe,
} from './competitionFeeTiers.js'

test('accepts prices with up to two decimal places', () => {
  assert.equal(tierPriceIssue(159.2, 199), '')
  assert.equal(tierPriceIssue('159.20', 199), '')
  assert.equal(tierPriceIssue(199, 199), '')
})

test('rejects a third decimal place', () => {
  assert.equal(tierPriceIssue(159.205, 199), '折后单价最多保留 2 位小数')
})

test('requires a positive price', () => {
  assert.equal(tierPriceIssue('', 199), '请填写折后单价')
  assert.equal(tierPriceIssue(0, 199), '折后单价需大于 0')
  assert.equal(tierPriceIssue(-1, 199), '折后单价需大于 0')
})

test('rejects a price above the entry fee', () => {
  assert.equal(tierPriceIssue(209, 199), '折后单价不能高于普通报名费')
})

test('rejects a later tier whose price exceeds the previous tier', () => {
  assert.equal(
    feeTierIssue({ price: 169 }, 1, [{ price: 159 }], 199),
    '后面档位的折后单价不能高于前一档',
  )
  assert.equal(feeTierIssue({ price: 159 }, 1, [{ price: 159 }], 199), '')
})

test('keeps an empty price quiet until save', () => {
  assert.equal(feeTierDisplayIssue({ price: '' }, 0, [], 199), '')
  assert.equal(feeTierDisplayIssue({ price: 209 }, 0, [], 199), '折后单价不能高于普通报名费')
})

test('shows the implied discount for the provider form', () => {
  assert.equal(tierPriceToZhe(159.2, 199), '8 折')
  assert.equal(tierPriceToZhe(150, 199), '7.5 折')
  assert.equal(tierPriceToZhe('', 199), '')
  assert.equal(tierPriceToZhe(159, 0), '')
})
