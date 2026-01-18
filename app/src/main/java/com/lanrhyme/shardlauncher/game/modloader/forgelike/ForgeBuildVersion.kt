package com.lanrhyme.shardlauncher.game.modloader.forgelike

/**
 * Forge构建版本解析器
 * 用于解析和比较Forge版本号
 */
class ForgeBuildVersion private constructor(
    val major: Int,
    val minor: Int,
    val revision: Int,
    val build: Int,
    val branch: String?
) : Comparable<ForgeBuildVersion> {

    override fun compareTo(other: ForgeBuildVersion): Int {
        if (major != other.major) return major - other.major
        if (minor != other.minor) return minor - other.minor
        if (revision != other.revision) return revision - other.revision
        if (build != other.build) return build - other.build
        return 0
    }

    override fun toString(): String = buildString {
        append("$major.$minor.$revision.$build")
        branch?.let { append("-$it") }
    }

    companion object {
        fun parse(version: String): ForgeBuildVersion {
            val parts = version.split(".", "-")
            val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val revision = parts.getOrNull(2)?.toIntOrNull() ?: 0
            val build = parts.getOrNull(3)?.toIntOrNull() ?: 0
            val branch = if (parts.size > 4) parts.subList(4, parts.size).joinToString("-") else null
            
            return ForgeBuildVersion(major, minor, revision, build, branch)
        }
    }
}
