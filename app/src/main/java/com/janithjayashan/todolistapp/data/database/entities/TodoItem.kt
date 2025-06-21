package com.janithjayashan.todolistapp.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "todo_items",
    foreignKeys = [ForeignKey(
        entity = TodoList::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("listId"),
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("listId")]
)
data class TodoItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val listId: Long,
    val title: String,
    val description: String = "",
    val dueDate: Long = System.currentTimeMillis(),
    val dueTime: String = "00:00",
    val position: Int = 0,
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)