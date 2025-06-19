package com.janithjayashan.todolistapp.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todo_lists")
data class TodoList(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val selectedDate: Long = System.currentTimeMillis(),
    val selectedTime: String = "",
    val createdAt: Long = System.currentTimeMillis()
)