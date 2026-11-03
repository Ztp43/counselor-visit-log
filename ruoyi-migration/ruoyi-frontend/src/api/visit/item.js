import request from '@/utils/request'

// ---------- 走访事项台账（issue #11） ----------
export function listItems(query) {
  return request({
    url: '/visit/item/list',
    method: 'get',
    params: query
  })
}

export function getItem(itemId) {
  return request({
    url: '/visit/item/' + itemId,
    method: 'get'
  })
}

export function addItem(data) {
  return request({
    url: '/visit/item',
    method: 'post',
    data
  })
}

export function updateItem(data) {
  return request({
    url: '/visit/item',
    method: 'put',
    data
  })
}

export function deleteItem(itemId) {
  return request({
    url: '/visit/item/' + itemId,
    method: 'delete'
  })
}

// 跟进时间线
export function listItemLogs(itemId) {
  return request({
    url: '/visit/item/' + itemId + '/logs',
    method: 'get'
  }).then(res => res.data || [])
}

export function addFollowLog(itemId, content) {
  return request({
    url: '/visit/item/' + itemId + '/follow',
    method: 'post',
    data: { content }
  })
}
