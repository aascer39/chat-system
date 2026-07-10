# Skill: Vue 3 + TypeScript Frontend Development Standards

## Purpose

Provide engineering-grade, reusable frontend development standards for Vue 3 + TypeScript + Vite + Pinia + Element Plus projects. When Claude Code uses this skill, it MUST automatically validate project structure, code conventions, state management, API layer, environment configuration, and component design against the rules defined below. It MUST reject violations and generate compliant code only.

## Scope

**Use this skill when:**
- Starting a new Vue 3 + TypeScript + Vite frontend project
- Reviewing or refactoring an existing Vue 3 project to meet production standards
- Generating Vue 3 components, stores, API modules, pages, or composables
- Setting up project scaffolding (ESLint, Prettier, Husky, etc.)
- Implementing authentication, routing, permission, or data-fetching logic

**DO NOT use this skill when:**
- The project uses Vue 2, Options API, Vuex, or JavaScript (no TypeScript)
- The project uses a different UI framework (Ant Design, Naive UI, etc.) — Element Plus rules do not apply
- The task is backend-only or infrastructure-only (Docker, CI/CD, database schema)
- Writing unit tests only — the testing section applies but is not the primary focus

## Rules

### R1 — Technology Stack
- MUST use Vue 3 with Composition API (`<script setup lang="ts">`)
- MUST use TypeScript in strict mode (`strict: true` in `tsconfig.json`)
- MUST use Vite as build tool
- MUST use Pinia for state management
- MUST use Vue Router for routing
- MUST use Element Plus for UI components
- MUST use Axios for HTTP requests
- MUST use Vitest + Vue Test Utils for testing
- MUST NOT use Vue 2 or Options API
- MUST NOT use Vuex
- MUST NOT use JavaScript in business code (`.js` / `.jsx` files are prohibited for business logic; only `vite.config.ts`, `eslint.config.js` and similar tooling config files are exempt)
- MUST NOT use jQuery or any direct DOM manipulation library
- MUST NOT use global variables to share state across components

### R2 — Project Directory Structure
Every Vue project MUST enforce the following directory layout under `src/`:

```
src/
├── api/            # API request modules (one file per domain)
│   ├── user.ts
│   ├── auth.ts
│   └── ...
├── assets/         # Static assets (images, fonts, etc.)
├── components/     # Shared/reusable components
│   ├── common/     # Base components (buttons, dialogs, etc.)
│   └── business/   # Business-specific components
├── composables/    # Shared composable functions (use-prefixed)
├── hooks/          # Lifecycle hooks or custom hook-like functions
├── layouts/        # Layout components (default layout, auth layout)
├── router/         # Vue Router configuration
│   ├── index.ts
│   ├── modules/    # Route modules split by domain
│   └── guards/     # Navigation guards
├── stores/         # Pinia stores (one file per store module)
│   ├── user.ts
│   ├── permission.ts
│   └── app.ts
├── types/          # TypeScript type/interface definitions
│   ├── api.d.ts    # API request/response types
│   ├── store.d.ts  # Store-related types
│   └── ...
├── utils/          # Utility functions
├── views/          # Page-level components (one directory per route module)
│   ├── system/
│   ├── dashboard/
│   └── ...
├── constants/      # Constants and enums
└── styles/         # Global styles and Element Plus overrides
```

- Each directory MUST have a clear, single responsibility
- MUST NOT place business logic in `utils/` — `utils/` is for pure helper functions only
- `views/` MUST NOT contain API calls directly (use `api/` modules and `composables/`)

### R3 — Component Development

- MUST use `<script setup lang="ts">` for all Vue components
- MUST use Composition API (`ref`, `reactive`, `computed`, `watch`, `onMounted`, etc.)
- MUST define Props with TypeScript type annotations:
  ```ts
  const props = defineProps<{
    visible: boolean
    title?: string
    data: TableData
  }>()
  ```
- MUST define Emits with TypeScript type annotations:
  ```ts
  const emit = defineEmits<{
    (e: 'update:visible', val: boolean): void
    (e: 'submit', data: FormData): void
  }>()
  ```
- MUST NOT use `defineProps` with runtime-only declarations (avoid the non-generic array/object form)
- MUST NOT write complex logic in templates — extract to `computed`, functions, or composables
- Template complexity MUST be kept low: no more than one ternary operator per expression; no chained method calls in interpolation
- Components MUST have a single responsibility — if a component does more than one thing, extract sub-components
- MUST use `v-bind` with shorthand `:prop` consistently
- MUST NOT use `this` inside `<script setup>` (it is not available)

### R4 — TypeScript Usage

- `tsconfig.json` MUST set `"strict": true`
- ALL API responses MUST have defined types:
  ```ts
  interface ApiResponse<T> {
    code: number
    message: string
    data: T
  }

  interface LoginResponse {
    token: string
    userInfo: UserInfo
  }
  ```
- ALL DTOs and business objects MUST be typed with `interface` or `type`
- MUST use `interface` for object shapes; use `type` for unions, intersections, and primitives
- MUST NOT use `any` — use `unknown` and narrow with type guards when the type is uncertain
- MUST NOT use `as` type assertion to silence type errors — fix the root type issue instead
- MUST use `const` assertions for literal types where appropriate (`as const`)
- Generic types MUST have meaningful names (`T`, `K`, `V` are acceptable for simple generics; prefer domain names like `UserInfo`, `PageResult`)
- `.d.ts` files MUST be used for global type augmentation (e.g., `env.d.ts`, `global.d.ts`)

### R5 — Pinia State Management

- All global state MUST be managed through Pinia stores
- Stores MUST be split by business domain (user, permission, app config, etc.)
- Store naming: `useXxxStore` (e.g., `useUserStore`, `usePermissionStore`)
- Store files MUST use camelCase: `user.ts`, `permission.ts`, `app.ts`
- MUST NOT create circular dependencies between stores — if Store A needs Store B, inject it inside an action/getter, not at module level:
  ```ts
  // CORRECT
  const someAction = () => {
    const otherStore = useOtherStore()
    otherStore.doSomething()
  }

  // WRONG — import outside setup causes circular dependency
  const otherStore = useOtherStore()
  ```
- MUST NOT directly manipulate DOM inside a store
- MUST NOT include UI logic (toast messages, dialog visibility) inside a store
- Store actions MUST return Promises for async operations
- MUST use `storeToRefs` when destructuring store in components:
  ```ts
  const userStore = useUserStore()
  const { token, userInfo } = storeToRefs(userStore)
  const { login, logout } = userStore
  ```
- Local page state MUST use `ref` / `reactive` — do NOT put page-only state into Pinia

### R6 — API Layer

- All HTTP requests MUST go through a centralized Axios instance
- Axios instance MUST be configured in a single file (e.g., `src/utils/request.ts`)
- The Axios instance MUST include:
  - `baseURL` read from environment variable (`import.meta.env.VITE_API_BASE_URL`)
  - Request interceptor: attach `Authorization: Bearer <token>` from `useUserStore`
  - Response interceptor: unwrap `ApiResponse<T>.data` on success; reject on business error code
  - Response interceptor: on 401, clear user store and redirect to login
- API modules MUST be in `src/api/` — one file per domain, exporting pure functions:
  ```ts
  // src/api/user.ts
  import request from '@/utils/request'

  export interface GetUserListParams {
    page: number
    pageSize: number
    keyword?: string
  }

  export interface UserListResponse {
    list: UserInfo[]
    total: number
  }

  export const getUserList = (params: GetUserListParams) =>
    request.post<UserListResponse>('/user/list', params)
  ```
- MUST NOT call Axios directly in `.vue` files — always use `api/` modules
- MUST NOT hardcode API URLs in components or stores
- All API error handling MUST go through the Axios interceptor; individual callers MAY add `.catch` for domain-specific error handling

### R7 — Routing & Permissions

- Router MUST use lazy loading for all route components:
  ```ts
  {
    path: '/system/user',
    component: () => import('@/views/system/user/index.vue')
  }
  ```
- Navigation guard MUST:
  1. Check if a token exists in `useUserStore`
  2. If no token and route requires auth → redirect to `/login`
  3. If token exists but user info is not loaded → fetch user info + permission routes
  4. Generate dynamic routes based on permission data and add via `router.addRoute()`
- Route meta MUST include permission roles:
  ```ts
  interface RouteMeta {
    title: string
    icon?: string
    roles?: string[]
    hidden?: boolean
    keepAlive?: boolean
  }
  ```
- MUST support dynamic route generation from server-side permission data
- MUST NOT expose sensitive routes before authentication

### R8 — Element Plus Conventions

- MUST use Element Plus components as the primary UI building blocks
- MUST use `ElMessage` / `ElMessageBox` / `ElNotification` through a unified wrapper (e.g., `src/utils/message.ts`) for consistent styling and easier mocking in tests
- Forms MUST use `ElForm` with model binding AND `rules` validation:
  ```ts
  const rules: FormRules = {
    username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    password: [{ required: true, min: 6, message: '密码至少6位', trigger: 'blur' }]
  }
  ```
- Tables MUST use `ElTable` + `ElPagination` — pagination MUST be a reusable component or consistently configured
- Dialogs MUST be encapsulated into business components with `v-model:visible` pattern:
  ```ts
  // UserFormDialog.vue
  const props = defineProps<{ visible: boolean }>()
  const emit = defineEmits<{ (e: 'update:visible', val: boolean): void }>()
  ```
- MUST use `@element-plus/icons-vue` for icons — NOT inline SVG or font icon classes
- Element Plus MUST be imported on-demand using `unplugin-vue-components` and `unplugin-element-plus`
- MUST NOT override Element Plus component styles globally — use scoped styles or CSS variables

### R9 — Environment Variables

- All environment-specific configuration MUST use `.env` files:
  ```
  .env                # loaded in all cases (defaults)
  .env.development    # loaded in dev mode
  .env.production     # loaded in prod mode
  .env.local          # loaded in all cases, gitignored (secret overrides)
  ```
- MUST provide `.env.example` with placeholder values and documentation:
  ```env
  # .env.example
  VITE_API_BASE_URL=http://localhost:8080/api
  VITE_APP_TITLE=My App
  ```
- `.env.local` MUST be in `.gitignore`
- MUST NOT hardcode API base URLs or any environment-specific values in source code
- MUST NOT store tokens in `.env` files — tokens are runtime values, not build-time config
- All environment variables MUST use the `VITE_` prefix (Vite convention)
- Access MUST use `import.meta.env.VITE_XXX` — never `process.env`

### R10 — Code Quality & Tooling

- ESLint MUST be configured with `@typescript-eslint` rules:
  - `@typescript-eslint/no-explicit-any`: error
  - `@typescript-eslint/explicit-function-return-type`: warn
  - `vue/multi-word-component-names`: error (component names must be multi-word)
- Prettier MUST be configured with consistent settings (single quotes, trailing commas, semi)
- EditorConfig MUST exist at project root
- Husky + lint-staged MUST be configured to run ESLint and Prettier on pre-commit
- Import order MUST be sorted automatically using `eslint-plugin-import` or `@trivago/prettier-plugin-sort-imports`

### R11 — Naming Conventions

| Category | Convention | Example |
|----------|-----------|---------|
| Vue component file | PascalCase | `UserFormDialog.vue` |
| Vue component name (in template) | PascalCase | `<UserFormDialog />` |
| Directory name | kebab-case | `system-management/` |
| Non-component file | kebab-case | `user-api.ts`, `auth-store.ts` |
| Composable function | `use` prefix | `usePagination()`, `usePermission()` |
| Composable file | `use` prefix + kebab-case | `use-pagination.ts` |
| Pinia store | `use` + domain + `Store` | `useUserStore`, `useAppStore` |
| Pinia store file | domain + `.ts` | `user.ts`, `app.ts` |
| API function | camelCase | `getUserList`, `createUser` |
| TypeScript interface | PascalCase | `UserInfo`, `PageResult` |
| TypeScript type file | camelCase `.d.ts` or `.ts` | `api.d.ts`, `user.ts` |
| Enum | PascalCase | `UserStatus`, `MsgType` |
| Constant | uppercase with underscore | `MAX_FILE_SIZE`, `DEFAULT_PAGE_SIZE` |
| CSS class | kebab-case (BEM if complex) | `user-card`, `user-card__title` |

### R12 — Component Encapsulation

- Common/base components MUST be placed in `src/components/common/`
- Business components MUST be placed in `src/components/business/`
- Any component used in more than two places MUST be extracted as a shared component
- Complex logic MUST be extracted into composables (`src/composables/`):
  ```ts
  // composables/use-pagination.ts
  export function usePagination(fetchFn: () => Promise<void>) {
    const page = ref(1)
    const pageSize = ref(20)
    const total = ref(0)

    const handlePageChange = (p: number) => { page.value = p; fetchFn() }

    return { page, pageSize, total, handlePageChange }
  }
  ```
- A page component MUST NOT exceed 300 lines — extract child components or composables when it does
- `composables/` is for logic shared across components; `hooks/` is for lifecycle-interception logic (e.g., `usePageTitle`, `useKeepAlive`)

### R13 — Performance

- ALL route components MUST use dynamic import (lazy loading)
- Element Plus MUST be imported on-demand via `unplugin-vue-components` and `unplugin-element-plus`
- Large lists (1000+ items) MUST use virtual scrolling (`el-table-v2` or `vueuc`'s `VirtualList`)
- Images MUST use modern formats (WebP) and responsive loading where feasible
- Components conditionally displayed with `v-if` are preferred over `v-show` for large DOM trees
- `computed` MUST be used over method calls in templates where caching is beneficial

### R14 — Engineering

- Git commit messages MUST follow [Conventional Commits](https://www.conventionalcommits.org/):
  ```
  feat: add user management page
  fix: resolve login redirect loop
  refactor: extract pagination composable
  ```
- Husky MUST be configured with at least:
  - `pre-commit`: lint-staged
  - `commit-msg`: commitlint
- lint-staged MUST run ESLint and Prettier on staged files only
- CI MUST run type checking (`vue-tsc --noEmit`) and unit tests

### R15 — Testing

- Vitest MUST be the test runner
- Vue Test Utils MUST be used for component testing
- Core business logic (stores, composables, API modules) MUST have unit tests
- Test files MUST be placed alongside the source file or in a `__tests__/` directory:
  ```
  src/stores/__tests__/user.spec.ts
  src/composables/__tests__/use-pagination.spec.ts
  ```
- Store tests MUST use `pinia` testing utilities (`createPinia`, `setActivePinia`)
- MUST NOT test Element Plus internals — test only your business logic and component behavior

## Implementation Steps

When implementing code for a Vue 3 + TypeScript frontend project, follow these steps in order:

### Step 1 — Validate Project Structure
Check that `src/` contains the required directories (R2). If any are missing, report them as violations unless the project is in the scaffolding phase.

### Step 2 — Validate Configuration
Check `tsconfig.json` for `strict: true`. Verify `.env` files follow the convention (R9). Verify ESLint, Prettier, and EditorConfig exist.

### Step 3 — Determine Component Level
- Is the code a **page**? → Create in `views/<module>/`, use route lazy loading (R7)
- Is the code a **shared component**? → Create in `components/common/` or `components/business/` (R12)
- Is the code a **composable**? → Create in `composables/` with `use` prefix (R11)
- Is the code a **store**? → Create in `stores/` split by domain (R5)
- Is the code an **API module**? → Create in `api/` (R6)

### Step 4 — Implement with Full Typing
Write all code with TypeScript strict mode. Define all interfaces and types before implementing logic. Use `interface` for object types, `type` for unions.

### Step 5 — Apply Conventions
- Use `<script setup lang="ts">` with typed `defineProps` / `defineEmits` (R3)
- Extract complex template logic to `computed` or functions
- Use `storeToRefs` when consuming store state (R5)
- Use `api/` modules for all HTTP calls (R6)

### Step 6 — Add Error Handling
Ensure API error handling flows through the interceptor. Add domain-specific error handling where needed. Never leave unhandled promise rejections.

### Step 7 — Verify Prohibited Patterns
Scan the output for:
- `any` type → replace with specific type or `unknown`
- Direct `axios` calls in `.vue` files → move to `api/` module
- `this` in `<script setup>` → remove
- Hardcoded URLs → move to `.env`
- Missing type definitions → add

## Code Patterns

### Pattern 1 — Axios Instance (`src/utils/request.ts`)
```ts
import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/stores/user'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000,
})

// Request interceptor
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error: AxiosError) => Promise.reject(error),
)

// Response interceptor
request.interceptors.response.use(
  (response) => {
    const { code, message, data } = response.data
    if (code === 0 || code === 200) {
      return data
    }
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message))
  },
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      const userStore = useUserStore()
      userStore.resetToken()
      router.push('/login')
    }
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  },
)

export default request
```

### Pattern 2 — Pinia User Store (`src/stores/user.ts`)
```ts
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login, getUserInfo, type LoginParams } from '@/api/auth'
import router from '@/router'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>('')
  const userInfo = ref<UserInfo | null>(null)
  const roles = ref<string[]>([])

  const getToken = computed(() => token.value)
  const isLoggedIn = computed(() => !!token.value)

  async function loginAction(params: LoginParams) {
    const res = await login(params)
    token.value = res.token
    userInfo.value = res.userInfo
  }

  async function fetchUserInfo() {
    const info = await getUserInfo()
    userInfo.value = info
    roles.value = info.roles
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    roles.value = []
    router.push('/login')
  }

  function resetToken() {
    token.value = ''
  }

  return {
    token,
    userInfo,
    roles,
    getToken,
    isLoggedIn,
    loginAction,
    fetchUserInfo,
    logout,
    resetToken,
  }
})
```

### Pattern 3 — API Module (`src/api/auth.ts`)
```ts
import request from '@/utils/request'

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  userInfo: UserInfo
}

export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar: string
  email: string
  roles: string[]
}

export const login = (data: LoginParams) =>
  request.post<LoginResult>('/auth/login', data)

export const getUserInfo = () =>
  request.get<UserInfo>('/user/info')

export const logout = () =>
  request.post('/auth/logout')
```

### Pattern 4 — Route Guard (`src/router/guards/permission.ts`)
```ts
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'

const whiteList = ['/login', '/register', '/forgot-password']

export function createPermissionGuard(router) {
  router.beforeEach(async (to, _from, next) => {
    const userStore = useUserStore()
    const permissionStore = usePermissionStore()

    if (userStore.token) {
      if (to.path === '/login') {
        next('/')
        return
      }
      if (!userStore.userInfo) {
        try {
          await userStore.fetchUserInfo()
          const accessRoutes = await permissionStore.generateRoutes(userStore.roles)
          accessRoutes.forEach((route: RouteRecordRaw) => {
            router.addRoute(route)
          })
          next({ ...to, replace: true })
        } catch {
          userStore.resetToken()
          next('/login')
        }
        return
      }
      next()
    } else {
      if (whiteList.includes(to.path)) {
        next()
      } else {
        next(`/login?redirect=${to.path}`)
      }
    }
  })
}
```

## Edge Cases

### Edge Case 1 — Circular Store Dependencies
**Problem**: Store A imports Store B, Store B imports Store A → runtime error.
**Solution**: Always call `useXxxStore()` inside a function (action/getter), never at the top level of the store file. If circular dependency still occurs, extract shared logic into a third store or composable.

### Edge Case 2 — Token Expired Mid-Session
**Problem**: User's token expires during active use; subsequent API calls fail with 401.
**Solution**: The Axios response interceptor (R6) catches 401 globally. Implement a token refresh queue: if a refresh is in progress, queue pending requests and retry after refresh completes. If refresh fails, clear state and redirect to login.

### Edge Case 3 — Route Permission Flash on Page Reload
**Problem**: On page refresh, the store resets and dynamic routes disappear before the permission guard runs.
**Solution**: The navigation guard MUST call `userStore.fetchUserInfo()` + `permissionStore.generateRoutes()` before resolving the route when `userInfo` is null. Use `router.addRoute()` to re-register dynamic routes on each page load.

### Edge Case 4 — Element Plus Component Version Mismatch
**Problem**: `unplugin-vue-components` auto-imports a component that does not exist in the installed Element Plus version.
**Solution**: Pin the Element Plus version in `package.json`. Match the `unplugin-vue-components` resolver version to the Element Plus version. Use `components.d.ts` to verify resolved types.

### Edge Case 5 — Environment Variable Missing at Runtime
**Problem**: `import.meta.env.VITE_XXX` returns `undefined`, causing silent failures.
**Solution**: Validate required environment variables at app startup. Create a `src/utils/env.ts` that checks for required `VITE_` variables and throws a descriptive error if any are missing.

### Edge Case 6 — Large Form with Deep Nesting
**Problem**: A deeply nested reactive form object causes performance degradation and complex validation.
**Solution**: Split the form into multiple sub-components, each managing its own slice of the form state. Use `provide`/`inject` or a dedicated form store if cross-component validation is needed.

### Edge Case 7 — IE11 / Legacy Browser Compatibility
**Problem**: Vite does not support IE11 by default, but the project may need legacy browser support.
**Solution**: Use `@vitejs/plugin-legacy` with `targets: ['defaults', 'not IE 11']` for modern browsers. If IE11 is strictly required, this skill does not apply — use a different build tool.

## Output Format Rules

When Claude Code uses this skill to generate or review code:

1. **Structure**: Output MUST follow this order — validate project structure → validate config → generate/review code → verify prohibitions
2. **Violation reporting**: For each rule violation, report:
   - Rule ID (e.g., R3 — Component Development)
   - File path and line
   - Violation description
   - Fix instruction with code example
3. **Generated code**: MUST include full TypeScript types. MUST NOT use `any`. MUST NOT omit error handling.
4. **Review output**: Group findings by category (Structure, TypeScript, Store, API, Component, Performance). Tag each finding with severity: `ERROR` (violates MUST rule), `WARNING` (violates SHOULD/MAY rule), `INFO` (suggestion).
5. **When giving examples**: Use `// CORRECT` / `// WRONG` annotations for comparison. Always show WHY the wrong pattern is wrong.
6. **When scaffolding**: Provide the complete file content for every generated file. Do not use "..." or "similar" — every line must be explicit.

## Example

### Task
Generate a user management page with a searchable, paginated table and a create/edit dialog.

### Correct Output (Claude Code uses this skill)

**Step 1**: Validate project structure — `api/`, `stores/`, `components/business/`, `views/system/` exist.

**Step 2**: Implement API module `src/api/system/user.ts`:
```ts
import request from '@/utils/request'

export interface UserQuery {
  page: number
  pageSize: number
  keyword?: string
  status?: number
}

export interface UserItem {
  id: number
  username: string
  nickname: string
  email: string
  status: number
  createdAt: string
}

export interface UserPageResult {
  list: UserItem[]
  total: number
}

export const getUserPage = (params: UserQuery) =>
  request.post<UserPageResult>('/system/user/page', params)

export const createUser = (data: Partial<UserItem>) =>
  request.post('/system/user/create', data)

export const updateUser = (data: Partial<UserItem>) =>
  request.put('/system/user/update', data)

export const deleteUser = (id: number) =>
  request.delete(`/system/user/${id}`)
```

**Step 3**: Implement composable `src/composables/use-user-crud.ts`:
```ts
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { getUserPage, createUser, updateUser, deleteUser, type UserQuery, type UserItem } from '@/api/system/user'

export function useUserCrud() {
  const loading = ref(false)
  const tableData = ref<UserItem[]>([])
  const total = ref(0)
  const query = reactive<UserQuery>({ page: 1, pageSize: 20, keyword: '' })

  async function fetchData() {
    loading.value = true
    try {
      const res = await getUserPage(query)
      tableData.value = res.list
      total.value = res.total
    } finally {
      loading.value = false
    }
  }

  async function handleDelete(id: number) {
    await deleteUser(id)
    ElMessage.success('删除成功')
    await fetchData()
  }

  return { loading, tableData, total, query, fetchData, handleDelete }
}
```

**Step 4**: Implement page `src/views/system/user/index.vue`:
```vue
<script setup lang="ts">
import { onActivated } from 'vue'
import { storeToRefs } from 'pinia'
import { useUserCrud } from '@/composables/use-user-crud'
import UserFormDialog from '@/components/business/UserFormDialog.vue'

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const { loading, tableData, total, query, fetchData } = useUserCrud()

onActivated(() => {
  fetchData()
})

function handleEdit(id: number) {
  editingId.value = id
  dialogVisible.value = true
}

function handleCreate() {
  editingId.value = null
  dialogVisible.value = true
}

function handleDialogClose() {
  dialogVisible.value = false
  editingId.value = null
  fetchData()
}
</script>

<template>
  <div class="user-management">
    <el-card>
      <el-form :model="query" inline>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="用户名/昵称" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button type="success" @click="handleCreate">新增用户</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="nickname" label="昵称" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row.id)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @change="fetchData"
      />
    </el-card>

    <user-form-dialog
      v-model:visible="dialogVisible"
      :user-id="editingId"
      @success="handleDialogClose"
    />
  </div>
</template>
```

**Step 5**: Verify — no `any`, no direct Axios calls, no hardcoded URLs, types defined, lazy loading in router, store not overused for local state.
