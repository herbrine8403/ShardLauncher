package com.lanrhyme.shardlauncher.game.download.game.forge

import com.lanrhyme.shardlauncher.game.download.game.models.ForgeLikeInstallProcessor
import java.io.File

/**
 * Forge安装工具类
 * [Reference HMCL](https://github.com/HMCL-dev/HMCL/blob/6e05b5ee58e67cd40e58c6f6002f3599897ca358/HMCLCore/src/main/java/org/jackhuang/hmcl/download/forge/ForgeNewInstallTask.java#L249-L258)
 */
fun parseLiteral(
    baseDir: File,
    literal: String,
    vars: Map<String, String> = emptyMap(),
    plainConverter: (String) -> String = { it -> it }
): String? {
    return when {
        literal.startsWith("{") && literal.endsWith("}") -> {
            vars[literal.removeSurrounding("{", "}")]
        }
        literal.startsWith("'") && literal.endsWith("'") -> {
            literal.removeSurrounding("'")
        }
        literal.startsWith("[") && literal.endsWith("]") -> {
            val path = literal.removeSurrounding("[", "]")
            File(baseDir, "libraries/$path").absolutePath
        }
        else -> {
            plainConverter(replaceTokens(vars, literal))
        }
    }
}

/**
 * [Reference HMCL](https://github.com/HMCL-dev/HMCL/blob/6e05b5ee58e67cd40e58c6f6002f3599897ca358/HMCLCore/src/main/java/org/jackhuang/hmcl/download/forge/ForgeNewInstallTask.java#L308-L330)
 */
fun parseOptions(baseDir: File, args: List<String>, vars: Map<String, String>): Map<String, String> {
    val options = LinkedHashMap<String, String>()
    var optionName: String? = null

    for (arg in args) {
        if (arg.startsWith("--")) {
            optionName?.let { options[it] = "" }
            optionName = arg.removePrefix("--")
        } else {
            if (optionName != null) {
                options[optionName] = parseLiteral(baseDir, arg, vars)!!
                optionName = null
            }
        }
    }

    optionName?.let { options[it] = "" }

    return options
}

/**
 * [Modified from HMCL](https://github.com/HMCL-dev/HMCL/blob/6e05b5ee58e67cd40e58c6f6002f3599897ca358/HMCLCore/src/main/java/org/jackhuang/hmcl/download/forge/ForgeNewInstallTask.java#L205-L247)
 */
private fun replaceTokens(tokens: Map<String, String>, value: String): String {
    val buf = StringBuilder()
    var x = 0
    while (x < value.length) {
        val c = value[x]
        if (c == '\\') {
            require(x != value.length - 1) { "Illegal pattern (Bad escape): $value" }
            buf.append(value[++x])
        } else if (c == '{' || c == '\'') {
            val key = StringBuilder()
            var y = x + 1
            while (y <= value.length) {
                require(y != value.length) { "Illegal pattern (Unclosed $c): $value" }
                val d = value[y]
                if (d == '\\') {
                    require(y != value.length - 1) { "Illegal pattern (Bad escape): $value" }
                    key.append(value[++y])
                } else {
                    if (c == '{' && d == '}') {
                        x = y
                        break
                    }
                    if (c == '\'' && d == '\'') {
                        x = y
                        break
                    }
                    key.append(d)
                }
                y++
            }
            if (c == '\'') {
                buf.append(key)
            } else {
                require(tokens.containsKey(key.toString())) { "Illegal pattern: $value Missing Key: $key" }
                buf.append(tokens[key.toString()])
            }
        } else {
            buf.append(c)
        }
        x++
    }
    return buf.toString()
}

fun ForgeLikeInstallProcessor.isSide(side: String): Boolean {
    return getSides().isEmpty() || getSides().contains(side)
}