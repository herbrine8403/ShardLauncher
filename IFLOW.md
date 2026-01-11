# ShardLauncher 项目文档

> 最后更新: 2026年1月11日  
> 当前版本: a0.25.1221 - NEBULA  
> 包名: com.lanrhyme.shardlauncher

## 项目概述

ShardLauncher 是一个基于 Jetpack Compose 的现代化 Minecraft Java 版 Android 启动器应用。它提供了丰富的自定义选项、现代化的用户界面和强大的游戏管理功能。

### 核心特性

- **多种登录方式**: 支持微软账号和离线模式登录
- **智能游戏管理**: 自动下载和管理 Minecraft 游戏文件（客户端、资源、库文件）
- **高度可定制的 UI**:
  - 深色/浅色主题切换
  - 多种预设主题颜色 + 自定义主题色
  - 自定义启动器背景（图片/视频，支持视差效果）
  - 可调整的 UI 动画速度和缩放比例
  - 可自定义的侧边栏位置
  - 背景光效和模糊效果
- **音乐播放器**: 内置音乐播放功能
- **版本管理**: 支持多版本游戏安装、配置和管理
- **模组支持**: 模组、资源包、光影包管理
- **渲染器管理**: 支持多种 OpenGL 渲染器（GL4ES、VirGL、Zink 等）
- **运行时管理**: 支持多 Java 运行时
- **自定义布局**: 支持通过 XAML 自定义主页布局（类似 PCL2）
- **开发者选项**: 组件演示和调试工具

## 技术栈

### 核心技术

| 技术 | 版本 | 用途 |
|------|------|------|
| Kotlin | 2.1.10 | 主要开发语言 |
| Android Gradle Plugin | 8.13.0 | 构建工具 |
| Jetpack Compose | BOM 2024.09.00 | UI 框架 |
| Material Design 3 | - | 设计系统 |
| Room Database | 2.6.1 | 本地数据库 |
| Ktor | 3.0.3 | 网络请求和本地服务器 |
| Kotlinx Serialization | 1.7.3 | JSON 序列化 |

### 主要依赖库

#### UI 和媒体
- `androidx.navigation:navigation-compose:2.9.4` - 页面导航
- `io.coil-kt:coil-compose:2.6.0` - 图片加载
- `io.coil-kt:coil-video:2.6.0` - 视频加载
- `androidx.media3:media3-exoplayer:1.3.1` - 视频播放和音乐播放
- `dev.chrisbanes.haze:haze:1.7.1` - 模糊效果

#### 网络和数据处理
- `com.squareup.retrofit2:retrofit:2.9.0` - REST API 客户端
- `com.google.code.gson:gson:2.10.1` - JSON 解析
- `org.apache.maven:maven-artifact:3.8.6` - Maven 依赖解析

#### 文件和压缩
- `commons-io:commons-io:2.16.1` - IO 工具
- `commons-codec:commons-codec:1.16.1` - 编解码
- `org.apache.commons:commons-compress:1.26.1` - 压缩工具
- `org.tukaani:xz:1.9` - XZ 压缩支持

#### 系统和原生
- `com.bytedance:bytehook:1.0.9` - PLT Hook 库
- `org.ow2.asm:asm-all:5.0.4` - 字节码操作
- `com.github.oshi:oshi-core:6.3.0` - 系统信息获取

## 构建配置

### SDK 版本
- **编译 SDK**: 36
- **最小 SDK**: 26 (Android 8.0)
- **目标 SDK**: 36

### 构建工具
- **JDK**: 11
- **NDK**: 25.2.9519653
- **Kotlin 编译目标**: JVM 11

### 支持的架构
- x86
- x86_64
- armeabi-v7a
- arm64-v8a

### 构建变体
- **release**: 正式版，无后缀
- **debug**: 开发版，后缀 `.debug`

## 项目架构

### 整体架构

项目采用典型的 Android 应用架构，遵循 MVVM 模式：

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                             │
│  (Jetpack Compose Screens, Components, ViewModels)         │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                      Data Layer                             │
│  (Repositories, Database, API Services)                    │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                  Game Layer                                 │
│  (Game Launcher, Version Manager, Account Manager)         │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                  Native Layer (JNI)                         │
│  (AWT Bridge, EGL Bridge, Input Bridge, etc.)              │
└─────────────────────────────────────────────────────────────┘
```

### 模块结构

#### 主要包结构

```
com.lanrhyme.shardlauncher/
├── MainActivity.kt                    # 应用主入口
├── ShardLauncherApp.kt                # Application 类
│
├── api/                               # API 接口定义
│   ├── ApiClient.kt
│   ├── BmclapiService.kt
│   ├── FabricApiService.kt
│   ├── ForgeApiService.kt
│   ├── MicrosoftAuthService.kt
│   ├── MinecraftAuthService.kt
│   ├── MojangApiService.kt
│   ├── NeoForgeApiService.kt
│   ├── OptiFineApiService.kt
│   ├── QuiltApiService.kt
│   ├── RmsApiService.kt
│   └── VersionApiService.kt
│
├── bridge/                            # 原生桥接
│   ├── LoggerBridge.java
│   ├── ZLBridge.java
│   ├── ZLBridgeStates.kt
│   └── ZLNativeInvoker.kt
│
├── common/                            # 公共数据类
│   └── SidebarPosition.kt
│
├── components/                        # 组件管理
│   ├── AbstractUnpackTask.kt
│   ├── Components.kt
│   ├── ComponentUnpacker.kt
│   ├── UnpackComponentsTask.kt
│   └── UnpackSingleTask.kt
│
├── coroutine/                         # 协程任务
│   ├── Task.kt
│   └── TaskState.kt
│
├── data/                              # 数据层
│   ├── AccountRepository.kt
│   ├── MusicRepository.kt
│   └── SettingsRepository.kt
│
├── database/                          # 数据库
│   ├── AppDatabase.kt
│   └── Converters.kt
│
├── game/                              # 游戏核心
│   ├── account/                       # 账户管理
│   │   ├── Account.kt
│   │   ├── AccountDao.kt
│   │   ├── AccountsManager.kt
│   │   ├── auth_server/              # 认证服务器
│   │   ├── microsoft/                # 微软登录
│   │   ├── offline/                  # 离线登录
│   │   └── wardrobe/                 # 皮肤管理
│   │
│   ├── addons/                        # 插件和扩展
│   │   ├── mirror/
│   │   │   └── BMCLAPI.kt
│   │   └── modloader/
│   │       └── ModLoader.kt
│   │
│   ├── input/                         # 输入处理
│   │   └── CriticalNativeTest.java
│   │
│   ├── launch/                        # 游戏启动
│   │   ├── GameLauncher.kt
│   │   ├── GameLaunchManager.kt
│   │   ├── JvmLauncher.kt
│   │   ├── LanguageHelper.kt
│   │   ├── LaunchArgs.kt
│   │   ├── Launcher.kt
│   │   ├── LauncherFactory.kt
│   │   ├── LaunchGame.kt
│   │   ├── MCOptions.kt
│   │   ├── LaunchExample.kt
│   │   └── LaunchTest.kt
│   │
│   ├── mod/                           # 模组管理
│   │   ├── api/
│   │   │   └── ModrinthApiService.kt
│   │   ├── ModCache.kt
│   │   └── ModMetadata.kt
│   │
│   ├── multirt/                       # 多运行时
│   │   ├── Runtime.kt
│   │   ├── RuntimeInstaller.kt
│   │   └── RuntimesManager.kt
│   │
│   ├── path/                          # 路径管理
│   │   ├── GamePath.kt
│   │   └── GamePathManager.kt
│   │
│   ├── plugin/                        # 插件系统
│   │   ├── driver/
│   │   │   └── DriverPluginManager.kt
│   │   ├── ffmpeg/
│   │   │   └── FFmpegPluginManager.kt
│   │   └── renderer/
│   │       ├── RendererPlugin.kt
│   │       └── RendererPluginManager.kt
│   │
│   ├── renderer/                      # 渲染器管理
│   │   ├── RendererInstaller.kt
│   │   ├── RendererInterface.kt
│   │   ├── Renderers.kt
│   │   ├── RenderersList.kt
│   │   └── renderers/
│   │       ├── FreedrenoRenderer.kt
│   │       ├── GL4ESRenderer.kt
│   │       ├── NGGL4ESRenderer.kt
│   │       ├── PanfrostRenderer.kt
│   │       ├── VirGLRenderer.kt
│   │       └── VulkanZinkRenderer.kt
│   │
│   ├── resource/                      # 资源管理
│   │   └── ResourceManager.kt
│   │
│   ├── version/                       # 版本管理
│   │   ├── download/                  # 版本下载
│   │   │   ├── game/
│   │   │   │   └── GameLibDownloader.kt
│   │   │   ├── BaseMinecraftDownloader.kt
│   │   │   ├── DownloadFailedException.kt
│   │   │   ├── DownloadMode.kt
│   │   │   ├── DownloadTask.kt
│   │   │   ├── MinecraftDownloader.kt
│   │   │   ├── VersionDownloadUtils.kt
│   │   │   └── _LibraryReplacement.kt
│   │   ├── installed/                 # 已安装版本
│   │   │   ├── utils/
│   │   │   │   └── VersionInfoParser.kt
│   │   │   ├── GameManifestUtils.kt
│   │   │   ├── SampleVersionCreator.kt
│   │   │   ├── Version.kt
│   │   │   ├── VersionComparator.kt
│   │   │   ├── VersionConfig.kt
│   │   │   ├── VersionInfo.kt
│   │   │   ├── VersionsManager.kt
│   │   │   └── VersionType.kt
│   │   └── mod/                      # 模组读取
│   │       ├── AllModReader.kt
│   │       ├── LocalMod.kt
│   │       └── ModReader.kt
│   │
│   ├── versioninfo/                   # 版本信息
│   │   ├── models/
│   │   ├── _MinecraftVersionCatalog.kt
│   │   ├── MinecraftVersion.kt
│   │   └── MinecraftVersions.kt
│   │
│   └── keycodes/                      # 按键映射
│
├── info/                              # 信息分发
│   └── InfoDistributor.kt
│
├── model/                             # 数据模型
│   ├── Account.kt
│   ├── MusicItem.kt
│   ├── Version.kt
│   ├── auth/
│   ├── minecraft/
│   ├── mojang/
│   ├── version/
│   ├── BmclapiManifest.kt
│   ├── FabricLoaderVersion.kt
│   ├── ForgeVersion.kt
│   ├── ForgeVersionToken.kt
│   ├── LoaderVersion.kt
│   ├── NeoForgeVersion.kt
│   ├── OptiFineVersionToken.kt
│   └── QuiltVersion.kt
│
├── path/                              # 路径管理
│   ├── LibPath.kt
│   ├── PathManager.kt
│   └── UrlManager.kt
│
├── service/                           # 服务
│   └── MusicPlayerService.kt
│
├── setting/                           # 设置
│   └── enums/
│       └── MirrorSourceType.kt
│
├── settings/                          # 设置系统
│   ├── enums/
│   │   └── MirrorSourceType.kt
│   ├── unit/
│   │   ├── AbstractSettingUnit.kt
│   │   ├── BooleanSettingUnit.kt
│   │   ├── EnumSettingUnit.kt
│   │   ├── IntSettingUnit.kt
│   │   └── StringSettingUnit.kt
│   ├── AllSettings.kt
│   └── SettingsRegistry.kt
│
├── ui/                                # UI 层
│   ├── account/                       # 账户界面
│   │   ├── AccountCard.kt
│   │   ├── AccountScreen.kt
│   │   ├── AccountViewModel.kt
│   │   └── AccountViewModelFactory.kt
│   │
│   ├── components/                    # UI 组件
│   │   ├── BackgroundLightEffect.kt
│   │   ├── CommonComponents.kt
│   │   ├── CustomButton.kt
│   │   ├── CustomCard.kt
│   │   ├── CustomDialog.kt
│   │   ├── CustomTextField.kt
│   │   ├── FluidFab.kt
│   │   ├── Layouts.kt
│   │   ├── LoaderVersionDropdown.kt
│   │   ├── LocalLayoutConfig.kt
│   │   ├── MusicPlayerDialog.kt
│   │   ├── README.md
│   │   ├── ResourceInstallDialog.kt
│   │   ├── SimpleDialogs.kt
│   │   └── VersionItem.kt
│   │
│   ├── composables/                   # 可组合组件
│   │   ├── HsvColorPicker.kt
│   │   └── ThemeColorEditor.kt
│   │
│   ├── crash/                         # 崩溃处理
│   │   ├── CrashActivity.kt
│   │   └── CrashScreen.kt
│   │
│   ├── custom/                        # 自定义 XAML
│   │   ├── model/
│   │   │   └── XamlNode.kt
│   │   ├── Vector.kt
│   │   ├── XamlComponents.kt
│   │   ├── XamlEvents.kt
│   │   └── XamlParser.kt
│   │
│   ├── developeroptions/              # 开发者选项
│   │   ├── ComponentDemoScreen.kt
│   │   └── DeveloperOptionsScreen.kt
│   │
│   ├── downloads/                     # 下载界面
│   │   ├── DownloadScreen.kt
│   │   ├── GameDownloadContent.kt
│   │   ├── GameDownloadViewModel.kt
│   │   ├── VersionDetailScreen.kt
│   │   ├── VersionDetailViewModel.kt
│   │   └── VersionType.kt
│   │
│   ├── home/                          # 主页
│   │   ├── HomeAccountCard.kt
│   │   ├── HomeScreen.kt
│   │   └── VersionSelector.kt
│   │
│   ├── music/                         # 音乐播放器
│   │   └── MusicPlayerViewModel.kt
│   │
│   ├── navigation/                    # 导航
│   │   └── Navigation.kt
│   │
│   ├── notification/                  # 通知系统
│   │   ├── Notification.kt
│   │   ├── NotificationComponents.kt
│   │   ├── NotificationDialog.kt
│   │   ├── NotificationManager.kt
│   │   ├── NotificationUI.kt
│   │   └── README.md
│   │
│   ├── settings/                      # 设置界面
│   │   ├── AboutScreen.kt
│   │   ├── GameSettings.kt
│   │   ├── LauncherSettings.kt
│   │   ├── OtherSettings.kt
│   │   ├── RendererManageScreen.kt
│   │   ├── RuntimeManageScreen.kt
│   │   ├── SettingItems.kt
│   │   └── SettingsScreen.kt
│   │
│   ├── theme/                         # 主题
│   │   ├── Color.kt
│   │   ├── Shape.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   │
│   ├── version/                       # 版本管理界面
│   │   ├── ModDetailsDialog.kt
│   │   ├── ModsManagementScreen.kt
│   │   ├── ResourcePacksManagementScreen.kt
│   │   ├── SavesManagementScreen.kt
│   │   ├── ShaderPacksManagementScreen.kt
│   │   ├── VersionCategory.kt
│   │   ├── VersionConfigScreen.kt
│   │   ├── VersionManageElements.kt
│   │   ├── VersionManageScreen.kt
│   │   ├── VersionOperationDialogs.kt
│   │   ├── VersionOverviewScreen.kt
│   │   └── VersionScreen.kt
│   │
│   ├── LocalSettingsProvider.kt
│   └── SplashScreen.kt
│
└── utils/                             # 工具类
    ├── asset/
    │   └── AssetExtractor.kt
    ├── classes/
    │   └── Quadruple.kt
    ├── device/
    │   ├── Architecture.kt
    │   └── DeviceUtils.kt
    ├── file/
    │   ├── CompressZipEntryAdapter.kt
    │   ├── FileUtils.kt
    │   ├── FolderUtils.kt
    │   ├── HashUtils.kt
    │   ├── InvalidFilenameException.java
    │   ├── JavaZipEntryAdapter.kt
    │   ├── PathHelper.kt
    │   └── ZipEntryBase.kt
    ├── json/
    │   ├── JsonUtils.kt
    │   └── JsonValueUtils.kt
    ├── logging/
    │   ├── Level.kt
    │   ├── Logger.kt
    │   └── LogMessage.kt
    ├── network/
    │   ├── NetworkDownloadUtils.kt
    │   ├── NetworkUtils.kt
    │   ├── NoNetworkException.java
    │   └── ServerAddress.kt
    ├── platform/
    │   └── MemoryUtils.kt
    ├── string/
    │   ├── _CompareString.kt
    │   ├── ShiftDirection.kt
    │   ├── StringSplitUtils.kt
    │   └── StringUtils.kt
    ├── version/
    │   ├── VersionCompare.kt
    │   └── VersionUtils.kt
    ├── AvatarUtils.kt
    ├── ContextUtils.kt
    ├── GSON.kt
    ├── LocalUtils.kt
    ├── Logger.kt
    ├── ParallaxSensorHelper.kt
    └── Utils.kt
```

#### 原生代码 (JNI)

```
app/src/main/jni/
├── Android.mk                        # NDK 构建配置
├── Application.mk                    # NDK 应用配置
├── awt_bridge.c                      # AWT 桥接
├── bigcoreaffinity.c                 # 大核亲和性
├── egl_bridge.c                      # EGL 桥接
├── exit_hook.c                       # 退出钩子
├── input_bridge_v3.c                 # 输入桥接 v3
├── java_exec_hooks.c                 # Java 执行钩子
├── jre_launcher.c                    # JRE 启动器
├── lwjgl_dlopen_hook.c               # LWJGL 动态加载钩子
├── stdio_is.c                        # 标准输入输出
├── stdio_is.h
├── utils.c                           # 工具函数
├── utils.h
├── awt_xawt/                         # AWT XAWT
├── ctxbridges/                       # 上下文桥接
├── driver_helper/                    # 驱动助手
├── environ/                          # 环境变量
├── GL/                               # OpenGL
├── linkerhook/                       # 链接器钩子
└── logger/                           # 日志
```

#### 资源文件

```
app/src/main/
├── assets/
│   ├── home.xaml                     # 自定义主页布局
│   ├── components/                   # 组件资源
│   └── runtimes/                     # 运行时资源
├── res/
│   ├── drawable/                     # 图片资源
│   ├── mipmap-*/                     # 应用图标
│   ├── values/                       # 值资源
│   ├── values-night/                 # 深色主题值
│   └── xml/                          # XML 配置
└── AndroidManifest.xml               # Android 清单
```

## 数据库结构

### AppDatabase

使用 Room Database，版本 3，包含以下表：

| 表名 | 用途 |
|------|------|
| Account | 启动器账号 |
| AuthServer | 认证服务器配置 |
| GamePath | 游戏目录配置 |

### DAOs

- `AccountDao`: 账号数据访问对象
- `AuthServerDao`: 认证服务器数据访问对象
- `GamePathDao`: 游戏目录数据访问对象

## 开发规范

### UI 开发
- 使用 Jetpack Compose 进行声明式 UI 开发
- 遵循 Material Design 3 设计规范
- 使用 `@Composable` 注解标记可组合函数
- 保持组件的可重用性和可组合性

### 状态管理
- 使用 Compose 的 `State` 和 `remember` 管理本地状态
- 使用 `ViewModel` 管理复杂状态
- 使用 `LaunchedEffect` 处理副作用
- 使用 `mutableStateOf` 创建可变状态

### 导航
- 使用 `androidx.navigation:navigation-compose` 进行页面导航
- 采用单 Activity 多 Composable 架构
- 使用 `NavHost` 定义导航图
- 使用 `rememberNavController` 创建导航控制器

### 主题系统
- 使用 Material Design 3 主题系统
- 支持深色/浅色主题切换
- 支持多种预设主题颜色
- 支持自定义主题颜色
- 使用 `CompositionLocalProvider` 提供主题上下文

### 数据持久化
- 使用 Room Database 存储结构化数据
- 使用 `SharedPreferences`（通过 `SettingsRepository` 封装）存储用户设置
- 使用 `SettingsRegistry` 管理所有设置项

### 网络请求
- 使用 Retrofit 进行 REST API 调用
- 使用 Ktor Client 进行 HTTP 请求
- 使用协程处理异步操作
- 使用 `suspend` 函数标记异步操作

### 日志系统
- 使用自定义的 `Logger` 类记录日志
- 支持多种日志级别（DEBUG, INFO, WARNING, ERROR）
- 使用 `LoggerBridge` 连接原生日志

## 构建和运行

### 环境要求

- **Android Studio**: 最新稳定版
- **Android SDK**: API 35 或更高
- **JDK**: 11
- **NDK**: 25.2.9519653

### 环境配置

在项目根目录的 `local.properties` 文件中添加以下配置：

```properties
# 微软客户端 ID（用于微软登录）
MICROSOFT_CLIENT_ID=your_client_id_here
```

或者设置环境变量：

```bash
export MICROSOFT_CLIENT_ID=your_client_id_here
```

### 构建步骤

1. **克隆项目仓库**:
   ```bash
   git clone https://github.com/LanRhyme/ShardLauncher.git
   cd ShardLauncher
   ```

2. **同步 Gradle**:
   - 在 Android Studio 中打开项目
   - 等待 Gradle 同步完成

3. **连接设备**:
   - 连接 Android 设备或启动模拟器
   - 确保设备已启用开发者选项和 USB 调试

4. **构建应用**:
   ```bash
   # Debug 版本
   ./gradlew assembleDebug

   # Release 版本
   ./gradlew assembleRelease

   # 通用 APK（包含所有架构）
   ./gradlew assembleUniversalDebug

   # 特定架构 APK
   ./gradlew assembleArm64-v8aDebug
   ```

5. **安装应用**:
   ```bash
   # Debug 版本
   ./gradlew installDebug

   # Release 版本
   ./gradlew installRelease
   ```

6. **在 Android Studio 中运行**:
   - 点击 "Run" 按钮（绿色三角形）
   - 或使用快捷键 `Shift + F10`

### 常用 Gradle 任务

```bash
# 清理构建
./gradlew clean

# 构建所有变体
./gradlew assemble

# 运行测试
./gradlew test

# 运行 Android 测试
./gradlew connectedAndroidTest

# 生成依赖报告
./gradlew app:dependencies

# 查看构建变体
./gradlew tasks
```

## Git 工作流

### 分支策略

- `master`: 主分支，稳定版本
- `develop`: 开发分支
- `feature/*`: 功能分支
- `bugfix/*`: 修复分支

### 提交规范

提交信息应清晰描述更改内容：

```
<type>(<scope>): <subject>

<body>

<footer>
```

类型（type）：
- `feat`: 新功能
- `fix`: 修复
- `docs`: 文档
- `style`: 格式
- `refactor`: 重构
- `test`: 测试
- `chore`: 构建/工具

示例：
```
feat(ui): add music player dialog

Add a new dialog for music player control with play/pause,
next/previous track, and volume control.

Closes #123
```

### 版本号格式

版本号格式：`a<主版本>.<次版本>.<构建号> - <代号>`

示例：`a0.25.1221 - NEBULA`

## 特殊功能

### 自定义 XAML 布局

启动器支持通过 XAML 自定义主页布局，类似于 PCL2。

**位置**: `app/src/main/assets/home.xaml`

**示例**:
```xml
<local:MyCard Title="ShardLauncher" Margin="0,0,0,15">
    <StackPanel Margin="25,40,23,15">
        <TextBlock Margin="0,0,0,4" FontSize="13" HorizontalAlignment="Center" 
                   Foreground="{DynamicResource ColorBrush1}"
                   Text="欢迎使用 ShardLauncher！" />
    </StackPanel>
</local:MyCard>
```

**相关文件**:
- `ui/custom/XamlParser.kt` - XAML 解析器
- `ui/custom/XamlComponents.kt` - XAML 组件映射
- `ui/custom/XamlEvents.kt` - XAML 事件处理

### 视差效果

支持基于陀螺仪的视差效果背景。

**配置**:
- `enableParallax`: 启用/禁用视差效果
- `parallaxMagnitude`: 视差幅度（1-10）

**实现**: `utils/ParallaxSensorHelper.kt`

### 背景光效

支持动态背景光效效果。

**配置**:
- `enableBackgroundLightEffect`: 启用/禁用光效
- `enableBackgroundLightEffectCustomColor`: 使用自定义颜色
- `backgroundLightEffectCustomColor`: 自定义光效颜色
- `lightEffectAnimationSpeed`: 光效动画速度

**实现**: `ui/components/BackgroundLightEffect.kt`

### 音乐播放器

内置音乐播放器，支持后台播放。

**服务**: `service/MusicPlayerService.kt`

**界面**: `ui/music/MusicPlayerViewModel.kt`

### 崩溃处理

全局崩溃处理，显示崩溃信息。

**实现**:
- `ShardLauncherApp.kt` - 设置崩溃处理器
- `ui/crash/CrashActivity.kt` - 崩溃界面
- `ui/crash/CrashScreen.kt` - 崩溃显示

### 通知系统

内置通知系统，支持多种通知类型。

**实现**:
- `ui/notification/NotificationManager.kt` - 通知管理器
- `ui/notification/Notification.kt` - 通知数据类
- `ui/notification/NotificationComponents.kt` - 通知组件
- `ui/notification/NotificationUI.kt` - 通知界面

## 第三方项目

项目包含以下第三方子项目：

### NG-GL4ES
OpenGL 到 OpenGL ES 转换库

**位置**: `third_party/NG-GL4ES/`

### FoldCraftLauncher
参考项目

**位置**: `third_party/FoldCraftLauncher/`

### ZalithLauncher2
参考项目

**位置**: `third_party/ZalithLauncher2/`

## 故障排除

### 常见问题

1. **Gradle 同步失败**
   - 检查网络连接
   - 清理 Gradle 缓存：`./gradlew clean --no-daemon`
   - 检查 JDK 版本是否为 11

2. **NDK 构建失败**
   - 确保已安装 NDK 25.2.9519653
   - 检查 `local.properties` 中的 NDK 路径
   - 运行 `./gradlew clean` 后重新构建

3. **应用崩溃**
   - 查看崩溃日志
   - 检查 `CrashActivity` 显示的错误信息
   - 确保所有必需的资源已安装

4. **游戏无法启动**
   - 检查 Java 运行时是否已安装
   - 检查渲染器是否已安装
   - 查看游戏日志

### 调试技巧

1. **启用详细日志**:
   ```kotlin
   Logger.setLevel(Level.DEBUG)
   ```

2. **查看数据库**:
   - 使用 Android Studio 的 Database Inspector
   - 数据库文件位置：`/data/data/com.lanrhyme.shardlauncher/databases/launcher_data.db`

3. **查看原生日志**:
   ```bash
   adb logcat | grep -E "(ShardLauncher|native)"
   ```

## 贡献指南

1. Fork 项目
2. 创建功能分支：`git checkout -b feature/amazing-feature`
3. 提交更改：`git commit -m 'feat: add amazing feature'`
4. 推送到分支：`git push origin feature/amazing-feature`
5. 创建 Pull Request

## 许可证

本项目采用开源许可证，详细信息请参阅 [LICENSE](LICENSE) 文件。

## 相关资源

- **GitHub 仓库**: https://github.com/LanRhyme/ShardLauncher
- **问题反馈**: https://github.com/LanRhyme/ShardLauncher/issues
- **功能请求**: https://github.com/LanRhyme/ShardLauncher/issues/new?template=feature_request.md
- **Bug 报告**: https://github.com/LanRhyme/ShardLauncher/issues/new?template=bug_report.md

## 更新日志

### a0.25.1221 - NEBULA
- 添加音乐播放器功能
- 改进渲染器管理系统
- 优化性能和稳定性
- 修复多个已知问题

（更多更新日志请查看 GitHub Releases）

---

**注意**: 本项目正在积极开发中，部分功能可能尚未完成或有变动。欢迎贡献代码和反馈问题！