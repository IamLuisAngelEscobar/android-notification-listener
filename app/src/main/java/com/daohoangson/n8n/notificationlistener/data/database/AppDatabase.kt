package com.daohoangson.n8n.notificationlistener.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.daohoangson.n8n.notificationlistener.utils.Constants

@Database(
    entities = [FailedNotification::class, UndecidedNotification::class, PendingCapture::class, ListenedApp::class],
    version = Constants.DATABASE_VERSION,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun failedNotificationDao(): FailedNotificationDao
    abstract fun undecidedNotificationDao(): UndecidedNotificationDao
    abstract fun pendingCaptureDao(): PendingCaptureDao
    abstract fun listenedAppDao(): ListenedAppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Adds `listened_apps`. Explicit so the upgrade does not fall through to
         * the destructive fallback, which would wipe the unsent captures in
         * `pending_captures`.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `listened_apps` (" +
                        "`packageName` TEXT NOT NULL, " +
                        "`account` TEXT NOT NULL, " +
                        "`defaultCurrency` TEXT NOT NULL, " +
                        "`enabled` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`packageName`))"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    Constants.DATABASE_NAME
                ).addMigrations(MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
