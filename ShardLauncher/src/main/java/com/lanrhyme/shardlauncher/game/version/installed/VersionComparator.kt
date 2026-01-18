package com.lanrhyme.shardlauncher.game.version.installed

/**
 * 版本比较器，用于对版本列表进行排序
 * 排序规则：
 * 1. 置顶版本优先
 * 2. 有效版本优先于无效版本
 * 3. 按版本名称字母顺序排序
 */
object VersionComparator : Comparator<Version> {
    override fun compare(o1: Version, o2: Version): Int {
        // 1. 置顶版本优先
        val pin1 = o1.pinnedState
        val pin2 = o2.pinnedState
        if (pin1 != pin2) {
            return if (pin1) -1 else 1
        }

        // 2. 有效版本优先于无效版本
        val valid1 = o1.isValid()
        val valid2 = o2.isValid()
        if (valid1 != valid2) {
            return if (valid1) -1 else 1
        }

        // 3. 按版本名称字母顺序排序
        return o1.getVersionName().compareTo(o2.getVersionName(), ignoreCase = true)
    }
}