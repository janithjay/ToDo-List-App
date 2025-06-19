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
import com.google.firebase.database.GenericTypeIndicator

class FirebaseBackupManager(private val context: Context) {
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val pendingBackups = mutableListOf<suspend () -> Unit>()

    // Check if user is logged in
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    // Get current user ID
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

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
                val lists = todoDao.getAllListsSync()
                val items = todoDao.getAllItemsSync()

                val userRef = database.getReference("users").child(userId)
                
                // Backup lists with all fields
                val listsData = lists.map { list ->
                    mapOf(
                        "id" to list.id,
                        "title" to list.title,
                        "description" to list.description,
                        "selectedDate" to list.selectedDate,
                        "selectedTime" to list.selectedTime,
                        "createdAt" to list.createdAt
                    )
                }
                userRef.child("lists").setValue(listsData).await()

                // Backup items
                val itemsData = items.map { item ->
                    mapOf(
                        "id" to item.id,
                        "listId" to item.listId,
                        "description" to item.description,
                        "position" to item.position,
                        "completed" to item.completed
                    )
                }
                userRef.child("items").setValue(itemsData).await()

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

    suspend fun restoreFromFirebase() {
        try {
            val userId = getCurrentUserId()
            if (!isNetworkAvailable()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "No internet connection available", Toast.LENGTH_SHORT).show()
                }
                return
            }

            if (userId != null) {
                val userRef = database.getReference("users").child(userId)
                val dataSnapshot = userRef.get().await()

                if (dataSnapshot.exists()) {
                    val todoDao = TodoDatabase.getDatabase(context).todoDao()

                    // Clear existing data
                    todoDao.deleteAllLists()
                    todoDao.deleteAllItems()

                    // Restore lists with all fields
                    val listsSnapshot = dataSnapshot.child("lists")
                    val lists = mutableListOf<TodoList>()
                    for (listSnapshot in listsSnapshot.children) {
                        val map = listSnapshot.getValue(object : GenericTypeIndicator<Map<String, Any>>() {})
                        if (map != null) {
                            lists.add(TodoList(
                                id = (map["id"] as? Long) ?: 0L,
                                title = (map["title"] as? String) ?: "",
                                description = (map["description"] as? String) ?: "",
                                selectedDate = (map["selectedDate"] as? Long) ?: System.currentTimeMillis(),
                                selectedTime = (map["selectedTime"] as? String) ?: "",
                                createdAt = (map["createdAt"] as? Long) ?: System.currentTimeMillis()
                            ))
                        }
                    }
                    todoDao.insertAllLists(lists)

                    // Restore items
                    val itemsSnapshot = dataSnapshot.child("items")
                    val items = mutableListOf<TodoItem>()
                    for (itemSnapshot in itemsSnapshot.children) {
                        val map = itemSnapshot.getValue(object : GenericTypeIndicator<Map<String, Any>>() {})
                        if (map != null) {
                            items.add(TodoItem(
                                id = (map["id"] as? Long) ?: 0L,
                                listId = (map["listId"] as? Long) ?: 0L,
                                description = (map["description"] as? String) ?: "",
                                position = ((map["position"] as? Long)?.toInt()) ?: 0,
                                completed = (map["completed"] as? Boolean) ?: false
                            ))
                        }
                    }
                    todoDao.insertAllItems(items)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Restore completed successfully", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "No backup data found", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Restore failed: ${e.message}", Toast.LENGTH_SHORT).show()
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
            if (userId == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "No user logged in", Toast.LENGTH_SHORT).show()
                }
                return
            }

            if (!isNetworkAvailable()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "No internet connection available", Toast.LENGTH_SHORT).show()
                }
                return
            }

            val userRef = database.getReference("users").child(userId)
            val dataSnapshot = userRef.get().await()

            if (dataSnapshot.exists() && !dataSnapshot.child("lists").exists()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "No backup data found for this user", Toast.LENGTH_SHORT).show()
                }
                return
            }

            val todoDao = TodoDatabase.getDatabase(context).todoDao()

            if (dataSnapshot.exists()) {
                // Clear existing data before restoring
                todoDao.clearAllData()

                // Restore lists with all fields
                val listsSnapshot = dataSnapshot.child("lists")
                val lists = mutableListOf<TodoList>()
                for (listSnapshot in listsSnapshot.children) {
                    val map = listSnapshot.getValue(object : GenericTypeIndicator<Map<String, Any>>() {})
                    if (map != null) {
                        lists.add(TodoList(
                            id = (map["id"] as? Long) ?: 0L,
                            title = (map["title"] as? String) ?: "",
                            description = (map["description"] as? String) ?: "",
                            selectedDate = (map["selectedDate"] as? Long) ?: System.currentTimeMillis(),
                            selectedTime = (map["selectedTime"] as? String) ?: "",
                            createdAt = (map["createdAt"] as? Long) ?: System.currentTimeMillis()
                        ))
                    }
                }

                if (lists.isNotEmpty()) {
                    todoDao.insertAllLists(lists)
                }

                // Restore items if they exist
                val itemsSnapshot = dataSnapshot.child("items")
                if (itemsSnapshot.exists()) {
                    val items = mutableListOf<TodoItem>()
                    for (itemSnapshot in itemsSnapshot.children) {
                        val map = itemSnapshot.getValue(object : GenericTypeIndicator<Map<String, Any>>() {})
                        if (map != null) {
                            items.add(TodoItem(
                                id = (map["id"] as? Long) ?: 0L,
                                listId = (map["listId"] as? Long) ?: 0L,
                                description = (map["description"] as? String) ?: "",
                                position = ((map["position"] as? Long)?.toInt()) ?: 0,
                                completed = (map["completed"] as? Boolean) ?: false
                            ))
                        }
                    }
                    if (items.isNotEmpty()) {
                        todoDao.insertAllItems(items)
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "User data restored successfully", Toast.LENGTH_SHORT).show()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Starting with fresh data for new user", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Restore failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun handleBackupFailure(e: Exception) {
        showToast("Backup failed: ${e.message}")
    }

    private suspend fun handleRestoreFailure(e: Exception) {
        showToast("Restore failed: ${e.message}")
    }

    private suspend fun showToast(message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Call this when network becomes available
    fun processPendingBackups() {
        if (isNetworkAvailable() && pendingBackups.isNotEmpty()) {
            scope.launch {
                pendingBackups.forEach { pendingBackup ->
                    try {
                        pendingBackup()
                    } catch (e: Exception) {
                        handleBackupFailure(e)
                    }
                }
                pendingBackups.clear()
            }
        }
    }

    fun onDestroy() {
        scope.cancel()
    }
}