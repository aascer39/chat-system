import request from '@/utils/request'
import type { GroupMember } from '@/types/api'

export interface GroupVO {
  id: number
  name: string
  avatar?: string
  description?: string
  ownerId: number
  memberCount: number
  role: string
  createdAt: string
}

/** 创建群组 */
export const createGroup = (name: string, description?: string) =>
  request.post<GroupVO>('/chat/groups', null, { params: { name, description } })

/** 我的群组列表 */
export const getMyGroups = () =>
  request.get<GroupVO[]>('/chat/groups')

/** 获取群详情 */
export const getGroupDetail = (groupId: number | string) =>
  request.get<GroupVO>(`/chat/groups/${groupId}`)

/** 添加成员 */
export const addGroupMember = (groupId: number | string, userId: number | string) =>
  request.post(`/chat/groups/${groupId}/members`, null, { params: { userId } })

/** 获取群成员列表 */
export const getGroupMembers = (groupId: number | string) =>
  request.get<GroupMember[]>(`/chat/groups/${groupId}/members`)
