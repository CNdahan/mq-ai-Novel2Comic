import request from '@/utils/request'

export function getAiConfig() {
  return request({
    url: '/ai/config',
    method: 'get'
  })
}

export function saveAiConfig(data) {
  return request({
    url: '/ai/config',
    method: 'put',
    data
  })
}

export function resetAiConfig() {
  return request({
    url: '/ai/config',
    method: 'delete'
  })
}
