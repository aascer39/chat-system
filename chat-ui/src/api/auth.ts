import request from '@/utils/request'
import type { LoginParams, LoginResult, RegisterParams, UserInfo } from '@/types/api'

/** 登录 */
export const login = (data: LoginParams) =>
  request.post<LoginResult>('/auth/login', data)

/** 注册 */
export const register = (data: RegisterParams) =>
  request.post<UserInfo>('/auth/register', data)
