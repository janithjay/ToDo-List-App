package com.janithjayashan.todolistapp.data.repository

import androidx.lifecycle.LiveData
import com.janithjayashan.todolistapp.data.database.TodoDao
import com.janithjayashan.todolistapp.data.database.entities.TodoItem
import com.janithjayashan.todolistapp.data.database.entities.TodoList

class TodoRepository(private val todoDao: TodoDao) {

    fun getAllLists(): LiveData<List<TodoList>> = todoDao.getAllLists()

    fun getItemsByListId(listId: Long): LiveData<List<TodoItem>> = todoDao.getItemsByListId(listId)

    suspend fun insertList(todoList: TodoList): Long = todoDao.insertList(todoList)

    suspend fun updateList(todoList: TodoList) = todoDao.updateList(todoList)

    suspend fun deleteList(todoList: TodoList) = todoDao.deleteList(todoList)

    suspend fun getListById(listId: Long): TodoList? = todoDao.getListById(listId)

    suspend fun insertItem(todoItem: TodoItem): Long = todoDao.insertItem(todoItem)

    suspend fun updateItem(todoItem: TodoItem) = todoDao.updateItem(todoItem)

    suspend fun deleteItem(todoItem: TodoItem) = todoDao.deleteItem(todoItem)

    suspend fun updateItemPosition(itemId: Long, newPosition: Int) =
        todoDao.updateItemPosition(itemId, newPosition)

    suspend fun searchLists(searchQuery: String): List<TodoList> = todoDao.searchLists(searchQuery)

    suspend fun searchItems(searchQuery: String): List<TodoItem> = todoDao.searchItems(searchQuery)

    fun getTotalTaskCount(listId: Long): LiveData<Int> = todoDao.getTotalTaskCount(listId)

    fun getCompletedTaskCount(listId: Long): LiveData<Int> = todoDao.getCompletedTaskCount(listId)
}