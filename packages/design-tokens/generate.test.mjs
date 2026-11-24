import assert from 'node:assert/strict'
import { test } from 'node:test'
import { hexToArgb, renderKotlin, renderSwift } from './generate.mjs'

const tokens = {
  color: { accent: '#ff6a1a' },
  spacing: { lg: 16 },
  radius: { md: 6 },
  typography: { body: { size: 15, lineHeight: 20, weight: 400 } },
  motion: { pressFadeMs: 150 },
}

test('hexToArgb adds an opaque alpha channel', () => {
  assert.equal(hexToArgb('#ff6a1a'), '0xFFFF6A1A')
})

test('hexToArgb rejects shorthand and alpha hex', () => {
  assert.throws(() => hexToArgb('#fff'))
  assert.throws(() => hexToArgb('#ff6a1a80'))
})

test('both platforms receive every token', () => {
  const swift = renderSwift(tokens)
  const kotlin = renderKotlin(tokens, 'com.example')
  assert.match(swift, /static let accent: UInt32 = 0xFFFF6A1A/)
  assert.match(swift, /static let pressFade: TimeInterval = 0.15/)
  assert.match(kotlin, /val Accent = Color\(0xFFFF6A1A\)/)
  assert.match(kotlin, /val Lg = 16\.dp/)
  assert.match(kotlin, /const val PressFadeMs = 150/)
})
