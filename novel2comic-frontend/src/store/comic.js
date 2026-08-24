import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useComicStore = defineStore('comic', () => {
  // 当前小说信息
  const currentNovel = ref(null)
  
  // 当前角色列表
  const currentCharacters = ref([])
  
  // 当前分镜列表
  const currentStoryboard = ref([])
  
  // 当前漫画信息
  const currentComic = ref(null)
  
  // 生成任务信息
  const currentTask = ref(null)
  
  // 生成进度
  const generationProgress = ref({
    percent: 0,
    currentStep: '',
    completedPanels: 0,
    totalPanels: 0
  })
  
  // 设置当前小说
  const setCurrentNovel = (novel) => {
    currentNovel.value = novel
  }
  
  // 设置角色列表
  const setCurrentCharacters = (characters) => {
    currentCharacters.value = characters
  }
  
  // 设置分镜列表
  const setCurrentStoryboard = (storyboard) => {
    currentStoryboard.value = storyboard
  }
  
  // 设置当前漫画
  const setCurrentComic = (comic) => {
    currentComic.value = comic
  }
  
  // 设置任务信息
  const setCurrentTask = (task) => {
    currentTask.value = task
  }
  
  // 更新生成进度
  const updateProgress = (progress) => {
    generationProgress.value = {
      percent: progress.progressPercent || 0,
      currentStep: progress.currentStep || '',
      completedPanels: progress.completedPanels || 0,
      totalPanels: progress.totalPanels || 0
    }
  }
  
  // 重置进度
  const resetProgress = () => {
    generationProgress.value = {
      percent: 0,
      currentStep: '',
      completedPanels: 0,
      totalPanels: 0
    }
  }
  
  // 清除所有数据
  const clearAll = () => {
    currentNovel.value = null
    currentCharacters.value = []
    currentStoryboard.value = []
    currentComic.value = null
    currentTask.value = null
    resetProgress()
  }
  
  return {
    currentNovel,
    currentCharacters,
    currentStoryboard,
    currentComic,
    currentTask,
    generationProgress,
    setCurrentNovel,
    setCurrentCharacters,
    setCurrentStoryboard,
    setCurrentComic,
    setCurrentTask,
    updateProgress,
    resetProgress,
    clearAll
  }
})

