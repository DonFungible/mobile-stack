#!/usr/bin/env bun
// Renames the template: app name, bundle / application id, and URL scheme.
//   bun run rename --name Acme --bundle com.acme.app [--scheme acme]
// Run once on a clean tree, then `bun run generate && bun run format && bun run check`
// (a new package name changes import order, which the formatter fixes).
import { execFileSync } from 'node:child_process'
import {
  existsSync,
  lstatSync,
  mkdirSync,
  readdirSync,
  readFileSync,
  renameSync,
  rmdirSync,
  writeFileSync,
} from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const OLD = {
  name: 'MobileStack',
  bundle: 'com.example.mobilestack',
  scheme: 'mobilestack',
}

function parseArgs(argv) {
  const args = {}
  for (let i = 0; i < argv.length; i += 2) {
    args[argv[i].replace(/^--/, '')] = argv[i + 1]
  }
  return args
}

const args = parseArgs(process.argv.slice(2))
const name = args.name
const bundle = args.bundle
const scheme = args.scheme ?? name?.toLowerCase()

const valid =
  /^[A-Z][A-Za-z0-9]*$/.test(name ?? '') &&
  /^[a-z][a-z0-9]*(\.[a-z][a-z0-9]*)+$/.test(bundle ?? '') &&
  /^[a-z][a-z0-9]*$/.test(scheme ?? '')
if (!valid) {
  console.error(
    'Usage: bun run rename --name Acme --bundle com.acme.app [--scheme acme]\n' +
      '  --name    PascalCase, letters and digits\n' +
      '  --bundle  lowercase reverse-DNS, at least two segments\n' +
      '  --scheme  lowercase letters and digits (defaults to the name, lowercased)',
  )
  process.exit(1)
}

const root = join(dirname(fileURLToPath(import.meta.url)), '..')
const files = execFileSync(
  'git',
  ['ls-files', '--cached', '--others', '--exclude-standard'],
  { cwd: root, encoding: 'utf8' },
)
  .split('\n')
  .filter(Boolean)
  .filter(file =>
    lstatSync(join(root, file), { throwIfNoEntry: false })?.isFile(),
  )
  .filter(file => !/(\.keystore|\.jar|\.png|bun\.lock)$/.test(file))
  .filter(file => !file.startsWith('scripts/rename.mjs'))
  .filter(file => !file.startsWith('docs/'))

// Longest match first, so the bundle id is not half-rewritten by the scheme rule.
const replacements = [
  [OLD.bundle.replaceAll('.', '/'), bundle.replaceAll('.', '/')],
  [OLD.bundle, bundle],
  [OLD.name, name],
  [OLD.scheme, scheme],
]

let changed = 0
for (const file of files) {
  const path = join(root, file)
  const before = readFileSync(path, 'utf8')
  const after = replacements.reduce(
    (text, [from, to]) => text.replaceAll(from, to),
    before,
  )
  if (after !== before) {
    writeFileSync(path, after)
    changed += 1
  }
}

function moveTree(from, to) {
  if (!existsSync(from) || from === to) return
  mkdirSync(dirname(to), { recursive: true })
  renameSync(from, to)
  // Drop the now-empty package directories left behind.
  for (
    let dir = dirname(from);
    readdirSync(dir).length === 0;
    dir = dirname(dir)
  ) {
    rmdirSync(dir)
  }
}

for (const module of ['app', 'core', 'cli', 'introspection']) {
  for (const sourceSet of ['main', 'test', 'debug', 'release']) {
    const base = join(root, 'apps/android', module, 'src', sourceSet, 'kotlin')
    moveTree(
      join(base, ...OLD.bundle.split('.')),
      join(base, ...bundle.split('.')),
    )
  }
}

// Files and folders named after the app or its scheme follow the new name.
const tracked = execFileSync(
  'git',
  ['ls-files', '--cached', '--others', '--exclude-standard'],
  { cwd: root, encoding: 'utf8' },
)
  .split('\n')
  .filter(file => /MobileStack|mobilestack/.test(file))
for (const file of tracked) {
  const renamed = file.replaceAll(OLD.name, name).replaceAll(OLD.scheme, scheme)
  if (renamed === file || !existsSync(join(root, file))) continue
  mkdirSync(dirname(join(root, renamed)), { recursive: true })
  renameSync(join(root, file), join(root, renamed))
  for (
    let dir = dirname(join(root, file));
    readdirSync(dir).length === 0;
    dir = dirname(dir)
  ) {
    rmdirSync(dir)
  }
}

console.log(
  `Renamed to ${name} (${bundle}, ${scheme}://) across ${changed} files.\n` +
    'Next: bun run generate && bun run format && bun run check',
)
