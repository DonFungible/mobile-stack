import assert from 'node:assert/strict'
import { readdirSync, readFileSync } from 'node:fs'
import { test } from 'node:test'

const read = name =>
  JSON.parse(readFileSync(new URL(name, import.meta.url), 'utf8'))

test('every deep link case names a url and a route', () => {
  const { cases } = read('./deeplinks.json')
  assert.ok(cases.length > 0)
  for (const c of cases) {
    assert.equal(typeof c.url, 'string')
    assert.ok(c.route === null || typeof c.route === 'string')
    if (c.route === 'itemDetail') assert.match(c.id, /^[1-9]\d*$/)
  }
})

const scenarios = readdirSync(new URL('./fixtures', import.meta.url))

test('every scenario is named Area.situation and uses only known fields', () => {
  assert.ok(scenarios.includes('App.default.json'))
  for (const file of scenarios) {
    assert.match(file, /^[A-Z][A-Za-z]*\.[a-z][A-Za-z]*\.json$/, file)
    const unknown = Object.keys(read(`./fixtures/${file}`)).filter(
      key => !['session', 'http', 'flags', 'preferences'].includes(key),
    )
    assert.deepEqual(unknown, [], file)
  }
})

test('every canned response names a method, a bare path and a status', () => {
  for (const file of scenarios) {
    for (const response of read(`./fixtures/${file}`).http ?? []) {
      assert.match(response.method, /^(GET|POST|PUT|PATCH|DELETE)$/, file)
      assert.match(response.path, /^[^/?][^?]*$/, `${file}: ${response.path}`)
      assert.ok(Number.isInteger(response.status), file)
    }
  }
})
