package com.janithjayashan.todolistapp.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.janithjayashan.todolistapp.data.database.entities.TodoItem
import com.janithjayashan.todolistapp.data.database.entities.TodoList

@Database(
    entities = [TodoList::class, TodoItem::class],
    version = 3,
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
                    object : androidx.room.migration.Migration(2, 3) {
                        override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                            // Create temporary table for todo_items
                            database.execSQL("""
                                CREATE TABLE IF NOT EXISTS todo_items_new (
                                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                    listId INTEGER NOT NULL,
                                    description TEXT NOT NULL,
                                    position INTEGER NOT NULL,
                                    completed INTEGER NOT NULL DEFAULT 0,
                                    FOREIGN KEY(listId) REFERENCES todo_lists(id) ON DELETE CASCADE
                                )
                            """)

                            // Copy data from old table to new table, converting isCompleted to completed
                            database.execSQL("""
                                INSERT INTO todo_items_new (id, listId, description, position, completed)
                                SELECT id, listId, description, position, isCompleted
                                FROM todo_items
                            """)

                            // Drop old table
                            database.execSQL("DROP TABLE todo_items")

                            // Rename new table to todo_items
                            database.execSQL("ALTER TABLE todo_items_new RENAME TO todo_items")

                            // Recreate the index
                            database.execSQL("CREATE INDEX IF NOT EXISTS index_todo_items_listId ON todo_items(listId)")

                            // Add new columns to todo_lists table
                            database.execSQL("ALTER TABLE todo_lists ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                            database.execSQL("ALTER TABLE todo_lists ADD COLUMN selectedDate INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
                            database.execSQL("ALTER TABLE todo_lists ADD COLUMN selectedTime TEXT NOT NULL DEFAULT ''")
                        }
                    }
                )
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}