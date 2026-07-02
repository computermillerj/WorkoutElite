package com.workoutelite.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

actual class DatabaseFactory(
    private val context: Context,
) {
    actual fun create(): RoomDatabase.Builder<AppDatabase> {
        val dbFile = context.applicationContext.getDatabasePath(AppDatabase.DATABASE_NAME)
        return Room.databaseBuilder(
            context = context.applicationContext,
            name = dbFile.absolutePath,
        )
    }
}
