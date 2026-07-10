import request from '@/utils/request'
import type { UserInfo } from '@/types/api'

/** 获取当前用户信息 */
export const getCurrentUser = () =>
  request.get<UserInfo>('/chat/users')

/** 获取在线用户列表 */
export const getOnlineUsers = () =>
  request.get<UserInfo[]>('/chat/online-users')
