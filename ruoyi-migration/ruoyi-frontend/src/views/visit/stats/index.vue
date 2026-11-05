<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <span>📊 月度统计</span>
      </template>

      <!-- 领导/管理员：三级目录下钻筛选 -->
      <div v-if="!isCounselor" class="org-filter">
        <el-cascader
          v-model="selectedPath"
          :options="cascaderOptions"
          :props="{ checkStrictly: true, value: 'value', label: 'label', children: 'children', emitPath: true }"
          placeholder="按 学校 → 学院 → 辅导员 筛选"
          clearable
          style="width:100%"
          @change="onOrgChange"
        />
      </div>

      <div v-if="errorMsg" class="empty">
        <div>统计加载失败：{{ errorMsg }}</div>
        <el-button size="small" style="margin-top:8px" @click="load()">重试</el-button>
      </div>
      <template v-else>
        <div class="stat-grid">
          <div v-for="(cn, k) in EVENT_TYPES" :key="k" class="stat-item" :class="'t-' + k">
            <div class="num">{{ latest ? (latest[k] ?? '--') : '--' }}</div>
            <div class="label">{{ cn }}</div>
          </div>
        </div>

        <!-- 处置成效（issue #12：区分工作量与处置成效） -->
        <div class="effect-block">
          <div class="effect-title">🎯 处置成效（事项闭环）</div>
          <div class="effect-grid">
            <div class="effect-item e-pending">
              <div class="enum">{{ effect.pending ?? '--' }}</div>
              <div class="elabel">待处理</div>
            </div>
            <div class="effect-item e-following">
              <div class="enum">{{ effect.following ?? '--' }}</div>
              <div class="elabel">跟进中</div>
            </div>
            <div class="effect-item e-closed">
              <div class="enum">{{ effect.closed ?? '--' }}</div>
              <div class="elabel">已结案</div>
            </div>
            <div class="effect-item e-rate">
              <div class="enum">{{ effectClosedRate }}</div>
              <div class="elabel">闭环率</div>
            </div>
          </div>
          <div class="effect-hint">已结案事项 = 处置闭环的学生问题；闭环率 = 已结案 ÷ 全部事项</div>
        </div>

        <div class="chart">
          <div v-if="!recent.length" class="empty empty-state">
            <div class="empty-icon">📊</div>
            <div class="empty-title">暂无统计数据</div>
            <div class="empty-desc">录入工作记录后，这里会自动生成月度统计</div>
          </div>
          <div v-else class="chart-cols">
            <div v-for="s in recent" :key="s.month" class="chart-col">
              <div class="chart-val">{{ s.total }}</div>
              <div class="chart-bar" :class="{ zero: s.total === 0 }" :style="{ height: barHeight(s.total, maxTotal) }"></div>
              <div class="chart-label">{{ monthLabel(s.month) }}</div>
            </div>
          </div>
        </div>
      </template>
    </el-card>
  </div>
</template>

<script setup name="Stats">
import { ref, computed, onMounted } from 'vue'
import useUserStore from '@/store/modules/user'
import { monthlyStats, orgTree } from '@/api/visit/record'
import { itemStats } from '@/api/visit/item'

const userStore = useUserStore()
const roles = computed(() => userStore.roles || [])
const isCounselor = computed(() => roles.value.includes('counselor'))

// 九类工作类型（与后端 EventTypes 一致）
const EVENT_TYPES = {
  dorm_check: '查寝点名',
  hygiene: '安全卫生',
  talk: '谈心谈话',
  class_check: '查课听课',
  meeting: '班会活动',
  visit_help: '走访帮扶',
  crisis: '危机处置',
  admin: '事务工作',
  other: '其他'
}

const stats = ref([])
const errorMsg = ref('')
const effect = ref({})   // 处置成效（事项闭环数据）

// 三级目录（领导/管理员可见）
const orgNodes = ref([])
const selectedPath = ref([])

const cascaderOptions = computed(() => {
  return orgNodes.value.map(n => ({
    value: n.collegeId,
    label: n.collegeName,
    children: (n.counselors || []).map(c => ({ value: c.id, label: c.name }))
  }))
})

function onOrgChange(path) {
  if (path && path.length >= 2) {
    load(path[path.length - 1])
  } else {
    load(undefined)
  }
}

const latest = computed(() => stats.value.length ? stats.value[stats.value.length - 1] : null)
const recent = computed(() => stats.value.slice(-6))
const maxTotal = computed(() => Math.max(...recent.value.map(s => s.total), 1))

function barHeight(v, max) {
  return v > 0 ? (v / max * 82) + '%' : '4px'
}

function monthLabel(m) {
  return parseInt(m.slice(5), 10) + '月'
}

// 处置成效：闭环率 = 已结案 / 全部
const effectClosedRate = computed(() => {
  const total = Number(effect.value.total || 0)
  const closed = Number(effect.value.closed || 0)
  if (total === 0) return '--'
  return Math.round(closed / total * 100) + '%'
})

async function load(userId) {
  errorMsg.value = ''
  try {
    stats.value = await monthlyStats(userId ? { userId } : {})
  } catch (e) {
    errorMsg.value = e?.message || '未知错误'
    stats.value = []
  }
  // 处置成效（事项闭环）：失败不影响主统计
  try {
    effect.value = await itemStats()
  } catch { effect.value = {} }
}

onMounted(async () => {
  load()
  if (!isCounselor.value) {
    try {
      orgNodes.value = await orgTree()
    } catch { /* 目录加载失败不影响统计 */ }
  }
})
</script>

<style scoped>
.org-filter { margin-bottom: 14px; }
.stat-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 20px; }
.stat-item {
  border-radius: 10px; padding: 16px 14px; text-align: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  transition: transform .2s, box-shadow .2s;
}
.stat-item:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
.num { font-size: 28px; font-weight: 700; }
.label { font-size: 12px; margin-top: 4px; opacity: 0.85; }

/* 九类工作各自主题色（浅底 + 深色字，数据一眼可辨） */
.t-dorm_check  { background: #eaf3ff; } .t-dorm_check  .num { color: #3375d1; } .t-dorm_check  .label { color: #3375d1; }
.t-hygiene     { background: #e8f7ee; } .t-hygiene     .num { color: #2aa86a; } .t-hygiene     .label { color: #2aa86a; }
.t-talk        { background: #fdf0f5; } .t-talk        .num { color: #d6608a; } .t-talk        .label { color: #d6608a; }
.t-class_check { background: #f3efff; } .t-class_check .num { color: #7a5cd6; } .t-class_check .label { color: #7a5cd6; }
.t-meeting     { background: #fff6e5; } .t-meeting     .num { color: #e6a23c; } .t-meeting     .label { color: #e6a23c; }
.t-visit_help  { background: #e6f9f7; } .t-visit_help  .num { color: #0fa8a0; } .t-visit_help  .label { color: #0fa8a0; }
.t-crisis      { background: #fdecea; } .t-crisis      .num { color: #e5543f; } .t-crisis      .label { color: #e5543f; }
.t-admin       { background: #eef3f8; } .t-admin       .num { color: #5b7a99; } .t-admin       .label { color: #5b7a99; }
.t-other       { background: #f4f4f5; } .t-other       .num { color: #909399; } .t-other       .label { color: #909399; }

/* 处置成效区块（issue #12） */
.effect-block { margin-bottom: 20px; padding: 14px; border-radius: 10px; background: #fbfbfc; border: 1px solid #f0f0f0; }
.effect-title { font-size: 14px; font-weight: 600; color: #303133; margin-bottom: 12px; }
.effect-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.effect-item { border-radius: 8px; padding: 12px; text-align: center; }
.effect-item .enum { font-size: 24px; font-weight: 700; }
.effect-item .elabel { font-size: 12px; margin-top: 4px; opacity: 0.85; }
.e-pending   { background: #fdecea; } .e-pending   .enum { color: #f56c6c; } .e-pending   .elabel { color: #f56c6c; }
.e-following { background: #fff6e5; } .e-following .enum { color: #e6a23c; } .e-following .elabel { color: #e6a23c; }
.e-closed    { background: #e8f7ee; } .e-closed    .enum { color: #2aa86a; } .e-closed    .elabel { color: #2aa86a; }
.e-rate      { background: #eaf3ff; } .e-rate      .enum { color: #3375d1; } .e-rate      .elabel { color: #3375d1; }
.effect-hint { font-size: 12px; color: #909399; margin-top: 10px; }

.chart { margin-top: 8px; }
.chart-cols { display: flex; align-items: flex-end; gap: 12px; height: 140px; }
.chart-col { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: flex-end; height: 100%; }
.chart-val { font-size: 12px; color: #909399; margin-bottom: 4px; }
.chart-bar { width: 60%; background: #409eff; border-radius: 4px 4px 0 0; min-height: 4px; transition: height .3s; }
.chart-bar.zero { background: #e4e7ed; }
.chart-label { font-size: 12px; color: #606266; margin-top: 6px; }
.empty { text-align: center; color: #909399; padding: 30px 0; }
.empty-state { padding: 48px 0; }
.empty-icon { font-size: 44px; margin-bottom: 12px; }
.empty-title { font-size: 15px; color: #606266; margin-bottom: 6px; }
.empty-desc { font-size: 13px; color: #909399; }
</style>

<style scoped>
/* ---------- 深色模式适配（issue #2）：9 类卡片深色底 + 亮色字 ---------- */
.dark .t-dorm_check  { background: rgba(51,117,209,0.16); } .dark .t-dorm_check  .num, .dark .t-dorm_check  .label { color: #82b1ec; }
.dark .t-hygiene     { background: rgba(42,168,106,0.16); } .dark .t-hygiene     .num, .dark .t-hygiene     .label { color: #6fd3a0; }
.dark .t-talk        { background: rgba(214,96,138,0.16); } .dark .t-talk        .num, .dark .t-talk        .label { color: #ef9cbc; }
.dark .t-class_check { background: rgba(122,92,214,0.18); } .dark .t-class_check .num, .dark .t-class_check .label { color: #ab97ea; }
.dark .t-meeting     { background: rgba(230,162,60,0.16); } .dark .t-meeting     .num, .dark .t-meeting     .label { color: #f2c379; }
.dark .t-visit_help  { background: rgba(15,168,160,0.16); } .dark .t-visit_help  .num, .dark .t-visit_help  .label { color: #63d6cf; }
.dark .t-crisis      { background: rgba(229,84,63,0.16); }  .dark .t-crisis      .num, .dark .t-crisis      .label { color: #f5927f; }
.dark .t-admin       { background: rgba(91,122,153,0.18); } .dark .t-admin       .num, .dark .t-admin       .label { color: #92aec9; }
.dark .t-other       { background: rgba(144,147,153,0.14); } .dark .t-other       .num, .dark .t-other       .label { color: #a3a6ad; }

/* 成效区块深色 */
.dark .effect-block { background: #1d1e20; border-color: #2a2b2e; }
.dark .effect-title { color: #e5e6eb; }
.dark .effect-hint { color: #8a8d93; }
.dark .e-pending   { background: rgba(245,108,108,0.16); } .dark .e-pending   .enum, .dark .e-pending   .elabel { color: #f5927f; }
.dark .e-following { background: rgba(230,162,60,0.16); } .dark .e-following .enum, .dark .e-following .elabel { color: #f2c379; }
.dark .e-closed    { background: rgba(42,168,106,0.16); } .dark .e-closed    .enum, .dark .e-closed    .elabel { color: #6fd3a0; }
.dark .e-rate      { background: rgba(51,117,209,0.16); } .dark .e-rate      .enum, .dark .e-rate      .elabel { color: #82b1ec; }
</style>
