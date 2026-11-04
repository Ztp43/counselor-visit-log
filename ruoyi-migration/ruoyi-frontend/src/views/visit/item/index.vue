<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>📋 事项台账</span>
          <div class="head-btns">
            <el-button v-if="canEdit" size="small" type="primary" @click="openAdd"><el-icon><Plus /></el-icon>&nbsp;新建事项</el-button>
          </div>
        </div>
      </template>

      <!-- 统计条（各状态数量 + 超期提醒） -->
      <div class="stats-bar">
        <span class="stat-item">全部 <b>{{ stats.total || 0 }}</b></span>
        <span class="stat-item stat-pending">待处理 <b>{{ stats.pending || 0 }}</b></span>
        <span class="stat-item stat-following">跟进中 <b>{{ stats.following || 0 }}</b></span>
        <span class="stat-item stat-closed">已结案 <b>{{ stats.closed || 0 }}</b></span>
        <span class="stat-item stat-overdue" :class="{ 'has-overdue': (stats.overdue || 0) > 0 }">
          超期 <b>{{ stats.overdue || 0 }}</b>
        </span>
      </div>

      <!-- 筛选 -->
      <div class="filters">
        <el-input v-model="query.title" placeholder="搜索标题 / 学生…" clearable style="flex:1;min-width:160px" @input="onSearch" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width:130px" @change="onSearch">
          <el-option label="待处理" value="pending" />
          <el-option label="跟进中" value="following" />
          <el-option label="已结案" value="closed" />
        </el-select>
        <el-select v-model="query.category" placeholder="类别" clearable style="width:130px" @change="onSearch">
          <el-option v-for="(cn, k) in CATEGORIES" :key="k" :label="cn" :value="k" />
        </el-select>
      </div>

      <div v-if="loading" class="empty">加载中…</div>
      <div v-else-if="!items.length" class="empty empty-state">
        <div class="empty-icon">📋</div>
        <div class="empty-title">暂无跟进事项</div>
        <div class="empty-desc">需要持续跟进的学生问题，可在这里建事项、跟踪闭环</div>
      </div>
      <el-table v-else :data="items" style="width:100%">
        <el-table-column prop="itemNo" label="编号" width="130" />
        <el-table-column prop="title" label="事项标题" min-width="180" show-overflow-tooltip />
        <el-table-column label="类别" width="100">
          <template #default="scope">{{ categoryCn(scope.row.category) }}</template>
        </el-table-column>
        <el-table-column prop="studentName" label="涉及学生" width="110" />
        <el-table-column label="状态" width="90">
          <template #default="scope">
            <el-tag :type="statusTag(scope.row.status)" size="small">{{ statusCn(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assigneeName" label="责任人" width="100" />
        <el-table-column label="截止" width="120">
          <template #default="scope">
            <span v-if="isOverdue(scope.row)" class="overdue-text">超期 {{ overdueDays(scope.row) }} 天</span>
            <span v-else>{{ scope.row.dueDate ? scope.row.dueDate.slice(0, 10) : '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="scope">
            <el-button link type="info" size="small" @click="openDetail(scope.row)">详情</el-button>
            <el-button v-if="canEdit" link type="primary" size="small" @click="openEdit(scope.row)">编辑</el-button>
            <el-button v-if="canEdit && scope.row.status !== 'closed'" link type="success" size="small" @click="doClose(scope.row)">结案</el-button>
            <el-button v-if="canEdit" link type="danger" size="small" @click="doDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > pageSize"
        layout="prev, pager, next, total"
        :total="total"
        :page-size="pageSize"
        :current-page="page"
        @current-change="onPageChange"
        style="margin-top:16px;justify-content:center"
      />
    </el-card>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editing ? '✏️ 编辑事项' : '📋 新建事项'" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="事项标题" required>
          <el-input v-model="form.title" placeholder="一句话说清要跟进什么事" />
        </el-form-item>
        <el-form-item label="问题类别" required>
          <el-select v-model="form.category" style="width:100%">
            <el-option v-for="(cn, k) in CATEGORIES" :key="k" :label="cn" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="涉及学生">
          <el-input v-model="form.studentName" placeholder="没有具体学生可留空；多人用「、」分隔" />
        </el-form-item>
        <el-form-item label="责任人">
          <el-select v-model="form.assigneeId" placeholder="不指派（默认创建人跟进）" clearable style="width:100%">
            <el-option v-for="a in assignees" :key="a.userId" :label="a.name" :value="a.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="事项详情">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="问题的具体情况、背景、进展…" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="待处理" value="pending" />
            <el-option label="跟进中" value="following" />
            <el-option label="已结案" value="closed" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker v-model="form.dueDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 事项详情 + 跟进时间线 -->
    <el-dialog v-model="detailVisible" :title="detailItem ? `${detailItem.itemNo} · ${detailItem.title}` : '事项详情'" width="560px">
      <div v-if="detailItem" class="detail-meta">
        <el-tag :type="statusTag(detailItem.status)" size="small">{{ statusCn(detailItem.status) }}</el-tag>
        <span class="meta-item">类别：{{ categoryCn(detailItem.category) }}</span>
        <span class="meta-item">责任人：{{ detailItem.assigneeName || '—' }}</span>
        <span class="meta-item">截止：{{ detailItem.dueDate ? detailItem.dueDate.slice(0, 10) : '—' }}</span>
      </div>
      <div v-if="detailItem" class="detail-desc">{{ detailItem.description || '（无详情描述）' }}</div>

      <div class="timeline-title">跟进时间线</div>
      <div v-if="!logs.length" class="empty">暂无跟进记录</div>
      <el-timeline v-else>
        <el-timeline-item
          v-for="lg in logs"
          :key="lg.logId"
          :type="actionType(lg.action)"
          :timestamp="fmtTime(lg.createTime)"
        >
          <div class="log-content">{{ actionLabel(lg.action) }}：{{ lg.content }}</div>
          <div class="log-by">{{ lg.createBy }}</div>
        </el-timeline-item>
      </el-timeline>

      <div v-if="canEdit && detailItem && detailItem.status !== 'closed'" class="follow-row">
        <el-input v-model="followContent" placeholder="记录一次跟进进展，如「已联系家长，约定下周面谈」" />
        <el-button type="primary" size="small" @click="addFollow">新增跟进</el-button>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Item">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import useUserStore from '@/store/modules/user'
import { listItems, addItem, updateItem, deleteItem, listItemLogs, addFollowLog, listAssignees, itemStats } from '@/api/visit/item'

const userStore = useUserStore()
const roles = computed(() => userStore.roles || [])
const isAdmin = computed(() => roles.value.includes('admin'))
const isCounselor = computed(() => roles.value.includes('counselor'))

// 九类工作类型（与后端 EventTypes 一致，作为事项的问题类别）
const CATEGORIES = {
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

// 状态
const STATUS = { pending: '待处理', following: '跟进中', closed: '已结案' }
function categoryCn(k) { return CATEGORIES[k] || k || '其他' }
function statusCn(s) { return STATUS[s] || s || '待处理' }
function statusTag(s) {
  return { pending: 'danger', following: 'warning', closed: 'success' }[s] || 'info'
}

// 能否编辑：管理员=全部；辅导员=全部（事项本就是辅导员跟进自己的学生）
const canEdit = computed(() => isAdmin.value || isCounselor.value)

const items = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const loading = ref(false)
const query = reactive({ title: '', status: '', category: '' })

async function load() {
  loading.value = true
  try {
    const params = { pageNum: page.value, pageSize }
    if (query.title.trim()) params.title = query.title.trim()
    if (query.status) params.status = query.status
    if (query.category) params.category = query.category
    const { rows, total: t } = await listItems(params)
    items.value = rows
    total.value = t
  } catch (e) {
    ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

let timer = null
function onSearch() {
  clearTimeout(timer)
  timer = setTimeout(() => { page.value = 1; load() }, 300)
}
function onPageChange(p) { page.value = p; load() }

// 新建/编辑
const dialogVisible = ref(false)
const editing = ref(false)
const form = reactive({})
let editingId = null
const assignees = ref([])

async function loadAssignees() {
  try {
    assignees.value = await listAssignees()
  } catch { assignees.value = [] }
}

// ---------- 统计 + 超期 ----------
const stats = ref({})
async function loadStats() {
  try {
    stats.value = await itemStats()
  } catch { stats.value = {} }
}

function isOverdue(row) {
  return row.status !== 'closed' && row.dueDate && new Date(row.dueDate) < new Date(todayOnly())
}
function overdueDays(row) {
  if (!row.dueDate) return 0
  const diff = new Date(todayOnly()) - new Date(row.dueDate)
  return Math.floor(diff / (1000 * 60 * 60 * 24))
}
function todayOnly() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function openAdd() {
  editing.value = false
  editingId = null
  Object.keys(form).forEach(k => delete form[k])
  form.status = 'pending'
  loadAssignees()
  dialogVisible.value = true
}

function openEdit(row) {
  editing.value = true
  editingId = row.itemId
  form.title = row.title
  form.category = row.category
  form.studentName = row.studentName
  form.assigneeId = row.assigneeId
  form.description = row.description
  form.status = row.status
  form.dueDate = row.dueDate ? row.dueDate.slice(0, 10) : ''
  dialogVisible.value = true
}

async function save() {
  if (!form.title || !form.title.trim()) { ElMessage.warning('请填写事项标题'); return }
  if (!form.category) { ElMessage.warning('请选择问题类别'); return }
  try {
    if (editing.value) {
      await updateItem({ itemId: editingId, ...form })
    } else {
      await addItem({ ...form })
    }
    dialogVisible.value = false
    ElMessage.success('已保存')
    load()
    loadStats()
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  }
}

async function doClose(row) {
  try {
    await ElMessageBox.confirm(`将事项「${row.title}」标记为已结案？`, '提示', { type: 'warning' })
  } catch { return }
  try {
    await updateItem({ itemId: row.itemId, status: 'closed' })
    ElMessage.success('已结案')
    load()
    loadStats()
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  }
}

async function doDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除事项「${row.title}」吗？删除后不可恢复。`, '警告', { type: 'warning' })
  } catch { return }
  try {
    await deleteItem(row.itemId)
    ElMessage.success('已删除')
    load()
    loadStats()
  } catch (e) {
    ElMessage.error(e?.message || '删除失败')
  }
}

onMounted(() => { load(); loadAssignees(); loadStats() })

// ---------- 详情 + 跟进时间线 ----------
const detailVisible = ref(false)
const detailItem = ref(null)
const logs = ref([])
const followContent = ref('')

function actionLabel(a) {
  return { create: '创建', follow: '跟进', status: '状态变更', remark: '备注' }[a] || a
}
function actionType(a) {
  return { create: 'primary', follow: 'success', status: 'warning', remark: 'info' }[a] || 'primary'
}
function fmtTime(t) {
  if (!t) return ''
  const d = new Date(t)
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

async function openDetail(row) {
  detailItem.value = row
  detailVisible.value = true
  logs.value = []
  followContent.value = ''
  try {
    logs.value = await listItemLogs(row.itemId)
  } catch {
    logs.value = []
  }
}

async function addFollow() {
  if (!followContent.value.trim()) { ElMessage.warning('请填写跟进内容'); return }
  try {
    await addFollowLog(detailItem.value.itemId, followContent.value.trim())
    followContent.value = ''
    ElMessage.success('已记录跟进')
    logs.value = await listItemLogs(detailItem.value.itemId)
  } catch (e) {
    ElMessage.error(e?.message || '记录失败')
  }
}
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.head-btns { display: flex; gap: 8px; }
.filters { display: flex; gap: 8px; margin-bottom: 14px; flex-wrap: wrap; }
.stats-bar { display: flex; gap: 18px; align-items: center; padding: 10px 14px; background: #f7f8fa; border-radius: 8px; margin-bottom: 14px; flex-wrap: wrap; }
.stat-item { font-size: 13px; color: #606266; }
.stat-item b { font-size: 16px; margin-left: 2px; }
.stat-pending b { color: #f56c6c; }
.stat-following b { color: #e6a23c; }
.stat-closed b { color: #67c23a; }
.stat-overdue b { color: #909399; }
.stat-overdue.has-overdue b { color: #f56c6c; }
.stat-overdue.has-overdue { font-weight: 600; }
.overdue-text { color: #f56c6c; font-weight: 600; font-size: 13px; }
.empty { text-align: center; color: #909399; padding: 30px 0; font-size: 14px; }
.empty-state { padding: 48px 0; }
.empty-icon { font-size: 44px; margin-bottom: 12px; }
.empty-title { font-size: 15px; color: #606266; margin-bottom: 6px; }
.empty-desc { font-size: 13px; color: #909399; }
.detail-meta { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; margin-bottom: 10px; }
.meta-item { font-size: 13px; color: #606266; }
.detail-desc { font-size: 14px; color: #303133; background: #f7f8fa; padding: 10px 12px; border-radius: 6px; margin-bottom: 16px; line-height: 1.6; }
.timeline-title { font-size: 14px; font-weight: 600; color: #303133; margin-bottom: 12px; }
.log-content { font-size: 13px; color: #303133; }
.log-by { font-size: 12px; color: #909399; margin-top: 2px; }
.follow-row { display: flex; gap: 8px; margin-top: 16px; }
</style>
