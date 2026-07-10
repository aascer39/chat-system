import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login, register } from '@/api/auth'
import { getCurrentUser } from '@/api/user'
import type { LoginParams, RegisterParams, UserInfo } from '@/types/api'
import router from '@/router'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>('')
  const userInfo = ref<UserInfo | null>(null)

  const isLoggedIn = computed(() => !!token.value)

  /** 从 localStorage 恢复 token */
  function initToken() {
    const saved = localStorage.getItem('token')
    if (saved) {
      token.value = saved
    }
  }

  /** 登录 */
  async function loginAction(params: LoginParams) {
    const res = await login(params)
    token.value = res.token
    userInfo.value = res.user
    localStorage.setItem('token', res.token)
  }

  /** 注册 */
  async function registerAction(params: RegisterParams) {
    const user = await register(params)
    return user
  }

  /** 获取当前用户信息 */
  async function fetchUserInfo() {
    const info = await getCurrentUser()
    userInfo.value = info
  }

  /** 登出 */
  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    router.push('/login')
  }

  /** 重置 token */
  function resetToken() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    initToken,
    loginAction,
    registerAction,
    fetchUserInfo,
    logout,
    resetToken,
  }
})
