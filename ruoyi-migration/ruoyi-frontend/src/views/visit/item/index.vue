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
        <el-table-column prop="dueDate" label="截止" width="100">
          <template #default="scope">{{ scope.row.dueDate ? scope.row.dueDate.slice(0, 10) : '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
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
  </div>
</template>

<script setup name="Item">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import useUserStore from '@/store/modules/user'
import { listItems, addItem, updateItem, deleteItem } from '@/api/visit/item'

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

function openAdd() {
  editing.value = false
  editingId = null
  Object.keys(form).forEach(k => delete form[k])
  form.status = 'pending'
  dialogVisible.value = true
}

function openEdit(row) {
  editing.value = true
  editingId = row.itemId
  form.title = row.title
  form.category = row.category
  form.studentName = row.studentName
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
  } catch (e) {
    ElMessage.error(e?.message || '删除失败')
  }
}

onMounted(load)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.head-btns { display: flex; gap: 8px; }
.filters { display: flex; gap: 8px; margin-bottom: 14px; flex-wrap: wrap; }
.empty { text-align: center; color: #909399; padding: 30px 0; font-size: 14px; }
.empty-state { padding: 48px 0; }
.empty-icon { font-size: 44px; margin-bottom: 12px; }
.empty-title { font-size: 15px; color: #606266; margin-bottom: 6px; }
.empty-desc { font-size: 13px; color: #909399; }
</style>
