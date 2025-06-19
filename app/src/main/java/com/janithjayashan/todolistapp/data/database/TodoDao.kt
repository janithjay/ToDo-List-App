package com.janithjayashan.todolistapp.data.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.janithjayashan.todolistapp.data.database.entities.TodoItem
import com.janithjayashan.todolistapp.data.database.entities.TodoList

@Dao
interface TodoDao {

    // TodoList operations
    @Query("SELECT * FROM todo_lists ORDER BY createdAt DESC")
    fun getAllLists(): LiveData<List<TodoList>>

    @Insert
    suspend fun insertList(todoList: TodoList): Long

    @Update
    suspend fun updateList(todoList: TodoList)

    @Delete
    suspend fun deleteList(todoList: TodoList)

    @Query("SELECT * FROM todo_lists WHERE id = :listId")
    suspend fun getListById(listId: Long): TodoList?

    // TodoItem operations
    @Query("SELECT * FROM todo_items WHERE listId = :listId ORDER BY position ASC")
    fun getItemsByListId(listId: Long): LiveData<List<TodoItem>>

    @Insert
    suspend fun insertItem(todoItem: TodoItem): Long

    @Update
    suspend fun updateItem(todoItem: TodoItem)

    @Delete
    suspend fun deleteItem(todoItem: TodoItem)

    @Query("UPDATE todo_items SET position = :newPosition WHERE id = :itemId")
    suspend fun updateItemPosition(itemId: Long, newPosition: Int)

    // Search functionality
    @Query("SELECT DISTINCT todo_lists.* FROM todo_lists INNER JOIN todo_items ON todo_lists.id = todo_items.listId WHERE todo_items.description LIKE '%' || :searchQuery || '%' OR todo_lists.title LIKE '%' || :searchQuery || '%'")
    suspend fun searchLists(searchQuery: String): List<TodoList>

    @Query("SELECT * FROM todo_items WHERE description LIKE '%' || :searchQuery || '%'")
    suspend fun searchItems(searchQuery: String): List<TodoItem>

    // Backup and Restore functionality
    @Query("DELETE FROM todo_lists")
    suspend fun deleteAllLists()

    @Query("DELETE FROM todo_items")
    suspend fun deleteAllItems()

    @Query("SELECT * FROM todo_lists ORDER BY createdAt DESC")
    suspend fun getAllListsSync(): List<TodoList>

    @Query("SELECT * FROM todo_items ORDER BY listId, position")
    suspend fun getAllItemsSync(): List<TodoItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLists(lists: List<TodoList>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllItems(items: List<TodoItem>)

    @Transaction
    suspend fun clearAllData() {
        deleteAllItems()
        deleteAllLists()
    }

    // Backup specific operations that return direct lists instead of LiveData
    @Query("SELECT * FROM todo_lists ORDER BY createdAt DESC")
    suspend fun getAllListsForBackup(): List<TodoList>

    @Query("SELECT * FROM todo_items WHERE listId = :listId ORDER BY position ASC")
    suspend fun getItemsByListIdForBackup(listId: Long): List<TodoItem>
}