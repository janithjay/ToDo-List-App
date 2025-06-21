package com.janithjayashan.todolistapp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth
import com.janithjayashan.todolistapp.data.database.TodoDatabase
import com.janithjayashan.todolistapp.data.database.entities.TodoItem
import com.janithjayashan.todolistapp.data.database.entities.TodoList
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class FirebaseBackupManager(private val context: Context) {
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    // Check if user is logged in
    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    // Get current user ID
    fun getCurrentUserId(): String? = auth.currentUser?.uid

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null && (
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        )
    }

    suspend fun backupToFirebase() {
        try {
            val userId = getCurrentUserId()
            if (!isNetworkAvailable()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "No internet connection available", Toast.LENGTH_SHORT).show()
                }
                return
            }

            if (userId != null) {
                val todoDao = TodoDatabase.getDatabase(context).todoDao()

                // Get all lists and items using sync methods
                val lists = todoDao.getAllListsSync()
                val items = todoDao.getAllItemsSync()

                // Create reference to user's data
                val userRef = database.reference.child("users").child(userId)

                // Backup lists with all fields
                val listsToBackup = lists.map { list ->
                    mapOf<String, Any>(
                        "id" to list.id,
                        "title" to list.title,
                        "createdAt" to list.createdAt
                    )
                }
                userRef.child("lists").setValue(listsToBackup).await()

                // Backup items with all fields
                val itemsToBackup = items.map { item ->
                    mapOf<String, Any>(
                        "id" to item.id,
                        "listId" to item.listId,
                        "title" to item.title,
                        "description" to item.description,
                        "dueDate" to item.dueDate,
                        "dueTime" to item.dueTime,
                        "position" to item.position,
                        "completed" to item.completed
                    )
                }
                userRef.child("items").setValue(itemsToBackup).await()

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Backup completed successfully", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    suspend fun clearLocalData() {
        val todoDao = TodoDatabase.getDatabase(context).todoDao()
        todoDao.clearAllData()
    }

    suspend fun restoreUserData() {
        try {
            val userId = getCurrentUserId()
            if (!isNetworkAvailable()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "No internet connection available", Toast.LENGTH_SHORT).show()
                }
                return
            }

            if (userId != null) {
                val userRef = database.reference.child("users").child(userId)
                val dataSnapshot = userRef.get().await()

                val todoDao = TodoDatabase.getDatabase(context).todoDao()

                // Clear existing data
                todoDao.clearAllData()

                // Restore lists
                val listsSnapshot = dataSnapshot.child("lists")
                val lists = mutableListOf<TodoList>()
                listsSnapshot.children.forEach { listSnapshot ->
                    val id = listSnapshot.child("id").getValue(Long::class.java) ?: 0L
                    val title = listSnapshot.child("title").getValue(String::class.java) ?: ""
                    val description = listSnapshot.child("description").getValue(String::class.java) ?: ""
                    val selectedDate = listSnapshot.child("selectedDate").getValue(Long::class.java) ?: 0L
                    val selectedTime = listSnapshot.child("selectedTime").getValue(Long::class.java) ?: 0L
                    val createdAt = listSnapshot.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()

                    lists.add(TodoList(
                        id = id,
                        title = title,
                        createdAt = createdAt
                    ))
                }
                if (lists.isNotEmpty()) {
                    todoDao.insertAllLists(lists)
                }

                // Restore items
                val itemsSnapshot = dataSnapshot.child("items")
                val items = mutableListOf<TodoItem>()
                itemsSnapshot.children.forEach { itemSnapshot ->
                    val id = itemSnapshot.child("id").getValue(Long::class.java) ?: 0L
                    val listId = itemSnapshot.child("listId").getValue(Long::class.java) ?: 0L
                    val title = itemSnapshot.child("title").getValue(String::class.java) ?: ""
                    val description = itemSnapshot.child("description").getValue(String::class.java) ?: ""
                    val dueDate = itemSnapshot.child("dueDate").getValue(Long::class.java) ?: System.currentTimeMillis()
                    val dueTime = itemSnapshot.child("dueTime").getValue(String::class.java) ?: "00:00"
                    val position = itemSnapshot.child("position").getValue(Int::class.java) ?: 0
                    val completed = itemSnapshot.child("completed").getValue(Boolean::class.java) ?: false

                    items.add(TodoItem(
                        id = id,
                        listId = listId,
                        title = title,
                        description = description,
                        dueDate = dueDate,
                        dueTime = dueTime,
                        position = position,
                        completed = completed
                    ))
                }
                if (items.isNotEmpty()) {
                    todoDao.insertAllItems(items)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Data restored successfully", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Restore failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun processPendingBackups() {
        if (isUserLoggedIn() && isNetworkAvailable()) {
            scope.launch {
                try {
                    backupToFirebase()
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to process pending backups", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun onDestroy() {
        scope.cancel()
    }
}