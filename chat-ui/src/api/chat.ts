import request from '@/utils/request'
import type { ChatMessage } from '@/types/api'

export interface PageData<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
}

/** 获取与指定用户的聊天历史（分页，按时间倒序） */
export const getHistory = (userId: number, page = 1, size = 20) =>
  request.get<PageData<ChatMessage>>('/chat/history', { params: { userId, page, size } })
