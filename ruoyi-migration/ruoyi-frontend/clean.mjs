// issue #6：构建前清理 dist 产物 + Vite 增量缓存
// 用 Node 内置 fs.rmSync（零依赖），跨平台（Windows/Linux/macOS）可靠删除。
// 根因：Vite 增量构建会复用 node_modules/.vite 缓存，且 dist 残留历史 chunk，
// 源码改了但产物不变，导致上线后 UI 不更新。
import { rmSync, existsSync } from 'node:fs'
import { resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(fileURLToPath(import.meta.url), '..')

const targets = ['dist', 'node_modules/.vite']

for (const t of targets) {
  const p = resolve(root, t)
  if (existsSync(p)) {
    rmSync(p, { recursive: true, force: true })
    console.log(`[clean] 已清理 ${t}`)
  } else {
    console.log(`[clean] 跳过（不存在） ${t}`)
  }
}

console.log('[clean] 完成，可执行全量构建')
