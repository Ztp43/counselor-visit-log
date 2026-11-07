<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>📈 总结报告</span>
          <div class="head-btns">
            <el-button size="small" type="primary" @click="printReport">
              <el-icon><Printer /></el-icon>&nbsp;导出 PDF
            </el-button>
          </div>
        </div>
      </template>

      <!-- 周期选择 + 辅导员下钻（领导） -->
      <div class="filters">
        <el-radio-group v-model="period" @change="onPeriodChange">
          <el-radio-button value="week">本周</el-radio-button>
          <el-radio-button value="month">本月</el-radio-button>
          <el-radio-button value="term">本学期</el-radio-button>
          <el-radio-button value="year">本年度</el-radio-button>
        </el-radio-group>
        <el-cascader
          v-if="!isCounselor"
          v-model="selectedPath"
          :options="cascaderOptions"
          :props="{ checkStrictly: true, value: 'value', label: 'label', children: 'children', emitPath: true }"
          placeholder="按 学校 → 学院 → 辅导员 筛选（默认全部）"
          clearable
          style="width:320px"
          @change="onOrgChange"
        />
      </div>

      <!-- 报告正文（打印区） -->
      <div id="report-area" class="report-area" v-loading="loading">
        <template v-if="report">
          <div class="report-title">辅导员工作总结报告</div>
          <div class="report-meta">
            <span>统计周期：{{ report.rangeLabel }}</span>
            <span v-if="report.reporterName">报告人：{{ report.reporterName }}</span>
          </div>

          <!-- AI 文字总结 -->
          <div class="summary-section">
            <div class="section-title">📝 工作总结</div>
            <div class="summary-text">{{ report.summary }}</div>
          </div>

          <!-- 量化指标 -->
          <div class="metrics-section">
            <div class="section-title">📊 量化指标</div>
            <div class="metric-total">
              工作日志总数 <b>{{ metrics.total ?? 0 }}</b> 条
            </div>
            <div class="metric-grid">
              <div v-for="(cn, k) in EVENT_TYPES" :key="k" class="metric-item" :class="'t-' + k">
                <div class="m-num">{{ metrics[k] ?? 0 }}</div>
                <div class="m-label">{{ cn }}</div>
              </div>
            </div>

            <div class="item-metrics">
              <div class="section-title">🎯 处置成效（事项闭环）</div>
              <div class="metric-grid">
                <div class="metric-item e-pending">
                  <div class="m-num">{{ metrics.itemPending ?? 0 }}</div>
                  <div class="m-label">待处理</div>
                </div>
                <div class="metric-item e-following">
                  <div class="m-num">{{ metrics.itemFollowing ?? 0 }}</div>
                  <div class="m-label">跟进中</div>
                </div>
                <div class="metric-item e-closed">
                  <div class="m-num">{{ metrics.itemClosed ?? 0 }}</div>
                  <div class="m-label">已结案</div>
                </div>
                <div class="metric-item e-overdue">
                  <div class="m-num">{{ metrics.itemOverdue ?? 0 }}</div>
                  <div class="m-label">超期</div>
                </div>
              </div>
            </div>
          </div>
        </template>
        <div v-else-if="!loading" class="empty">
          <div class="empty-icon">📈</div>
          <div class="empty-title">暂无报告数据</div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup name="Summary">
import { ref, computed, onMounted } from 'vue'
import { Printer } from '@element-plus/icons-vue'
import useUserStore from '@/store/modules/user'
import { getSummary } from '@/api/visit/summary'
import { orgTree } from '@/api/visit/record'

const userStore = useUserStore()
const roles = computed(() => userStore.roles || [])
const isCounselor = computed(() => roles.value.includes('counselor'))

// 九类工作类型
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

const period = ref('week')
const report = ref(null)
const loading = ref(false)
const metrics = computed(() => report.value?.metrics || {})

// 三级目录（领导）
const orgNodes = ref([])
const selectedPath = ref([])
const cascaderOptions = computed(() =>
  orgNodes.value.map(n => ({
    value: n.collegeId,
    label: n.collegeName,
    children: (n.counselors || []).map(c => ({ value: c.id, label: c.name }))
  }))
)

function onOrgChange(path) {
  const userId = path && path.length >= 2 ? path[path.length - 1] : undefined
  load(userId)
}

// issue #41：el-radio-group 的 change 事件会把选中值（week/month/...）作为参数传给回调，
// 之前直接 @change="load" 导致 load('month') 把 'month' 当成 targetUserId 传给后端 → 类型不匹配。
// 这里用独立 handler 忽略 change 传入的 value，并保留当前已选的辅导员下钻。
function onPeriodChange() {
  const userId = selectedPath.value && selectedPath.value.length >= 2
    ? selectedPath.value[selectedPath.value.length - 1]
    : undefined
  load(userId)
}

async function load(targetUserId) {
  loading.value = true
  try {
    const params = { period: period.value }
    if (targetUserId) params.targetUserId = targetUserId
    report.value = await getSummary(params)
  } catch (e) {
    report.value = null
  } finally {
    loading.value = false
  }
}

// 导出 PDF：调用浏览器打印（打印样式已配好）
function printReport() {
  window.print()
}

onMounted(async () => {
  load()
  if (!isCounselor.value) {
    try {
      orgNodes.value = await orgTree()
    } catch { /* 目录加载失败不影响 */ }
  }
})
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.head-btns { display: flex; gap: 8px; }
.filters { display: flex; gap: 12px; align-items: center; margin-bottom: 16px; flex-wrap: wrap; }

.report-area { min-height: 200px; }
.report-title { font-size: 20px; font-weight: 700; text-align: center; margin-bottom: 8px; color: #303133; }
.report-meta { text-align: center; color: #909399; font-size: 13px; margin-bottom: 20px; display: flex; gap: 16px; justify-content: center; }
.section-title { font-size: 15px; font-weight: 600; color: #303133; margin-bottom: 10px; }
.summary-section { margin-bottom: 20px; }
.summary-text { font-size: 14px; line-height: 1.8; color: #303133; background: #f7f8fa; padding: 14px 16px; border-radius: 8px; }
.metrics-section { }
.metric-total { font-size: 14px; color: #606266; margin-bottom: 10px; }
.metric-total b { font-size: 18px; color: #303133; }
.metric-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin-bottom: 16px; }
.metric-item { border-radius: 8px; padding: 12px; text-align: center; }
.metric-item .m-num { font-size: 22px; font-weight: 700; }
.metric-item .m-label { font-size: 12px; margin-top: 2px; opacity: 0.85; }
.item-metrics { margin-top: 4px; }

/* 九类配色 */
.t-dorm_check  { background: #eaf3ff; } .t-dorm_check  .m-num, .t-dorm_check  .m-label { color: #3375d1; }
.t-hygiene     { background: #e8f7ee; } .t-hygiene     .m-num, .t-hygiene     .m-label { color: #2aa86a; }
.t-talk        { background: #fdf0f5; } .t-talk        .m-num, .t-talk        .m-label { color: #d6608a; }
.t-class_check { background: #f3efff; } .t-class_check .m-num, .t-class_check .m-label { color: #7a5cd6; }
.t-meeting     { background: #fff6e5; } .t-meeting     .m-num, .t-meeting     .m-label { color: #e6a23c; }
.t-visit_help  { background: #e6f9f7; } .t-visit_help  .m-num, .t-visit_help  .m-label { color: #0fa8a0; }
.t-crisis      { background: #fdecea; } .t-crisis      .m-num, .t-crisis      .m-label { color: #e5543f; }
.t-admin       { background: #eef3f8; } .t-admin       .m-num, .t-admin       .m-label { color: #5b7a99; }
.t-other       { background: #f4f4f5; } .t-other       .m-num, .t-other       .m-label { color: #909399; }

/* 事项成效配色 */
.e-pending   { background: #fdecea; } .e-pending   .m-num, .e-pending   .m-label { color: #f56c6c; }
.e-following { background: #fff6e5; } .e-following .m-num, .e-following .m-label { color: #e6a23c; }
.e-closed    { background: #e8f7ee; } .e-closed    .m-num, .e-closed    .m-label { color: #2aa86a; }
.e-overdue   { background: #f4f4f5; } .e-overdue   .m-num, .e-overdue   .m-label { color: #909399; }

.empty { text-align: center; color: #909399; padding: 40px 0; }
.empty-icon { font-size: 44px; margin-bottom: 12px; }
.empty-title { font-size: 15px; color: #606266; }

/* 打印样式：只打印报告区 */
@media print {
  :deep(.app-container) { padding: 0; }
  .head-btns, .filters { display: none; }
  .report-area { border: none; }
}
</style>

<style>
/* issue #40：打印时只导出报告正文，隐藏若依全局布局（侧边栏/导航/页签/卡片头） */
@media print {
  .sidebar-container,
  .navbar,
  .tags-view-container,
  .el-card__header {
    display: none !important;
  }
  .main-container {
    margin-left: 0 !important;
  }
  .app-main {
    padding: 0 !important;
  }
  .app-container {
    padding: 0 !important;
  }
}
</style>
