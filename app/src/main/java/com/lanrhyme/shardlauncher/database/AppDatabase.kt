package com.lanrhyme.shardlauncher.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.lanrhyme.shardlauncher.game.account.Account
import com.lanrhyme.shardlauncher.game.account.AccountDao
import com.lanrhyme.shardlauncher.game.account.auth_server.data.AuthServer
import com.lanrhyme.shardlauncher.game.account.auth_server.data.AuthServerDao

import com.lanrhyme.shardlauncher.game.path.GamePath
import com.lanrhyme.shardlauncher.game.path.GamePathDao

@Database(
    entities = [Account::class, AuthServer::class, GamePath::class],
    version = 2,
    exportSchema = false
)
// @TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    /**
     * 启动器账号
     */
    abstract fun accountDao(): AccountDao

    /**
     * 认证服务器
     */
    abstract fun authServerDao(): AuthServerDao

    /**
     * 游戏目录
     */
    abstract fun gamePathDao(): GamePathDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * 获取全局数据库实例
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "launcher_data.db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
