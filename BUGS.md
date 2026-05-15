# Bugs Status

生成时间: 2026-05-15 14:42

## ✅ 已验证修复

| 编号 | 描述 | 状态 |
|------|------|------|
| BUG-001 | SettingsViewModel.kt 缺失 | ✅ 已修复 — 文件存在于 `app/.../ui/settings/SettingsViewModel.kt`，HiltViewModel 实现完整 |
| BUG-002 | ConversationListSheet/VM 缺失 | ✅ 已修复 — 文件存在于 `app/.../ui/conversation/ConversationListSheet.kt` 和 `ConversationViewModel.kt`，实现完整 |
| BUG-005 | SSE trySend 可能丢数据 | ✅ 已处理 — `ChatStreamApi.kt` 使用 `.buffer(Channel.BUFFERED)` 无界缓冲区，消除了背压导致 trySend 失败的场景 |
| BUG-006 | 流式消息逻辑缺陷/协程泄漏 | ✅ 已修复 — `ChatViewModel.kt` 每次发消息前 `streamJob?.cancel()`，`onCleared()` 也取消 job，单协程收集消除竞争 |
| BUG-007 | camera surfaceProvider 未定义 | ✅ 已修复 — `CameraScreen.kt` 中 `p.setSurfaceProvider(this@apply.surfaceProvider)` 正确引用 PreviewView 的 surfaceProvider |
| BUG-008 | 麦克风权限未运行时申请 | ✅ 已修复 — `ChatScreen.kt` 使用 `rememberLauncherForActivityResult` 运行时申请 RECORD_AUDIO 权限 |
| BUG-009 | Domain层依赖Android Intent | ✅ 已修复 — `VoiceRepository` 接口和 `StartListeningUseCase` 均无 Android 依赖；`VoiceRepositoryImpl` (data层) 持有 Context 是合理的 |

## 🔧 新修复（本次自查发现）

| 编号 | 描述 | 文件 | 修复 |
|------|------|------|------|
| BUG-010 | override 函数不允许默认参数值 | `AuthRepositoryImpl.kt:47` | 删除 `register()` 中 `email: String? = null` 的默认值（接口层已有默认值） |

## 🏗 编译状态

- `./gradlew assembleDebug` — **BUILD SUCCESSFUL** ✅
