import request from '@/utils/request'
import type { ChatMessage } from '@/types/api'

export interface PageData<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
}

/** 获取聊天历史（分页，按时间倒序）。单聊传 userId，群聊传 groupId */
export const getHistory = (params: { userId?: number; groupId?: number; page?: number; size?: number }) =>
  request.get<PageData<ChatMessage>>('/chat/history', { params })
