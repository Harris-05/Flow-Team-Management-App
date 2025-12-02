package com.ahmedprojects.flow
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(
    entities = [ProjectEntity::class, PendingProjectEntity::class, TaskEntity::class], // add TaskEntity
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun pendingProjectDao(): PendingProjectDao
    abstract fun taskDao(): TaskDao   // add taskDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "flow_local.db"
                )
                    .fallbackToDestructiveMigration() // auto drop old DB if schema changed
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
