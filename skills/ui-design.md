---
name: ui-design
description: Vue 3 前端页面美术设计规范 — 企业级 SaaS 风格、布局、色彩、排版、交互、响应式
metadata:
  type: reference
  level: mandatory
  appliesTo: vue-frontend
---

# UI 页面美术设计规范

## Skill Name

`ui-design`

## Purpose

统一 Vue 3 前端项目（使用 Element Plus）的 UI 美术设计标准，确保所有页面达到企业级 SaaS 产品的视觉品质（对标 GitHub、Notion、Linear、Vercel、Ant Design Pro 的设计水准），在可读性、信息层级、留白、响应式四个维度上优先保障。

## Scope

- **适用**: 所有 Vue 3 + Element Plus 前端页面的 UI 设计、组件样式、布局排版、交互反馈
- **覆盖**: 页面布局 | 色彩系统 | 排版规范 | 间距与留白 | 组件样式 | 交互反馈 | 响应式适配 | 深色模式 | 动效
- **不适用**: 非 Web 端（移动原生/桌面客户端）、不使用 Element Plus 的项目、后端仅 API 项目

## Rules

### R0: 设计总纲（强制）

> **宁可简洁，不可杂乱。**
> **当存在多个设计方案时，选择最简洁、最现代、最符合 2025 年 Web SaaS 设计趋势的方案。**

所有设计决策必须按此优先级排序：

1. **可读性** — 文字清晰、对比充足、行距适宜，用户 3 秒内能定位关键信息
2. **信息层级** — 页面结构一目了然，标题/正文/辅助信息视觉区分明确
3. **留白** — 元素之间呼吸感充足，不拥挤，不堆砌
4. **响应式** — 从 320px 到 2560px 均保持良好的阅读体验

当上述四项与"展示更多信息"冲突时，**永远优先满足前四项**。

### R1: 色彩系统（强制）

#### 1.1 主色与中性色

采用 SaaS 产品最通用的 60-30-10 色彩法则：

```css
/* === 中性色（占 60%）—— 背景、卡片、边框 === */
--color-bg:           #ffffff;           /* 页面背景 */
--color-bg-secondary: #f6f8fa;           /* 次级背景（侧栏、代码块） */
--color-bg-tertiary:  #eef1f5;           /* hover/按压态底色 */
--color-border:       #d0d7de;           /* 常规边框 */
--color-border-light: #e8ecf0;           /* 弱化边框（分割线） */

/* === 品牌色（占 30%）—— 主按钮、链接、激活态 === */
--color-primary:      #2563eb;           /* 主色（蓝 600） */
--color-primary-hover:#1d4ed8;           /* hover 加深 */
--color-primary-light:#eff6ff;           /* 浅色背景（tag/弱提示） */

/* === 功能色（占 10%）—— 语义反馈 === */
--color-success:      #059669;           /* 成功 */
--color-warning:      #d97706;           /* 警告 */
--color-danger:       #dc2626;           /* 危险/错误 */
--color-info:         #6b7280;           /* 信息提示 */

/* === 文字色 === */
--color-text-primary:   #1f2328;         /* 主要文字（标题/正文） */
--color-text-secondary: #656d76;         /* 次级文字（描述/辅助） */
--color-text-tertiary:  #8b949e;         /* 三级文字（占位/禁用） */
--color-text-inverse:   #ffffff;         /* 反色文字（按钮上） */
```

#### 1.2 深色模式

必须配套深色模式色板，与浅色模式一一对应：

```css
/* 深色模式 — 暗色背景 + 高对比文字 */
--color-bg:           #0d1117;
--color-bg-secondary: #161b22;
--color-bg-tertiary:  #21262d;
--color-border:       #30363d;
--color-border-light: #21262d;
--color-primary:      #3b82f6;
--color-primary-hover:#60a5fa;
--color-primary-light:#1a2332;
--color-text-primary:   #e6edf3;
--color-text-secondary: #8b949e;
--color-text-tertiary:  #6e7681;
```

#### 1.3 禁止规则

- ❌ 不使用高饱和纯色作为大面积背景（如 `#FF0000`、`#00FF00`）
- ❌ 不使用 3 种以上品牌色（最多 1 主色 + 1 辅色）
- ❌ 不使用纯黑 `#000000` 文字（用 `#1f2328` 代替）
- ❌ 不使用纯白 `#ffffff` 在深色模式中（用 `#e6edf3` 代替）
- ✅ 功能色（成功/警告/危险）仅在反馈场景使用，不作为 UI 装饰色

### R2: 排版规范（强制）

#### 2.1 字号层级

采用 4px 递增体系（符合 8px 网格）：

```css
--font-size-xs:     13px;   /* 辅助信息、标签、时间戳 */
--font-size-sm:     14px;   /* 次要文字、表格内容 */
--font-size-base:   15px;   /* 正文（默认） */
--font-size-lg:     17px;   /* 大号正文、导航 */
--font-size-xl:     20px;   /* 三级标题 */
--font-size-2xl:    24px;   /* 二级标题 */
--font-size-3xl:    30px;   /* 一级标题 */
--font-size-4xl:    36px;   /* 页面大标题 */
```

#### 2.2 字重

- `<strong>` / `<b>` — 600 (semibold)，不用 700 (bold)
- 标题: 600, 正文: 400, 辅助文字: 400
- 按钮文字: 500 (medium) / 600 (primary)

#### 2.3 行高

- 标题: `1.3` ~ `1.4`
- 正文: `1.6` ~ `1.7`
- 辅助文字: `1.4`
- 代码: `1.6`

#### 2.4 字体栈

```css
--font-sans:  -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Noto Sans', Helvetica, Arial, sans-serif;
--font-mono:  'SF Mono', 'Fira Code', 'Fira Mono', 'Roboto Mono', 'JetBrains Mono', 'Cascadia Code', monospace;
```

#### 2.5 禁止规则

- ❌ 不使用小于 13px 的文字
- ❌ 不使用 700+ 的粗体（600 semibold 为上限）
- ❌ 同一页面不超过 4 种字号
- ❌ 正文不使用斜体（_italic_）作为强调方式
- ❌ 不使用 `text-align: justify` 两端对齐（中文排版效果差）

### R3: 间距与留白（强制）

#### 3.1 8px 网格体系

所有间距、内边距、外边距必须是 8 的倍数（含 4px 特例）：

```css
--space-1:   4px;    /* 最小间距（图标与文字之间） */
--space-2:   8px;    /* 元素内间距（按钮内边距） */
--space-3:   12px;   /* 表格单元格内边距 */
--space-4:   16px;   /* 表单字段间距、卡片内边距 */
--space-5:   20px;   /* 标准间距 */
--space-6:   24px;   /* 列表项间距、段落间距 */
--space-8:   32px;   /* 区域间距、卡片组间距 */
--space-10:  40px;   /* 大段间距 */
--space-12:  48px;   /* 页面段间距 */
--space-16:  64px;   /* 页面大区块间距 */
```

#### 3.2 留白密度

| 页面类型 | 内容区左右留白 | 卡片内边距 | 列表项间距 |
|---------|--------------|-----------|-----------|
| 仪表盘/概览 | `--space-8` ~ `--space-12` | `--space-6` | `--space-4` |
| 列表/表格页 | `--space-6` ~ `--space-8` | `--space-5` | `--space-3` |
| 详情/表单页 | `--space-8` ~ `--space-10` | `--space-6` | `--space-5` |
| 登录/注册 | `--space-10` ~ `--space-12` | `--space-8` | `--space-6` |

#### 3.3 留白原则（必须遵守）

1. **呼吸感优先**: 元素之间宁可多留白也不要紧凑。有疑问时，增加 8px。
2. **分组留白**: 相关元素间距 `--space-2` ~ `--space-3`，无关元素组间距 `--space-6` ~ `--space-8`。
3. **内容区最大宽度**: 阅读类页面（详情、文章）限制内容区最大宽度为 720px~800px，避免行长过长。
4. **列表页左右留白**: 列表页/表格页内容区左右不得贴边，至少保留 `--space-6`。

#### 3.4 禁止规则

- ❌ 元素之间不留间距堆叠摆放
- ❌ 页面内容贴边（全屏表格/列表左右无留白）
- ❌ 按钮之间间距小于 8px
- ❌ 段落行高小于 1.5
- ❌ 同一层级元素间距不一致

### R4: 布局结构（强制）

#### 4.1 页面基础布局模板

```
┌──────────────────────────────────────────────┐
│  Navigation Bar (64px)                       │
│  Logo  NavLinks  Search           Avatar     │
├──────────────────────────────────────────────┤
│  Page Header                                  │
│  ← Back    标题       操作按钮组              │
│  面包屑/路径                                  │
├──────────────────────────────────────────────┤
│  Content Area                                 │  ← 左右 min(1200px, 100vw - 64px)
│  ┌─────────┐ ┌─────────┐ ┌─────────┐         │
│  │  Card   │ │  Card   │ │  Card   │         │
│  └─────────┘ └─────────┘ └─────────┘         │
│  ┌──────────────────────────────────┐         │
│  │  Table / List                    │         │
│  └──────────────────────────────────┘         │
├──────────────────────────────────────────────┤
│  Footer (optional, 48px)                      │
└──────────────────────────────────────────────┘
```

#### 4.2 页面布局规则

1. **页面标题区**: 顶部固定 48px~56px 高度的标题栏，包含页面名称和主要操作按钮
2. **面包屑导航**: 标题区下方可选面包屑，字号 `--font-size-sm`，色值 `--color-text-secondary`
3. **内容区最大宽度**: `max-width: 1200px`（宽屏适配）、`max-width: 800px`（阅读型页面）
4. **卡片网格**: `display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: var(--space-6);`
5. **侧边布局**: 左侧导航宽度 240px~280px，右侧内容区自适应

#### 4.3 导航栏规范

- 高度固定 64px（可自定义 56px~72px）
- 左侧 Logo + 产品名（字号 `--font-size-lg`，字重 600）
- 中间/右侧导航链接（字号 `--font-size-base`，字重 500）
- 激活态使用 `--color-primary` 或底部 2px 实线指示器
- 用户头像区域: 32px×32px 圆形头像

#### 4.4 禁止规则

- ❌ 页面内容宽度超过 1400px 不设限制（行长过长影响可读性）
- ❌ 导航栏高度超过 80px
- ❌ 卡片网格使用固定列数而非自动填充
- ❌ 缺少页面标题（每个页面必须有清晰标题）

### R5: Element Plus 组件样式覆盖（强制）

#### 5.1 全局覆盖原则

使用 CSS 变量覆盖 Element Plus 默认样式，**禁止**大量编写 `!important` 或深层次选择器覆盖。

```css
/* ✅ 正确：通过 CSS 变量统一覆盖 */
:root {
  --el-color-primary: #2563eb;
  --el-color-primary-light-3: #3b82f6;
  --el-color-primary-light-9: #eff6ff;
  --el-border-radius-base: 8px;
  --el-border-radius-small: 6px;
  --el-font-size-base: 15px;
  --el-font-size-small: 14px;
  --el-font-size-large: 17px;
}

/* ✅ 对特定组件做精细化覆盖 */
.el-button--primary {
  font-weight: 500;
  padding: 8px 20px;
  border-radius: 8px;
}

.el-card {
  border-radius: 10px;
  border: 1px solid var(--el-border-color-light);
  box-shadow: none;  /* SaaS 风格卡片不加阴影，或极轻微 shadow-sm */
}

.el-dialog {
  border-radius: 12px;
}
```

#### 5.2 按钮规范

| 按钮类型 | 样式 | 用途 |
|---------|------|------|
| Primary | 实心背景 `--color-primary` | 主要操作（每页最多 1 个） |
| Default | 白色背景 + 边框 | 次要操作 |
| Plain | 浅色背景 + 浅边框 | 弱操作（取消、返回） |
| Text/Link | 无边框 + 文字色 | 最低优先级操作 |

- 按钮高度: default 36px, small 28px, large 44px
- 按钮圆角: 8px（全部统一）
- 按钮组间距: `gap: 8px`（水平）、`gap: 12px`（垂直）

#### 5.3 表单规范

- 标签位于输入框上方（vertical layout），非左侧
- 输入框高度统一 36px，圆角 8px
- 输入框聚焦态使用 `--color-primary` 2px 实线，禁用默认 outline
- 错误提示位于输入框下方，字号 `--font-size-xs`，色值 `--color-danger`
- 表单字段间距: `--space-5`（20px）

#### 5.4 表格规范

- 表格移除斑马纹（2025 年趋势：纯色行更简洁）
- 行高: 48px~52px（提供充足触控区域）
- 表头背景: `--color-bg-secondary`，字重 600
- 排序/过滤图标使用 `--color-text-tertiary`
- 分页器置于表格下方，居中或右对齐
- 默认不显示表格边框（`border: false`），或仅水平线

```vue
<!-- ✅ SaaS 风格表格 -->
<el-table :data="list" stripe={false} border={false} style="width: 100%">
  <el-table-column prop="name" label="名称" min-width="160" />
  <el-table-column prop="status" label="状态" width="100" />
  <el-table-column prop="createdAt" label="创建时间" width="180" />
  <el-table-column label="操作" width="160" fixed="right">
    <template #default="{ row }">
      <el-button type="text" size="small" @click="handleEdit(row)">编辑</el-button>
    </template>
  </el-table-column>
</el-table>
```

#### 5.5 卡片规范

- 卡片圆角: 10px~12px
- 卡片边框: 1px solid `--el-border-color-light`
- 卡片阴影: `box-shadow: none`（不投射阴影）或用极轻微 `shadow-sm`
- 卡片内边距: `--space-5` ~ `--space-6`
- 卡片标题与内容之间: 24px 间距

#### 5.6 对话框（Dialog/Drawer）

- 对话框圆角: 12px
- 内边距: 24px
- 遮罩层背景: rgba(0, 0, 0, 0.4)（浅色模式）、rgba(0, 0, 0, 0.6)（深色模式）
- 标题字号: `--font-size-lg`（16px），字重 600
- 底部按钮区: 右对齐，"取消"在左，"确认"在右

#### 5.7 Tag / Badge

- Tag 圆角: 6px
- 内边距: 4px 8px
- 字号: 12px
- 语义色使用浅色背景 + 深色文字（如 `--color-primary-light` 背景 + `--color-primary` 文字）

#### 5.8 禁止规则

- ❌ 覆盖 Element Plus 组件时使用 `::v-deep` 深层穿透（用 CSS 变量替代）
- ❌ 表格显示斑马纹
- ❌ 卡片使用明显阴影（Elevation）
- ❌ 输入框聚焦态使用蓝色外发光（应使用 2px 实线边框）
- ❌ 对话框不使用圆角
- ❌ 同一页面出现 2 个以上的 Primary 按钮

### R6: 交互反馈（强制）

#### 6.1 微交互动效

所有交互反馈必须满足以下标准：

| 场景 | 动效 | 时长 | 缓动函数 |
|------|------|------|---------|
| 按钮 hover | 背景色微变 | 150ms | ease |
| 按钮点击 | 轻微缩放 (0.97) | 100ms | ease-out |
| 卡片 hover | 边框色微变 | 200ms | ease |
| 弹窗出现 | fade + translateY(-8px) | 200ms | ease-out |
| 弹窗消失 | fade + translateY(-4px) | 150ms | ease-in |
| 页面切换 | fade (无滑动) | 200ms | ease |
| Toast 出现 | slide-down + fade | 250ms | ease-out |
| 下拉展开 | fade + translateY(-4px) | 150ms | ease-out |

```css
/* ✅ 标准微动效定义 */
--transition-fast:   150ms ease;
--transition-base:   200ms ease;
--transition-slow:   300ms ease;

.button {
  transition: background-color var(--transition-fast),
              transform var(--transition-fast);
}
.button:active {
  transform: scale(0.97);
}

.card {
  transition: border-color var(--transition-base);
}
.card:hover {
  border-color: var(--color-primary);
}
```

#### 6.2 加载状态

- 页面初次加载: 使用骨架屏（Skeleton），禁止使用全屏 Spinner
- 局部刷新: 使用 `v-loading` 指令 + 局部遮罩
- 按钮提交: 按钮进入 loading 状态，禁止再次点击
- 列表加载更多: 底部显示加载指示器，不阻塞页面

#### 6.3 空状态

每个列表/数据展示组件必须设计空状态（Empty State）：

```
┌──────────────────────────────────┐
│                                  │
│          🗂️ (Illustration)       │
│                                  │
│    还没有任何消息                 │
│    开始你的第一次对话吧           │
│                                  │
│    ┌──────────────────────┐      │
│    │  发起对话             │      │
│    └──────────────────────┘      │
│                                  │
└──────────────────────────────────┘
```

- 空状态包含: 插图/图标 + 标题(16px) + 描述(14px, 次级色) + 操作按钮(可选)
- 禁止直接展示空白页面或仅文字 "暂无数据"

#### 6.4 错误/异常状态

- 网络错误: 显示友好提示 + "重试"按钮
- 权限不足: 显示 403 提示 + "返回首页"链接
- 404: 显示 404 插图 + "页面不存在" + "返回首页"
- 表单提交失败: 输入框高亮错误字段 + 全局顶部错误提示（ElMessage）

#### 6.5 反馈一致性

- 成功操作: `ElMessage.success('操作成功')` — 绿色浅提示，2 秒自动消失
- 失败操作: `ElMessage.error('失败原因')` — 红色浅提示，3 秒自动消失
- 确认操作: `ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' })`
- 通知: 重要通知使用 `ElNotification`（右上角弹出），非重要使用 `ElMessage`

#### 6.6 禁止规则

- ❌ 页面跳转使用滑动/翻页动画（用 fade 代替）
- ❌ 全局使用全屏 loading spinner
- ❌ 空状态仅显示文字 "暂无数据"
- ❌ 动效时长超过 400ms（显得拖沓）
- ❌ 表单提交成功/失败不做任何反馈

### R7: 响应式设计（强制）

#### 7.1 断点体系

采用 Element Plus 默认断点 + 一个超宽屏断点：

```scss
// 参考，不在代码中重复定义，直接使用 Element Plus 的 el-col 响应式
$breakpoints: (
  'xs':  < 768px,    // 手机
  'sm':  768px~,     // 平板竖屏
  'md':  992px~,     // 平板横屏/小屏笔记本
  'lg':  1200px~,    // 笔记本/桌面
  'xl':  1920px~,    // 宽屏桌面
);
```

#### 7.2 响应式要点

| 断点 | 布局变化 | 导航 | 表格/列表 |
|------|---------|------|-----------|
| `< 768px` | 单列布局 | 汉堡菜单 | 卡片列表（替换表格） |
| `768px ~ 991px` | 2 列网格 | 收起图标导航 | 表格水平滚动 |
| `992px ~ 1199px` | 2~3 列网格 | 完整导航 | 表格完整显示 |
| `1200px+` | 3~4 列网格 | 完整导航 | 表格完整显示 + 更多列 |

#### 7.3 响应式组件规则

1. **表格**: 768px 以下使用卡片列表代替表格，或使用 `el-table` 的 `max-height` + 水平滚动
2. **导航**: 768px 以下折叠为汉堡菜单（Drawer 形式侧滑）
3. **表单**: 768px 以下输入框占满整行，取消多列布局
4. **卡片网格**: 使用 `auto-fill` + `minmax` 自动适应
5. **对话框**: 768px 以下宽度占满屏幕（留 16px 边距），768px+ 使用固定宽度 520px~640px
6. **侧边栏**: 768px 以下自动隐藏，通过按钮触发

```vue
<!-- ✅ 响应式网格 -->
<el-row :gutter="[24, 24]">
  <el-col :xs="24" :sm="12" :lg="8" :xl="6">
    <stat-card />
  </el-col>
</el-row>

<!-- ✅ 响应式表格 -->
<el-table :data="list" :max-height="isMobile ? undefined : 600">
  <!-- 移动端隐藏非关键列 -->
  <el-table-column prop="name" label="名称" />
  <el-table-column prop="email" label="邮箱" :show-overflow-tooltip="true"
    :class-name="isMobile ? 'hidden-col' : ''" />
</el-table>
```

#### 7.4 禁止规则

- ❌ 桌面端内容区占满 100% 宽度无限制（必须有 `max-width`）
- ❌ 768px 以下仍然并排显示多列表格且不可滚动
- ❌ 移动端使用 hover 交互（hover 在触屏上不生效）
- ❌ 导航栏在移动端不折叠
- ❌ 不对 `320px` 最小宽度做适配

### R8: 深色模式（强制）

#### 8.1 实现方式

使用 CSS 自定义属性 + `prefers-color-scheme` 媒体查询 + 手动切换开关：

```css
/* 浅色模式（默认） */
:root {
  --color-bg: #ffffff;
  --color-text-primary: #1f2328;
  /* ... 其他变量 */
}

/* 深色模式 — 跟随系统 */
@media (prefers-color-scheme: dark) {
  :root {
    --color-bg: #0d1117;
    --color-text-primary: #e6edf3;
    /* ... 深色变量覆盖 */
  }
}

/* 深色模式 — 手动切换（class 方式） */
:root.dark {
  --color-bg: #0d1117;
  --color-text-primary: #e6edf3;
  /* ... 同上 */
}
```

#### 8.2 深色模式设计原则

- 不使用纯黑背景（用 `#0d1117`，`#161b22`）
- 文字色不使用纯白（用 `#e6edf3`）
- 卡片/容器的层级通过微妙背景色变化区分，而非阴影
- 图片/插图可能需要反相或替换为深色版本
- 边框色比背景色亮 10%~15%，确保可感知但不过分突出

#### 8.3 禁止规则

- ❌ 深色模式仅对背景取反，不对其他变量做处理
- ❌ 深色模式中图片/Logo 不可见（需替换为深色版资源）
- ❌ 深色模式中对比度过低（WCAG AA 标准：文字对比度 ≥ 4.5:1）

### R9: 动画与过渡（建议）

1. **页面切换**: 使用 Vue Router 的 `<transition name="fade">`，仅 fade 效果，不滑动
2. **列表动画**: 使用 `<TransitionGroup>` 实现列表项的进入/离开动画
3. **展开/收起**: 使用 `el-collapse-transition` 组件
4. **避免动画**:
   - ❌ 不需要循环动画（loading spinner 除外）
   - ❌ 不需要弹跳/弹性动画（不符合 SaaS 专业感）
   - ❌ 不需要视差滚动
   - ❌ 不需要粒子效果

### R10: 断言检查清单

在完成每个页面的 UI 设计或开发后，必须逐项检查以下清单：

**可读性检查**:
- [ ] 所有文字对比度 ≥ 4.5:1（WCAG AA）
- [ ] 正文最小字号 17px
- [ ] 行高不低于 1.6
- [ ] 没有文字溢出/截断问题
- [ ] 没有过多文字堆叠

**信息层级检查**:
- [ ] 页面标题清晰可见
- [ ] 内容按"标题→正文→辅助信息"三层级区分
- [ ] 操作按钮按主次排列
- [ ] 相关功能分组明确

**留白检查**:
- [ ] 页面内容不贴边
- [ ] 元素间距一致（8px 倍数）
- [ ] 卡片内边距充足
- [ ] 列表项间距均匀

**响应式检查**:
- [ ] 在 375px、768px、1200px、1920px 下分别预览
- [ ] 移动端导航折叠正常
- [ ] 表格在小屏下可水平滚动或切换为卡片布局
- [ ] 字体在小屏下没有缩小

**一致性检查**:
- [ ] 颜色使用符合色板定义（没有额外颜色）
- [ ] 按钮风格统一
- [ ] 圆角统一
- [ ] 表单布局统一（标签在上方）
- [ ] 空状态/错误状态均已覆盖

### R11: 全局字体可读性规范（强制）

> 全局禁止使用过小字体，避免影响 PC 端阅读体验。

#### 11.1 字号底线

| 场景 | 最小字号 | 说明 |
|------|---------|------|
| 页面标题 | `20px` | 页面名称、聊天对象名称等 |
| 主要正文、列表内容、表单内容 | `17px` | 用户信息、消息内容、列表项文字等 |
| 用户名、用户标识 | `17px` | 侧栏用户列表、消息发送者标识等 |
| 可交互文字（按钮、菜单、标签） | `15px` | 所有按钮文字、导航链接、标签文本 |
| 输入框文字 | `15px` | 输入框内的用户输入和占位提示 |
| 次要说明、辅助信息、状态提示 | `14px` | 时间戳、在线状态、提示文字、角标数字等 |

> **PC 端正文、列表内容、表单内容等主要信息文字不得小于 17px。**
>
> **禁止使用 13px 及以下字体作为主要交互文字、功能入口、按钮文字、导航文字或信息展示文字。**

#### 11.2 可读性原则

1. **布局优先，而非缩小字体** — 当页面内容展示不下时，优先调整间距、布局结构和信息层级，**禁止通过缩小字体解决布局拥挤问题**
2. **全局一致性** — 所有页面需保持统一字体层级，禁止局部区域随意降低字号导致视觉不一致
3. **留白替代缩字** — 需要容纳更多内容时，增加容器尺寸或重组内容结构，而非降低字号
4. **辅助信息降级** — 只有时间戳、计数角标、次要状态提示等真正的辅助信息才可使用 `13px`

#### 11.3 侧边栏规范

- **宽度**: `280px` ~ `320px`，确保信息展示空间充足
- **头像尺寸**: 用户列表头像统一 `28px` × `28px`，当前用户头像 `32px` × `32px`
- **列表项高度**: `44px` ~ `52px`，悬停背景色 `--color-bg-tertiary`
- **选中态**: 背景色 `--color-primary-light`，无需额外边框或指示器
- **标签栏**: 高度 `36px`，激活态下划线 2px

#### 11.4 消息与输入区规范

- **消息气泡文字**: `15px` ~ `17px`
- **气泡圆角**: `12px`，内边距 `12px 16px`
- **己方气泡**: 背景色 `--color-primary`，文字色 `--color-text-inverse`
- **对方气泡**: 背景色 `--color-bg-secondary`，文字色 `--color-text-primary`
- **输入框文字**: `15px` 以上，最小高度 `48px`
- **多行输入**: 支持 `Enter` 发送 + `Shift+Enter` 换行

#### 11.5 禁止规则

- ❌ 全局任何位置使用 `13px` 及以下字号作为主要交互文字、功能入口、按钮文字、导航文字或信息展示文字
- ❌ 通过缩小字体解决布局拥挤问题
- ❌ 同一页面局部区域字体明显小于其他区域（视觉不统一）
- ❌ 侧边栏宽度小于 `280px`
- ❌ 输入框高度小于 `44px`

## Implementation Steps

当为一个新页面或新功能实现 UI 时，按以下步骤执行：

1. **确定页面类型** → 列表/表格页、详情页、表单页、仪表盘 — 选择对应的布局模板（R4）
2. **设计信息层级** → 确定页面上最重要信息、次要信息、辅助操作，按视觉权重递减排列
3. **搭建布局骨架** → 使用 `el-row`/`el-col` 网格系统 + 8px 间距体系搭建框架
4. **填充组件内容** → 使用 Element Plus 组件，应用样式覆盖规则（R5）
5. **处理边界状态** → 加载态（骨架屏）、空状态、错误状态、边缘数据（R6）
6. **适配响应式** → 按断点调整布局、导航、表格展示方式（R7）
7. **应用深色模式** → 确保所有自定义颜色使用 CSS 变量，双端验证（R8）
8. **微调留白与排版** → 逐一检查间距、字号、行高是否符合规范
9. **断言检查** → 逐项执行 R10 检查清单

## Code Patterns

### Pattern 1 — 页面基础模板（空白页脚手架）

```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const data = ref<any[]>([])

async function fetchData() {
  loading.value = true
  try {
    // const res = await api.getList()
    // data.value = res
  } catch (e) {
    ElMessage.error('加载失败，请重试')
  } finally {
    loading.value = false
  }
}

onMounted(() => fetchData())
</script>

<template>
  <div class="page-container">
    <!-- 页面标题区 -->
    <div class="page-header">
      <div class="page-header__left">
        <h1 class="page-title">页面标题</h1>
        <p class="page-description">页面的简要描述或说明文字</p>
      </div>
      <div class="page-header__right">
        <el-button type="primary">主要操作</el-button>
        <el-button>次要操作</el-button>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="page-content">
      <el-card v-loading="loading">
        <template #header>
          <span>卡片标题</span>
        </template>
        <!-- 内容 -->
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.page-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--space-8);
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-6);
  margin-bottom: var(--space-8);
}

.page-header__left {
  flex: 1;
  min-width: 0;
}

.page-title {
  font-size: var(--font-size-3xl, 30px);
  font-weight: 600;
  line-height: 1.3;
  color: var(--color-text-primary);
  margin: 0 0 var(--space-2) 0;
}

.page-description {
  font-size: var(--font-size-base, 14px);
  line-height: 1.6;
  color: var(--color-text-secondary);
  margin: 0;
}

.page-header__right {
  display: flex;
  gap: var(--space-2);
  flex-shrink: 0;
}

.page-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}
</style>
```

### Pattern 2 — 空状态组件

```vue
<script setup lang="ts">
defineProps<{
  title?: string
  description?: string
  actionText?: string
  icon?: string
}>()

const emit = defineEmits<{
  (e: 'action'): void
}>()
</script>

<template>
  <div class="empty-state">
    <div class="empty-state__icon">{{ icon || '📭' }}</div>
    <h3 class="empty-state__title">{{ title || '暂无数据' }}</h3>
    <p class="empty-state__description">{{ description || '' }}</p>
    <el-button v-if="actionText" type="primary" @click="emit('action')">
      {{ actionText }}
    </el-button>
  </div>
</template>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-16) var(--space-6);
  text-align: center;
}

.empty-state__icon {
  font-size: 48px;
  line-height: 1;
  margin-bottom: var(--space-6);
}

.empty-state__title {
  font-size: var(--font-size-lg, 16px);
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 var(--space-2) 0;
}

.empty-state__description {
  font-size: var(--font-size-base, 14px);
  color: var(--color-text-secondary);
  line-height: 1.6;
  margin: 0 0 var(--space-6) 0;
  max-width: 360px;
}
</style>
```

### Pattern 3 — 骨架屏组件

```vue
<script setup lang="ts">
defineProps<{
  rows?: number
  type?: 'card' | 'table' | 'detail'
}>()
</script>

<template>
  <div class="skeleton-wrapper">
    <template v-if="type === 'card'">
      <el-skeleton :count="rows || 6" animated>
        <template #template>
          <div class="skeleton-card">
            <el-skeleton-item variant="image" style="width: 100%; height: 120px" />
            <div style="padding: 12px">
              <el-skeleton-item variant="h3" style="width: 60%; height: 16px" />
              <div style="margin-top: 8px">
                <el-skeleton-item variant="text" style="width: 90%" />
                <el-skeleton-item variant="text" style="width: 70%" />
              </div>
            </div>
          </div>
        </template>
      </el-skeleton>
    </template>
    <template v-else>
      <el-skeleton :rows="rows || 5" animated />
    </template>
  </div>
</template>

<style scoped>
.skeleton-wrapper {
  width: 100%;
}
.skeleton-card {
  border: 1px solid var(--color-border-light);
  border-radius: 10px;
  overflow: hidden;
}
</style>
```

### Pattern 4 — CSS 变量统一声明文件

```css
/* src/styles/design-tokens.css */

/* ============ 色彩 ============ */
:root {
  --color-bg: #ffffff;
  --color-bg-secondary: #f6f8fa;
  --color-bg-tertiary: #eef1f5;
  --color-border: #d0d7de;
  --color-border-light: #e8ecf0;
  --color-primary: #2563eb;
  --color-primary-hover: #1d4ed8;
  --color-primary-light: #eff6ff;
  --color-success: #059669;
  --color-warning: #d97706;
  --color-danger: #dc2626;
  --color-info: #6b7280;
  --color-text-primary: #1f2328;
  --color-text-secondary: #656d76;
  --color-text-tertiary: #8b949e;
  --color-text-inverse: #ffffff;
}

/* ============ 排版 ============ */
:root {
  --font-sans: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Noto Sans', Helvetica, Arial, sans-serif;
  --font-mono: 'SF Mono', 'Fira Code', 'Fira Mono', 'Roboto Mono', monospace;
  --font-size-xs: 13px;
  --font-size-sm: 14px;
  --font-size-base: 15px;
  --font-size-lg: 17px;
  --font-size-xl: 20px;
  --font-size-2xl: 24px;
  --font-size-3xl: 30px;
  --font-size-4xl: 36px;
}

/* ============ 间距 ============ */
:root {
  --space-1: 4px;
  --space-2: 8px;
  --space-3: 12px;
  --space-4: 16px;
  --space-5: 20px;
  --space-6: 24px;
  --space-8: 32px;
  --space-10: 40px;
  --space-12: 48px;
  --space-16: 64px;
}

/* ============ 过渡 ============ */
:root {
  --transition-fast: 150ms ease;
  --transition-base: 200ms ease;
  --transition-slow: 300ms ease;
}

/* ============ Element Plus 覆盖 ============ */
:root {
  --el-color-primary: #2563eb;
  --el-color-primary-light-3: #3b82f6;
  --el-color-primary-light-9: #eff6ff;
  --el-border-radius-base: 8px;
  --el-border-radius-small: 6px;
  --el-font-size-base: 15px;
  --el-font-size-small: 14px;
  --el-font-size-large: 17px;
}
```

### Pattern 5 — 响应式工具 composable

```ts
// src/composables/use-responsive.ts
import { ref, onMounted, onUnmounted } from 'vue'

const breakpoints = {
  xs: 768,
  sm: 992,
  md: 1200,
  lg: 1920,
} as const

export function useResponsive() {
  const width = ref(window.innerWidth)

  const isMobile = ref(width.value < breakpoints.xs)
  const isTablet = ref(width.value >= breakpoints.xs && width.value < breakpoints.md)
  const isDesktop = ref(width.value >= breakpoints.md)

  function onResize() {
    width.value = window.innerWidth
    isMobile.value = width.value < breakpoints.xs
    isTablet.value = width.value >= breakpoints.xs && width.value < breakpoints.md
    isDesktop.value = width.value >= breakpoints.md
  }

  onMounted(() => window.addEventListener('resize', onResize))
  onUnmounted(() => window.removeEventListener('resize', onResize))

  return { width, isMobile, isTablet, isDesktop }
}
```

## Edge Cases

1. **超长文本溢出**: 表格列中文字过长时，使用 `:show-overflow-tooltip="true"` 或 `text-overflow: ellipsis`，而非让内容换行撑高行高。标题超出省略保留前 30 个字符。
2. **中英文混排**: 中英文之间加空格（CSS 层面无法自动处理，需代码规范约束：「中文 English 中文」），数字与单位之间不加空格（如 `16px`、`8px`）。
3. **长数字/URL 不换行**: 使用 `word-break: break-all` 或 `overflow-wrap: break-word` 防止长串数字或 URL 溢出容器。
4. **浏览器兼容性**: CSS 变量在 IE11 不支持。本项目目标现代浏览器（Chrome/Firefox/Safari/Edge 最新 2 个版本），无需兼容 IE。
5. **字体回退**: 中文字体在不同操作系统表现差异大（Windows 默认微软雅黑，macOS 默认苹方）。必须提供完整的 font-family 回退栈。
6. **打印样式**: 如果页面需要打印（如订单详情），添加 `@media print` 样式：移除导航、背景色、阴影，确保文字为黑色。
7. **页面加载 FOUC**: 使用 `<link rel="preload">` 或 `<script>` 在 DOM 解析前加载关键 CSS，避免样式闪烁。
8. **缩放失真**: 页面最小宽度 320px，最大宽度不限。使用 `rem` 或 `em` 确保用户缩放浏览器时布局不崩。
9. **弹窗嵌套**: 一个页面上最多允许一个弹窗（Dialog），禁止弹窗上再弹窗。确需多层级时使用 Drawer 代替。
10. **超宽屏适配 (>2560px)**: 内容区 `max-width: 1400px`，两侧留白均匀放大。避免文字行过长影响阅读。

## Output Format Rules

当生成 UI 设计相关的代码或文档时：

1. **设计稿生成** — 输出格式顺序：色彩方案 → 布局草图 → 组件选型 → 交互说明 → 响应式断点适配说明
2. **Vue 组件生成** — 必须包含完整的 `<script setup>` + `<template>` + `<style scoped>`，且 style 中必须引用 CSS 变量而非硬编码色值/间距值
3. **样式覆盖** — 标注覆盖的是哪个 Element Plus 组件，说明为什么要覆盖（如 `// 覆盖 ElCard 圆角以符合 SaaS 设计规范`）
4. **响应式说明** — 标注每个断点下的具体布局变化（如 `// <768px: 表格转为卡片列表`）
5. **深色模式适配** — 深色模式下的设计必须写明对比度满足 WCAG AA 标准

## Example

### 场景：设计一个「用户列表」页面

需求：展示系统所有用户，支持搜索和分页，点击可查看详情。

**设计分析**:

| 维度 | 决策 | 依据 |
|------|------|------|
| 页面类型 | 列表/表格页 | 需展示多条结构一致的数据 |
| 信息层级 | 标题(用户管理) > 搜索区 > 表格 > 分页 | 从上到下，从全局到具体 |
| 主要操作 | "新增用户"按钮 — 页面右上角 Primary | 每页最多 1 个 Primary |
| 次要操作 | 每行的"编辑/删除" — Text 类型按钮 | 不干扰数据阅读 |
| 色彩 | 白色背景 + 浅灰表头 + 蓝主色 | 标准 SaaS 清单页配色 |
| 响应式 | 768px 以下隐藏邮箱列，表格支持横向滚动 | 保证移动端可用 |

**页面结构**:

```
┌──────────────────────────────────────────────────┐
│  用户管理          [搜索框]   [+ 新增用户]        │  ← page-header
│  共 128 位用户                                   │
├──────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────────┐│
│  │ 用户名 │ 昵称 │ 邮箱 │ 状态 │ 创建时间 │ 操作 ││  ← el-table (borderless)
│  ├──────────────────────────────────────────────┤│     stripe=false
│  │ zhangsan │ 张三 │ z***@m.com │ ✅ │ 7/10 │ 编辑 ││
│  │ lisi     │ 李四 │ l***@m.com │ ✅ │ 7/09 │ 编辑 ││
│  └──────────────────────────────────────────────┘│
│                                                  │
│  [< 1  2  3  ...  11 >]  10条/页  共106条        │  ← el-pagination
└──────────────────────────────────────────────────┘
```

**实现**:

```vue
<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useResponsive } from '@/composables/use-responsive'
import EmptyState from '@/components/common/EmptyState.vue'
import SkeletonLoader from '@/components/common/SkeletonLoader.vue'

interface UserItem {
  id: number
  username: string
  nickname: string
  email: string
  status: number
  createdAt: string
}

const { isMobile } = useResponsive()
const loading = ref(false)
const list = ref<UserItem[]>([])
const total = ref(0)
const query = reactive({ page: 1, pageSize: 10, keyword: '' })

async function fetchData() {
  loading.value = true
  try {
    // const res = await getUserPage(query)
    // list.value = res.records
    // total.value = res.total
  } catch {
    ElMessage.error('加载失败，请重试')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  fetchData()
}

function handleCreate() {
  // 打开新增对话框
}

function handleEdit(row: UserItem) {
  // 打开编辑对话框
}

onMounted(() => fetchData())
</script>

<template>
  <div class="page-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <div>
        <h1 class="page-title">用户管理</h1>
        <p class="page-description">共 {{ total }} 位用户</p>
      </div>
      <div class="page-header__actions">
        <el-input
          v-model="query.keyword"
          placeholder="搜索用户名或昵称"
          clearable
          style="width: 240px"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleCreate">
          + 新增用户
        </el-button>
      </div>
    </div>

    <!-- 加载态 -->
    <SkeletonLoader v-if="loading && list.length === 0" type="table" />

    <!-- 空状态 -->
    <EmptyState
      v-else-if="!loading && list.length === 0"
      title="暂无用户"
      description="还没有创建任何用户，点击下方按钮开始添加"
      action-text="新增用户"
      @action="handleCreate"
    />

    <!-- 表格 -->
    <el-card v-else>
      <el-table
        :data="list"
        v-loading="loading"
        stripe={false}
        border={false}
        style="width: 100%"
        :max-height="isMobile ? undefined : 600"
      >
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column
          prop="email"
          label="邮箱"
          min-width="180"
          :class-name="isMobile ? 'hidden-col' : ''"
          show-overflow-tooltip
        />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="text" size="small" @click="handleEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @change="fetchData"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.page-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--space-8);
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-6);
  margin-bottom: var(--space-8);
  flex-wrap: wrap;
}

.page-title {
  font-size: var(--font-size-3xl, 30px);
  font-weight: 600;
  line-height: 1.3;
  color: var(--color-text-primary);
  margin: 0 0 var(--space-1) 0;
}

.page-description {
  font-size: var(--font-size-base, 14px);
  color: var(--color-text-secondary);
  margin: 0;
}

.page-header__actions {
  display: flex;
  gap: var(--space-3);
  align-items: center;
  flex-wrap: wrap;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding-top: var(--space-5);
}

/* 移动端隐藏列 */
:deep(.hidden-col) {
  display: none;
}

@media (max-width: 768px) {
  .page-container {
    padding: var(--space-5);
  }

  .page-header {
    flex-direction: column;
  }

  .page-header__actions {
    width: 100%;
  }

  .page-header__actions .el-input {
    width: 100% !important;
  }

  .page-title {
    font-size: var(--font-size-2xl, 24px);
  }
}
</style>
```
