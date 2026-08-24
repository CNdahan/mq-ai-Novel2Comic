import request from '@/utils/request'

/**
 * 获取角色列表
 * @param {number} novelId - 小说ID
 */
export function getCharacterList(novelId) {
  return request({
    url: `/character/list/${novelId}`,
    method: 'get'
  })
}

/**
 * 更新角色描述
 * @param {number} characterId - 角色ID
 * @param {Object} data - 角色数据
 * @param {string} data.descriptionCn - 中文描述
 * @param {string} data.descriptionEn - 英文描述
 * @param {Object} data.appearanceData - 外貌数据
 */
export function updateCharacter(characterId, data) {
  return request({
    url: `/character/${characterId}`,
    method: 'put',
    data
  })
}

/**
 * 删除角色
 * @param {number} characterId - 角色ID
 */
export function deleteCharacter(characterId) {
  return request({
    url: `/character/${characterId}`,
    method: 'delete'
  })
}

