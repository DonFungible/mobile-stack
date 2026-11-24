import { expect, test } from 'bun:test'
import contract from '@mobile-stack/contracts/deeplinks.json'
import { createBehaviorSession, fixture } from '../src/session'
import { enterAsGuest } from './journeys'

const SCREEN_FOR_ROUTE: Record<string, string> = {
  home: 'ItemList',
  itemDetail: 'ItemDetail',
  settings: 'Settings',
  about: 'About',
}

const signedIn = [
  fixture('Items.three'),
  fixture('Items.deepLinkTargets'),
  fixture('Session.signedIn'),
]

// The contract file is the single list of links the product supports. Both platforms answer every case.
for (const { url, route, id } of contract.cases) {
  const screen = route ? (SCREEN_FOR_ROUTE[route] ?? route) : 'ItemList'
  const outcome = route ? `opens ${screen}` : 'is ignored'
  test(`link ${url} ${outcome}`, async () => {
    await using session = await createBehaviorSession({ fixtures: signedIn })
    await session.precondition('ItemList')

    await session.do({
      action: 'openLink',
      payload: { url },
      expectation: ({ name, state }) => {
        expect(name).toBe(screen)
        if (id !== undefined) expect(String(state.itemId)).toBe(id)
      },
    })
  })
}

test('a link to a nested screen lands with a working back stack', async () => {
  await using session = await createBehaviorSession({ fixtures: signedIn })
  await session.precondition('ItemList')

  await session.do({
    action: 'openLink',
    payload: { url: 'mobilestack://settings/about' },
    expectation: ({ name }) => expect(name).toBe('About'),
  })
  await session.do({
    action: 'goBack',
    expectation: ({ name }) => expect(name).toBe('Settings'),
  })
  await session.do({
    action: 'goBack',
    expectation: ({ name }) => expect(name).toBe('ItemList'),
  })
})

test('a link that arrives before sign-in is held, then replayed once', async () => {
  await using session = await createBehaviorSession({
    fixtures: [fixture('Items.three')],
  })
  await session.precondition('Welcome')
  await session.do({
    action: 'openLink',
    payload: { url: 'mobilestack://settings' },
    expectation: ({ name }) => expect(name).toBe('Welcome'),
  })

  await session.do({
    action: 'ContinueAsGuest',
    expectation: ({ name }) => expect(name).toBe('Settings'),
  })

  await session.do({
    action: 'SignOut',
    expectation: ({ name }) => expect(name).toBe('Welcome'),
  })
  await enterAsGuest(session)
})
