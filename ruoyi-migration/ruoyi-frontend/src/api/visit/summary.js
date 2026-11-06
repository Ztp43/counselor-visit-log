import request from '@/utils/request'

// ---------- 领导总结报告（周/月/学期/年） ----------
export function getSummary(params) {
  return request({
    url: '/visit/report/summary',
    method: 'get',
    params
  }).then(res => res.data || {})
}
