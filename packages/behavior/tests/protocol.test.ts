import { expect, test } from 'bun:test'
import { createBehaviorSession, fixture, type Json } from '../src/session'

test('an action that is not on screen is refused, with what is available', async () => {
  await using session = await createBehaviorSession({
    fixtures: [fixture('Items.three')],
  })
  await session.precondition('Welcome')

  const response = await session.attempt('SignOut')

  expect(response.ok).toBe(false)
  if (response.ok) return
  expect(response.error.code).toBe('unknown_action')
  expect(response.error.available).toEqual(['ContinueAsGuest', 'openLink'])
})

test('a malformed payload is rejected before it reaches the view model', async () => {
  await using session = await createBehaviorSession({
    fixtures: [fixture('Items.three'), fixture('Session.signedIn')],
  })
  const node = await session.precondition('ItemList')
  expect(
    node.actions.find(action => action.title === 'SelectItem')?.input,
  ).toEqual({
    type: 'object',
    properties: { id: { type: 'integer' } },
    required: ['id'],
  })

  const malformed: (Json | undefined)[] = [{ id: 'two' }, {}, undefined]
  for (const payload of malformed) {
    const response = await session.attempt('SelectItem', payload)
    expect(response.ok).toBe(false)
    if (!response.ok) expect(response.error.code).toBe('invalid_payload')
  }
  expect((await session.ls()).name).toBe('ItemList')
})

test('a request with no fixture is a violation, even though the app recovers', async () => {
  await using session = await createBehaviorSession({
    fixtures: [fixture('Session.signedIn')],
  })
  session.violationsExpected = true

  // the screen handles the failure gracefully...
  await session.precondition('ItemList')
  await session.do({
    action: 'Refresh',
    expectation: ({ state }) => expect(state.status).toBe('failed'),
  })

  // ...and the session still reports it, so the test cannot pass by accident
  expect(await session.violations()).toContain('no fixture for GET items')
})
