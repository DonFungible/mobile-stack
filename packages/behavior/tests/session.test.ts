import { expect, test } from 'bun:test'
import { createBehaviorSession, fixture } from '../src/session'
import { enterAsGuest } from './journeys'

test('a guest can change a setting, read About, and sign out', async () => {
  await using session = await createBehaviorSession({
    fixtures: [fixture('Items.three')],
  })
  await enterAsGuest(session)

  await session.do({
    action: 'OpenSettings',
    expectation: ({ name, state }) => {
      expect(name).toBe('Settings')
      expect(state.hapticsEnabled).toBe(true)
    },
  })
  await session.do({
    action: 'SetHaptics',
    payload: { enabled: false },
    expectation: ({ state }) => expect(state.hapticsEnabled).toBe(false),
  })

  await session.do({
    action: 'OpenAbout',
    expectation: ({ name, state, path }) => {
      expect(name).toBe('About')
      expect(state.dataSource).toBe('Bundled fixtures')
      expect(state.environment).toBe('development')
      expect(path).toEqual(['App', 'Stack', 'About'])
    },
  })
  await session.do({
    action: 'goBack',
    // the setting survived leaving and re-entering the screen's view model
    expectation: ({ name, state }) => {
      expect(name).toBe('Settings')
      expect(state.hapticsEnabled).toBe(false)
    },
  })

  await session.do({
    action: 'SignOut',
    expectation: ({ name }) => expect(name).toBe('Welcome'),
  })

  // signing back in starts from a fresh stack, not the one left behind
  await session.do({
    action: 'ContinueAsGuest',
    expectation: ({ name, path }) => {
      expect(name).toBe('ItemList')
      expect(path).toEqual(['App', 'Stack', 'ItemList'])
    },
  })
})

test('given a stored session, the app opens straight to the list', async () => {
  await using session = await createBehaviorSession({
    fixtures: [fixture('Items.three'), fixture('Session.signedIn')],
  })
  const node = await session.precondition('ItemList')
  expect(node.actions.map(action => action.title)).toEqual([
    'Refresh',
    'SelectItem',
    'OpenSettings',
    'goBack',
    'openLink',
  ])
})

test('given the server rejects the session, the user lands on Welcome', async () => {
  await using session = await createBehaviorSession({
    fixtures: [fixture('Items.unauthorized'), fixture('Session.signedIn')],
  })
  await session.precondition('Welcome')
})

test('given the server requires a newer version, nothing else is reachable', async () => {
  await using session = await createBehaviorSession({
    fixtures: [
      fixture('Items.three'),
      fixture('Session.signedIn'),
      fixture('Update.required'),
    ],
  })
  const node = await session.precondition('UpdateRequired')
  expect(node.actions.map(action => action.title)).toEqual([
    'OpenStore',
    'openLink',
  ])

  // a link cannot be used to walk around the gate
  await session.do({
    action: 'openLink',
    payload: { url: 'mobilestack://settings' },
    expectation: ({ name }) => expect(name).toBe('UpdateRequired'),
  })
})

test('given the version check fails, the user is let in', async () => {
  await using session = await createBehaviorSession({
    fixtures: [fixture('Items.three'), fixture('Session.signedIn')],
    appVersion: 'not-a-version',
  })
  await session.precondition('ItemList')
})
