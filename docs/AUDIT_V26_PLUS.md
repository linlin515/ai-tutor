# CEO 审计报告：AI 学伴项目剩余迭代清单（v2.6+）

> 日期：2026-05-18
> 状态：v2.4 ~ v2.5.1 已完成，以下为下一步迭代

---

## ✅ 已完成迭代

| 版本 | 内容 | 状态 |
|:---:|:---|:---:|
| v2.4 | 骨架屏+深色动画+列表分页+国际化+Deprecation | ✅ 0531f59 |
| v2.5 | 消息长按复制/收藏/AI反馈👍👎/收藏夹/学习提醒推送 | ✅ 4947cf9 |
| v2.5.1 | base_url 域名→IP:port（测试兼容） | ✅ ab52b11 |
| v2.6 | signing 配置支持环境变量 | ✅ 本地未提交 |

---

## 🔴 P0 — 上线阻塞（4 项）

| # | 事项 | 说明 | 建议工时 |
|:---|:---|:-----|:---:|
| 1 | **GitHub Secrets 未配置** | CI workflow 引用 3 个 Secret（KEYSTORE_PASSWORD/ALIAS/PASSWORD），仓库未设置 | 0.5h |
| 2 | **生产 keystore** | 仍用 debug.keystore 自签，不能用于发布 | 0.5h |
| 3 | **版本号未更新** | version.properties 仍是 2.4.0，含 v2.5 + v2.5.1 改动 | 0.1h |
| 4 | **后端测试失败** | 8 个测试文件，pytest 报 "No tests collected" — 缺 async pytest 配置 | 1天 |

---

## 🟡 P1 — 重要优化（4 项）

| # | 事项 | 说明 | 建议工时 |
|:---|:---|:-----|:---:|
| 5 | **错题本 UI** | WrongAnswerRepository 已存在，缺独立页面入口 | 1.5天 |
| 6 | **后端 CI/CD** | Dockerfile 已有，缺 GitHub Actions workflow | 0.5天 |
| 7 | **ProGuard 验证** | 290 行规则，release APK 未冒烟测试混淆后功能 | 1天 |
| 8 | **仪表盘分页** | Dashboard 数据当用户量大时需分页 | 1天 |

---

## ⚪ P2 — 锦上添花（4 项）

| # | 事项 | 说明 | 建议工时 |
|:---|:---|:-----|:---:|
| 9 | **无障碍** | contentDescription/semantics 未评估 | 0.5天 |
| 10 | **Firebase Crashlytics** | 缺 google-services.json，线上崩溃盲区 | 1天 |
| 11 | **SettingsViewModel 测试** | 5 个 @Disabled 待 AndroidX Test | 0.5天 |
| 12 | **学习报告推送** | 已有 ReportExportScreen，可加定时报告推送 | 1天 |

---

## 📊 项目健康卡

| 指标 | 值 | 评分 |
|:---|:---|:---:|
| 源文件 | 238 .kt | 🟢 |
| API 接口数 | 未统计 | 🟡 |
| 测试文件 | 14（175 tests） | 🟡 |
| 后端文件 | 46 .py | 🟢 |
| 后端测试 | 8 文件，无法收集 ⚠️ | 🔴 |
| 编译状态 | ✅ BUILD SUCCESSFUL | 🟢 |
| Deprecation 警告 | 0 | 🟢 |
| TODOs | 2（VoiceRepository + PhotoPreview） | 🟢 |
| 版本号 | 2.4.0（应 2.5.0） | 🟡 |

---

## 建议优先级

> **第一优先** 🔴：版本号 + GitHub Secrets + 后端测试修复（~1.5天）
> **第二优先** 🟡：错题本 UI + 后端 CI/CD + ProGuard 验证（~3天）
> **第三优先** ⚪：无障碍 + Firebase + 测试补全（~2天）
