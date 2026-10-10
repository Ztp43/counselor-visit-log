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
          <div v-for="(cn, k) in EVENT_TYPES" :key="k" class="stat-item">
            <div class="num">{{ latest ? (latest[k] ?? '--') : '--' }}</div>
            <div class="label">{{ cn }}</div>
          </div>
        </div>

        <div class="chart">
          <div v-if="!recent.length" class="empty">暂无统计数据</div>
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

async function load(userId) {
  errorMsg.value = ''
  try {
    stats.value = await monthlyStats(userId ? { userId } : {})
  } catch (e) {
    errorMsg.value = e?.message || '未知错误'
    stats.value = []
  }
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
.stat-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin-bottom: 20px; }
.stat-item { background: #f8fafc; border-radius: 10px; padding: 14px; text-align: center; }
.num { font-size: 26px; font-weight: 700; color: #303133; }
.label { font-size: 12px; color: #909399; margin-top: 4px; }
.chart { margin-top: 8px; }
.chart-cols { display: flex; align-items: flex-end; gap: 12px; height: 140px; }
.chart-col { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: flex-end; height: 100%; }
.chart-val { font-size: 12px; color: #909399; margin-bottom: 4px; }
.chart-bar { width: 60%; background: #409eff; border-radius: 4px 4px 0 0; min-height: 4px; transition: height .3s; }
.chart-bar.zero { background: #e4e7ed; }
.chart-label { font-size: 12px; color: #606266; margin-top: 6px; }
.empty { text-align: center; color: #909399; padding: 30px 0; }
</style>
