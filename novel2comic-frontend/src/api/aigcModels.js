import request from '@/utils/request'

export function getAigcModels(data) {
  return request({
    url: '/aigc/config/models',
    method: 'post',
    data
  })
}
