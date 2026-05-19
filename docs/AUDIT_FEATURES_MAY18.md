# CEO 功能审计：App 功能优化方向

> 日期：2026-05-18
> 基于：233 源文件完整遍历

---

## 已有功能盘点

| 功能 | UI Screen | ViewModel | Repository | 完成度 |
|:---|:---:|:---:|:---:|:---:|
| 📷 拍照解题 | CameraScreen | ✅ | ✅ | 🟢 |
| 💬 AI 对话 | ChatScreen | ✅ | ✅ | 🟢 |
| 📊 学习仪表盘 | DashboardScreen | ✅ | ✅ | 🟢 |
| 📝 互动测验 | QuizScreen | ✅ | ✅ | 🟢 |
| 🔁 间隔复习 | ReviewScreen | ✅ | ✅ | 🟢 |
| 💳 订阅管理 | SubscriptionScreen | ✅ | ✅ | 🟢 |
| 📄 学习报告导出 | ReportExportScreen | ✅ | ✅ | 🟢 |
| ⚙️ 设置 | SettingsScreen | ✅ | ✅ | 🟢 |
| 🧭 新手引导 | OnboardingScreen | ✅ | — | 🟢 |
| 👤 个人资料 | ProfileScreen | ✅ | ✅ | 🟢 |

---

## 🔴 缺少的用户功能（P0-P1）

### 1. 消息长按操作
- 问题：聊天消息无法长按复制、分享、收藏
- 影响：用户看到好的解答无法保存
- 方案：ChatMessageBubble 添加 long-press → 弹出菜单（复制/收藏/分享/反馈）

### 2. AI 回复反馈（👍👎）
- 问题：用户无法对 AI 回答质量评分
- 影响：无法收集反馈来优化模型
- 方案：每个 AI 消息底部加 👍👎 按钮

### 3. 消息收藏夹
- 问题：重要解答无法标记收藏
- 影响：用户需要反复翻找历史
- 方案：收藏按钮 + "我的收藏"列表页

### 4. 学习提醒推送
- 问题：NotificationChannels 已创建但没有任何提醒调度
- 影响：用户容易忘记学习
- 方案：SettingsScreen 添加"每日学习提醒"开关 + WorkManager 定时推送

---

## 🟡 已有但体验欠佳的功能（P1-P2）

### 5. 对话搜索 UI
- 已有：`searchMessagesByKeyword` (Repository)
- 缺少：ChatScreen 上搜索入口 / 搜索结果高亮
- 方案：ChatScreen 顶栏加 🔍 搜索图标

### 6. 错题本入口
- 已有：`WrongAnswerRepository` / `getDueReviews`
- 缺少：Dashboard 上错题本入口 / 独立错题本页面
- 方案：Dashboard 卡片 → WrongAnswerScreen

### 7. 每日学习目标
- 已有：StreakCalculator + GamificationEngine
- 缺少：用户自定义目标（每天解 N 题、学 M 分钟）
- 方案：SettingsScreen 添加目标设定

### 8. 成就/仪表盘分享
- 已有：完整成就系统、学习报告
- 缺少：一键分享成就卡片到微信 / 保存图片
- 方案：DashboardScreen 添加"分享学习成果"按钮

---

## ⚪ 体验细节（P2）

| # | 事项 | 说明 |
|:---|:---|:---|
| 9 | 语音练习模式 | 当前 TTS/ASR 只在聊天中用，无独立口语练习 |
| 10 | 多科目快捷切换 | Subject 已传递但 UI 上无切换入口 |
| 11 | 离线内容缓存 | NetworkBanner 已存在，但断网后上次对话不可查看 |

---

## 优先级建议

| 优先级 | 事项 | 预估工时 |
|:---:|:---|:---:|
| 🔴 P0 | 消息长按操作 | 1 天 |
| 🔴 P0 | AI 回复反馈 | 0.5 天 |
| 🟡 P1 | 消息收藏夹 | 1.5 天 |
| 🟡 P1 | 学习提醒推送 | 1 天 |
| 🟡 P1 | 对话搜索 UI | 0.5 天 |
| 🟡 P1 | 错题本入口 | 1 天 |
| ⚪ P2 | 每日学习目标 | 1 天 |
| ⚪ P2 | 成就分享 | 0.5 天 |
| ⚪ P2 | 语音练习 | 2 天 |

**总计**: ~9.5 人日，建议分两轮迭代。
