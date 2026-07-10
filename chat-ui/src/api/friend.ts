import request from '@/utils/request'
import type { FriendRequestVO, FriendVO, UserInfo } from '@/types/api'

/** 发送好友请求 */
export const sendFriendRequest = (toUserId: number) =>
  request.post<FriendRequestVO>('/friend/request', { toUserId })

/** 接受好友请求 */
export const acceptFriendRequest = (requestId: number) =>
  request.post<FriendVO>(`/friend/accept/${requestId}`)

/** 拒绝好友请求 */
export const rejectFriendRequest = (requestId: number) =>
  request.post(`/friend/reject/${requestId}`)

/** 获取收到的待处理好友请求 */
export const getPendingRequests = () =>
  request.get<FriendRequestVO[]>('/friend/requests')

/** 获取已发送的好友请求 */
export const getSentRequests = () =>
  request.get<FriendRequestVO[]>('/friend/requests/sent')

/** 获取好友列表 */
export const getFriendList = () =>
  request.get<FriendVO[]>('/friend/list')

/** 搜索用户 */
export const searchUsers = (keyword: string) =>
  request.post<UserInfo[]>('/friend/search', { keyword })

/** 删除好友 */
export const removeFriend = (friendUserId: number) =>
  request.delete(`/friend/remove/${friendUserId}`)
