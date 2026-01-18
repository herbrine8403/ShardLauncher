package com.lanrhyme.shardlauncher.ui.version

import com.lanrhyme.shardlauncher.R

enum class VersionCategory(val textRes: Int) {
    /** 全部 */
    ALL(R.string.generic_all),
    /** 原版 */
    VANILLA(R.string.versions_manage_category_vanilla),
    /** 带有模组加载器 */
    MODLOADER(R.string.versions_manage_category_modloader)
}