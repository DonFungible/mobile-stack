import { expect } from 'bun:test'
import type { BehaviorSession } from '../src/session'

// Every signed-out journey starts on Welcome and enters as a guest.
export async function enterAsGuest(session: BehaviorSession) {
  await session.precondition('Welcome')
  await session.do({
    action: 'ContinueAsGuest',
    expectation: ({ name }) => expect(name).toBe('ItemList'),
  })
}

export async function openItem(session: BehaviorSession, id: number) {
  await session.do({
    action: 'SelectItem',
    payload: { id },
    expectation: ({ name, state }) => {
      expect(name).toBe('ItemDetail')
      expect(state.itemId).toBe(id)
    },
  })
}
