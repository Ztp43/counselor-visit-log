// 走访记录 API（迁移自旧系统 api/index.ts，适配若依 /visit/record/* 路径）
// 若依前端是 JS 项目，无 TS 类型，字段结构见各函数注释。
import request from '@/utils/request'

// ---------- 列表（TableDataInfo：{code,msg,rows,total}） ----------
// 返回 { rows: VisitRecord[], total: number }
// VisitRecord: { recordId, eventType, location, studentName, detail, aiReply,
//                rawText, audioPath, recordDate, createBy, createTime, recorderName,
//                photos: string[] }   // photos 为 /profile/xxx.jpg 数组
export function listRecords(query) {
  return request({
    url: '/visit/record/list',
    method: 'get',
    params: query
  }).then(res => ({ rows: res.rows || [], total: res.total || 0 }))
}

// ---------- AI 文字录入（chat：text + 可选 recordDate + dryRun + files） ----------
// dryRun=true 只识别不落库，data 为 ChatPreview（含补录日期 recordDate）
//   ChatPreview: { eventType, studentName, location, detail, reply, recordDate }
// 正式落库 data 为 VisitRecord（含 photos）
// ⚠️ 文本参数走 URL query（params）而不是 FormData：浏览器端 FormData 文本字段
//    在 axios 全局 JSON Content-Type 下曾出现后端解析不到 text 的问题（2026-09-29）；
//    query 传参是当初浏览器验证过的稳定路径。files 单独走 multipart FormData。
export function chatRecord(text, opts) {
  const params = { text }
  if (opts && opts.dryRun) params.dryRun = true
  if (opts && opts.recordDate) params.recordDate = opts.recordDate
  let data
  if (opts && opts.files && opts.files.length) {
    const fd = new FormData()
    opts.files.forEach(f => fd.append('files', f))
    data = fd
  }
  const headers = { repeatSubmit: false }
  // ⚠️ request.js 全局 Content-Type 是 application/json，axios 1.x 遇到
  // 「JSON 头 + FormData」会把 FormData 序列化成 JSON（formDataToJSON），
  // 导致后端报 "Current request is not a multipart request"。
  // 带 FormData 时必须显式声明 multipart（axios 会自动补 boundary）。
  if (data) headers['Content-Type'] = 'multipart/form-data'
  return request({
    url: '/visit/record/chat',
    method: 'post',
    params,
    data,
    headers
  })
}

// ---------- 语音录入（wav → 腾讯云 ASR → AI 识别 → 落库） ----------
// data: { text, record }
export function voiceRecord(wav) {
  const fd = new FormData()
  fd.append('audio', wav, 'record.wav')
  return request({
    url: '/visit/record/voice',
    method: 'post',
    data: fd,
    // ⚠️ 同 chatRecord：必须显式 multipart，否则 axios 把 FormData 转 JSON（2026-09-29 实测）
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// ---------- 月度统计（12 行，data 为数组） ----------
export function monthlyStats(query) {
  return request({
    url: '/visit/record/stats',
    method: 'get',
    params: query
  }).then(res => res.data)
}

// ---------- 编辑 / 删除 / 恢复 / 彻底删除 ----------
export function updateRecord(recordId, data) {
  return request({
    url: '/visit/record',
    method: 'put',
    data: { ...data, recordId }
  })
}

export function deleteRecord(recordId) {
  return request({
    url: '/visit/record/' + recordId,
    method: 'delete'
  })
}

export function restoreRecord(recordId) {
  return request({
    url: '/visit/record/restore/' + recordId,
    method: 'post'
  })
}

export function purgeRecord(recordId) {
  return request({
    url: '/visit/record/purge/' + recordId,
    method: 'delete'
  })
}

// ---------- 回收站（软删除列表） ----------
export function listTrash(query) {
  return request({
    url: '/visit/record/trash',
    method: 'get',
    params: query
  }).then(res => ({ rows: res.rows || [], total: res.total || 0 }))
}

// ---------- 导出 Excel（blob 下载） ----------
export function exportReport(params) {
  return request({
    url: '/visit/record/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

export function exportWeekly(targetUserId) {
  return request({
    url: '/visit/record/weekly-report',
    method: 'get',
    params: targetUserId ? { targetUserId } : {},
    responseType: 'blob'
  })
}

// ---------- 三级目录（学校 → 学院 → 辅导员，供领导下钻） ----------
// data: [{ collegeId, collegeName, counselors: [{id,name,username,role}] }]
export function orgTree() {
  return request({
    url: '/visit/record/org-tree',
    method: 'get'
  }).then(res => res.data || [])
}
