# CEO 终审：剩余迭代清单

> 日期：2026-05-19  

---

## ✅ 已完成迭代

| 版本 | 内容 | 提交 |
|:---:|:---|:---:|
| v2.4-v2.8 | 体验打磨/核心功能/错题本/后端CI/signing | 67b02db → f77e021 |

---

## 🔴 P0 — 上线阻塞（1 项唯一）

| # | 事项 | 说明 |
|:---:|:---|:----|
| 1 | **APK 大小为 0！** | `ls -lh` 无输出，APK 未正常生成或损坏 |
| 2 | **GitHub Secrets 空** | CI workflow 引用 secrets 但仓库没配（需要用户手动在 GitHub Settings 配置） |

---

## 🟡 P1（3 项）

| # | 事项 |
|:---:|:---|
| 3 | **生产 keystore 缺失** — 仍用 debug.keystore + 硬编码密码 |
| 4 | **minSdk 过低** — 26（Android 8）无法使用系统级后台提醒 |
| 5 | **2 个 TODOs 未清理** — VoiceRepository / PhotoPreviewDialog |

---

## ⚪ P2（3 项）

| # | 事项 |
|:---:|:---|
| 6 | Firebase Crashlytics |
| 7 | 仪表盘分页 |
| 8 | SettingsViewModel 5 个 @Disabled 测试 |

---

## ⚠️ 最重要的问题：APK 大小

上次终端输出 `ls -lh` 无结果（空管道），需立即编译新 APK 确认。