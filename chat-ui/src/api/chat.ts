import request from '@/utils/request'
import type { ChatMessage } from '@/types/api'

export interface PageData<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
}

/** 获取当前用户的未读消息数（key=好友userId, value=条数） */
export const getUnreadCounts = () =>
  request.get<Record<string, number>>('/chat/unread-counts')

/** 获取聊天历史（分页，按时间倒序）。单聊传 userId，群聊传 groupId */
export const getHistory = (params: { userId?: number; groupId?: number; page?: number; size?: number }) =>
  request.get<PageData<ChatMessage>>('/chat/history', { params })
