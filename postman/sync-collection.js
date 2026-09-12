#!/usr/bin/env node
/**
 * Auto-Sync Script: api-docs.yaml → postman/specs/api-docs.yaml
 *
 * Watches the root api-docs.yaml for changes and automatically copies it
 * to postman/specs/api-docs.yaml so Postman picks up the updated spec
 * and re-syncs the linked "Student Management API" collection.
 *
 * Usage:
 *   node postman/sync-collection.js          # watch mode (keeps running)
 *   node postman/sync-collection.js --once   # one-shot copy and exit
 *
 * Hook into your workflow:
 *   - Git pre-commit:  add `node postman/sync-collection.js --once` to .git/hooks/pre-commit
 *   - Maven build:     add exec-maven-plugin to run this after compile phase
 *   - npm scripts:     "sync-spec": "node postman/sync-collection.js --once"
 */

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname  = path.dirname(__filename);

// ── Paths (relative to project root) ────────────────────────────────────────
const ROOT        = path.resolve(__dirname, '..');
const SOURCE_SPEC = path.join(ROOT, 'api-docs.yaml');
const TARGET_SPEC = path.join(ROOT, 'postman', 'specs', 'api-docs.yaml');

// ── Helpers ──────────────────────────────────────────────────────────────────
function timestamp() {
  return new Date().toISOString().replace('T', ' ').substring(0, 19);
}

function log(msg) {
  console.log(`[${timestamp()}] ${msg}`);
}

function syncSpec() {
  try {
    fs.copyFileSync(SOURCE_SPEC, TARGET_SPEC);
    const size = fs.statSync(TARGET_SPEC).size;
    log(`✅  Synced api-docs.yaml → postman/specs/api-docs.yaml (${size} bytes)`);
    log('    Postman will detect the change and update the linked collection.');
  } catch (err) {
    console.error(`[${timestamp()}] ❌  Sync failed: ${err.message}`);
    process.exit(1);
  }
}

// ── One-shot mode ─────────────────────────────────────────────────────────────
if (process.argv.includes('--once')) {
  log('Running one-shot sync...');
  syncSpec();
  process.exit(0);
}

// ── Watch mode ────────────────────────────────────────────────────────────────
if (!fs.existsSync(SOURCE_SPEC)) {
  console.error(`❌  Source spec not found: ${SOURCE_SPEC}`);
  process.exit(1);
}

log('👀  Watching api-docs.yaml for changes... (Ctrl+C to stop)');
log(`    Source : ${SOURCE_SPEC}`);
log(`    Target : ${TARGET_SPEC}`);

// Debounce to avoid double-fire on some editors
let debounceTimer = null;

fs.watch(SOURCE_SPEC, (eventType) => {
  if (eventType === 'change' || eventType === 'rename') {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
      log(`🔄  Detected change (${eventType}) in api-docs.yaml — syncing...`);
      syncSpec();
    }, 300);
  }
});

// Keep the process alive
process.on('SIGINT', () => {
  log('👋  Watcher stopped.');
  process.exit(0);
});
