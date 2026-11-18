import request from '@/utils/request'

// ---------- 敏感记录访问审计日志（issue #37，仅管理员） ----------
export function listAccessLogs(query) {
  return request({
    url: '/visit/record/access-log/list',
    method: 'get',
    params: query
  })
}
