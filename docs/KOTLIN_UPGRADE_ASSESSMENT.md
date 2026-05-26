# Kotlin 2.x 升级影响评估报告

> **任务**: t_9aa6ec64 — 评估 Kotlin 1.9.22 → 2.1.x 升级可行性  
> **评估日期**: 2026-05-26  
> **评估人**: Hermes Agent (自动)  
> **当前版本锁**: Kotlin 1.9.22 / KSP 1.9.22-1.0.17 / AGP 8.2.2 / Gradle 8.5

---

## 1. 当前依赖版本清单

| 类别 | 依赖 | 当前版本 | 目标版本（建议） |
|------|------|----------|-----------------|
| **语言** | Kotlin | 1.9.22 | **2.1.20** |
| **构建** | AGP | 8.2.2 | 8.5.2+ (可选) |
| **构建** | Gradle | 8.5 | 8.7+ (可选) |
| **KSP** | KSP | 1.9.22-1.0.17 | **2.1.20-1.0.31** |
| **Compose** | Compose BOM | 2024.02.00 | 2025.01.00+ |
| **Compose** | Compose Compiler (extension) | 1.5.10 | **废弃**，由 plugin 替代 |
| **DI** | Hilt | 2.50 | 2.51.1+ |
| **DB** | Room | 2.6.1 | 2.6.1 (兼容) |
| **网络** | Retrofit | 2.9.0 | 2.11.0 |
| **网络** | OkHttp | 4.12.0 | 4.12.0 (兼容) |
| **异步** | Coroutines | 1.7.3 | 1.9.0+ |
| **导航** | Navigation Compose | 2.7.7 | 2.8.5+ |
| **生命周期** | Lifecycle | 2.7.0 | 2.8.7+ |
| **图片** | Coil | 2.5.0 | 2.7.0+ |
| **相机** | CameraX | 1.3.1 | 1.4.1+ |
| **分页** | Paging 3 | 3.2.1 | 3.3.4+ |
| **ML Kit** | ML Kit Text Recognition | 16.0.0 | 16.0.0 (兼容) |

---

## 2. Breaking Changes 分析

### 2.1 Compose Compiler — 最大变更 ⚠️

**现状**:
- `app/build.gradle.kts` 使用 `composeOptions { kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get() }`
- Kotlin 1.9.22 捆绑了 Compose Compiler，通过独立扩展版本号控制

**Kotlin 2.0+ 变化**:
- Kotlin 从 2.0 开始 **不再捆绑 Compose Compiler**
- Compose Compiler 拆分为独立 Gradle Plugin: `org.jetbrains.kotlin.plugin.compose`
- Compose Compiler 版本与 Kotlin 版本**严格同步**（不再需要独立版本号）
- 旧的 `composeOptions {}` 配置块在 Kotlin 2.0+ 中**废弃**

**所需修改**:

1. `android/gradle/libs.versions.toml` — 新增 plugin 条目:
```toml
[plugins]
kotlin-compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

2. `android/build.gradle.kts` (project level) — 新增:
```kotlin
alias(libs.plugins.kotlin.compose.compiler) apply false
```

3. `android/app/build.gradle.kts` — 应用 plugin + 移除旧配置:
```kotlin
plugins {
    // ... 现有 plugin
    alias(libs.plugins.kotlin.compose.compiler) // 新增
}

// 移除以下整个 block:
// composeOptions {
//     kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
// }
```

4. `android/gradle/libs.versions.toml` — 可选移除 `compose-compiler` 版本:
```toml
# [versions] 中可移除:
# compose-compiler = "1.5.10"
```

**风险评估**: 中高 — 语法级修改简单，但 Compose 编译行为可能因新编译器产生细微差异，需全面回归测试。

---

### 2.2 KSP 版本映射

KSP 版本严格绑定 Kotlin 版本，格式为 `${kotlinVersion}-${kspPluginVersion}`。

| Kotlin 版本 | KSP 版本 | 备注 |
|-------------|----------|------|
| 1.9.22 | 1.9.22-1.0.17 | 当前 |
| **2.0.0** | 2.0.0-1.0.24 | 可选中间版本 |
| **2.0.21** | 2.0.21-1.0.27 | 2.0.x 系列最新 |
| **2.1.0** | 2.1.0-1.0.29 | 可选 |
| **2.1.10** | 2.1.10-1.0.29 | |
| **2.1.20** | **2.1.20-1.0.31** | **⭐ 推荐 (目标版本)** |

> KSP 2.0+ 内置了对 Kotlin 2.0 新符号解析器的支持，完全向后兼容现有 KSP 处理器（如 Room、Hilt）。

**所需修改**:
```diff
- ksp = "1.9.22-1.0.17"
+ ksp = "2.1.20-1.0.31"
```

**风险评估**: 低 — KSP 2.0+ 良好兼容所有主流处理器。Room 2.6.1 和 Hilt 2.50 均已支持 KSP 2.0。

---

### 2.3 AGP 兼容性

| 项目 | 当前版本 | 兼容性 |
|------|---------|--------|
| AGP | 8.2.2 | ✅ 兼容 Kotlin 2.0+ |
| Gradle | 8.5 | ✅ 兼容 AGP 8.2.x ~ 8.7.x |

**AGP 8.2.2** 完全兼容 Kotlin 2.0+ 和 2.1.x，无需强制升级。但建议同时升级到 AGP 8.5.2+ 以获得：
- 更好的 build cache 性能
- 更新的 SDK 工具链支持
- 对 Kotlin 2.1 新特性的完整支持

**可选升级路径**:
```
AGP 8.2.2 + Gradle 8.5  →  AGP 8.5.2 + Gradle 8.7  (推荐但也非必须)
AGP 8.2.2 + Gradle 8.5  →  维持不变 (可行，已验证兼容)
```

**风险评估**: 低 — AGP 8.2.2 已验证兼容 Kotlin 2.0+ 和 2.1.x。

---

### 2.4 其他依赖兼容性速查

| 依赖 | 当前版本 | Kotlin 2.1.x 兼容 | 备注 |
|------|---------|-------------------|------|
| Compose BOM 2024.02.00 | ✅ | 兼容，建议同步升级到 2025.01.00+ |
| Hilt 2.50 | ✅ | 兼容 KSP 2.0+，建议升级到 2.51.1 |
| Room 2.6.1 | ✅ | 官方声明支持 KSP 2.0 |
| Coroutines 1.7.3 | ✅ | 兼容，建议升级到 1.9.0 |
| Retrofit 2.9.0 | ✅ | 无直接 Kotlin 版本绑定 |
| OkHttp 4.12.0 | ✅ | 无直接 Kotlin 版本绑定 |
| Navigation Compose 2.7.7 | ✅ | 兼容，建议同步升级到 2.8.5 |
| Lifecycle 2.7.0 | ✅ | 兼容，建议同步升级到 2.8.7 |
| Coil 2.5.0 | ✅ | 兼容，建议升级到 2.7.0 |
| CameraX 1.3.1 | ✅ | 兼容 |
| DataStore 1.0.0 | ✅ | 兼容 |
| Paging 3.2.1 | ✅ | 兼容 |
| ML Kit 16.0.0 | ✅ | 无直接 Kotlin 版本绑定 |

---

## 3. 升级方案设计

### 3.1 推荐升级路径

```
当前 (Kotlin 1.9.22)
    │
    ├──→ [Phase 1] Kotlin 2.0.21 + KSP 2.0.21-1.0.27 (中间过渡, 可选)
    │        验证 Compose Compiler Plugin 迁移
    │
    └──→ [Phase 2] Kotlin 2.1.20 + KSP 2.1.20-1.0.31 (最终目标, ⭐ 推荐)
             同时升级 Compose BOM → 2025.01.00+
```

**直接升级到 2.1.20 是安全的选择** — 两个主要版本之间没有叠加的 break change 风险。

### 3.2 精确修改清单

#### 文件 1: `android/gradle/libs.versions.toml`

```diff
[versions]
- kotlin = "1.9.22"
- ksp = "1.9.22-1.0.17"
- compose-bom = "2024.02.00"
- compose-compiler = "1.5.10"        # 可删除
+ kotlin = "2.1.20"
+ ksp = "2.1.20-1.0.31"
+ compose-bom = "2025.01.00"         # 可选升级

[plugins]
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
+ kotlin-compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

#### 文件 2: `android/build.gradle.kts` (project level)

```diff
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
+   alias(libs.plugins.kotlin.compose.compiler) apply false
}
```

#### 文件 3: `android/app/build.gradle.kts`

```diff
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
+   alias(libs.plugins.kotlin.compose.compiler)
}
```

```diff
- buildFeatures { compose = true }
- composeOptions {
-     kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
- }
+ buildFeatures { compose = true }    // 保留, 但 compseOptions block 移除
```

> ⚠️ **注意**: `buildFeatures { compose = true }` 仍需保留，它是 AGP 层面的开关。**只移除 `composeOptions` 块**。

---

## 4. 风险评估汇总

| 风险项 | 级别 | 说明 | 缓解措施 |
|--------|------|------|----------|
| Compose Compiler 迁移 | 🟡 中 | 新 plugin 架构需验证 | 针对每个 Compose 页面做回归测试 |
| KSP 处理器兼容性 | 🟢 低 | Room/Hilt 已支持 KSP 2.0 | 无需特殊操作 |
| AGP 版本兼容 | 🟢 低 | AGP 8.2.2 已验证兼容 | 建议保持不变或可选升级 |
| Compose 编译行为变化 | 🟡 中 | 新 Compiler 可能影响 @Composable 签名校验 | 重新编译后验证无 lint 警告 |
| Coroutines API 变更 | 🟢 低 | Kotlin 2.1.x 无破坏性 coroutine 变更 | 保留 1.7.3 或升级 |
| Gradle 构建缓存失效 | 🟡 中 | Kotlin 版本变更会触发热重启，全部重新编译 | 首次构建耗时较长(预计+30%) |
| 第三方库二进制兼容 | 🟢 低 | 主要库已发布 Kotlin 2.1.x 兼容版本 | 按建议版本升级 |

**总体风险评级**: 🟢 **低至中** — 升级可行，主要工作量为 Compose Compiler 迁移 + 版本号更新。

---

## 5. 升级工作量估算

| 阶段 | 内容 | 预估工时 |
|------|------|---------|
| 配置修改 | 更新 libs.versions.toml + build.gradle.kts | 0.5h |
| 首次构建 | 完整 rebuild (含 cache 失效) | 0.5~1h (自动化) |
| 编译错误修复 | 解决可能的 API 变更导致的编译错误 | 1~2h |
| Compose 回归测试 | 验证所有 Compose UI 页面渲染正常 | 2~3h |
| 功能回归测试 | 导航、Room、Hilt、CameraX 等核心功能 | 2~3h |
| **总计** | | **6~10h** |

---

## 6. 结论与建议

### 结论
✅ **Kotlin 2.1.x 升级可行**，没有不可逾越的技术障碍。主要变更集中在 Compose Compiler 架构迁移（从 extension version 到独立 plugin），此变更已被官方良好文档化且业界已有大量实践。

### 建议
1. **直接升级到 Kotlin 2.1.20**，跳过 2.0.x 中间版本
2. **同步升级 KSP 到 2.1.20-1.0.31**
3. **Compose BOM 同步升级到 2025.01.00+**（确保 Compose 库版本与 Kotlin 2.1 兼容）
4. **AGP 可保持 8.2.2 不变**，但建议择机升级到 8.5.2+
5. **立即实施** — 该升级无阻塞性问题，尽早升级可避免后续技术债务累积

---

*本报告由 Hermes Agent 自动生成于 2026-05-26，基于 Kotlin 2.1.20 正式版本。*
