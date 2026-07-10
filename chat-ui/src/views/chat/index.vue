<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElInput } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getCurrentUser } from '@/api/user'
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
import { safeParse } from '@/utils/json'
import type { UserInfo, ChatMessage, FriendRequestVO, FriendVO } from '@/types/api'

const router = useRouter()
const userStore = useUserStore()

// ── State ──
const connected = ref(false)
const onlineCount = ref(0)
const currentUser = ref<UserInfo | null>(null)
const messages = ref<ChatMessage[]>([])
const selectedUser = ref<UserInfo | null>(null)
const inputContent = ref('')
const loading = ref(true)

// Sidebar tabs: 'friends' | 'requests' | 'add'
const activeTab = ref<'friends' | 'requests' | 'add'>('friends')

// Friends
const friends = ref<FriendVO[]>([])
const pendingRequests = ref<FriendRequestVO[]>([])

// Search
const searchKeyword = ref('')
const searchResults = ref<UserInfo[]>([])
const searching = ref(false)

let ws: WebSocket | null = null
let heartbeatTimer: ReturnType<typeof setInterval> | null = null
const messagesContainer = ref<HTMLElement | null>(null)

const pendingCount = computed(() => pendingRequests.value.length)

// ── Init ──
onMounted(async () => {
  await loadCurrentUser()
  await loadFriends()
  await loadPendingRequests()
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

async function loadPendingRequests() {
  try {
    pendingRequests.value = await getPendingRequests()
  } catch {
    pendingRequests.value = []
  }
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
      ws?.send(JSON.stringify({ type: 'PING' }))
    }, 30000)
  }

  ws.onmessage = (event: MessageEvent) => {
    try {
      const data = safeParse(event.data) as Record<string, unknown>
      if (data.type === 'ONLINE_COUNT') {
        onlineCount.value = Number(data.count)
        return
      }
      const msg = data as unknown as ChatMessage
      messages.value.push(msg)
      scrollToBottom()
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

  if (!selectedUser.value) {
    ElMessage.warning('请选择一个好友开始聊天')
    return
  }

  const msg: ChatMessage = {
    receiverId: selectedUser.value.userId,
    content,
    messageType: 'TEXT',
  }

  ws.send(JSON.stringify(msg))
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
}

function getFriendStatus(userId: number): boolean {
  // For now, just check if this user is online based on WebSocket presence
  // In a full implementation, you'd maintain a Set of online user IDs
  return connected.value
}

function switchTab(tab: 'friends' | 'requests' | 'add') {
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
      <!-- ===== Sidebar ===== -->
      <aside class="chat-sidebar">
        <!-- Current User -->
        <div class="sidebar-header">
          <div class="sidebar-user">
            <el-avatar :size="32" :src="currentUser?.avatar" class="user-avatar">
              {{ currentUser?.nickname?.charAt(0) || 'U' }}
            </el-avatar>
            <div class="sidebar-user-info">
              <span class="sidebar-username">{{ currentUser?.nickname || currentUser?.username }}</span>
              <span class="sidebar-status" :class="{ online: connected }">
                {{ connected ? '在线' : '离线' }}
              </span>
            </div>
          </div>
          <el-button text circle @click="handleLogout" title="退出登录">
            <el-icon><svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg></el-icon>
          </el-button>
        </div>

        <!-- Search Bar -->
        <div class="sidebar-search">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索用户..."
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

        <!-- Online Count (R11.5: 8px green dot) -->
        <div class="sidebar-online">
          <span class="online-dot" :class="{ active: connected }"></span>
          <span>{{ onlineCount }} 人在线</span>
        </div>

        <!-- Tabs -->
        <div class="sidebar-tabs">
          <button
            class="sidebar-tab"
            :class="{ active: activeTab === 'friends' }"
            @click="switchTab('friends')"
          >
            好友
          </button>
          <button
            class="sidebar-tab"
            :class="{ active: activeTab === 'requests' }"
            @click="switchTab('requests')"
          >
            请求
            <span v-if="pendingCount > 0" class="tab-badge">{{ pendingCount }}</span>
          </button>
          <button
            class="sidebar-tab"
            :class="{ active: activeTab === 'add' }"
            @click="switchTab('add')"
          >
            添加
          </button>
        </div>

        <!-- Tab Content: Friends -->
        <div v-show="activeTab === 'friends'" class="sidebar-list">
          <div v-if="friends.length === 0" class="sidebar-empty">
            <p>暂无好友</p>
            <el-button type="text" size="small" @click="switchTab('add')">去添加好友</el-button>
          </div>
          <div
            v-for="friend in friends"
            :key="friend.userId"
            class="sidebar-user-item"
            :class="{ active: selectedUser?.userId === friend.userId }"
            @click="selectFriend(friend)"
          >
            <el-avatar :size="28" :src="friend.avatar" class="user-avatar">
              {{ friend.nickname?.charAt(0) || 'U' }}
            </el-avatar>
            <span class="user-name">{{ friend.nickname || friend.username }}</span>
            <div class="user-actions">
              <el-button
                type="danger"
                text
                size="small"
                title="删除好友"
                @click.stop="handleRemoveFriend(friend.userId)"
              >
                <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
              </el-button>
            </div>
          </div>
        </div>

        <!-- Tab Content: Requests -->
        <div v-show="activeTab === 'requests'" class="sidebar-list">
          <div v-if="pendingRequests.length === 0" class="sidebar-empty">
            <p>暂无待处理的好友请求</p>
          </div>
          <div
            v-for="req in pendingRequests"
            :key="req.id"
            class="request-item"
          >
            <div class="request-info">
              <el-avatar :size="28" :src="req.fromAvatar" class="user-avatar">
                {{ req.fromNickname?.charAt(0) || 'U' }}
              </el-avatar>
              <span class="user-name">{{ req.fromNickname || req.fromUsername }}</span>
            </div>
            <div class="request-actions">
              <el-button type="primary" size="small" @click="handleAcceptRequest(req.id)">接受</el-button>
              <el-button size="small" @click="handleRejectRequest(req.id)">拒绝</el-button>
            </div>
          </div>
        </div>

        <!-- Tab Content: Add Friend (Search Results) -->
        <div v-show="activeTab === 'add'" class="sidebar-list">
          <div v-if="searching" class="sidebar-empty">
            <p>搜索中...</p>
          </div>
          <div v-else-if="searchKeyword && searchResults.length === 0" class="sidebar-empty">
            <p>未找到匹配的用户</p>
          </div>
          <div v-else-if="!searchKeyword" class="sidebar-empty">
            <p>输入用户名或昵称搜索用户</p>
          </div>
          <div
            v-for="user in searchResults"
            :key="user.id"
            class="sidebar-user-item"
          >
            <el-avatar :size="28" :src="user.avatar" class="user-avatar">
              {{ user.nickname?.charAt(0) || 'U' }}
            </el-avatar>
            <span class="user-name">{{ user.nickname || user.username }}</span>
            <el-button type="primary" text size="small" @click="handleAddFriend(user.id)">
              + 添加
            </el-button>
          </div>
        </div>
      </aside>

      <!-- ===== Main Chat Area ===== -->
      <main class="chat-main">
        <!-- Chat Header -->
        <div class="chat-header">
          <template v-if="selectedUser">
            <el-avatar :size="28" :src="selectedUser.avatar">
              {{ selectedUser.nickname?.charAt(0) || 'U' }}
            </el-avatar>
            <span class="chat-header-name">{{ selectedUser.nickname || selectedUser.username }}</span>
          </template>
          <template v-else>
            <span class="chat-header-name">选择一个好友开始聊天</span>
          </template>
        </div>

        <!-- Messages Area -->
        <div ref="messagesContainer" class="chat-messages">
          <div v-if="messages.length === 0" class="chat-empty">
            <div class="chat-empty-icon">💬</div>
            <h3>暂无消息</h3>
            <p>选择一个好友，开始你的第一次对话吧</p>
          </div>

          <div
            v-for="(msg, index) in messages"
            :key="index"
            class="message-item"
            :class="getMessageSide(msg)"
          >
            <div class="message-bubble">
              <div class="message-content">{{ msg.content }}</div>
              <div class="message-time">{{ formatTime(msg.createdAt) }}</div>
            </div>
          </div>
        </div>

        <!-- Input Area -->
        <div class="chat-input-area">
          <el-input
            v-model="inputContent"
            type="textarea"
            :rows="2"
            :placeholder="selectedUser ? '输入消息，Enter 发送，Shift+Enter 换行' : '请先选择一位好友'"
            :disabled="!selectedUser"
            @keydown="handleKeyDown"
          />
          <div class="chat-input-actions">
            <span class="chat-input-hint">Enter 发送</span>
            <el-button
              type="primary"
              :disabled="!selectedUser || !inputContent.trim()"
              @click="sendMessage"
            >
              发送
            </el-button>
          </div>
        </div>
      </main>
    </template>
  </div>
</template>

<style scoped>

/* ═════════════════════════════════
   R11 规范重写 — IM 聊天页面
   ═════════════════════════════════ */

/* ── 字号体系（页面仅 3 种字号: 18px / 15px / 13px） ── */
.chat-header-name { font-size: 18px; }
.sidebar-username,
.user-name,
.message-content,
.sidebar-tab,
.sidebar-empty { font-size: 15px; }
.sidebar-status,
.sidebar-search :deep(.el-input__inner),
.chat-input-hint,
.request-actions .el-button { font-size: 14px; }
.message-time,
.tab-badge { font-size: 12px; }

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

/* ════════════════════ Sidebar ════════════════════ */
.chat-sidebar {
  width: 300px;
  min-width: 300px;
  border-right: 1px solid var(--color-border-light);
  display: flex;
  flex-direction: column;
  background-color: var(--color-bg-secondary);
}

/* -- Header (current user) -- */
.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4) var(--space-5);
  border-bottom: 1px solid var(--color-border-light);
  min-height: 56px;
}

.sidebar-user {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  min-width: 0;
}

.sidebar-user-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.sidebar-username {
  font-weight: 600;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.4;
}

.sidebar-status {
  color: var(--color-text-tertiary);
  line-height: 1.4;
}

.sidebar-status.online {
  color: var(--color-success);
}

/* -- Search -- */
.sidebar-search {
  padding: var(--space-3) var(--space-5);
  border-bottom: 1px solid var(--color-border-light);
}

.sidebar-search :deep(.el-input__wrapper) {
  background-color: var(--color-bg-tertiary);
  box-shadow: none;
  border-radius: 8px;
}

.sidebar-search :deep(.el-input__inner) {
  color: var(--color-text-primary);
}

.sidebar-search :deep(.el-input__inner::placeholder) {
  color: var(--color-text-tertiary);
}

/* -- Online indicator (8px green dot) -- */
.sidebar-online {
  padding: var(--space-2) var(--space-5);
  display: flex;
  align-items: center;
  gap: 6px;
  min-height: 32px;
  border-bottom: 1px solid var(--color-border-light);
  font-size: 13px;
  color: var(--color-text-secondary);
}

.online-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: var(--color-text-tertiary);
  flex-shrink: 0;
}

.online-dot.active {
  background-color: var(--color-success);
}

/* -- Tabs (36px height) -- */
.sidebar-tabs {
  display: flex;
  border-bottom: 1px solid var(--color-border-light);
}

.sidebar-tab {
  flex: 1;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  background: none;
  border: none;
  cursor: pointer;
  font-weight: 500;
  color: var(--color-text-secondary);
  position: relative;
  transition: color var(--transition-fast);
}

.sidebar-tab:hover {
  color: var(--color-text-primary);
}

.sidebar-tab.active {
  color: var(--color-primary);
  font-weight: 600;
}

.sidebar-tab.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 24%;
  right: 24%;
  height: 2px;
  background: var(--color-primary);
  border-radius: 1px;
}

.tab-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background: var(--color-danger);
  color: #fff;
  line-height: 1;
  border-radius: 9px;
}

/* -- List area -- */
.sidebar-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 0;
}

.sidebar-empty {
  padding: var(--space-10) var(--space-5);
  text-align: center;
  color: var(--color-text-tertiary);
  line-height: 1.6;
}

.sidebar-empty p {
  margin: 0 0 var(--space-2) 0;
}

/* -- User item (48px height) -- */
.sidebar-user-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  height: 48px;
  padding: 0 var(--space-5);
  cursor: pointer;
  transition: background-color var(--transition-fast);
}

.sidebar-user-item:hover {
  background-color: var(--color-bg-tertiary);
}

.sidebar-user-item.active {
  background-color: var(--color-primary-light);
}

.user-name {
  flex: 1;
  font-weight: 500;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.4;
}

.user-actions {
  flex-shrink: 0;
  opacity: 0;
  transition: opacity var(--transition-fast);
}

.sidebar-user-item:hover .user-actions {
  opacity: 1;
}

/* -- Request item -- */
.request-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 52px;
  padding: var(--space-2) var(--space-5);
  border-bottom: 1px solid var(--color-border-light);
}

.request-info {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  min-width: 0;
}

.request-actions {
  display: flex;
  gap: var(--space-1);
  flex-shrink: 0;
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

.message-time {
  font-size: 12px;
  margin-top: 4px;
  opacity: 0.65;
  text-align: right;
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
