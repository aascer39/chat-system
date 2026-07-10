<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElInput } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getCurrentUser } from '@/api/user'
import { getHistory, getUnreadCounts } from '@/api/chat'
import {
  getFriendList,
  getPendingRequests,
  getSentRequests,
  sendFriendRequest,
  acceptFriendRequest,
  rejectFriendRequest,
  searchUsers,
  removeFriend,
} from '@/api/friend'
import { getMyGroups, createGroup, addGroupMember, getGroupMembers, type GroupVO } from '@/api/group'
import { safeParse, safeStringify } from '@/utils/json'
import type { UserInfo, ChatMessage, FriendRequestVO, FriendVO, GroupMember } from '@/types/api'

const router = useRouter()
const userStore = useUserStore()

// ── State ──
const connected = ref(false)
const onlineCount = ref(0)
const onlineUserIds = ref<string[]>([])
const currentUser = ref<UserInfo | null>(null)
const messages = ref<ChatMessage[]>([])
const selectedUser = ref<UserInfo | null>(null)
const selectedGroup = ref<GroupVO | null>(null)
const conversationType = ref<'user' | 'group' | null>(null)
const inputContent = ref('')
const loading = ref(true)

// History pagination
const historyPage = ref(1)
const historyTotal = ref(0)
const loadingHistory = ref(false)
const hasMoreHistory = computed(() => historyTotal.value > messages.value.length)

// Navigation tabs (QQ layout: left icon bar + content panel)
const activeNav = ref<'friends' | 'groups'>('friends')
const activeTab = ref<'friends' | 'groups' | 'requests' | 'add'>('friends')

// Friends
const friends = ref<FriendVO[]>([])
const pendingRequests = ref<FriendRequestVO[]>([])
const groupList = ref<GroupVO[]>([])
const createGroupDialogVisible = ref(false)
const newGroupName = ref('')
const groupInfoDialogVisible = ref(false)
const groupMembers = ref<GroupMember[]>([])
const loadingGroupMembers = ref(false)
const addMemberDialogVisible = ref(false)
const addMemberSearchKeyword = ref('')
const addMemberSearchResults = ref<UserInfo[]>([])
const searchingAddMember = ref(false)

// Unread counts
const unreadCounts = ref<Record<string, number>>({})

// Search
const searchKeyword = ref('')
const searchResults = ref<UserInfo[]>([])
const searching = ref(false)

let ws: WebSocket | null = null
let heartbeatTimer: ReturnType<typeof setInterval> | null = null
const messagesContainer = ref<HTMLElement | null>(null)

const pendingCount = computed(() => pendingRequests.value.length)

const friendOnlineCount = computed(() => {
  const currentId = String(currentUser.value?.id ?? '')
  return onlineUserIds.value.filter(id => id !== currentId).length
})

// ── Init ──
onMounted(async () => {
  await loadCurrentUser()
  await loadFriends()
  await loadGroups()
  await loadPendingRequests()
  await loadUnreadCounts()
  connectWebSocket()
})

onUnmounted(() => {
  disconnectWebSocket()
})

// ── Data Loading ──
async function loadCurrentUser() {
  try {
    currentUser.value = await getCurrentUser()
  } catch {
    userStore.logout()
  } finally {
    loading.value = false
  }
}

async function loadFriends() {
  try {
    friends.value = await getFriendList()
  } catch {
    friends.value = []
  }
}

async function loadGroups() {
  try {
    groupList.value = await getMyGroups()
  } catch {
    groupList.value = []
  }
}

async function loadUnreadCounts() {
  try {
    unreadCounts.value = await getUnreadCounts()
  } catch {
    unreadCounts.value = {}
  }
}

async function loadPendingRequests() {
  try {
    pendingRequests.value = await getPendingRequests()
  } catch {
    pendingRequests.value = []
  }
}

// ── History ──
async function loadHistory() {
  if (!selectedUser.value && !selectedGroup.value) return
  loadingHistory.value = true
  try {
    const params: { userId?: number; groupId?: number | string; page: number; size: number } = {
      page: historyPage.value,
      size: 20,
    }
    if (conversationType.value === 'group' && selectedGroup.value) {
      params.groupId = selectedGroup.value.id
    } else if (selectedUser.value) {
      params.userId = selectedUser.value.userId
    }
    const res = await getHistory(params)
    const list = (res.records || []).reverse() // API 返回倒序（最新在前），反转后最早在前
    if (historyPage.value === 1) {
      messages.value = list
    } else {
      messages.value = [...list, ...messages.value]
    }
    historyTotal.value = Number(res.total || 0)
    await nextTick()
    scrollToBottom()

    // 标记已读：单聊中将对方发送的未读消息标记为已读
    if (conversationType.value === 'user' && selectedUser.value) {
      messages.value = messages.value.map(msg => {
        if (String(msg.senderId) === String(selectedUser.value.userId) && msg.id) {
          if (msg.status === 'SENT' || msg.status === 'DELIVERED') {
            sendReadReceipt(msg.id)
            return { ...msg, status: 'READ' as const }
          }
        }
        return msg
      })
    }
  } catch {
    // ignore
  } finally {
    loadingHistory.value = false
  }
}

async function loadMoreHistory() {
  if (loadingHistory.value || !hasMoreHistory.value) return
  historyPage.value++
  await loadHistory()
}

// ── WebSocket ──
function connectWebSocket() {
  const token = userStore.token
  if (!token) return

  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const host = window.location.host
  const wsUrl = `${protocol}//${host}/ws/chat?token=${token}`

  ws = new WebSocket(wsUrl)

  ws.onopen = () => {
    connected.value = true
    heartbeatTimer = setInterval(() => {
      ws?.send(safeStringify({ type: 'PING' }))
    }, 30000)
  }

  ws.onmessage = (event: MessageEvent) => {
    try {
      const data = safeParse(event.data) as Record<string, unknown>

      // ── 系统消息：在线人数变化 ──
      if (data.type === 'ONLINE_COUNT') {
        onlineCount.value = Number(data.count)
        onlineUserIds.value = (data.onlineUserIds as string[]) || []
        return
      }

      // ── 系统消息：心跳响应 ──
      if (data.type === 'PONG') {
        return
      }

      // ── 已读回执 ──
      if (data.type === 'READ_RECEIPT') {
        const msgId = String(data.messageId ?? '')
        const idx = messages.value.findIndex(m => String(m.id) === msgId)
        if (idx >= 0) {
          messages.value[idx] = { ...messages.value[idx], status: 'READ' as const }
        }
        return
      }

      // ── 聊天消息 ──
      const msg = data as ChatMessage
      if (!msg.senderId && !msg.receiverId && !msg.groupId) {
        return
      }

      // 判断是否属于当前会话
      const currentUserId = String(currentUser.value?.id ?? '')
      const msgSenderId = String(msg.senderId ?? '')
      const msgGroupId = String(msg.groupId ?? '')
      let isCurrentConversation = false

      if (msgGroupId && msgGroupId !== 'null') {
        // 群消息
        const currentGroupId = String(selectedGroup.value?.id ?? '')
        isCurrentConversation = msgGroupId === currentGroupId
      } else {
        // 单聊消息
        const otherUserId = String(selectedUser.value?.userId ?? '')
        const msgReceiverId = String(msg.receiverId ?? '')
        isCurrentConversation =
          (msgSenderId === currentUserId && msgReceiverId === otherUserId) ||
          (msgSenderId === otherUserId && msgReceiverId === currentUserId)
      }
      if (!isCurrentConversation) {
        // 不在当前会话 → 计入未读
        if (msgGroupId && msgGroupId !== 'null') {
          const key = 'g_' + msgGroupId
          unreadCounts.value = { ...unreadCounts.value, [key]: (unreadCounts.value[key] || 0) + 1 }
        } else if (msgSenderId !== currentUserId) {
          unreadCounts.value = { ...unreadCounts.value, [msgSenderId]: (unreadCounts.value[msgSenderId] || 0) + 1 }
        }
        return
      }

      // 去重（按 ID 去重）
      const isDuplicate = msg.id ? messages.value.some(m => String(m.id) === String(msg.id)) : false
      if (!isDuplicate) {
        messages.value.push(msg)
        scrollToBottom()

        // 单聊：对方发来的消息自动发送已读回执
        if (!msgGroupId && msg.id && msgSenderId !== currentUserId) {
          sendReadReceipt(msg.id)
          const lastIdx = messages.value.length - 1
          messages.value[lastIdx] = { ...messages.value[lastIdx], status: 'READ' as const }
        }
      }
    } catch {
      // ignore
    }
  }

  ws.onclose = () => {
    connected.value = false
    stopHeartbeat()
  }

  ws.onerror = () => {
    connected.value = false
  }
}

function disconnectWebSocket() {
  stopHeartbeat()
  ws?.close()
  ws = null
  connected.value = false
}

function stopHeartbeat() {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
}

// ── Send Message ──
function sendMessage() {
  const content = inputContent.value.trim()
  if (!content) return

  if (!ws || ws.readyState !== WebSocket.OPEN) {
    ElMessage.warning('连接未建立，请稍后重试')
    return
  }

  if (!selectedUser.value && !selectedGroup.value) {
    ElMessage.warning('请选择一个好友或群组开始聊天')
    return
  }

  let msg: ChatMessage
  if (conversationType.value === 'group' && selectedGroup.value) {
    msg = {
      groupId: selectedGroup.value.id,
      content,
      messageType: 'TEXT',
    }
  } else if (selectedUser.value) {
    msg = {
      receiverId: selectedUser.value.userId,
      content,
      messageType: 'TEXT',
    }
  } else {
    return
  }

  ws.send(safeStringify(msg))
  inputContent.value = ''
}

function handleKeyDown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

// ── Friend Operations ──
async function handleAddFriend(toUserId: number) {
  try {
    await sendFriendRequest(toUserId)
    ElMessage.success('好友请求已发送')
    searchKeyword.value = ''
    searchResults.value = []
    activeTab.value = 'requests'
  } catch {
    // error handled by interceptor
  }
}

async function handleAcceptRequest(requestId: number) {
  try {
    const friend = await acceptFriendRequest(requestId)
    ElMessage.success('已添加好友')
    await loadPendingRequests()
    await loadFriends()
    // If this friend is the currently selected user, update
    if (selectedUser.value && friend.userId === selectedUser.value.userId) {
      selectedUser.value = friend as unknown as UserInfo
    }
  } catch {
    // error handled by interceptor
  }
}

async function handleRejectRequest(requestId: number) {
  try {
    await rejectFriendRequest(requestId)
    ElMessage.info('已拒绝')
    await loadPendingRequests()
  } catch {
    // error handled by interceptor
  }
}

async function handleRemoveFriend(friendUserId: number) {
  try {
    await ElMessageBox.confirm('确定要删除该好友吗？', '提示', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await removeFriend(friendUserId)
    ElMessage.success('已删除好友')
    await loadFriends()
    if (selectedUser.value?.userId === friendUserId) {
      selectedUser.value = null
    }
  } catch {
    // cancelled or error
  }
}

async function handleSearch() {
  const keyword = searchKeyword.value.trim()
  if (!keyword) {
    searchResults.value = []
    return
  }
  searching.value = true
  try {
    searchResults.value = await searchUsers(keyword)
  } catch {
    searchResults.value = []
  } finally {
    searching.value = false
  }
}

function selectFriend(friend: FriendVO) {
  selectedUser.value = {
    id: friend.userId,
    userId: friend.userId,
    username: friend.username,
    nickname: friend.nickname,
    avatar: friend.avatar,
    status: friend.status,
    email: '',
    createdAt: friend.createdAt,
  }
  selectedGroup.value = null
  conversationType.value = 'user'
  // 清除该好友的未读计数
  unreadCounts.value = { ...unreadCounts.value, [String(friend.userId)]: 0 }
  // 切换好友时重置消息并加载历史记录
  messages.value = []
  historyPage.value = 1
  historyTotal.value = 0
  loadHistory()
}

async function openGroupInfo() {
  if (!selectedGroup.value) return
  loadingGroupMembers.value = true
  groupInfoDialogVisible.value = true
  try {
    groupMembers.value = await getGroupMembers(selectedGroup.value.id)
  } catch {
    groupMembers.value = []
  } finally {
    loadingGroupMembers.value = false
  }
}

async function handleAddMemberToGroup(userId: number) {
  if (!selectedGroup.value) return
  try {
    await addGroupMember(selectedGroup.value.id, userId)
    ElMessage.success('已添加')
    addMemberDialogVisible.value = false
    addMemberSearchResults.value = []
    addMemberSearchKeyword.value = ''
    // 刷新成员列表
    groupMembers.value = await getGroupMembers(selectedGroup.value.id)
    // 更新群成员数
    selectedGroup.value = { ...selectedGroup.value, memberCount: groupMembers.value.length }
    const idx = groupList.value.findIndex(g => String(g.id) === String(selectedGroup.value!.id))
    if (idx >= 0) {
      groupList.value[idx] = { ...groupList.value[idx], memberCount: groupMembers.value.length }
    }
  } catch {
    // error handled by interceptor
  }
}

async function searchAddMember() {
  const keyword = addMemberSearchKeyword.value.trim()
  if (!keyword) { addMemberSearchResults.value = []; return }
  searchingAddMember.value = true
  try {
    const results = await searchUsers(keyword)
    // 过滤掉已经是群成员的用户
    const memberIds = new Set(groupMembers.value.map(m => String(m.userId)))
    addMemberSearchResults.value = results.filter(u => !memberIds.has(String(u.id)))
  } catch {
    addMemberSearchResults.value = []
  } finally {
    searchingAddMember.value = false
  }
}

function selectGroup(group: GroupVO) {
  selectedGroup.value = group
  selectedUser.value = null
  conversationType.value = 'group'
  // 清除该群的未读计数（预留）
  unreadCounts.value = { ...unreadCounts.value, ['g_' + String(group.id)]: 0 }
  messages.value = []
  historyPage.value = 1
  historyTotal.value = 0
  loadHistory()
}

function openCreateGroupDialog() {
  newGroupName.value = ''
  createGroupDialogVisible.value = true
}

async function handleCreateGroup() {
  const name = newGroupName.value.trim()
  if (!name) return
  try {
    await createGroup(name)
    ElMessage.success('群组创建成功')
    createGroupDialogVisible.value = false
    await loadGroups()
    activeTab.value = 'groups'
  } catch {
    // error handled by interceptor
  }
}

function isFriendOnline(userId: number | string): boolean {
  return onlineUserIds.value.includes(String(userId))
}

async function goToRequests() {
  activeNav.value = 'friends'
  activeTab.value = 'requests'
  await loadPendingRequests()
  await nextTick()
  // 滚动到面板顶部，确保好友请求区域可见
  const el = document.querySelector('.panel-list')
  if (el) el.scrollTop = 0
}

function switchTab(tab: 'friends' | 'groups' | 'requests' | 'add') {
  activeTab.value = tab
  if (tab === 'requests') {
    loadPendingRequests()
  }
}

// ── Chat Utils ──
function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

async function onMessagesScroll() {
  const el = messagesContainer.value
  if (!el || loadingHistory.value || !hasMoreHistory.value) return
  // 滚动到顶部附近时加载更早的消息
  if (el.scrollTop < 80) {
    const prevHeight = el.scrollHeight
    await loadMoreHistory()
    await nextTick()
    // 保持滚动位置（新内容插入到顶部后，恢复之前的可视位置）
    el.scrollTop = el.scrollHeight - prevHeight
  }
}

function sendReadReceipt(messageId: number | string) {
  if (!ws || ws.readyState !== WebSocket.OPEN) return
  // 以字符串形式发送，避免雪花 ID 精度丢失
  ws.send(safeStringify({ type: 'READ', messageId: messageId }))
}

function handleLogout() {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    type: 'warning',
    confirmButtonText: '退出',
    cancelButtonText: '取消',
  }).then(() => {
    userStore.logout()
  }).catch(() => {
    // cancelled
  })
}

function getMessageSide(msg: ChatMessage): 'left' | 'right' {
  return msg.senderId === currentUser.value?.id ? 'right' : 'left'
}

function formatTime(dateStr?: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function getChatPartnerName(): string {
  return selectedUser.value?.nickname || selectedUser.value?.username || ''
}
</script>

<template>
  <div class="chat-page">
    <!-- Loading -->
    <div v-if="loading" class="chat-loading">
      <el-skeleton :rows="3" animated />
    </div>

    <template v-else>
      <!-- ===== Left Nav Bar (QQ style) ===== -->
      <nav class="chat-nav">
        <div class="nav-section nav-top">
          <el-avatar :size="36" :src="currentUser?.avatar" class="nav-avatar"
            :class="{ online: connected }">
            {{ currentUser?.nickname?.charAt(0) || 'U' }}
          </el-avatar>
        </div>
        <div class="nav-section nav-middle">
          <button class="nav-btn" :class="{ active: activeNav === 'friends' }"
            @click="activeNav = 'friends'" title="好友">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
          </button>
          <button class="nav-btn" :class="{ active: activeNav === 'groups' }"
            @click="activeNav = 'groups'" title="群组">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
          </button>
          <button class="nav-btn" @click="goToRequests()" title="请求">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
            <span v-if="pendingCount > 0" class="nav-badge">{{ pendingCount }}</span>
          </button>
        </div>
        <div class="nav-section nav-bottom">
          <button class="nav-btn" @click="handleLogout" title="退出">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
          </button>
        </div>
      </nav>

      <!-- ===== Content Panel (QQ style) ===== -->
      <aside class="chat-panel">
        <!-- Panel Header -->
        <div class="panel-header">
          <template v-if="activeNav === 'friends'">
            <h2 class="panel-title">好友列表</h2>
            <el-button text circle size="small" @click="activeTab = 'add'" title="添加好友">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            </el-button>
          </template>
          <template v-else>
            <h2 class="panel-title">群组</h2>
            <el-button text circle size="small" @click="openCreateGroupDialog" title="创建群组">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            </el-button>
          </template>
        </div>

        <!-- Search -->
        <div class="panel-search">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索..."
            size="small"
            clearable
            :prefix-icon="null"
            @input="activeTab = 'add'; handleSearch()"
            @clear="searchResults = []"
          >
            <template #prefix>
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
            </template>
          </el-input>
        </div>

        <!-- Online indicator -->
        <div class="panel-online">
          <span class="online-dot" :class="{ active: connected }"></span>
          <span>{{ onlineCount }} 人在线 · 好友 {{ friendOnlineCount }} 人</span>
        </div>

        <!-- Friends Content -->
        <template v-if="activeNav === 'friends'">
          <div class="panel-list">
            <div v-if="activeTab === 'add' && searchKeyword" class="list-section">
              <div v-if="searching" class="panel-empty">搜索中...</div>
              <div v-else-if="searchResults.length === 0" class="panel-empty">未找到匹配的用户</div>
              <div v-for="user in searchResults" :key="user.id" class="list-item">
                <el-avatar :size="36" :src="user.avatar">{{ user.nickname?.charAt(0) || 'U' }}</el-avatar>
                <div class="list-item-info">
                  <span class="list-item-name">{{ user.nickname || user.username }}</span>
                </div>
                <el-button type="primary" text size="small" @click.stop="handleAddFriend(user.id)">+ 添加</el-button>
              </div>
            </div>

            <div v-if="activeTab === 'requests' || pendingRequests.length > 0" class="list-section">
              <div class="list-section-title">好友请求 ({{ pendingRequests.length }})</div>
              <div v-if="pendingRequests.length === 0" class="panel-empty">
                <p>暂无待处理的好友请求</p>
              </div>
              <div v-for="req in pendingRequests" :key="req.id" class="list-item">
                <el-avatar :size="36" :src="req.fromAvatar">{{ req.fromNickname?.charAt(0) || 'U' }}</el-avatar>
                <div class="list-item-info">
                  <span class="list-item-name">{{ req.fromNickname || req.fromUsername }}</span>
                  <span class="list-item-sub">请求添加好友</span>
                </div>
                <div class="list-item-actions">
                  <el-button type="primary" size="small" @click.stop="handleAcceptRequest(req.id)">接受</el-button>
                  <el-button size="small" @click.stop="handleRejectRequest(req.id)">拒绝</el-button>
                </div>
              </div>
            </div>

            <div class="list-section">
              <div class="list-section-title">我的好友 ({{ friends.length }})</div>
              <div v-if="friends.length === 0" class="panel-empty">
                <p>暂无好友</p>
                <el-button type="text" size="small" @click="activeTab = 'add'">添加好友</el-button>
              </div>
              <div v-for="friend in friends" :key="friend.userId" class="list-item"
                :class="{ active: selectedUser?.userId === friend.userId }"
                @click="selectFriend(friend)">
                <div class="list-avatar-wrap">
                  <el-avatar :size="36" :src="friend.avatar">{{ friend.nickname?.charAt(0) || 'U' }}</el-avatar>
                  <span class="list-online-dot" :class="{ online: isFriendOnline(friend.userId) }"></span>
                </div>
                <div class="list-item-info">
                  <span class="list-item-name">{{ friend.nickname || friend.username }}</span>
                </div>
                <span v-if="unreadCounts[String(friend.userId)]" class="list-unread">{{ unreadCounts[String(friend.userId)] }}</span>
                <el-button type="danger" text size="small" title="删除好友"
                  @click.stop="handleRemoveFriend(friend.userId)">
                  <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                </el-button>
              </div>
            </div>
          </div>
        </template>

        <!-- Groups Content -->
        <template v-if="activeNav === 'groups'">
          <div class="panel-list">
            <div class="list-section">
              <div class="list-section-title">我的群组 ({{ groupList.length }})</div>
              <div v-if="groupList.length === 0" class="panel-empty">
                <p>暂无群组</p>
              </div>
              <div v-for="group in groupList" :key="group.id" class="list-item"
                :class="{ active: selectedGroup?.id === group.id }"
                @click="selectGroup(group)">
                <el-avatar :size="36" shape="square">{{ group.name.charAt(0) || 'G' }}</el-avatar>
                <div class="list-item-info">
                  <span class="list-item-name">{{ group.name }}</span>
                  <span class="list-item-sub">{{ group.memberCount }} 人</span>
                </div>
              </div>
            </div>
          </div>
        </template>
      </aside>

      <!-- ===== Main Chat Area ===== -->
      <main class="chat-main">
        <!-- Chat Header -->
        <div class="chat-header">
          <template v-if="conversationType === 'group' && selectedGroup">
            <el-avatar :size="28" shape="square" class="user-avatar">
              {{ selectedGroup.name.charAt(0) || 'G' }}
            </el-avatar>
            <div class="chat-header-info" style="cursor:pointer" @click="openGroupInfo">
              <span class="chat-header-name">{{ selectedGroup.name }}</span>
              <span class="chat-header-meta">群组 · {{ selectedGroup.memberCount }} 人 ›</span>
            </div>
          </template>
          <template v-else-if="selectedUser">
            <el-avatar :size="28" :src="selectedUser.avatar">
              {{ selectedUser.nickname?.charAt(0) || 'U' }}
            </el-avatar>
            <span class="chat-header-name">{{ selectedUser.nickname || selectedUser.username }}</span>
          </template>
          <template v-else>
            <span class="chat-header-name">选择一个好友或群组开始聊天</span>
          </template>
        </div>

        <!-- Messages Area -->
        <div ref="messagesContainer" class="chat-messages" @scroll="onMessagesScroll">
          <!-- Load More -->
          <div v-if="hasMoreHistory && messages.length > 0" class="chat-load-more">
            <el-button v-if="!loadingHistory" text size="small" @click="loadMoreHistory">
              加载更早的消息
            </el-button>
            <el-button v-else text size="small" loading>
              加载中...
            </el-button>
          </div>

          <!-- Empty State -->
          <div v-if="messages.length === 0 && !loadingHistory" class="chat-empty">
            <div class="chat-empty-icon">💬</div>
            <h3>暂无消息</h3>
            <p>选择一个好友，开始你的第一次对话吧</p>
          </div>

          <!-- History Loading -->
          <div v-if="loadingHistory && messages.length === 0" class="chat-empty">
            <el-skeleton :rows="3" animated />
          </div>

          <!-- Messages -->
          <div
            v-for="(msg, index) in messages"
            :key="msg.id || index"
            class="message-item"
            :class="getMessageSide(msg)"
          >
            <!-- 群聊：对方消息显示发送者名称 -->
            <div v-if="conversationType === 'group' && getMessageSide(msg) === 'left' && msg.senderName" class="message-sender">
              {{ msg.senderName }}
            </div>
            <div class="message-bubble">
              <div class="message-content">{{ msg.content }}</div>
              <div class="message-meta">
                <span v-if="getMessageSide(msg) === 'right'" class="message-status" :class="'status-' + (msg.status || 'SENT').toLowerCase()">
                  <template v-if="msg.status === 'READ'">✓✓</template>
                  <template v-else>✓</template>
                </span>
                <span class="message-time">{{ formatTime(msg.createdAt) }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Input Area -->
        <div class="chat-input-area">
          <el-input
            v-model="inputContent"
            type="textarea"
            :rows="2"
            :placeholder="selectedUser || selectedGroup ? '输入消息，Enter 发送，Shift+Enter 换行' : '请先选择一个好友或群组'"
            :disabled="!selectedUser && !selectedGroup"
            @keydown="handleKeyDown"
          />
          <div class="chat-input-actions">
            <span class="chat-input-hint">Enter 发送</span>
            <el-button
              type="primary"
              :disabled="(!selectedUser && !selectedGroup) || !inputContent.trim()"
              @click="sendMessage"
            >
              发送
            </el-button>
          </div>
        </div>
      </main>
    </template>
  </div>

  <!-- 创建群组对话框 -->
  <el-dialog
    v-model="createGroupDialogVisible"
    title="创建群组"
    width="400px"
    :close-on-click-modal="false"
  >
    <el-form @submit.prevent="handleCreateGroup">
      <el-form-item label="群名称" required>
        <el-input
          v-model="newGroupName"
          placeholder="请输入群名称"
          maxlength="32"
          clearable
          @keyup.enter="handleCreateGroup"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="createGroupDialogVisible = false">取消</el-button>
      <el-button type="primary" :disabled="!newGroupName.trim()" @click="handleCreateGroup">
        创建
      </el-button>
    </template>
  </el-dialog>

  <!-- 群信息对话框 -->
  <el-dialog
    v-model="groupInfoDialogVisible"
    :title="selectedGroup?.name"
    width="420px"
  >
    <div class="group-info-section">
      <h4 class="group-info-title">成员 ({{ groupMembers.length }})</h4>
      <div v-if="loadingGroupMembers" style="text-align:center;padding:16px">
        <el-skeleton :rows="3" animated />
      </div>
      <div v-else-if="groupMembers.length === 0" class="group-info-empty">
        暂无成员
      </div>
      <div v-else class="group-member-list">
        <div v-for="m in groupMembers" :key="m.id" class="group-member-item">
          <el-avatar :size="28" :src="m.avatar">{{ (m.nickname || m.username).charAt(0) || 'U' }}</el-avatar>
          <span class="group-member-name">{{ m.nickname || m.username }}</span>
          <el-tag v-if="m.role === 'OWNER'" size="small" type="warning">群主</el-tag>
          <el-tag v-else-if="m.role === 'ADMIN'" size="small">管理员</el-tag>
        </div>
      </div>
    </div>
    <template #footer>
      <el-button type="primary" @click="addMemberDialogVisible = true; addMemberSearchKeyword = ''; addMemberSearchResults = []">
        + 添加成员
      </el-button>
      <el-button @click="groupInfoDialogVisible = false">关闭</el-button>
    </template>
  </el-dialog>

  <!-- 添加成员对话框 -->
  <el-dialog
    v-model="addMemberDialogVisible"
    title="添加成员"
    width="400px"
  >
    <el-input
      v-model="addMemberSearchKeyword"
      placeholder="搜索用户名或昵称"
      clearable
      @input="searchAddMember"
      @clear="addMemberSearchResults = []"
    />
    <div v-if="searchingAddMember" style="text-align:center;padding:16px">
      搜索中...
    </div>
    <div v-else-if="addMemberSearchResults.length === 0 && addMemberSearchKeyword" class="group-info-empty">
      未找到可添加的用户
    </div>
    <div v-else class="group-member-list" style="margin-top:12px">
      <div v-for="u in addMemberSearchResults" :key="u.id" class="group-member-item">
        <el-avatar :size="28" :src="u.avatar">{{ u.nickname?.charAt(0) || 'U' }}</el-avatar>
        <span class="group-member-name">{{ u.nickname || u.username }}</span>
        <el-button type="primary" text size="small" @click="handleAddMemberToGroup(u.id)">
          添加
        </el-button>
      </div>
    </div>
  </el-dialog>
</template>

<style scoped>

/* ═════════════════════════════════
   R11 规范重写 — IM 聊天页面
   ═════════════════════════════════ */

/* ── 字号体系（页面仅 3 种字号: 18px / 15px / 13px） ── */
.chat-header-name { font-size: 18px; }
.list-item-name,
.message-content { font-size: 15px; }
.list-item-sub,
.panel-online,
.chat-input-hint { font-size: 14px; }
.message-time,
.list-section-title { font-size: 12px; }

/* ── Layout ── */
.chat-page {
  display: flex;
  height: 100vh;
  background-color: var(--color-bg);
  overflow: hidden;
}

.chat-loading {
  padding: var(--space-8);
  width: 100%;
}

/* ════════════════════ QQ Nav Bar ════════════════════ */
.chat-nav {
  width: 56px; min-width: 56px;
  display: flex; flex-direction: column; align-items: center;
  background-color: #1e1e2f;
  padding: var(--space-3) 0; gap: var(--space-2);
}

.nav-section { display: flex; flex-direction: column; align-items: center; gap: 4px; }
.nav-top { flex: 0 0 auto; }
.nav-middle { flex: 1; justify-content: center; gap: 6px; }
.nav-bottom { flex: 0 0 auto; }

.nav-avatar { cursor: pointer; border: 2px solid transparent; transition: border-color var(--transition-fast); }
.nav-avatar.online { border-color: var(--color-success); }

.nav-btn {
  width: 40px; height: 40px; border-radius: 10px; border: none;
  background: transparent; color: rgba(255,255,255,0.55);
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  position: relative;
  transition: background-color var(--transition-fast), color var(--transition-fast);
}
.nav-btn:hover { background: rgba(255,255,255,0.1); color: rgba(255,255,255,0.85); }
.nav-btn.active { background: rgba(255,255,255,0.15); color: #fff; }

.nav-badge {
  position: absolute; top: 2px; right: 2px;
  min-width: 16px; height: 16px; padding: 0 4px;
  background: var(--color-danger); color: #fff;
  font-size: 10px; font-weight: 600; line-height: 16px; border-radius: 8px; text-align: center;
}

/* ════════════════════ Content Panel ════════════════════ */
.chat-panel {
  width: 280px; min-width: 280px;
  border-right: 1px solid var(--color-border-light);
  display: flex; flex-direction: column;
  background-color: var(--color-bg-secondary);
}

.panel-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: var(--space-4); min-height: 56px;
  border-bottom: 1px solid var(--color-border-light);
}

.panel-title { font-size: 16px; font-weight: 600; color: var(--color-text-primary); margin: 0; }

.panel-search { padding: var(--space-2) var(--space-4); border-bottom: 1px solid var(--color-border-light); }
.panel-search :deep(.el-input__wrapper) { background-color: var(--color-bg-tertiary); box-shadow: none; border-radius: 8px; }

.panel-online {
  padding: var(--space-2) var(--space-4); display: flex; align-items: center; gap: 6px;
  min-height: 28px; border-bottom: 1px solid var(--color-border-light);
  font-size: 12px; color: var(--color-text-tertiary);
}

.online-dot { width: 6px; height: 6px; border-radius: 50%; background-color: var(--color-text-tertiary); flex-shrink: 0; }
.online-dot.active { background-color: var(--color-success); }

.panel-list { flex: 1; overflow-y: auto; }

.list-section-title {
  padding: var(--space-2) var(--space-4) var(--space-1);
  font-size: 12px; font-weight: 600; color: var(--color-text-tertiary);
  text-transform: uppercase; letter-spacing: 0.5px;
}

.panel-empty { padding: var(--space-8) var(--space-4); text-align: center; color: var(--color-text-tertiary); font-size: 13px; line-height: 1.6; }
.panel-empty p { margin: 0 0 var(--space-1) 0; }

.list-item {
  display: flex; align-items: center; gap: var(--space-3);
  min-height: 52px; padding: var(--space-1) var(--space-4);
  cursor: pointer; transition: background-color var(--transition-fast);
}
.list-item:hover { background-color: var(--color-bg-tertiary); }
.list-item.active { background-color: var(--color-primary-light); }

.list-avatar-wrap { position: relative; flex-shrink: 0; line-height: 0; }

.list-online-dot {
  position: absolute; bottom: 0; right: 0; width: 8px; height: 8px; border-radius: 50%;
  background-color: var(--color-text-tertiary); border: 2px solid var(--color-bg-secondary);
  transition: background-color var(--transition-fast);
}
.list-online-dot.online { background-color: var(--color-success); }

.list-item-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.list-item-name { font-size: 14px; font-weight: 500; color: var(--color-text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; line-height: 1.3; }
.list-item-sub { font-size: 12px; color: var(--color-text-tertiary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; line-height: 1.3; }
.list-item-actions { display: flex; gap: 4px; flex-shrink: 0; }

.list-unread {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 20px; height: 20px; padding: 0 6px;
  background: var(--color-danger); color: #fff;
  font-size: 11px; font-weight: 600; border-radius: 10px; flex-shrink: 0;
}

/* ════════════════════ Main Chat ════════════════════ */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

/* -- Chat header (title 18px) -- */
.chat-header {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-4) var(--space-6);
  min-height: 56px;
  border-bottom: 1px solid var(--color-border-light);
  background-color: var(--color-bg);
}

.chat-header-name {
  font-weight: 600;
  color: var(--color-text-primary);
  line-height: 1.3;
}

/* -- Messages -- */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-6);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* Empty state */
.chat-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  color: var(--color-text-tertiary);
}

.chat-empty-icon {
  font-size: 48px;
  line-height: 1;
  margin-bottom: var(--space-4);
}

.chat-empty h3 {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-secondary);
  margin-bottom: var(--space-2);
}

.chat-empty p {
  font-size: 13px;
  margin: 0;
}

/* Message bubbles */
.message-item {
  display: flex;
  max-width: 70%;
}

.message-item.left {
  align-self: flex-start;
}

.message-item.right {
  align-self: flex-end;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.6;
  word-break: break-word;
}

.message-item.left .message-bubble {
  background-color: var(--color-bg-secondary);
  border: 1px solid var(--color-border-light);
  color: var(--color-text-primary);
  border-bottom-left-radius: 4px;
}

.message-item.right .message-bubble {
  background-color: var(--color-primary);
  color: var(--color-text-inverse);
  border-bottom-right-radius: 4px;
}

.message-content {
  font-size: 15px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.message-meta {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
  margin-top: 4px;
}

.message-time {
  font-size: 12px;
  opacity: 0.65;
  line-height: 1;
}

.message-status {
  font-size: 12px;
  line-height: 1;
  font-weight: 600;
}

/* 右侧蓝色气泡内：白色勾 + 不同透明度区分状态 */
.message-status.status-sent {
  color: var(--color-text-inverse);
  opacity: 0.45;
}

.message-status.status-delivered {
  color: var(--color-text-inverse);
  opacity: 0.7;
}

/* 已读 → 亮蓝色，与气泡色拉开对比 */
.message-status.status-read {
  color: #7dd3fc;
  opacity: 1;
}

/* -- Load More -- */
.chat-load-more {
  display: flex;
  justify-content: center;
  padding: var(--space-2) 0;
}

/* ── Chat Header Info ── */
.chat-header-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-header-meta {
  font-size: 13px;
  color: var(--color-text-tertiary);
  line-height: 1.3;
}

.message-sender {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-primary);
  margin-bottom: 2px;
  padding-left: 2px;
  line-height: 1.3;
}

.message-item.left .message-sender {
  align-self: flex-start;
}

/* ── Group Info Dialog ── */
.group-info-section {
  max-height: 360px;
  overflow-y: auto;
}

.group-info-title {
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 var(--space-3) 0;
  color: var(--color-text-primary);
}

.group-info-empty {
  text-align: center;
  padding: var(--space-8);
  color: var(--color-text-tertiary);
  font-size: 14px;
}

.group-member-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.group-member-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-2) var(--space-2);
  border-radius: 6px;
  transition: background-color var(--transition-fast);
}

.group-member-item:hover {
  background-color: var(--color-bg-tertiary);
}

.group-member-name {
  flex: 1;
  font-size: 14px;
  color: var(--color-text-primary);
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* -- Input area (min-height 48px) -- */
.chat-input-area {
  border-top: 1px solid var(--color-border-light);
  padding: var(--space-4) var(--space-6);
  background-color: var(--color-bg);
}

.chat-input-area :deep(.el-textarea__inner) {
  min-height: 48px !important;
  font-size: 15px;
  line-height: 1.5;
  border-radius: 8px;
}

.chat-input-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--space-3);
}

.chat-input-hint {
  color: var(--color-text-tertiary);
  line-height: 1;
}
</style>
