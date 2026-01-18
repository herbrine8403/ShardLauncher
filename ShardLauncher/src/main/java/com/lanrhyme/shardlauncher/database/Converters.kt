package com.lanrhyme.shardlauncher.database

import androidx.room.TypeConverter
import com.lanrhyme.shardlauncher.game.account.wardrobe.SkinModelType

class Converters {
    @TypeConverter
    fun fromSkinModelType(value: SkinModelType): String {
        return value.name
    }

    @TypeConverter
    fun toSkinModelType(value: String): SkinModelType {
        return runCatching { SkinModelType.valueOf(value) }.getOrDefault(SkinModelType.NONE)
    }
}
