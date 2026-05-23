# v2.9 P0 阻塞修复规划

**日期**: 2026-05-23
**状态**: 规划完成，等待分配 Coder

---

## 现状诊断

### APK 状态
| 路径 | 大小 | 类型 |
|------|------|------|
| `ai-tutor-android/app/build/outputs/apk/debug/app-debug.apk` | 58.1 MB | Debug（无签名） |
| `/var/www/html/ai-tutor.apk` | 58.2 MB | 部署版（疑似 Debug） |
| `/var/www/html/aitutor-app-release.apk` | 49.2 MB | 旧 Release |

**结论**: 生产部署 APK（ai-tutor.apk 58.2 MB）与 Debug APK 大小接近，极可能是未签名的 Debug 构建被错误部署。正确 Release APK 应为 ~49 MB 且有签名。

### CI Workflow
- `android-ci.yml`: 引用 3 个 Secrets (`KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`)，构建路径 `android/`，产出 `app-release.apk`
- `backend-ci.yml`: 仅跑 pytest，无问题

### Secrets 状态
`KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD` 需在 GitHub 仓库 Settings → Secrets and variables → Actions 中手动配置。当前大概率缺失，导致 CI 无法完成签名构建。

---

## P0 阻塞修复计划

### 任务 1: 重新编译 Release APK（签字）
**负责人**: Coder
**步骤**:
1. 在本地运行 `cd android && ./gradlew assembleRelease`（需本地 keystore）
2. 验证输出 APK 路径: `android/app/build/outputs/apk/release/app-release.apk`
3. 检查 APK 大小（预期 ~49 MB，签名后略大）
4. 部署到 `/var/www/html/ai-tutor.apk`

### 任务 2: GitHub Secrets 配置
**负责人**: CEO / 用户（需手动操作）
**步骤**:
1. 前往 GitHub 仓库 Settings → Secrets and variables → Actions
2. 添加 Repository secrets:
   - `KEYSTORE_PASSWORD`
   - `KEY_ALIAS`
   - `KEY_PASSWORD`
3. 推送代码触发 CI 验证签名构建通过

---

## 依赖
- 本地 keystore 文件（`android/app/keystore.jks` 或类似）必须存在，否则需先生成
- 如无 keystore，需先执行: `keytool -genkey ...`（生成自签名证书）

---

## 下一步
**CEO 指令**: 分配 Coder 执行任务 1（APK 重新编译），CEO 手动执行任务 2（配置 Secrets）。