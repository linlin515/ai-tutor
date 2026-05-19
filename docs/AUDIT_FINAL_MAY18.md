# CEO 终审：AI 学伴剩余迭代清单

> 日期：2026-05-18
> 状态：v2.4 全部完成，以下为剩余事项

---

## 🔴 P0 — 上线前必须（2 项）

| # | 事项 | 说明 |
|:---|:---|:-----|
| 1 | **生产签名** | 当前用 debug.keystore 自签。上线必须换成正式 keystore + 配置 GitHub Secrets (KEYSTORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD) |
| 2 | **CI/CD Secrets** | GitHub Actions workflow 缺少 Secret 配置，push 时 workflow 会失败 |

---

## 🟡 P1 — 重要优化（4 项）

| # | 事项 | 说明 |
|:---|:---|:-----|
| 3 | **后端 CI/CD** | 后端有 8 个测试文件 + Dockerfile，但没有 GitHub Actions workflow |
| 4 | **后端测试运行** | 后端测试是否通过未验证 |
| 5 | **ProGuard 验证** | 290 行规则，但未在 release 中实际验证混淆后 APK 功能正常 |
| 6 | **2 个 TODO** | VoiceRepositoryImpl:81（本地 ASR 置信度）、PhotoPreviewDialog:205（保存到相册） |

---

## ⚪ P2 — 后续迭代（3 项）

| # | 事项 | 说明 |
|:---|:---|:-----|
| 7 | **Firebase 集成** | 无 google-services.json，Firebase Crashlytics/Analytics 不可用 |
| 8 | **SettingsViewModel 测试** | 5 个 @Disabled 测试需要 AndroidX Hilt 环境才能跑 |
| 9 | **后端代码文档** | backend/ 目录无 README、无 API 文档 |

---

## ✅ 已验证安全的项

| 维度 | 状态 |
|:---|:----:|
| 网络安全配置 | ✅ network_security_config.xml 存在 |
| HTTPS | ✅ Let's Encrypt SSL |
| API 密钥 | ✅ 无硬编码密钥/URL |
| @JvmSuppressWildcards | ✅ ApiResponse 已加 |
| Gson 版本 | ✅ 2.11.0 |
| 版本号 | ✅ 2.4.0 |
| Monorepo 结构 | ✅ android/ backend/ docs/ |
| README | ✅ 完善 |

---

## 路线图对比

| 阶段 | 计划 | 实际 | 差距 |
|:---:|:---|:---|:---|
| v2.1 | P0-1/2/3 | ✅ 完成 | — |
| v2.2 | P1-1/2/3 | ✅ 完成 | — |
| v2.3 | P1-4 + P2-1 | ✅ 完成 | CI/CD Secrets 未配 |
| v2.4 | P2-2~P2-6 | ✅ 完成 | — |
| v3.0 | 上线前置 | — | P0-1 签名 + P0-2 CI/CD Secrets |

总计剩余 9 项，其中 P0 必须项 2 项，P1 优化项 4 项，P2 后续项 3 项。
