import request from '@/utils/request'

export function getAigcConfig() {
  return request({
    url: '/aigc/config',
    method: 'get'
  })
}

export function saveAigcConfig(data) {
  return request({
    url: '/aigc/config',
    method: 'put',
    data
  })
}

export function resetAigcConfig() {
  return request({
    url: '/aigc/config',
    method: 'delete'
  })
}
