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
    entities = [CartridgeEntity::class, DepartmentEntity::class, CartridgeHistoryEntity::class],
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cartridgeDao(): CartridgeDao
    abstract fun departmentDao(): DepartmentDao
    abstract fun cartridgeHistoryDao(): CartridgeHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: build(context).also { INSTANCE = it }
        }
        
        /**
         * Принудительно пересоздает подключение к базе данных
         * Используется после синхронизации или импорта
         */
        fun forceReconnect(context: Context): AppDatabase = synchronized(this) {
            // Закрываем текущее подключение
            INSTANCE?.close()
            INSTANCE = null
            
            // Создаем новое подключение
            build(context).also { INSTANCE = it }
        }


        private fun build(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "bits_helper.db"
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Инициализация базовых подразделений для новых пользователей
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = get(context)
                            val departmentDao = database.departmentDao()
                            
                            // Добавляем только базовые подразделения без демо-картриджей
                            val departments = listOf(
                                DepartmentEntity("IT-отдел", "101,102,103"),
                                DepartmentEntity("Бухгалтерия", "201,202,203"),
                                DepartmentEntity("Отдел кадров", "301,302,303"),
                                DepartmentEntity("Склад", "401,402,403")
                            )
                            departmentDao.insert(departments)
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
                
                // Добавляем базовые подразделения только если таблица пуста
                db.execSQL("INSERT OR IGNORE INTO departments (name, rooms) VALUES ('IT-отдел', '101,102,103')")
                db.execSQL("INSERT OR IGNORE INTO departments (name, rooms) VALUES ('Бухгалтерия', '201,202,203')")
                db.execSQL("INSERT OR IGNORE INTO departments (name, rooms) VALUES ('Отдел кадров', '301,302,303')")
                db.execSQL("INSERT OR IGNORE INTO departments (name, rooms) VALUES ('Склад', '401,402,403')")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS cartridge_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        cartridgeNumber TEXT NOT NULL,
                        date TEXT NOT NULL,
                        status TEXT NOT NULL,
                        room TEXT,
                        model TEXT
                    )
                """)
            }
        }
    }
}


