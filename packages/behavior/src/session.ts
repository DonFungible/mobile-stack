// The behavior-test SDK: a thin client for the introspection protocol.
// It boots a headless core (no simulator, no emulator), speaks newline-delimited JSON to it,
// and adds two helpers on top of bun:test: `precondition` and `do`.

import { fileURLToPath } from 'node:url'

const root = fileURLToPath(new URL('../../../', import.meta.url))

const HOSTS = {
  ios: 'apps/ios/Packages/AppCore/.build/debug/mobilestack-cli',
  android: 'apps/android/cli/build/install/cli/bin/cli',
} as const

export type Platform = keyof typeof HOSTS

export type Json =
  | null
  | boolean
  | number
  | string
  | Json[]
  | { [key: string]: Json }

export type Node = {
  name: string
  path: string[]
  // biome-ignore lint/suspicious/noExplicitAny: state is whatever the focused screen exposes
  state: Record<string, any>
  actions: { title: string; input?: Json }[]
}

type Response =
  | { id: number; ok: true; node: Node; violations: string[] }
  | {
      id: number
      ok: false
      error: { code: string; message: string; available?: string[] }
    }

export type Step = {
  action: string
  payload?: Json
  expectation?: (node: Node) => void
}

export type SessionOptions = {
  /** Scenario names from packages/contracts/fixtures. `App.default` is always loaded first. */
  fixtures?: string[]
  appVersion?: string
}

const POLL_MS = 100
const TIMEOUT_MS = Number(process.env.BEHAVIOR_TIMEOUT_MS ?? 2000)
// The first answer also covers process start-up, which on a cold JVM can exceed a normal step.
const BOOT_TIMEOUT_MS = 15_000

export function currentPlatform(): Platform {
  const platform = process.env.PLATFORM
  if (platform !== 'ios' && platform !== 'android') {
    throw new Error('Set PLATFORM=ios or PLATFORM=android')
  }
  return platform
}

export function fixture(name: string): string {
  return name
}

export class BehaviorError extends Error {}

export class BehaviorSession implements AsyncDisposable {
  private nextId = 1
  private booted = false
  private buffer = ''
  private readonly lines: string[] = []
  private readonly decoder = new TextDecoder()
  private readonly reader

  constructor(
    private readonly host: Bun.Subprocess<'pipe', 'pipe', 'inherit'>,
  ) {
    this.reader = host.stdout.getReader()
  }

  /** Replaces the running core with a fresh one on these fixtures. */
  async boot(fixtures: string[], appVersion?: string): Promise<void> {
    this.booted = false
    this.violationsExpected = false
    const response = await this.request({
      command: 'boot',
      fixtures,
      appVersion,
    })
    if (!response.ok) throw new BehaviorError(response.error.message)
  }

  /** The focused node, read fresh. */
  async ls(): Promise<Node> {
    const response = await this.request({ command: 'ls' })
    if (!response.ok) throw new BehaviorError(response.error.message)
    return response.node
  }

  /** Waits until the focused node is the one a journey expects to start from. */
  async precondition(name: string): Promise<Node> {
    return this.poll(`focused node to be ${name}`, node => {
      if (node.name !== name) {
        throw new BehaviorError(`focused node is ${node.name}`)
      }
    })
  }

  /** Runs an action, then polls the focused node until the expectation passes. */
  async do({ action, payload, expectation }: Step): Promise<Node> {
    const response = await this.request({ command: 'do', action, payload })
    if (!response.ok) {
      throw new BehaviorError(`do ${action}: ${response.error.message}`)
    }
    if (!expectation) return response.node
    return this.poll(`expectation after ${action}`, expectation)
  }

  /** The raw answer to an action, for asserting on refusals. */
  async attempt(action: string, payload?: Json): Promise<Response> {
    return this.request({ command: 'do', action, payload })
  }

  /** Requests that had no fixture, or tried to reach the network. A passing test has none. */
  async violations(): Promise<string[]> {
    const response = await this.request({ command: 'ls' })
    return response.ok ? response.violations : []
  }

  async [Symbol.asyncDispose](): Promise<void> {
    const violations = await this.violations().catch(() => [])
    if (violations.length > 0 && !this.violationsExpected) {
      throw new BehaviorError(
        `Zero-network guard: ${violations.join('; ')}. Every request needs a fixture.`,
      )
    }
  }

  /** For the rare test that is about the guard itself. */
  violationsExpected = false

  private async poll(what: string, check: (node: Node) => void): Promise<Node> {
    const deadline = Date.now() + (this.booted ? TIMEOUT_MS : BOOT_TIMEOUT_MS)
    let last: Node | undefined
    let failure: unknown
    do {
      last = await this.ls()
      try {
        check(last)
        this.booted = true
        return last
      } catch (error) {
        failure = error
      }
      await Bun.sleep(POLL_MS)
    } while (Date.now() < deadline)
    const reason = failure instanceof Error ? failure.message : String(failure)
    throw new BehaviorError(
      `Timed out waiting for ${what}.\n${reason}\nLast seen: ${last.name} ${JSON.stringify(last.state)}`,
    )
  }

  private async request(body: Record<string, unknown>): Promise<Response> {
    const id = this.nextId++
    this.host.stdin.write(`${JSON.stringify({ id, ...body })}\n`)
    await this.host.stdin.flush()
    const line = await this.readLine()
    const response = JSON.parse(line) as Response
    if (response.id !== id) {
      throw new BehaviorError(`out-of-order response: ${line}`)
    }
    return response
  }

  private async readLine(): Promise<string> {
    while (this.lines.length === 0) {
      const { value, done } = await this.reader.read()
      if (done) throw new BehaviorError('the core process exited')
      this.buffer += this.decoder.decode(value, { stream: true })
      const parts = this.buffer.split('\n')
      this.buffer = parts.pop() ?? ''
      this.lines.push(...parts.filter(Boolean))
    }
    return this.lines.shift() as string
  }
}

let host: BehaviorSession | undefined

/**
 * Boots a headless core with canned responses. Use with `await using` so violations are checked when
 * the test ends.
 *
 * One host process serves the whole run and each call boots a fresh core inside it, so tests share
 * nothing but the warm process. That also means tests must not run concurrently.
 */
export async function createBehaviorSession(
  options: SessionOptions = {},
): Promise<BehaviorSession> {
  host ??= new BehaviorSession(
    Bun.spawn(
      [
        root + HOSTS[currentPlatform()],
        '--fixtures-dir',
        `${root}packages/contracts/fixtures`,
      ],
      { stdin: 'pipe', stdout: 'pipe', stderr: 'inherit' },
    ),
  )
  await host.boot(
    ['App.default', ...(options.fixtures ?? [])],
    options.appVersion,
  )
  return host
}
