pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ShardLauncher"
include(":ShardLauncher", ":SL-GameCore")
// LayerController 模块 (从 ZalithLauncher2 复制)
include(":LayerController")
project(":LayerController").projectDir = file("LayerController")
// NG-GL4ES 已移除，使用预构建静态库
 