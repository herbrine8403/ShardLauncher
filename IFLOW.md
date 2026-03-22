# ShardLauncher Legacy 项目文档

## 项目概述

ShardLauncher Legacy 是一款专为 Android 设备设计的现代化 Minecraft Java 版启动器，是原版 ShardLauncher 的 Fork 版本。基于 **Jetpack Compose** 和 **Material Design 3** 构建，采用 **ZalithLauncher2** 启动核心，旨在提供极致的视觉体验和流畅的操作感受。

### 核心功能
- **多种登录方式**: 支持微软账号 (OAuth 2.0) 和离线登录
- **游戏管理**: 自动下载和管理 Minecraft 游戏文件，包括客户端、资源和库
- **高度可定制的 UI**: 深色模式、多种主题颜色、自定义背景、动画速度、侧边栏位置等
- **集成资源系统**: Java 运行时和渲染器库文件集成在 APK 中，支持零网络依赖
- **版本管理**: 支持多个 Minecraft 版本的管理和切换，含版本配置、Mod 管理、资源包管理等
- **音乐播放器**: 内置音乐播放功能，支持本地音乐文件管理和播放
- **开发者选项**: 提供日志查看、组件演示等开发调试工具
- **崩溃处理**: 完善的崩溃报告和恢复机制
- **XAML 组件系统**: 支持 XAML 解析和自定义组件渲染
- **游戏视图**: 专用 VMActivity 用于游戏画面渲染和输入处理

### 技术栈
- **语言**: Kotlin
- **UI 框架**: Jetpack Compose
- **设计语言**: Material Design 3
- **构建工具**: Gradle (Kotlin DSL)
- **本地代码**: C/C++ (JNI) 用于游戏桥接和系统调用
- **数据库**: Room
- **网络**: Retrofit, OkHttp, Ktor (Server & Client)
- **多媒体**: ExoPlayer (media3)
- **序列化**: Kotlinx Serialization

### 主要依赖库
- `androidx.navigation:navigation-compose` - 页面导航
- `io.coil-kt:coil-compose` - 图片加载
- `io.coil-kt:coil-video` - 视频加载
- `com.squareup.retrofit2:retrofit` - 网络请求
- `androidx.media3:media3-exoplayer` - 视频背景播放
- `androidx.media3:media3-ui` - 媒体 UI
- `androidx.media3:media3-session` - 媒体会话管理
- `com.google.android.material:material` - Material Design 组件
- `com.bytedance:bytehook` - JNI Hook
- `dev.chrisbanes.haze:haze` - 模糊效果
- `dev.chrisbanes.haze:haze-materials` - 模糊效果 Material 组件
- `androidx.room:room-*` - 本地数据库
- `org.apache.maven:maven-artifact` - 版本比较
- `io.ktor:ktor-*` - Ktor Server (本地皮肤服务器) & Client
- `org.jetbrains.kotlinx:kotlinx-serialization-json` - JSON 序列化

## 构建与运行

### 环境要求
- Android Studio (推荐最新稳定版)
- Android SDK (API 36)
- JDK 11
- NDK 25.2.9519653

### 构建步骤

1. **克隆项目仓库**
   ```bash
   git clone https://github.com/herbrine8403/ShardLauncher-Legacy.git
   cd ShardLauncher
   ```

2. **初始化子模块** (ZalithLauncher2)
   ```bash
   git submodule update --init --recursive
   ```

3. **配置 Microsoft Client ID (可选)**
   - 在项目根目录创建 `local.properties` 文件
   - 添加以下内容（用于微软登录）:
     ```properties
     MICROSOFT_CLIENT_ID=your_client_id_here
     ```
   - 或者通过环境变量设置: `MICROSOFT_CLIENT_ID`

4. **在 Android Studio 中打开项目**
   - 等待 Gradle 同步完成

5. **构建项目**
   ```bash
   # Debug 构建
   ./gradlew assembleDebug

   # Release 构建
   ./gradlew assembleRelease

   # 带 Release 标记的构建
   ./gradlew assembleRelease -PisReleaseBuild
   ```

6. **运行应用**
   - 连接 Android 设备或启动模拟器
   - 点击 Android Studio 中的 "Run" 按钮或使用快捷键 `Shift + F10`
   - 或使用命令行:
     ```bash
     ./gradlew :ShardLauncher:installDebug
     ```

### 多 ABI 构建
项目支持多架构构建，会生成以下 APK:
- `armeabi-v7a` - 32位 ARM
- `arm64-v8a` - 64位 ARM (主流设备)
- `x86` - 32位 x86
- `x86_64` - 64位 x86
- `universal` - 通用版 (包含所有架构)

### 清理构建
```bash
./gradlew clean
```

## 开发约定

### 项目模块化
项目分为以下主要模块：
- **`ShardLauncher`**: UI 层模块，包含所有界面和应用逻辑
- **`SL-GameCore`**: 游戏核心模块，包含 JNI 代码、游戏启动逻辑和基础工具类
- **`NG-GL4ES`**: 渲染器模块 (libng_gl4es.so)
- **`LayerController`**: 图层控制器模块 (从 ZalithLauncher2 引入)
- **`third_party`**: 第三方参考项目和库
  - `ZalithLauncher2`: 启动核心 (子模块)
  - `FoldCraftLauncher`: 参考项目

### UI 开发
- **声明式 UI**: 使用 Jetpack Compose 进行所有 UI 开发
- **组件化**: 
  - 通用 UI 组件位于 `ShardLauncher/ui/components/`
    - `basic/`: 基础组件
    - `business/`: 业务组件
    - `color/`: 颜色选择器
    - `dialog/`: 对话框
    - `effect/`: 视觉效果
    - `filemanager/`: 文件管理器
    - `layout/`: 布局组件
    - `tiles/`: 瓦片组件
  - 界面特定组件位于各自的 `ui/<screen>/` 目录
- **Activity**: 游戏视图使用独立的 `VMActivity` 进行渲染
- **主题系统**: 使用 Material Design 3 主题系统，支持深色模式和多种主题颜色
- **动画**: 使用 Compose 动画 API，支持全局动画速度自定义
- **XAML 系统**: 支持 XAML 解析和自定义组件渲染 (`ui/xaml/`)

### 状态管理
- **ViewModel**: 每个主要界面应有对应的 `ViewModel` 管理逻辑和状态
- **State**: 使用 Compose 的 `State` 和 `remember` 管理 UI 局部状态
- **SideEffect**: 使用 `LaunchedEffect` 处理副作用
- **Flow**: 使用 Kotlin Flow 进行数据流管理

### 导航
- **单 Activity 架构**: 所有页面都是 Composable (主界面)
- **双 Activity**: 主界面 + VMActivity (游戏视图)
- **导航定义**: 导航路由和逻辑定义在 `ShardLauncher/ui/navigation/Navigation.kt`
- **深链接支持**: 支持 `shardlauncher://auth/microsoft` 用于微软登录回调

### 数据持久化
- **设置存储**: 使用 `SharedPreferences` (通过 `SettingsRepository` 封装)
- **数据库**: 使用 Room 数据库，实体定义在 `ShardLauncher/database/`
- **音乐列表**: 音乐播放列表持久化存储

### 本地代码 (JNI)
- **JNI 代码**: 位于 `SL-GameCore/src/main/jni/`
- **构建系统**: 使用 NDK-Build (`Android.mk`)
- **桥接层**: `SL-GameCore/bridge/` 负责 Kotlin 与 Native 的交互
- **核心模块**:
  - `pojavexec`: 游戏执行核心
  - `exithook`: 退出钩子
  - `driver_helper`: 驱动辅助
  - `linkerhook`: 链接器钩子
  - `pojavexec_awt`: AWT 桥接

### 资源管理
- **集成资源**: Java 运行时集成在 `ShardLauncher/src/main/assets/runtimes/`
- **组件提取**: 使用 `AssetExtractor` 从 APK assets 提取资源
- **渲染器**: 渲染器库文件位于 `jniLibs/`
- **XAML 资源**: XAML 布局文件位于 `assets/` 目录

### 服务
- **MusicPlayerService**: 前台音乐播放服务，使用 Media3 ExoPlayer
- **服务类型**: `mediaPlayback` (Android 14+)

### 日志记录
- **Logger**: 使用 `com.lanrhyme.shardlauncher.gamecore.utils.Logger`
- **级别**: `i` (信息), `d` (调试), `e` (错误), `w` (警告)
- **日志查看**: 开发者选项中提供日志查看器

### 代码风格
- **Kotlin**: 遵循 Kotlin 官方代码风格
- **注解处理**: 使用 KSP (Kotlin Symbol Processing)
- **序列化**: 使用 Kotlinx Serialization
- **Parcelize**: 使用 `kotlin-parcelize` 插件

### UI 设计风格

ShardLauncher 采用现代化的 Material Design 3 设计语言，结合丰富的视觉效果和动画，打造沉浸式的用户体验。

#### 设计原则
- **沉浸式体验**: 全屏无边框设计，隐藏 system 状态栏和导航栏
- **高度可定制**: 支持用户自定义主题、颜色、背景、动画速度等
- **视觉层次**: 通过模糊 (Haze)、阴影、发光效果增强视觉层次感
- **流畅动画**: 所有交互都有平滑的动画过渡

#### 主题系统
基于 Material Design 3 的主题系统，支持多种预设主题和自定义主题：

**预设主题** (ThemeColor 枚举):
- **草碎影** (Green): 绿色系主题，清新自然
- **蓝璃梦** (Blue): 蓝色系主题，科技感强
- **紫晶泪** (Purple): 紫色系主题，优雅神秘
- **黄粱残** (Golden): 金色系主题，温暖明亮
- **动态** (Dynamic): Android 12+ 动态取色，跟随系统壁纸
- **自定义** (Custom): 用户自定义主题颜色

#### 视觉效果

**1. 毛玻璃/模糊效果**
- 使用 `dev.chrisbanes.haze:haze` 库实现
- 卡片背景支持模糊效果（Android 12+）
- 通过 `LocalCardLayoutConfig` 全局配置

**2. 发光效果**
- 自定义 `Modifier.glow()` 扩展函数
- 用于按钮、图标等交互元素

**3. 背景光效**
- `BackgroundLightEffect` 组件实现动态背景光效

**4. 背景支持**
- 支持静态图片和视频背景 (ExoPlayer)
- 支持视差效果 (Parallax)

#### 形状和圆角
统一 16.dp

### Git Commit 格式
项目遵循 Conventional Commits 规范，并使用 Emoji 来增强可读性。提交消息格式如下：

```
<type>(<scope>): <emoji><description>
```

#### 提交类型 (type)
- `feat`: 新功能
- `fix`: 修复 bug
- `style`: 代码格式、样式或 UI 优化（不影响功能）
- `refactor`: 重构代码
- `docs`: 文档更新
- `test`: 测试相关
- `chore`: 构建、工具或依赖更新

#### 作用域 (scope)
- `game`: 游戏核心相关
- `ui`: 用户界面相关
- `components`: 可复用组件
- `setting`: 设置相关
- `versions`: 版本管理
- `account`: 账户管理
- `bg`: 背景相关
- `color`: 配色相关
- `haze`: 模糊效果相关
- `music`: 音乐播放器相关
- `xaml`: XAML 组件相关
- `crash`: 崩溃处理相关

#### Emoji 使用
- ✨️: 新功能 (feat)
- 🪲️: 修复 bug (fix)
- 📚️: 样式/优化 (style)
- ♻️: 重构 (refactor)
- 📝: 文档 (docs)
- ✅: 测试 (test)
- 🔧: 构建/工具 (chore)

#### 示例
```
feat(game): ✨️游戏核心-游戏启动流程运行
fix(bg): 🪲️修复自定义背景预览效果和实际效果不符的问题
style(ui): 📚️主页的一些ui优化
feat(components): ✨️增加ScrollIndicator滚动导航指示条可复用组件
feat(music): ✨️添加音乐播放器功能
```

## 项目结构

```
ShardLauncher/
├── ShardLauncher/                    # 主应用/UI 模块
│   ├── src/main/
│   │   ├── java/com/lanrhyme/shardlauncher/
│   │   │   ├── MainActivity.kt       # 应用主入口
│   │   │   ├── ShardLauncherApp.kt   # Application 类
│   │   │   ├── api/                  # 网络 API
│   │   │   ├── common/               # 公共工具类
│   │   │   ├── coroutine/            # 协程工具
│   │   │   ├── data/                 # 数据仓库 (Repository)
│   │   │   │   ├── MusicRepository.kt # 音乐数据仓库
│   │   │   │   └── SettingsRepository.kt # 设置数据仓库
│   │   │   ├── database/             # Room 数据库
│   │   │   ├── game/                 # 游戏相关逻辑
│   │   │   │   ├── account/          # 账户管理
│   │   │   │   ├── addons/           # 附加组件
│   │   │   │   ├── auth_server/      # 认证服务器
│   │   │   │   ├── download/         # 下载管理
│   │   │   │   ├── input/            # 输入处理
│   │   │   │   ├── launch/           # 游戏启动核心
│   │   │   │   ├── microsoft/        # 微软登录
│   │   │   │   ├── mod/              # Mod 管理
│   │   │   │   ├── offline/          # 离线模式
│   │   │   │   ├── path/             # 路径管理
│   │   │   │   ├── resource/         # 资源管理
│   │   │   │   ├── version/         # 版本管理
│   │   │   │   ├── versioninfo/      # 版本信息
│   │   │   │   └── wardrobe/         # 皮肤管理
│   │   │   ├── info/                 # 信息常量
│   │   │   ├── model/                # 数据模型
│   │   │   ├── service/              # 服务
│   │   │   │   └── MusicPlayerService.kt # 音乐播放服务
│   │   │   ├── settings/             # 设置管理
│   │   │   ├── tasks/                # 后台任务
│   │   │   ├── ui/                   # UI 层 (Screen, ViewModel, Component)
│   │   │   │   ├── account/          # 账户管理界面
│   │   │   │   ├── activities/       # Activity
│   │   │   │   │   └── VMActivity.kt # 游戏视图 Activity
│   │   │   │   ├── common/           # 通用 UI 工具
│   │   │   │   ├── components/       # 通用 UI 组件
│   │   │   │   │   ├── basic/        # 基础组件
│   │   │   │   │   ├── business/     # 业务组件
│   │   │   │   │   ├── color/        # 颜色选择器
│   │   │   │   │   ├── dialog/       # 对话框
│   │   │   │   │   ├── effect/       # 视觉效果
│   │   │   │   │   ├── filemanager/  # 文件管理器
│   │   │   │   │   ├── layout/       # 布局组件
│   │   │   │   │   └── tiles/        # 瓦片组件
│   │   │   │   ├── crash/            # 崩溃处理界面
│   │   │   │   ├── developeroptions/ # 开发者选项
│   │   │   │   │   ├── ComponentDemoScreen.kt
│   │   │   │   │   ├── DeveloperOptionsScreen.kt
│   │   │   │   │   └── LogViewerScreen.kt
│   │   │   │   ├── downloads/        # 下载管理界面
│   │   │   │   │   ├── DownloadScreen.kt
│   │   │   │   │   └── VersionDetailScreen.kt
│   │   │   │   ├── home/             # 主页
│   │   │   │   │   ├── HomeScreen.kt
│   │   │   │   │   └── VersionSelector.kt
│   │   │   │   ├── music/            # 音乐播放器界面
│   │   │   │   ├── navigation/       # 导航逻辑
│   │   │   │   ├── notification/     # 通知系统
│   │   │   │   ├── settings/         # 设置界面
│   │   │   │   │   ├── SettingsScreen.kt
│   │   │   │   │   ├── AboutScreen.kt
│   │   │   │   │   ├── RendererManageScreen.kt
│   │   │   │   │   └── RuntimeManageScreen.kt
│   │   │   │   ├── splash/           # 启动画面
│   │   │   │   ├── theme/            # 主题定义
│   │   │   │   ├── version/          # 游戏版本管理界面
│   │   │   │   │   ├── list/          # 版本列表
│   │   │   │   │   ├── detail/        # 版本详情
│   │   │   │   │   ├── config/        # 版本配置
│   │   │   │   │   └── management/    # 版本管理
│   │   │   │   │       ├── ModsManagementScreen.kt
│   │   │   │   │       ├── ResourcePacksManagementScreen.kt
│   │   │   │   │       ├── SavesManagementScreen.kt
│   │   │   │   │       └── ShaderPacksManagementScreen.kt
│   │   │   │   └── xaml/             # XAML 解析器
│   │   │   │       ├── XamlParser.kt
│   │   │   │       ├── XamlComponents.kt
│   │   │   │       └── XamlEvents.kt
│   │   │   ├── utils/                # UI 工具类
│   │   │   └── viewmodel/            # 通用 ViewModel
│   │   │       ├── EventViewModel.kt
│   │   │       └── ErrorViewModel.kt
│   │   ├── assets/                   # 资源 (JRE, 外部组件, XAML)
│   │   │   ├── components/           # 自定义组件
│   │   │   ├── home.xaml            # 主页 XAML 布局
│   │   │   └── runtimes/            # Java 运行时
│   │   ├── jniLibs/                  # 本地库文件
│   │   │   ├── arm64-v8a/
│   │   │   ├── armeabi-v7a/
│   │   │   ├── x86/
│   │   │   └── x86_64/
│   │   └── res/                      # Android 资源
│   └── build.gradle.kts
├── SL-GameCore/                      # 游戏核心模块
│   ├── src/main/
│   │   ├── java/com/lanrhyme/shardlauncher/
│   │   │   ├── bridge/               # Kotlin/Native 桥接
│   │   │   ├── game/                 # 游戏启动与管理核心
│   │   │   │   ├── input/            # 输入处理
│   │   │   │   ├── keycodes/          # 键码映射
│   │   │   │   ├── launch/           # 启动逻辑
│   │   │   │   ├── multirt/          # 多运行时
│   │   │   │   ├── plugin/           # 插件支持
│   │   │   │   └── renderer/         # 渲染器管理
│   │   │   ├── path/                 # 路径管理
│   │   │   └── utils/                # 核心工具类 (Logger, File, Network)
│   │   └── jni/                      # C/C++ 本地代码
│   │       ├── Android.mk            # NDK 构建配置
│   │       ├── Application.mk        # 应用配置
│   │       ├── awt_bridge.c          # AWT 桥接
│   │       ├── bigcoreaffinity.c     # 大核亲和性
│   │       ├── egl_bridge.c          # EGL 桥接
│   │       ├── exit_hook.c           # 退出钩子
│   │       ├── input_bridge_v3.c     # 输入桥接 v3
│   │       ├── java_exec_hooks.c     # Java 执行钩子
│   │       ├── jre_launcher.c        # JRE 启动器
│   │       ├── lwjgl_dlopen_hook.c   # LWJGL 动态加载钩子
│   │       ├── stdio_is.c/h          # 标准输入输出
│   │       ├── utils.c/h             # 工具函数
│   │       ├── awt_xawt/             # AWT XAWT
│   │       ├── ctxbridges/           # 上下文桥接
│   │       ├── driver_helper/        # 驱动辅助
│   │       ├── environ/              # 环境变量
│   │       ├── GL/                   # OpenGL 相关
│   │       ├── linkerhook/           # 链接器钩子
│   │       └── logger/               # 日志记录
│   └── build.gradle.kts
├── NG-GL4ES/                         # 渲染器模块 (libng_gl4es.so)
├── third_party/                      # 第三方依赖/参考
│   ├── ZalithLauncher2/              # ZalithLauncher2 启动核心 (子模块)
│   │   ├── ColorPicker/              # 颜色选择器模块
│   │   ├── LayerController/          # 图层控制器
│   │   ├── LWJGL/                    # LWJGL 库
│   │   ├── NG-GL4ES/                 # GL4ES OpenGL 转 OpenGL ES
│   │   ├── Terracotta/               # Terracotta UI 组件
│   │   └── ZalithLauncher/           # ZalithLauncher 主模块
│   └── FoldCraftLauncher/            # FoldCraftLauncher 参考项目
├── gradle/                           # Gradle 配置
├── build.gradle.kts                  # 根构建脚本
└── settings.gradle.kts               # 项目设置
```

## 关键技术点

### 游戏启动流程
1. 用户选择 Minecraft 版本
2. 检查并下载必要的游戏文件（客户端、库、资源）
3. 检查 Java 运行时是否可用
4. 初始化渲染器（VirGL、OSMesa、Zink）
5. 通过 JNI 启动 Java 进程
6. 桥接输入、图形和系统调用

### 游戏视图 (VMActivity)
- 独立的 Activity 用于游戏画面渲染
- 支持 TextureView 进行视频渲染
- 完整的输入事件处理（键盘、鼠标、触摸）
- 屏幕旋转和配置变化处理
- 与主应用分离的渲染生命周期

### 资源集成系统
- Java 运行时（JRE 8, 17, 21）集成在 APK assets 中
- 使用 `AssetExtractor` 从 APK 提取资源到应用数据目录
- 支持设备架构自动检测和兼容性检查
- 支持手动导入外部 tar.xz 运行时文件

### 渲染器支持
- **VirGL**: 虚拟化 OpenGL，用于大多数设备
- **OSMesa**: 软件渲染，用于兼容性
- **Zink**: OpenGL over Vulkan（实验性）
- **NG-GL4ES**: 独立的渲染器模块

### 输入桥接
- 触摸事件转换为鼠标/键盘事件
- 支持虚拟摇杆和按键映射
- 通过 JNI 传递到 Java 进程
- 使用 `input_bridge_v3.c` 实现

### 账户系统
- 微软账号登录（OAuth 2.0）
- 离线模式支持
- 账户信息本地存储
- 深链接支持登录回调

### 版本管理
- 版本列表展示和选择
- 版本详情查看
- 版本配置文件编辑
- Mod 管理
- 资源包管理
- 存档管理
- Shader 包管理

### 主题系统
- 支持深色/浅色模式切换
- 多种预设主题颜色（绿色、蓝色、紫色等）
- 自定义主题颜色
- 动态主题切换（Android 12+）

### 音乐播放器
- 基于 ExoPlayer 的音乐播放服务
- 支持本地音乐文件扫描和管理
- 支持播放列表管理
- 支持重复模式切换
- 支持搜索和过滤
- 使用 Media3 Session API

### 开发者选项
- **日志查看器**: 实时查看应用日志
- **组件演示**: 测试 UI 组件效果
- **调试工具**: 各种开发调试功能

### XAML 组件系统
- 支持 XAML 格式的布局文件解析
- 自定义组件渲染
- 事件处理系统
- 可扩展的组件架构

### 崩溃处理
- 全局异常捕获
- 崩溃信息收集
- 崩溃界面展示
- 日志记录和报告

### 性能优化
- 使用 Coil 异步图片加载
- ExoPlayer 视频背景播放
- 懒加载和虚拟滚动
- JNI Hook 优化性能
- 大核亲和性优化 (`bigcoreaffinity.c`)

### 网络服务
- **Ktor Server**: 本地皮肤服务器
- **Ktor Client**: 网络请求
- **Retrofit**: API 请求
- **OkHttp**: HTTP 客户端

## 测试

### 运行测试
```bash
# 单元测试
./gradlew test

# Android 仪器测试
./gradlew connectedAndroidTest
```

### 调试
- 使用 Android Studio 的调试器
- 查看 Logcat 日志
- 使用 `Logger` 类记录自定义日志
- 使用开发者选项中的日志查看器

## 常见问题

### 构建失败
- 确保 JDK 11 已安装并配置正确
- 检查 Android SDK 和 NDK 版本
- 清理构建缓存: `./gradlew clean`
- 确保子模块已正确初始化: `git submodule update --init --recursive`

### 运行时崩溃
- 检查 Logcat 日志
- 确认 Java 运行时已正确安装
- 检查设备架构兼容性
- 查看崩溃报告界面

### 资源下载失败
- 检查网络连接
- 确认存储权限已授予
- 查看下载管理器日志

### 子模块问题
- 如果 ZalithLauncher2 子模块未正确初始化，运行:
  ```bash
  git submodule deinit -f third_party/ZalithLauncher2
  git submodule update --init --recursive third_party/ZalithLauncher2
  ```

## 版本信息

- **当前版本**: a0.25.1221 - NEBULA
- **版本代码**: 1221
- **目标 SDK**: 36
- **最低 SDK**: 26

## 贡献指南

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 许可证

本项目采用 GPL-3.0 许可证，详见 [LICENSE](LICENSE) 文件

## 联系方式

- GitHub: https://github.com/herbrine8403/ShardLauncher-Legacy
- Issues: https://github.com/herbrine8403/ShardLauncher-Legacy/issues
- 原始项目: https://github.com/LanRhyme/ShardLauncher
