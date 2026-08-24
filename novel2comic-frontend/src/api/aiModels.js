import request from '@/utils/request'

export function getAiModels(data) {
  return request({
    url: '/ai/config/models',
    method: 'post',
    data
  })
}
