package com.example.bits_helper.data

import android.content.Context
import androidx.room.Database
import androidx.room.TypeConverters
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [CartridgeEntity::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cartridgeDao(): CartridgeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: build(context).also { INSTANCE = it }
        }

        private fun build(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "bits_helper.db"
            )
                .addMigrations(MIGRATION_1_2)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Prepopulate demo data on first run
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = get(context)
                            val dao = database.cartridgeDao()
                            val demo = (1..12).map {
                                CartridgeEntity(
                                    number = (11800 + it).toString(),
                                    room = (it % 7 + 1).toString(),
                                    model = (6100 + it).toString(),
                                    date = "2024-09-25",
                                    status = if (it % 2 == 0) Status.ISSUED else Status.COLLECTED,
                                    notes = null
                                )
                            }
                            dao.insert(demo)
                        }
                    }
                })
                .build()
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cartridges ADD COLUMN status TEXT NOT NULL DEFAULT 'ISSUED'")
                db.execSQL("ALTER TABLE cartridges ADD COLUMN notes TEXT")
            }
        }
    }
}


