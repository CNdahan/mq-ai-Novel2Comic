import request from '@/utils/request'

/**
 * 上传小说
 * @param {Object} data - 小说数据
 * @param {string} data.title - 小说标题（可选）
 * @param {string} data.content - 小说内容
 * @param {string} data.sourceType - 来源类型：direct/file
 */
export function uploadNovel(data) {
  return request({
    url: '/novel/upload',
    method: 'post',
    data
  })
}

/**
 * 获取小说详情
 * @param {number} novelId - 小说ID
 */
export function getNovelDetail(novelId) {
  return request({
    url: `/novel/${novelId}`,
    method: 'get'
  })
}

/**
 * 获取小说列表
 * @param {Object} params - 查询参数
 * @param {number} params.page - 页码
 * @param {number} params.pageSize - 每页数量
 * @param {string} params.status - 状态：all/pending/processing/completed/failed
 */
export function getNovelList(params) {
  return request({
    url: '/novel/list',
    method: 'get',
    params
  })
}

/**
 * 重新提取小说角色，无需重新上传原文。
 * @param {number|string} novelId - 小说ID
 */
export function retryCharacterExtraction(novelId) {
  return request({
    url: `/novel/${novelId}/retry-character-extraction`,
    method: 'post'
  })
}

/** 获取包含小说、分镜和图片阶段的创作工作流列表。 */
export function getWorkflowList() {
  return request({
    url: '/novel/workflow-list',
    method: 'get'
  })
}

/**
 * 删除小说
 * @param {number} novelId - 小说ID
 */
export function deleteNovel(novelId) {
  return request({
    url: `/novel/${novelId}`,
    method: 'delete'
  })
}

/**
 * 获取每日推荐小说
 */
export function getDailyRecommendations() {
  return request({
    url: '/novel/recommendations',
    method: 'get'
  })
}

