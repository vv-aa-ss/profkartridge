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
    entities = [CartridgeEntity::class, DepartmentEntity::class],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cartridgeDao(): CartridgeDao
    abstract fun departmentDao(): DepartmentDao

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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Prepopulate demo data on first run
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = get(context)
                            val cartridgeDao = database.cartridgeDao()
                            val departmentDao = database.departmentDao()
                            
                            // Инициализация подразделений
                            val departments = listOf(
                                DepartmentEntity("СУ-201", "428,429,430,421,415"),
                                DepartmentEntity("СУ-202", "301,302,303,304,305"),
                                DepartmentEntity("СУ-203", "201,202,203,204,205"),
                                DepartmentEntity("СУ-204", "101,102,103,104,105")
                            )
                            departmentDao.insert(departments)
                            
                            val demo = (1..12).map {
                                CartridgeEntity(
                                    number = (11800 + it).toString(),
                                    room = (it % 7 + 1).toString(),
                                    model = (6100 + it).toString(),
                                    date = "2024-09-25",
                                    status = if (it % 2 == 0) Status.ISSUED else Status.COLLECTED,
                                    notes = null,
                                    department = null
                                )
                            }
                            cartridgeDao.insert(demo)
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Добавляем поле department в таблицу cartridges
                db.execSQL("ALTER TABLE cartridges ADD COLUMN department TEXT")
                
                // Создаем таблицу departments
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS departments (
                        name TEXT NOT NULL PRIMARY KEY,
                        rooms TEXT NOT NULL
                    )
                """)
                
                // Инициализируем подразделения
                db.execSQL("INSERT OR IGNORE INTO departments (name, rooms) VALUES ('СУ-201', '428,429,430,421,415')")
                db.execSQL("INSERT OR IGNORE INTO departments (name, rooms) VALUES ('СУ-202', '301,302,303,304,305')")
                db.execSQL("INSERT OR IGNORE INTO departments (name, rooms) VALUES ('СУ-203', '201,202,203,204,205')")
                db.execSQL("INSERT OR IGNORE INTO departments (name, rooms) VALUES ('СУ-204', '101,102,103,104,105')")
            }
        }
    }
}


