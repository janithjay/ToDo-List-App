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

    fun insertList(todoList: TodoList) {
        viewModelScope.launch {
            repository.insertList(todoList)
        }
    }

    // Keep this for backward compatibility
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

    fun insertItem(todoItem: TodoItem) {
        viewModelScope.launch {
            repository.insertItem(todoItem)
        }
    }

    // Keep the existing method for backward compatibility
    fun insertItem(listId: Long, description: String, position: Int) {
        viewModelScope.launch {
            repository.insertItem(TodoItem(
                id = 0,
                listId = listId,
                title = "",
                description = description,
                position = position,
                dueDate = System.currentTimeMillis(),
                dueTime = "00:00",
                completed = false
            ))
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

    fun updateItemPositions(updates: List<Pair<Long, Int>>) {
        viewModelScope.launch {
            updates.forEach { (itemId, position) ->
                repository.updateItemPosition(itemId, position)
            }
        }
    }

    private val _searchResults = MutableLiveData<SearchResult>()
    val searchResults: LiveData<SearchResult> = _searchResults

    fun searchLists(query: String) {
        viewModelScope.launch {
            val matchingLists = repository.searchLists(query)
            val matchingItems = repository.searchItems(query)
            val groupedItems = matchingItems.groupBy { it.listId }
            _searchResults.value = SearchResult(matchingLists, groupedItems)
        }
    }

    fun getTotalTaskCount(listId: Long): LiveData<Int> = repository.getTotalTaskCount(listId)

    fun getCompletedTaskCount(listId: Long): LiveData<Int> = repository.getCompletedTaskCount(listId)

    // Sorting state
    private val _currentListSortOrder = MutableLiveData<ListSortOrder>(ListSortOrder.NEWEST_FIRST)
    val currentListSortOrder: LiveData<ListSortOrder> = _currentListSortOrder

    // Sort methods for lists
    fun getListsByCurrentSort(): LiveData<List<TodoList>> {
        return when (_currentListSortOrder.value) {
            ListSortOrder.NEWEST_FIRST -> repository.getListsNewestFirst()
            ListSortOrder.OLDEST_FIRST -> repository.getListsOldestFirst()
            else -> repository.getListsNewestFirst()
        }
    }

    fun setListSortOrder(order: ListSortOrder) {
        _currentListSortOrder.value = order
    }

    // Sort order enums
    enum class ListSortOrder {
        NEWEST_FIRST,
        OLDEST_FIRST
    }

    enum class ItemSortOrder {
        NEWEST_FIRST,
        OLDEST_FIRST,
        DUE_EARLIEST,
        DUE_LATEST
    }

    private val _currentItemSortOrder = MutableLiveData<ItemSortOrder>()
    val currentItemSortOrder: LiveData<ItemSortOrder> = _currentItemSortOrder

    fun setItemSortOrder(order: ItemSortOrder) {
        _currentItemSortOrder.value = order
    }

    fun getItemsByCurrentSort(listId: Long): LiveData<List<TodoItem>> {
        return when (_currentItemSortOrder.value) {
            ItemSortOrder.NEWEST_FIRST -> repository.getItemsNewestFirst(listId)
            ItemSortOrder.OLDEST_FIRST -> repository.getItemsOldestFirst(listId)
            ItemSortOrder.DUE_EARLIEST -> repository.getItemsDueEarliest(listId)
            ItemSortOrder.DUE_LATEST -> repository.getItemsDueLatest(listId)
            null -> repository.getItemsNewestFirst(listId) // Default sorting
        }
    }

    suspend fun getListById(listId: Long): TodoList? {
        return repository.getListById(listId)
    }

    data class SearchResult(
        val lists: List<TodoList>,
        val items: Map<Long, List<TodoItem>>
    )
}