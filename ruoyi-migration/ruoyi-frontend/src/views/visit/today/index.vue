<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>💬 {{ isCounselor ? '告诉我你今天做了什么' : '今日工作概览' }}</span>
        </div>
      </template>

      <!-- 非辅导员：只读提示 -->
      <el-alert
        v-if="!isCounselor"
        title="仅可查看，无需录入。请前往工作日志或月度统计查看数据。"
        type="info"
        :closable="false"
        class="readonly-tip"
      />

      <!-- 领导/管理员：选取辅导员，查看某人的今日记录 -->
      <div v-if="!isCounselor" class="org-filter">
        <el-select
          v-model="selectedUserId"
          placeholder="选择辅导员（不选则显示全部）"
          clearable
          filterable
          style="width: 100%"
          @change="loadHistory"
        >
          <el-option-group
            v-for="college in orgNodes"
            :key="college.collegeId"
            :label="college.collegeName"
          >
            <el-option
              v-for="c in college.counselors"
              :key="c.id"
              :label="c.name + '（' + c.username + '）'"
              :value="c.id"
            />
          </el-option-group>
        </el-select>
      </div>

      <!-- 聊天区 -->
      <div class="chat-box" ref="chatBox">
        <div v-for="(m, i) in messages" :key="i" class="chat-msg" :class="m.role">
          <div class="bubble">{{ m.text }}</div>
        </div>
      </div>

      <!-- 输入区（仅辅导员） -->
      <div v-if="isCounselor" class="input-row">
        <el-input
          v-model="text"
          placeholder="输入一段话，回车发送"
          :disabled="sending"
          @keyup.enter="send"
        />
        <el-button :disabled="sending || recognizing" title="添加照片" @click="pickImage">📷</el-button>
        <el-button
          :type="recording ? 'danger' : 'default'"
          :disabled="sending || recognizing"
          title="点击开始录音，再点结束"
          @click="toggleRecord"
        >{{ recording ? '⏹' : '🎤' }}</el-button>
        <el-button type="primary" :loading="sending" @click="send">发送</el-button>
      </div>

      <!-- 录音提示 -->
      <div v-if="recording" class="rec-tip">
        <span class="rec-dot"></span>录音中 {{ recSeconds }}s（最长 60 秒，再次点击结束）
      </div>

      <!-- 照片预览 -->
      <input ref="fileInput" type="file" accept="image/*" multiple style="display:none" @change="onFiles" />
      <div v-if="previews.length" class="preview-row">
        <div v-for="(p, i) in previews" :key="p.url" class="preview-item">
          <img :src="p.url" />
          <span class="del" title="移除" @click="removeFile(i)">×</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup name="Today">
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import useUserStore from '@/store/modules/user'
import { chatRecord, voiceRecord, listRecords, orgTree } from '@/api/visit/record'

const userStore = useUserStore()

// 若依角色：roles 是 role_key 数组（如 ['counselor'] / ['admin']）
const roles = computed(() => userStore.roles || [])
const isCounselor = computed(() => roles.value.includes('counselor'))

// ---------- 领导/管理员：选取辅导员 ----------
const orgNodes = ref([])
const selectedUserId = ref(null)

const GREETING =
  '你好，我是你的工作日志助手。直接告诉我，比如「今晚去 3 号楼查寝」或「下午检查了 2 号楼卫生」。'

const text = ref('')
const messages = ref([])
const chatBox = ref(null)
const fileInput = ref(null)
const sending = ref(false)

// ---------- 待发送图片 ----------
const pendingFiles = ref([])
const previews = ref([])

function pickImage() {
  fileInput.value?.click()
}

function onFiles(e) {
  const target = e.target
  for (const f of Array.from(target.files || [])) {
    pendingFiles.value.push(f)
    previews.value.push({ url: URL.createObjectURL(f) })
  }
  target.value = ''
}

function removeFile(i) {
  pendingFiles.value.splice(i, 1)
  const p = previews.value.splice(i, 1)[0]
  if (p) URL.revokeObjectURL(p.url)
}

function clearFiles() {
  previews.value.forEach(p => URL.revokeObjectURL(p.url))
  pendingFiles.value = []
  previews.value = []
}

// 九类工作类型（与后端 EventTypes 一致，用于 AI 回复兜底文案）
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

// 记录成功后的 AI 回复文案（迁移自旧系统 format.recordReplyText）
// 优先用 AI 的回复；没有则拼「已记录：类型 · 学生 · 地点」
function recordReplyText(rec) {
  const ai = String(rec?.aiReply ?? '').trim()
  if (ai) return ai
  const typeCn = EVENT_TYPES[rec?.eventType] || rec?.eventType
  const parts = [typeCn, rec?.studentName, rec?.location].filter(Boolean)
  return parts.length ? '已记录：' + parts.join(' · ') : '已记录。'
}

// ---------- 文字 / 图片录入 ----------
async function send() {
  const t = text.value.trim()
  const files = pendingFiles.value.slice()
  if (!t && !files.length) {
    ElMessage.warning('请说点什么或至少上传一张图片')
    return
  }
  // 只有图片、没有文字：无法归类，不创建记录（否则会污染月度统计）
  if (!t && files.length) {
    ElMessage.warning('请配一句文字说明，图片会跟着文字一起归类')
    return
  }
  messages.value.push({ role: 'user', text: t || `📷 上传 ${files.length} 张照片` })
  text.value = ''
  clearFiles()
  scrollBottom()

  sending.value = true
  try {
    // 第一次 dryRun：只识别（含补录日期），不落库（dryRun 不传 files，避免重复上传）
    const res = await chatRecord(t, { dryRun: true })
    const preview = res.data || {}
    let targetDate = undefined

    // 识别出「非今天」的日期 → 弹确认
    if (preview.recordDate && preview.recordDate !== todayStr()) {
      const confirmed = await confirmBackfill(preview.recordDate)
      if (!confirmed) {
        messages.value.push({ role: 'ai', text: '已取消，这条没有记录。' })
        return
      }
      targetDate = preview.recordDate
    }

    // 第二次真正落库（带确认后的日期 + 图片）
    const res2 = await chatRecord(t, { recordDate: targetDate, files })
    const rec = res2.data || {}
    messages.value.push({ role: 'ai', text: recordReplyText(rec) })
  } catch (e) {
    // 失败必须可见：气泡里明说，不用 toast（会被下一轮操作顶掉）
    messages.value.push({ role: 'ai', text: '记录失败：' + (e?.message ?? '未知错误') })
  } finally {
    sending.value = false
    scrollBottom()
  }
}

function todayStr() {
  const d = new Date()
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

function confirmBackfill(date) {
  return ElMessageBox.confirm(
    `检测到你要补录过去的工作，将记录到 ${fmtCnDate(date)}。确认吗？`,
    '补录日期确认',
    { type: 'warning', confirmButtonText: '确认补录', cancelButtonText: '取消' }
  ).then(() => true).catch(() => false)
}

function fmtCnDate(iso) {
  const [y, m, d] = iso.split('-')
  return `${y}年${Number(m)}月${Number(d)}日`
}

// ---------- 语音录音（PCM 采集 → 16k WAV → 上传 ASR） ----------
const recording = ref(false)
const recognizing = ref(false)
const recSeconds = ref(0)
let recCtx = null
let recStream = null
let recProcessor = null
let recChunks = []
let recSampleRate = 16000
let recTimer = null

async function toggleRecord() {
  if (!recording.value) {
    await startRecording()
  } else {
    stopRecording()
  }
}

async function startRecording() {
  try {
    recStream = await navigator.mediaDevices.getUserMedia({
      audio: { channelCount: 1, echoCancellation: true, noiseSuppression: true }
    })
    let ctx
    try {
      ctx = new AudioContext({ sampleRate: 16000 })
    } catch {
      ctx = new AudioContext()
    }
    await ctx.resume()
    const src = ctx.createMediaStreamSource(recStream)
    const processor = ctx.createScriptProcessor(4096, 1, 1)
    recChunks = []
    processor.onaudioprocess = e => recChunks.push(new Float32Array(e.inputBuffer.getChannelData(0)))
    src.connect(processor)
    processor.connect(ctx.destination)
    recCtx = ctx
    recProcessor = processor
    recSampleRate = ctx.sampleRate
    recording.value = true
    recSeconds.value = 0
    recTimer = setInterval(() => {
      recSeconds.value++
      if (recSeconds.value >= 60) stopRecording() // 一句话识别上限 60 秒
    }, 1000)
  } catch (e) {
    ElMessage.error('无法访问麦克风：' + (e?.message ?? e) + '（请检查浏览器麦克风权限）')
  }
}

function stopRecording() {
  if (!recCtx) return
  clearInterval(recTimer)
  try {
    recProcessor?.disconnect()
    recStream?.getTracks().forEach(t => t.stop())
    recCtx.close()
  } catch { /* 尽力释放 */ }
  const sampleRate = recSampleRate
  const chunks = recChunks
  recCtx = null
  recProcessor = null
  recStream = null
  recChunks = []
  recording.value = false

  const total = chunks.reduce((n, c) => n + c.length, 0)
  const merged = new Float32Array(total)
  let off = 0
  chunks.forEach(c => { merged.set(c, off); off += c.length })
  if (merged.length < sampleRate * 0.4) {
    ElMessage.warning('录音太短了，请说一句话再松开')
    return
  }
  sendVoice(floatToWav(merged, sampleRate))
}

function floatToWav(samples, sampleRate) {
  const buf = new ArrayBuffer(44 + samples.length * 2)
  const v = new DataView(buf)
  const writeStr = (o, s) => {
    for (let i = 0; i < s.length; i++) v.setUint8(o + i, s.charCodeAt(i))
  }
  writeStr(0, 'RIFF'); v.setUint32(4, 36 + samples.length * 2, true); writeStr(8, 'WAVE')
  writeStr(12, 'fmt '); v.setUint32(16, 16, true); v.setUint16(20, 1, true); v.setUint16(22, 1, true)
  v.setUint32(24, sampleRate, true); v.setUint32(28, sampleRate * 2, true)
  v.setUint16(32, 2, true); v.setUint16(34, 16, true)
  writeStr(36, 'data'); v.setUint32(40, samples.length * 2, true)
  let o = 44
  for (let i = 0; i < samples.length; i++, o += 2) {
    const s = Math.max(-1, Math.min(1, samples[i]))
    v.setInt16(o, s < 0 ? s * 0x8000 : s * 0x7fff, true)
  }
  return new Blob([v], { type: 'audio/wav' })
}

async function sendVoice(wav) {
  const idx = messages.value.push({ role: 'user', text: '识别中…' }) - 1
  scrollBottom()
  recognizing.value = true
  try {
    const res = await voiceRecord(wav)
    const data = res.data || {}
    messages.value[idx].text = data.text || '（没听清）'
    messages.value.push({ role: 'ai', text: recordReplyText(data.record || {}) })
  } catch (e) {
    messages.value[idx].text = '（语音发送失败）'
    messages.value.push({ role: 'ai', text: '❌ ' + (e?.message ?? '未知错误') })
  } finally {
    recognizing.value = false
    scrollBottom()
  }
}

// ---------- 当天历史回填（退出重登/换设备不丢，凌晨 12 点自动切新一天） ----------
async function loadHistory() {
  const greet = isCounselor.value
    ? GREETING
    : '今天的工作记录概览（按你的查看权限）。'
  messages.value = [{ role: 'ai', text: greet }]
  try {
    // 用 beginTime/endTime 圈定「今天」：后端按 coalesce(record_date, create_time) 过滤。
    // ⚠️ 必须放 params 嵌套对象（若依标准绑定格式 params[beginTime]），
    //    顶层 beginTime 后端绑不上 → 时间过滤失效 → 会混入历史记录（2026-10-08 实测）。
    const query = {
      pageNum: 1,
      pageSize: 100,
      params: { beginTime: todayStr(), endTime: todayStr() }
    }
    // 领导/管理员选了某个辅导员 → 只看该辅导员（后端 @DataScope 已保证越权安全）
    if (selectedUserId.value) query.userId = selectedUserId.value
    const { rows } = await listRecords(query)
    const recs = [...(rows || [])].sort((a, b) => (a.createTime < b.createTime ? -1 : 1))
    if (!recs.length) return
    messages.value.push({ role: 'ai', text: `—— 今天共 ${recs.length} 条记录 ——` })
    for (const r of recs) {
      const userText = `${r.createBy || ''}：${r.rawText || r.detail || '（无文字）'}`
      messages.value.push({ role: 'user', text: userText })
      messages.value.push({ role: 'ai', text: recordReplyText(r) })
    }
  } catch {
    // 历史拉取失败不影响主流程
  }
}

function scrollBottom() {
  nextTick(() => {
    if (chatBox.value) chatBox.value.scrollTop = chatBox.value.scrollHeight
  })
}

onMounted(async () => {
  loadHistory()
  // 领导/管理员加载三级目录（供选取辅导员）
  if (!isCounselor.value) {
    try {
      orgNodes.value = await orgTree()
    } catch { /* 目录加载失败不影响主流程 */ }
  }
})
</script>

<style scoped>
.readonly-tip { margin-bottom: 12px; }
.org-filter { margin-bottom: 12px; }
.chat-box { height: 400px; overflow-y: auto; margin-bottom: 12px; }
.chat-msg { margin-bottom: 10px; display: flex; }
.chat-msg.user { justify-content: flex-end; }
.chat-msg.ai { justify-content: flex-start; }
.bubble {
  max-width: 75%; padding: 10px 14px; border-radius: 12px;
  font-size: 14px; line-height: 1.5; white-space: pre-wrap;
  word-break: break-word;
}
.user .bubble { background: #409eff; color: #fff; }
.ai .bubble { background: #f0f2f5; color: #303133; }
.input-row { display: flex; gap: 8px; }
.input-row .el-input { flex: 1; }
.rec-tip { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #f56c6c; margin-top: 8px; }
.rec-dot { width: 8px; height: 8px; background: #f56c6c; border-radius: 50%; animation: blink 1s infinite; }
@keyframes blink { 50% { opacity: 0.2; } }
.preview-row { display: flex; gap: 8px; margin-top: 8px; flex-wrap: wrap; }
.preview-item { position: relative; }
.preview-item img { width: 64px; height: 64px; object-fit: cover; border-radius: 6px; display: block; }
.preview-item .del {
  position: absolute; top: -6px; right: -6px;
  width: 18px; height: 18px; line-height: 16px; text-align: center;
  background: #f56c6c; color: #fff; border-radius: 50%;
  font-size: 12px; cursor: pointer;
}
</style>
