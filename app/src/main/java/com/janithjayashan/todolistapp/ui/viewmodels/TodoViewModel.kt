package com.janithjayashan.todolistapp.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.janithjayashan.todolistapp.data.database.TodoDatabase
import com.janithjayashan.todolistapp.data.database.entities.TodoItem
import com.janithjayashan.todolistapp.data.database.entities.TodoList
import com.janithjayashan.todolistapp.data.repository.TodoRepository
import kotlinx.coroutines.launch

class TodoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TodoRepository

    init {
        val todoDao = TodoDatabase.getDatabase(application).todoDao()
        repository = TodoRepository(todoDao)
    }

    fun getAllLists(): LiveData<List<TodoList>> = repository.getAllLists()

    fun getItemsByListId(listId: Long): LiveData<List<TodoItem>> = repository.getItemsByListId(listId)

    fun insertList(title: String) {
        viewModelScope.launch {
            repository.insertList(TodoList(title = title))
        }
    }

    fun updateList(todoList: TodoList) {
        viewModelScope.launch {
            repository.updateList(todoList)
        }
    }

    fun deleteList(todoList: TodoList) {
        viewModelScope.launch {
            repository.deleteList(todoList)
        }
    }

    fun insertItem(listId: Long, description: String, position: Int) {
        viewModelScope.launch {
            repository.insertItem(TodoItem(listId = listId, description = description, position = position))
        }
    }

    fun updateItem(todoItem: TodoItem) {
        viewModelScope.launch {
            repository.updateItem(todoItem)
        }
    }

    fun deleteItem(todoItem: TodoItem) {
        viewModelScope.launch {
            repository.deleteItem(todoItem)
        }
    }

    fun updateItemPosition(itemId: Long, newPosition: Int) {
        viewModelScope.launch {
            repository.updateItemPosition(itemId, newPosition)
        }
    }

    private val _searchResults = MutableLiveData<List<TodoList>>()
    val searchResults: LiveData<List<TodoList>> = _searchResults

    fun searchLists(query: String) {
        viewModelScope.launch {
            _searchResults.value = repository.searchLists(query)
        }
    }
}