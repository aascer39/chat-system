/** 统一后端返回格式 */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
  timestamp: number
}

/** 分页返回格式 */
export interface PageResponse<T> {
  code: number
  message: string
  records: T[]
  total: number
  page: number
  pageSize: number
  timestamp: number
}

/** 用户信息 */
export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar: string
  email: string
  status: number
  createdAt: string
}

/** 登录请求 */
export interface LoginParams {
  username: string
  password: string
}

/** 登录响应 data */
export interface LoginResult {
  token: string
  user: UserInfo
}

/** 注册请求 */
export interface RegisterParams {
  username: string
  password: string
  nickname?: string
}

/** WebSocket 聊天消息 */
export interface ChatMessage {
  id?: number
  senderId?: number
  receiverId: number
  content: string
  messageType?: 'TEXT' | 'IMAGE' | 'FILE' | 'SYSTEM'
  status?: 'SENT' | 'DELIVERED' | 'READ'
  groupId?: number
  createdAt?: string
  senderName?: string
}

/** WebSocket 系统消息 */
export interface WsSystemMessage {
  type: 'ONLINE_COUNT'
  count: number
}

/** 好友请求 */
export interface FriendRequestVO {
  id: number
  fromUserId: number
  fromUsername: string
  fromNickname: string
  fromAvatar: string
  toUserId: number
  toUsername: string
  toNickname: string
  toAvatar: string
  status: number
  createdAt: string
}

/** 群组信息 */
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

/** 群成员（含用户信息） */
export interface GroupMember {
  id: number
  groupId: number
  userId: number
  username: string
  nickname: string
  avatar?: string
  role: string
  joinedAt: string
}

/** 好友信息 */
export interface FriendVO {
  id?: number
  userId: number
  username: string
  nickname: string
  avatar: string
  status: number
  createdAt: string
}
