import request from '@/utils/request'

/**
 * 生成分镜脚本（支持多版本）
 * @param {number} novelId - 小说ID
 * @param {Object} params - 请求参数
 * @param {boolean} params.keepOld - 是否保留旧版本，默认true
 * @param {boolean} params.setCurrent - 是否设置为当前版本，默认true
 * @param {string} params.versionNote - 版本说明（可选）
 */
export function generateStoryboard(novelId, params = {}) {
  return request({
    url: `/api/storyboard/generate/${novelId}`,
    method: 'post',
    params
  })
}

/**
 * 获取分镜列表（支持版本选择）
 * @param {number} novelId - 小说ID
 * @param {number} version - 版本号（可选，默认获取当前版本）
 */
export function getStoryboardList(novelId, version) {
  return request({
    url: `/api/storyboard/list/${novelId}`,
    method: 'get',
    params: { version }
  })
}

/**
 * 更新分镜
 * @param {number} panelId - 分镜ID
 * @param {Object} data - 分镜数据
 */
export function updateStoryboardPanel(panelId, data) {
  return request({
    url: `/api/storyboard/${panelId}`,
    method: 'put',
    data
  })
}

/**
 * 删除分镜
 * @param {number} panelId - 分镜ID
 */
export function deleteStoryboardPanel(panelId) {
  return request({
    url: `/api/storyboard/${panelId}`,
    method: 'delete'
  })
}

// ==================== 版本管理相关API ====================

/**
 * 获取小说的所有分镜版本列表
 * @param {number} novelId - 小说ID
 */
export function getAllVersions(novelId) {
  return request({
    url: `/api/storyboard/versions/${novelId}`,
    method: 'get'
  })
}

/**
 * 设置当前使用的版本
 * @param {number} novelId - 小说ID
 * @param {number} version - 版本号
 */
export function setCurrentVersion(novelId, version) {
  return request({
    url: `/api/storyboard/versions/${novelId}/current/${version}`,
    method: 'post'
  })
}

/**
 * 删除指定版本
 * @param {number} novelId - 小说ID
 * @param {number} version - 版本号
 */
export function deleteVersion(novelId, version) {
  return request({
    url: `/api/storyboard/versions/${novelId}/${version}`,
    method: 'delete'
  })
}

/**
 * 复制版本
 * @param {number} novelId - 小说ID
 * @param {Object} data - 复制参数
 * @param {number} data.sourceVersion - 源版本号
 * @param {number} data.targetVersion - 目标版本号
 */
export function copyVersion(novelId, data) {
  return request({
    url: `/api/storyboard/versions/${novelId}/copy`,
    method: 'post',
    data
  })
}

