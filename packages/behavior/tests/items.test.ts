import { expect, test } from 'bun:test'
import { createBehaviorSession, fixture } from '../src/session'
import { enterAsGuest, openItem } from './journeys'

test('a guest can browse the list, open an item and come back', async () => {
  await using session = await createBehaviorSession({
    fixtures: [fixture('Items.three')],
  })
  await enterAsGuest(session)

  await session.do({
    action: 'Refresh',
    expectation: ({ state }) => {
      expect(state.status).toBe('loaded')
      expect(state.isRefreshing).toBe(false)
      expect(state.items.map((item: { id: number }) => item.id)).toEqual([
        1, 2, 3,
      ])
    },
  })

  await openItem(session, 2)
  await session.do({
    action: 'Refresh',
    expectation: ({ state }) => {
      expect(state.status).toBe('loaded')
      expect(state.item.title).toBe('Wire the real API')
      expect(state.canShare).toBe(false)
    },
  })

  await session.do({
    action: 'goBack',
    expectation: ({ name, state }) => {
      expect(name).toBe('ItemList')
      expect(state.items).toHaveLength(3)
    },
  })

  // goBack is always lent by the stack; at the root the navigator decides it does nothing
  await session.do({
    action: 'goBack',
    expectation: ({ name }) => expect(name).toBe('ItemList'),
  })
})

test('selecting an item that is not in the list goes nowhere', async () => {
  await using session = await createBehaviorSession({
    fixtures: [fixture('Items.three')],
  })
  await enterAsGuest(session)
  await session.precondition('ItemList')
  await session.do({
    action: 'Refresh',
    expectation: ({ state }) => expect(state.status).toBe('loaded'),
  })

  await session.do({
    action: 'SelectItem',
    payload: { id: 99 },
    expectation: ({ name }) => expect(name).toBe('ItemList'),
  })
})

test('given no items, the list is loaded and empty', async () => {
  await using session = await createBehaviorSession({
    fixtures: [fixture('Items.empty')],
  })
  await enterAsGuest(session)

  await session.do({
    action: 'Refresh',
    expectation: ({ state }) => {
      expect(state.status).toBe('loaded')
      expect(state.items).toEqual([])
    },
  })
})

test('given the list fails to load, the user sees a message and can retry', async () => {
  await using session = await createBehaviorSession({
    fixtures: [fixture('Items.loadFails')],
  })
  await enterAsGuest(session)

  await session.do({
    action: 'Refresh',
    expectation: ({ state }) => {
      expect(state.status).toBe('failed')
      expect(state.errorMessage).toBe('Something went wrong. Please try again.')
      expect(state.items).toEqual([])
    },
  })
})

test('given sharing is switched on, the detail screen offers it', async () => {
  await using session = await createBehaviorSession({
    fixtures: [fixture('Items.three'), fixture('Flags.sharingOn')],
  })
  await enterAsGuest(session)
  await session.do({
    action: 'Refresh',
    expectation: ({ state }) => expect(state.status).toBe('loaded'),
  })
  await openItem(session, 1)

  await session.do({
    action: 'Share',
    expectation: ({ state }) => expect(state.canShare).toBe(true),
  })
})
