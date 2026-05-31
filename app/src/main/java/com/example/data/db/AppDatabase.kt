package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.RunHistoryEntity
import com.example.data.model.StageProgressEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [RunHistoryEntity::class, StageProgressEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun runHistoryDao(): RunHistoryDao
    abstract fun stageProgressDao(): StageProgressDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "knovera_database"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        // Pre-populate stage progresses
                        val stageDao = database.stageProgressDao()
                        stageDao.insertProgressList(
                            listOf(
                                StageProgressEntity("normal", unlocked = true, completed = false),
                                StageProgressEntity("bbm", unlocked = false, completed = false),
                                StageProgressEntity("elnino_bbm", unlocked = false, completed = false),
                                StageProgressEntity("overfished", unlocked = false, completed = false)
                            )
                        )
                    }
                }
            }
        }
    }
}
