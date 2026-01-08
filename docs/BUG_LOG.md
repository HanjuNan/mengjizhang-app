# 萌记账 App - Bug 记录

## Bug 记录格式
```
### Bug #编号: 简短描述
- **发现时间**: YYYY-MM-DD
- **状态**: 待修复 / 已修复
- **严重程度**: 高 / 中 / 低

#### 问题描述
详细描述问题现象

#### 原因分析
分析 Bug 产生的原因

#### 解决方案
具体的修复方法

#### 相关文件
涉及的文件路径
```

---

## Bug 列表

### Bug #001: compileSdk 语法错误
- **发现时间**: 2026-01-06
- **状态**: 已修复
- **严重程度**: 高

#### 问题描述
`app/build.gradle.kts` 中的 compileSdk 配置语法不正确：
```kotlin
compileSdk {
    version = release(36)
}
```

#### 原因分析
这是不正确的 Kotlin DSL 语法，`compileSdk` 应该直接赋值整数。

#### 解决方案
修改为正确语法：
```kotlin
compileSdk = 35
```

#### 相关文件
- `app/build.gradle.kts`

---

### Bug #002: Gradle 下载超时/网络错误
- **发现时间**: 2026-01-06
- **状态**: 已修复
- **严重程度**: 中

#### 问题描述
构建时出现错误：
```
Could not get resource 'https://services.gradle.org/distributions/gradle-8.13-src.zip'
```

#### 原因分析
从国外 Gradle 服务器下载资源失败，这是网络访问问题，在中国大陆访问国外服务器时经常发生。

#### 解决方案
在 `settings.gradle.kts` 中添加阿里云镜像仓库：
```kotlin
pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        // ... 其他配置
    }
}
```

#### 相关文件
- `settings.gradle.kts`

---

### Bug #003: Gradle 缓存损坏
- **发现时间**: 2026-01-06
- **状态**: 已修复
- **严重程度**: 中

#### 问题描述
构建时出现错误：
```
Could not read workspace metadata from C:\Users\86159\.gradle\caches\8.13\transforms\xxx\metadata.bin
```

#### 原因分析
Gradle 8.13 的 transforms 缓存文件损坏，可能是由于之前构建中断或网络问题导致。

#### 解决方案
1. 关闭 Android Studio
2. 删除缓存目录：`C:\Users\86159\.gradle\caches\8.13\transforms`
3. 重新打开项目并执行 Build → Clean Project → Rebuild Project

#### 相关文件
- Gradle 缓存目录

---

### Bug #004: Material Icons 引用错误
- **发现时间**: 2026-01-06
- **状态**: 已修复
- **严重程度**: 高

#### 问题描述
编译时报错：
```
Unresolved reference 'Target'
```

#### 原因分析
`Icons.Default.Target` / `Icons.Filled.Target` 不存在于 Material Icons Extended 库中。

#### 解决方案
将 `Target` 替换为 `Flag`：
```kotlin
// 修改前
import androidx.compose.material.icons.filled.Target
MenuItem(Icons.Default.Target, "预算设置")

// 修改后
import androidx.compose.material.icons.filled.Flag
MenuItem(Icons.Default.Flag, "预算设置")
```

#### 相关文件
- `ui/screens/profile/ProfileScreen.kt`

---
