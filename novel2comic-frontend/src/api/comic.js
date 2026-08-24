import request from '@/utils/request'

/**
 * 生成漫画
 * @param {Object} data - 生成参数
 * @param {number} data.novelId - 小说ID
 * @param {string} data.style - 风格：japanese/chinese/realistic
 * @param {number} data.panelCount - 分镜数量（可选）
 */
export function generateComic(data) {
  return request({
    url: '/comic/generate',
    method: 'post',
    data
  })
}

/**
 * 获取任务进度
 * @param {string} taskUuid - 任务UUID
 */
export function getTaskProgress(taskUuid) {
  return request({
    url: `/task/progress/${taskUuid}`,
    method: 'get'
  })
}

/**
 * 取消任务
 * @param {string} taskUuid - 任务UUID
 */
export function cancelTask(taskUuid) {
  return request({
    url: `/task/cancel/${taskUuid}`,
    method: 'post'
  })
}

/**
 * 获取漫画结果
 * @param {number} comicId - 漫画ID
 */
export function getComicResult(comicId) {
  return request({
    url: `/comic/result/${comicId}`,
    method: 'get'
  })
}

/**
 * 通过novelId获取最新的漫画
 * @param {number} novelId - 小说ID
 */
export function getLatestComicByNovelId(novelId) {
  return request({
    url: `/comic/latest/${novelId}`,
    method: 'get'
  })
}

/**
 * 获取漫画列表
 * @param {Object} params - 查询参数
 * @param {number} params.page - 页码
 * @param {number} params.pageSize - 每页数量
 * @param {string} params.status - 状态：all/generating/completed/failed
 */
export function getComicList(params) {
  return request({
    url: '/comic/list',
    method: 'get',
    params
  })
}

/**
 * 删除漫画
 * @param {number} comicId - 漫画ID
 */
export function deleteComic(comicId) {
  return request({
    url: `/comic/${comicId}`,
    method: 'delete'
  })
}

/**
 * 代理获取图片（解决OSS 403问题）
 * @param {string} imageUrl - 图片URL
 */
export function getProxyImageUrl(imageUrl) {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api'
  return `${baseUrl}/image/proxy?url=${encodeURIComponent(imageUrl)}`
}

/**
 * 重新生成单个分镜
 * @param {number} comicId - 漫画ID
 * @param {number} panelIndex - 分镜序号
 */
export function regeneratePanel(comicId, panelIndex) {
  return request({
    url: `/comic/${comicId}/panel/${panelIndex}/regenerate`,
    method: 'post'
  })
}

