<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>📒 工作日志</span>
          <div class="head-btns">
            <el-button size="small" @click="openTrash">🗑️ 回收站</el-button>
            <el-button size="small" @click="openExport">📥 导出 Excel</el-button>
            <el-button size="small" type="primary" @click="downloadWeekly">📊 生成周报</el-button>
          </div>
        </div>
      </template>

      <!-- 搜索 + 日期筛选 -->
      <div class="filters">
        <el-input
          v-model="keyword"
          placeholder="搜索学生 / 地点 / 详情…"
          clearable
          style="flex:1;min-width:160px"
          @input="onKeywordInput"
        />
        <el-date-picker
          v-model="startDate"
          type="date"
          placeholder="开始日期"
          value-format="YYYY-MM-DD"
          style="width:150px"
          @change="onFilterChange"
        />
        <el-date-picker
          v-model="endDate"
          type="date"
          placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width:150px"
          @change="onFilterChange"
        />
        <el-cascader
          v-if="!isCounselor"
          v-model="selectedPath"
          :options="cascaderOptions"
          :props="{ checkStrictly: true, value: 'value', label: 'label', children: 'children', emitPath: true }"
          placeholder="学校→学院→辅导员"
          clearable
          style="width:200px"
          @change="onOrgChange"
        />
      </div>

      <div v-if="loading" class="empty">加载中…</div>
      <div v-else-if="errorMsg" class="empty">
        <div>加载失败：{{ errorMsg }}</div>
        <el-button size="small" style="margin-top:8px" @click="load">重试</el-button>
      </div>
      <div v-else-if="!records.length" class="empty">
        {{ filterDesc ? `没有符合「${filterDesc}」的记录` : '暂无记录，去「今日记录」说一句话吧' }}
      </div>
      <div v-else>
        <div v-for="r in records" :key="r.recordId" class="record-item">
          <div class="top">
            <el-tag size="small" :type="tagType(r.eventType)">{{ eventTypeCn(r.eventType) }}</el-tag>
            <span class="top-right">
              <span class="time">{{ fmtTime(r.createTime || r.recordDate) }}</span>
              <el-button v-if="canEdit(r)" link type="primary" size="small" @click="openEdit(r)">编辑</el-button>
              <el-button v-if="canEdit(r)" link type="danger" size="small" @click="doDelete(r)">删除</el-button>
            </span>
          </div>
          <div v-if="r.recorderName" class="recorder">👤 {{ r.recorderName }}</div>
          <div v-if="metaText(r)" class="loc">{{ metaText(r) }}</div>
          <div class="detail" v-html="highlight(r.detail || '')"></div>
          <div v-if="r.rawText && keyword" class="raw">原文：<span v-html="highlight(r.rawText)"></span></div>
          <div v-if="r.photos && r.photos.length" class="photos">
            <img v-for="(p, i) in r.photos" :key="i" :src="p" @click="openLightbox(p)" />
          </div>
        </div>

        <el-pagination
          v-if="total > pageSize"
          layout="prev, pager, next, total"
          :total="total"
          :page-size="pageSize"
          :current-page="page"
          @current-change="onPageChange"
          style="margin-top:16px;justify-content:center"
        />
      </div>
    </el-card>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editVisible" title="✏️ 编辑记录" width="460px">
      <el-form label-width="90px">
        <el-form-item label="工作类型">
          <el-select v-model="editForm.eventType" style="width:100%">
            <el-option v-for="(cn, k) in EVENT_TYPES" :key="k" :label="cn" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="发生时间">
          <el-date-picker v-model="editForm.recordDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="涉及学生">
          <el-input v-model="editForm.studentName" placeholder="没有具体学生可留空；多人用「、」分隔" />
        </el-form-item>
        <el-form-item label="地点">
          <el-input v-model="editForm.location" />
        </el-form-item>
        <el-form-item label="工作详情">
          <el-input v-model="editForm.detail" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="原文">
          <el-input v-model="editForm.rawText" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 回收站弹窗 -->
    <el-dialog v-model="trashVisible" title="🗑️ 回收站" width="560px">
      <div v-if="trashLoading" class="empty">加载中…</div>
      <div v-else-if="!trashItems.length" class="empty">回收站是空的</div>
      <div v-else>
        <div v-for="r in trashItems" :key="r.recordId" class="trash-item">
          <div class="trash-info">
            <el-tag size="small" :type="tagType(r.eventType)">{{ eventTypeCn(r.eventType) }}</el-tag>
            <span>{{ r.recorderName || r.createBy }} · {{ r.location || '无地点' }} · {{ fmtTime(r.deletedAt || r.recordDate) }}</span>
          </div>
          <div class="trash-actions">
            <el-button size="small" @click="doRestore(r)">恢复</el-button>
            <el-button size="small" type="danger" @click="doPurge(r)">彻底删除</el-button>
          </div>
        </div>
        <el-pagination
          v-if="trashTotal > trashSize"
          layout="prev, pager, next"
          :total="trashTotal"
          :page-size="trashSize"
          :current-page="trashPage"
          @current-change="loadTrash"
          style="margin-top:12px;justify-content:center"
        />
      </div>
    </el-dialog>

    <!-- 导出弹窗 -->
    <el-dialog v-model="exportVisible" title="📥 导出工作报表" width="460px">
      <div v-if="keyword.trim()" class="kw-row">
        <el-checkbox v-model="exportWithKeyword">只导出当前搜索「{{ keyword.trim() }}」命中的记录</el-checkbox>
      </div>
      <div class="quick-row">
        <span class="quick-label">快捷填充：</span>
        <el-button size="small" @click="fillMonth">本月</el-button>
        <el-button size="small" @click="fillLastMonth">上月</el-button>
        <el-button size="small" @click="fillYear">全年</el-button>
      </div>
      <div class="export-dates">
        <el-date-picker v-model="exportStart" type="date" placeholder="开始日期" value-format="YYYY-MM-DD" style="flex:1" :clearable="false" />
        <span class="to">至</span>
        <el-date-picker v-model="exportEnd" type="date" placeholder="结束日期" value-format="YYYY-MM-DD" style="flex:1" :clearable="false" />
      </div>
      <div class="hint">两个日期随意选择，互不影响，可跨任意时间段</div>
      <template #footer>
        <el-button @click="exportVisible = false">取消</el-button>
        <el-button type="primary" :loading="exporting" @click="doExport">导出</el-button>
      </template>
    </el-dialog>

    <!-- 大图 -->
    <el-dialog v-model="lightboxVisible" width="80%" :show-close="true">
      <img :src="lightboxSrc" style="width:100%" />
    </el-dialog>
  </div>
</template>

<script setup name="Record">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import useUserStore from '@/store/modules/user'
import { listRecords, listTrash, updateRecord, deleteRecord, restoreRecord, purgeRecord, exportReport, exportWeekly, orgTree } from '@/api/visit/record'

const userStore = useUserStore()
const roles = computed(() => userStore.roles || [])
const isAdmin = computed(() => roles.value.includes('admin'))
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

function eventTypeCn(et) {
  return EVENT_TYPES[et] || et || '其他'
}

// ---------- 列表 ----------
const records = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const loading = ref(false)
const errorMsg = ref('')
const keyword = ref('')
const startDate = ref('')
const endDate = ref('')
const filterUserId = ref(undefined)

// 三级目录（领导/管理员下钻）
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
  if (path && path.length >= 2) filterUserId.value = path[path.length - 1]
  else filterUserId.value = undefined
  onFilterChange()
}

const filterDesc = computed(() => {
  const parts = []
  if (keyword.value.trim()) parts.push(`搜索「${keyword.value.trim()}」`)
  if (startDate.value && endDate.value) parts.push(`${startDate.value} ~ ${endDate.value}`)
  else if (startDate.value) parts.push(`${startDate.value} 起`)
  else if (endDate.value) parts.push(`截至 ${endDate.value}`)
  return parts.join(' · ')
})

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    const params = {
      pageNum: page.value,
      pageSize: pageSize,
      keyword: keyword.value.trim() || undefined
    }
    if (filterUserId.value) params.userId = filterUserId.value
    // ⚠️ 日期区间必须放 params 嵌套对象（若依绑定格式 params[beginTime]），
    //    顶层 beginTime 后端绑不上 → 筛选失效（2026-10-08 实测发现）。
    if (startDate.value || endDate.value) {
      params.params = {}
      if (startDate.value) params.params.beginTime = startDate.value
      if (endDate.value) params.params.endTime = endDate.value
    }
    const { rows, total: t } = await listRecords(params)
    records.value = rows
    total.value = t
  } catch (e) {
    errorMsg.value = e?.message || '未知错误'
    records.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

let keywordTimer = null
function onKeywordInput() {
  clearTimeout(keywordTimer)
  keywordTimer = setTimeout(() => { page.value = 1; load() }, 300)
}
function onFilterChange() {
  page.value = 1
  load()
}
function onPageChange(p) {
  page.value = p
  load()
}

// ---------- 展示辅助 ----------
function fmtTime(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

function metaText(r) {
  const parts = [r.studentName, r.location].filter(Boolean)
  return parts.length ? parts.join(' · ') : ''
}

function highlight(text) {
  const raw = String(text ?? '')
  const kw = (keyword.value || '').trim()
  if (!kw) return escapeHtml(raw)
  const lower = raw.toLowerCase()
  const lkw = kw.toLowerCase()
  let out = ''
  let i = 0
  for (;;) {
    const at = lower.indexOf(lkw, i)
    if (at < 0) { out += escapeHtml(raw.slice(i)); break }
    out += escapeHtml(raw.slice(i, at)) + '<mark>' + escapeHtml(raw.slice(at, at + kw.length)) + '</mark>'
    i = at + kw.length
  }
  return out
}

function escapeHtml(s) {
  return String(s ?? '')
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;').replace(/'/g, '&#39;')
}

function tagType(et) {
  const map = {
    dorm_check: 'primary', hygiene: 'success', talk: 'warning', class_check: 'primary',
    meeting: 'info', visit_help: 'success', crisis: 'danger', admin: 'info', other: 'info'
  }
  return map[et] || 'info'
}

// 能否编辑/删除：管理员=全部；辅导员=仅自己的；领导=不能改
function canEdit(r) {
  if (isAdmin.value) return true
  if (isCounselor.value) return r.createBy === userStore.name
  return false
}

// ---------- 编辑 ----------
const editVisible = ref(false)
const editForm = reactive({})
let editingId = null

function openEdit(r) {
  editingId = r.recordId
  editForm.eventType = r.eventType
  editForm.recordDate = r.recordDate ? r.recordDate.slice(0, 10) : ''
  editForm.studentName = r.studentName
  editForm.location = r.location
  editForm.detail = r.detail
  editForm.rawText = r.rawText
  editVisible.value = true
}

async function saveEdit() {
  if (editingId == null) return
  try {
    await updateRecord(editingId, { ...editForm })
    editVisible.value = false
    ElMessage.success('已保存')
    load()
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  }
}

// ---------- 删除（软删除，进回收站） ----------
async function doDelete(r) {
  try {
    await ElMessageBox.confirm('确定删除这条记录吗？删除后会移入回收站，可恢复。', '提示')
  } catch { return }
  try {
    await deleteRecord(r.recordId)
    ElMessage.success('已移入回收站')
    load()
  } catch (e) {
    ElMessage.error(e?.message || '删除失败')
  }
}

// ---------- 回收站 ----------
const trashVisible = ref(false)
const trashItems = ref([])
const trashTotal = ref(0)
const trashPage = ref(1)
const trashSize = 10
const trashLoading = ref(false)

async function openTrash() {
  trashVisible.value = true
  trashPage.value = 1
  await loadTrash(1)
}

async function loadTrash(p) {
  trashPage.value = p
  trashLoading.value = true
  try {
    const { rows, total: t } = await listTrash({ pageNum: p, pageSize: trashSize })
    trashItems.value = rows
    trashTotal.value = t
  } catch (e) {
    ElMessage.error(e?.message || '加载回收站失败')
  } finally {
    trashLoading.value = false
  }
}

async function doRestore(r) {
  try {
    await restoreRecord(r.recordId)
    ElMessage.success('已恢复')
    loadTrash(trashPage.value)
    load()
  } catch (e) {
    ElMessage.error(e?.message || '恢复失败')
  }
}

async function doPurge(r) {
  try {
    await ElMessageBox.confirm('彻底删除后无法恢复，确定吗？', '警告', { type: 'warning' })
  } catch { return }
  try {
    await purgeRecord(r.recordId)
    ElMessage.success('已彻底删除')
    loadTrash(trashPage.value)
  } catch (e) {
    ElMessage.error(e?.message || '彻底删除失败')
  }
}

// ---------- 导出 Excel ----------
const exportVisible = ref(false)
const exportStart = ref('')
const exportEnd = ref('')
const exportWithKeyword = ref(true)
const exporting = ref(false)

const iso = (d) =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`

function openExport() {
  exportVisible.value = true
  exportWithKeyword.value = true
  fillMonth()
}

function fillMonth() {
  const now = new Date()
  exportStart.value = iso(new Date(now.getFullYear(), now.getMonth(), 1))
  exportEnd.value = iso(now)
}
function fillLastMonth() {
  const now = new Date()
  exportStart.value = iso(new Date(now.getFullYear(), now.getMonth() - 1, 1))
  exportEnd.value = iso(new Date(now.getFullYear(), now.getMonth(), 0))
}
function fillYear() {
  const y = new Date().getFullYear()
  exportStart.value = `${y}-01-01`
  exportEnd.value = `${y}-12-31`
}

function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}

async function doExport() {
  const s = exportStart.value
  const e = exportEnd.value
  if (!s || !e) { ElMessage.warning('请选择开始和结束日期'); return }
  if (s > e) { ElMessage.warning('开始日期不能晚于结束日期'); return }

  exporting.value = true
  try {
    const blob = await exportReport({
      start: s,
      end: e,
      keyword: exportWithKeyword.value ? keyword.value.trim() : ''
    })
    downloadBlob(blob, `辅导员工作日志_${s}_至_${e}.xlsx`)
    exportVisible.value = false
  } catch (e) {
    ElMessage.error('导出失败：' + (e?.message ?? '未知错误'))
  } finally {
    exporting.value = false
  }
}

async function downloadWeekly() {
  try {
    const blob = await exportWeekly()
    downloadBlob(blob, `周报_${iso(new Date())}.xlsx`)
  } catch (e) {
    ElMessage.error('生成周报失败：' + (e?.message ?? '未知错误'))
  }
}

// ---------- 大图 ----------
const lightboxVisible = ref(false)
const lightboxSrc = ref('')
function openLightbox(src) {
  lightboxSrc.value = src
  lightboxVisible.value = true
}

onMounted(async () => {
  load()
  if (!isCounselor.value) {
    try { orgNodes.value = await orgTree() } catch { /* 目录失败不影响列表 */ }
  }
})
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.head-btns { display: flex; gap: 8px; flex-wrap: wrap; }
.filters { display: flex; gap: 8px; margin-bottom: 14px; flex-wrap: wrap; }
.record-item { border: 1px solid #ebeef5; border-radius: 10px; padding: 12px; margin-bottom: 10px; }
.record-item .top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.top-right { display: flex; align-items: center; gap: 8px; }
.time { font-size: 12px; color: #909399; }
.recorder { font-size: 13px; color: #606266; margin-bottom: 4px; }
.loc { font-size: 13px; color: #909399; margin-bottom: 4px; }
.detail { font-size: 14px; color: #303133; white-space: pre-wrap; }
.raw { font-size: 12px; color: #909399; margin-top: 6px; }
.photos { display: flex; gap: 8px; margin-top: 8px; flex-wrap: wrap; }
.photos img { width: 80px; height: 80px; object-fit: cover; border-radius: 6px; cursor: pointer; }
.empty { text-align: center; color: #909399; padding: 30px 0; font-size: 14px; }
.trash-item { border-bottom: 1px solid #f0f2f5; padding: 10px 0; }
.trash-info { display: flex; align-items: center; gap: 8px; font-size: 13px; color: #606266; margin-bottom: 6px; }
.trash-actions { display: flex; gap: 8px; }
.kw-row { background: #eef4ff; border: 1px solid #c7ddff; border-radius: 8px; padding: 8px 12px; margin-bottom: 12px; }
.quick-row { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.quick-label { font-size: 13px; color: #606266; }
.export-dates { display: flex; gap: 8px; align-items: center; }
.export-dates .to { color: #909399; }
.hint { font-size: 12px; color: #909399; margin-top: 8px; }
</style>
