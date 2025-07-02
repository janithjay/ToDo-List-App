package com.janithjayashan.todolistapp.data.models

import com.janithjayashan.todolistapp.data.database.entities.TodoItem
import com.janithjayashan.todolistapp.data.database.entities.TodoList

data class SearchResult(
    val matchingLists: List<TodoList>,
    val matchingItems: Map<Long, List<TodoItem>> // Map listId to matching items
)
