package com.janithjayashan.todolistapp.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.janithjayashan.todolistapp.data.database.entities.TodoItem
import com.janithjayashan.todolistapp.data.database.entities.TodoList

@Database(
    entities = [TodoList::class, TodoItem::class],
    version = 4,
    exportSchema = false
)
abstract class TodoDatabase : RoomDatabase() {

    abstract fun todoDao(): TodoDao

    companion object {
        @Volatile
        private var INSTANCE: TodoDatabase? = null

        fun getDatabase(context: Context): TodoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TodoDatabase::class.java,
                    "todo_database"
                )
                .addMigrations(
                    // Migration from 1 to 2
                    object : androidx.room.migration.Migration(1, 2) {
                        override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                            // Create new todo_lists table with all required columns
                            database.execSQL("""
                                CREATE TABLE IF NOT EXISTS todo_lists_new (
                                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                    title TEXT NOT NULL,
                                    description TEXT NOT NULL DEFAULT '',
                                    selectedDate INTEGER NOT NULL DEFAULT 0,
                                    selectedTime TEXT NOT NULL DEFAULT '00:00',
                                    createdAt INTEGER NOT NULL DEFAULT 0
                                )
                            """)

                            // Copy data from old table
                            database.execSQL("""
                                INSERT INTO todo_lists_new (id, title)
                                SELECT id, title FROM todo_lists
                            """)

                            // Drop old table and rename new one
                            database.execSQL("DROP TABLE IF EXISTS todo_lists")
                            database.execSQL("ALTER TABLE todo_lists_new RENAME TO todo_lists")
                        }
                    },
                    // Migration from 2 to 3
                    object : androidx.room.migration.Migration(2, 3) {
                        override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                            // Drop existing todo_items table and its index if they exist
                            database.execSQL("DROP INDEX IF EXISTS index_todo_items_listId")
                            database.execSQL("DROP TABLE IF EXISTS todo_items")

                            // Create new todo_items table with all required columns and constraints
                            database.execSQL("""
                                CREATE TABLE todo_items (
                                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                    listId INTEGER NOT NULL,
                                    description TEXT NOT NULL,
                                    position INTEGER NOT NULL,
                                    completed INTEGER NOT NULL DEFAULT 0,
                                    FOREIGN KEY(listId) REFERENCES todo_lists(id) ON DELETE CASCADE
                                )
                            """)

                            // Create the index for listId
                            database.execSQL("CREATE INDEX index_todo_items_listId ON todo_items(listId)")
                        }
                    },
                    // Migration from 3 to 4
                    object : androidx.room.migration.Migration(3, 4) {
                        override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                            // Drop existing table and index
                            database.execSQL("DROP INDEX IF EXISTS index_todo_items_listId")
                            database.execSQL("DROP TABLE IF EXISTS todo_items_new")

                            // Create new table with all columns
                            database.execSQL("""
                                CREATE TABLE todo_items_new (
                                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                    listId INTEGER NOT NULL,
                                    title TEXT NOT NULL,
                                    description TEXT NOT NULL,
                                    position INTEGER NOT NULL,
                                    completed INTEGER NOT NULL,
                                    dueDate INTEGER NOT NULL,
                                    dueTime TEXT NOT NULL,
                                    FOREIGN KEY(listId) REFERENCES todo_lists(id) ON DELETE CASCADE
                                )
                            """)

                            // Copy data from old table with default values for new columns
                            database.execSQL("""
                                INSERT INTO todo_items_new (
                                    id, 
                                    listId, 
                                    title,
                                    description, 
                                    position, 
                                    completed,
                                    dueDate,
                                    dueTime
                                )
                                SELECT 
                                    id, 
                                    listId, 
                                    '',
                                    description, 
                                    position, 
                                    completed,
                                    0,
                                    '00:00'
                                FROM todo_items
                            """)

                            // Drop old table and rename new one
                            database.execSQL("DROP TABLE todo_items")
                            database.execSQL("ALTER TABLE todo_items_new RENAME TO todo_items")

                            // Create the index exactly as Room expects it
                            database.execSQL("CREATE INDEX index_todo_items_listId ON todo_items(listId)")
                        }
                    }
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}