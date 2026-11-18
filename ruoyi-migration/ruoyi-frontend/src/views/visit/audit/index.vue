<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>🔍 敏感记录访问审计</span>
        </div>
      </template>

      <!-- 筛选 -->
      <div class="filters">
        <el-input
          v-model="query.viewerName"
          placeholder="按访问人筛选"
          clearable
          style="width:200px"
          @keyup.enter="search"
          @clear="search"
        />
        <el-input
          v-model.number="query.recordId"
          placeholder="按记录ID筛选"
          clearable
          style="width:180px"
          @keyup.enter="search"
          @clear="search"
        />
        <el-button type="primary" size="small" @click="search">查询</el-button>
        <el-button size="small" @click="reset">重置</el-button>
      </div>

      <!-- 列表 -->
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="logId" label="日志ID" width="80" />
        <el-table-column prop="recordId" label="记录ID" width="90" />
        <el-table-column prop="recordType" label="类型" width="110">
          <template #default="scope">
            <el-tag size="small" type="danger">{{ recordTypeCn(scope.row.recordType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="viewerName" label="访问人" width="120" />
        <el-table-column prop="ip" label="访问IP" width="150" />
        <el-table-column prop="accessTime" label="访问时间" min-width="170">
          <template #default="scope">{{ fmtTime(scope.row.accessTime) }}</template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-if="total > 0"
        class="pager"
        layout="total, prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="page"
        @current-change="onPage"
      />

      <div v-if="!loading && !list.length" class="empty">
        <div class="empty-icon">🔍</div>
        <div class="empty-title">暂无审计记录</div>
        <div class="empty-desc">敏感记录（谈心谈话/危机处置）被查看时会自动记录访问行为</div>
      </div>
    </el-card>
  </div>
</template>

<script setup name="Audit">
import { ref, reactive, onMounted } from 'vue'
import { listAccessLogs } from '@/api/visit/audit'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const query = reactive({ viewerName: '', recordId: undefined })

function recordTypeCn(t) {
  return { talk: '谈心谈话', crisis: '危机处置' }[t] || t || '其他'
}

function fmtTime(t) {
  if (!t) return ''
  const d = new Date(t)
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

async function load() {
  loading.value = true
  try {
    const params = {
      pageNum: page.value,
      pageSize,
      viewerName: query.viewerName || undefined,
      recordId: query.recordId || undefined
    }
    const { rows, total: t } = await listAccessLogs(params)
    list.value = rows || []
    total.value = t || 0
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}
function reset() {
  query.viewerName = ''
  query.recordId = undefined
  search()
}
function onPage(p) {
  page.value = p
  load()
}

onMounted(() => load())
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.filters { display: flex; gap: 8px; margin-bottom: 14px; flex-wrap: wrap; }
.pager { margin-top: 14px; display: flex; justify-content: flex-end; }
.empty { text-align: center; color: #909399; padding: 40px 0; }
.empty-icon { font-size: 44px; margin-bottom: 12px; }
.empty-title { font-size: 15px; color: #606266; }
.empty-desc { font-size: 13px; margin-top: 6px; }
</style>
