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
// LayerController 模块 (从 ZalithLauncher2 引入)
include(":LayerController")
project(":LayerController").projectDir = file("third_party/ZalithLauncher2/LayerController")
 