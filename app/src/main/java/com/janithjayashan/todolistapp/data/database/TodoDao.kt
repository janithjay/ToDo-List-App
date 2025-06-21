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

    @Query("SELECT * FROM todo_lists ORDER BY createdAt DESC")
    suspend fun getAllListsSync(): List<TodoList>

    @Insert
    suspend fun insertList(todoList: TodoList): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLists(lists: List<TodoList>)

    @Update
    suspend fun updateList(todoList: TodoList)

    @Delete
    suspend fun deleteList(todoList: TodoList)

    @Query("SELECT * FROM todo_lists WHERE id = :listId")
    suspend fun getListById(listId: Long): TodoList?

    // TodoItem operations
    @Query("SELECT * FROM todo_items WHERE listId = :listId ORDER BY dueDate ASC, dueTime ASC, position ASC")
    fun getItemsByListId(listId: Long): LiveData<List<TodoItem>>

    @Query("SELECT * FROM todo_items")
    suspend fun getAllItemsSync(): List<TodoItem>

    @Insert
    suspend fun insertItem(todoItem: TodoItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllItems(items: List<TodoItem>)

    @Update
    suspend fun updateItem(todoItem: TodoItem)

    @Delete
    suspend fun deleteItem(todoItem: TodoItem)

    @Query("DELETE FROM todo_lists")
    suspend fun clearAllData()

    @Query("DELETE FROM todo_items WHERE listId = :listId")
    suspend fun deleteItemsByListId(listId: Long)

    @Query("UPDATE todo_items SET position = :newPosition WHERE id = :itemId")
    suspend fun updateItemPosition(itemId: Long, newPosition: Int)

    @Query("SELECT * FROM todo_lists WHERE title LIKE '%' || :searchQuery || '%'")
    suspend fun searchLists(searchQuery: String): List<TodoList>

    @Query("SELECT * FROM todo_items WHERE title LIKE '%' || :searchQuery || '%'")
    suspend fun searchItems(searchQuery: String): List<TodoItem>
}