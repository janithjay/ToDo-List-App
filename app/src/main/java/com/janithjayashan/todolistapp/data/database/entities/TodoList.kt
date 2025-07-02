package com.janithjayashan.todolistapp.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Ignore
import com.janithjayashan.todolistapp.data.database.entities.TodoItem

@Entity(tableName = "todo_lists")
data class TodoList(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    @Ignore
    var matchingItems: List<TodoItem> = emptyList()
}
