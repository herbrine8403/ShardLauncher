# ShardLauncher UI 组件库

本文档列出了 `ui/components` 目录下的所有可复用 UI 组件，按功能分类。

## 目录

*   [基础组件 (Basic)](#基础组件-basic)
*   [布局组件 (Layout)](#布局组件-layout)
*   [对话框组件 (Dialog)](#对话框组件-dialog)
*   [业务组件 (Business)](#业务组件-business)
*   [视觉效果 (Effect)](#视觉效果-effect)
*   [颜色相关 (Color)](#颜色相关-color)

## 基础组件 (Basic)

位于 `ui/components/basic` 目录下。

*   **CommonComponents.kt**
    *   `CollapsibleCard`: 可折叠卡片组件
    *   `CombinedCard`: 组合卡片组件（标题+内容）
    *   `ScalingActionButton`: 带缩放动画的动作按钮
    *   `TitleAndSummary`: 标题和摘要文本组件
    *   `SegmentedNavigationBar`: 分段式导航栏
    *   `SubPageNavigationBar`: 子页面导航栏（带返回）
    *   `StyledFilterChip`: 样式化的过滤芯片
    *   `SearchTextField`: 搜索文本框
    *   `CapsuleTextField`: 胶囊风格输入框
    *   `BackgroundTextTag`: 带背景的文字标签
    *   `TitledDivider`: 带标题的分割线
    *   `PopupContainer`: 弹出式容器
    *   `ScrollIndicator`: 滚动指示器
    *   `Modifier.glow`: 发光效果修饰符
    *   `Modifier.animatedAppearance`: 入场动画修饰符
    *   `Modifier.selectableCard`: 可选卡片修饰符

*   **ShardThemeComponents.kt**
    *   `ShardCard`: ShardTheme 风格卡片
    *   `ShardButton`: ShardTheme 风格按钮
    *   `ShardDialog`: ShardTheme 风格对话框
    *   `ShardDropdownMenu`: ShardTheme 风格下拉菜单
    *   `ShardInputField`: ShardTheme 风格输入框

## 布局组件 (Layout)

位于 `ui/components/layout` 目录下。

*   **LayoutCards.kt**
    *   `SimpleListLayoutCard`: 简单列表布局卡片
    *   `SimpleCard`: 简单卡片
    *   `ExpandableLayoutCard`: 可展开布局卡片
    *   `TextInputLayoutCard`: 文本输入布局卡片

*   **LocalLayoutConfig.kt**
    *   `CardLayoutConfig`: 卡片布局配置
    *   `LocalCardLayoutConfig`: 布局配置 CompositionLocal

## 对话框组件 (Dialog)

位于 `ui/components/dialog` 目录下。

*   **MusicPlayerDialog.kt**
    *   `MusicPlayerDialog`: 音乐播放器对话框
*   **ResourceInstallDialog.kt**
    *   `ResourceInstallDialog`: 资源安装对话框
*   **TaskFlowDialog.kt**
    *   `TaskFlowDialog`: 任务流对话框

## 业务组件 (Business)

位于 `ui/components/business` 目录下。

*   **FluidFab.kt**
    *   `FluidFab`: 流体动画悬浮按钮
*   **LoaderVersionDropdown.kt**
    *   `LoaderVersionDropdown`: 加载器版本下拉选择
    *   `DisableIntrinsicMeasurements`: 禁用内部测量包装器
*   **VersionItem.kt**
    *   `VersionItem`: 版本列表项

## 视觉效果 (Effect)

位于 `ui/components/effect` 目录下。

*   **BackgroundLightEffect.kt**
    *   `BackgroundLightEffect`: 背景光斑动画效果

## 颜色相关 (Color)

位于 `ui/components/color` 目录下。

*   **HsvColorPicker.kt**
    *   `HsvColorPicker`: HSV 颜色选择器
*   **ThemeColorEditor.kt**
    *   `ThemeColorEditor`: 主题颜色编辑器
*   **ColorExtensions.kt**
    *   `Color.toHsv()`: Color 转 HSV
    *   `Color.hsv()`: HSV 转 Color
    *   `String.toColorOrNull()`: Hex 字符串转 Color
