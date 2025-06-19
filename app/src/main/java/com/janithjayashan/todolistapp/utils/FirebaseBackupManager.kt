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
    private val database: FirebaseDatabase

    init {
        // Initialize Firebase with persistence
        FirebaseDatabase.getInstance().apply {
            setPersistenceEnabled(true)
            database = this
        }
    }
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
            if (userId == null) {
                showToast("Please log in to backup your data")
                return
            }
            performBackup(userId)
        } catch (e: Exception) {
            handleBackupFailure(e)
        }
    }

    private suspend fun performBackup(userId: String) {
        val backupRef = database.reference.child("user_backups").child(userId)
        val todoDao = TodoDatabase.getDatabase(context).todoDao()

        try {
            // Get all lists using the direct method
            val lists = todoDao.getAllListsForBackup()

            // Get all items for each list using the new direct method
            val allItems = mutableListOf<TodoItem>()
            for (list in lists) {
                val items = todoDao.getItemsByListIdForBackup(list.id)
                allItems.addAll(items)
            }

            if (lists.isEmpty()) {
                showToast("No data to backup")
                return
            }

            // Convert lists and items to serializable maps
            val serializedLists = lists.map { list ->
                mapOf(
                    "id" to list.id,
                    "title" to list.title,
                    "createdAt" to list.createdAt
                )
            }

            val serializedItems = allItems.map { item ->
                mapOf(
                    "id" to item.id,
                    "listId" to item.listId,
                    "description" to item.description,
                    "isCompleted" to item.isCompleted,
                    "position" to item.position,
                    "createdAt" to item.createdAt
                )
            }

            // Create the backup data
            val backupData = mapOf(
                "lists" to serializedLists,
                "items" to serializedItems,
                "timestamp" to System.currentTimeMillis()
            )

            if (isNetworkAvailable()) {
                backupRef.setValue(backupData).await()
                showToast("Backup successful: ${lists.size} lists and ${allItems.size} items")
            } else {
                pendingBackups.add { backupRef.setValue(backupData).await() }
                showToast("Backup queued for when network is available")
            }
        } catch (e: Exception) {
            showToast("Backup failed: ${e.localizedMessage ?: "Unknown error"}")
            e.printStackTrace()
        }
    }

    suspend fun restoreFromFirebase() {
        try {
            val userId = getCurrentUserId()
            if (userId == null) {
                showToast("Please log in to restore your data")
                return
            }
            performRestore(userId)
        } catch (e: Exception) {
            handleRestoreFailure(e)
        }
    }

    private suspend fun performRestore(userId: String) {
        val backupRef = database.reference.child("user_backups").child(userId)

        try {
            val snapshot = backupRef.get().await()
            if (!snapshot.exists()) {
                showToast("No backup found")
                return
            }

            val listsSnapshot = snapshot.child("lists")
            val itemsSnapshot = snapshot.child("items")

            if (!listsSnapshot.exists()) {
                showToast("No lists found in backup")
                return
            }

            val todoDao = TodoDatabase.getDatabase(context).todoDao()

            try {
                // Clear existing data
                todoDao.clearAllData()

                // Restore lists
                val lists = mutableListOf<TodoList>()
                listsSnapshot.children.forEach { listSnapshot ->
                    try {
                        val list = TodoList(
                            id = (listSnapshot.child("id").value as Number).toLong(),
                            title = listSnapshot.child("title").value as String,
                            createdAt = (listSnapshot.child("createdAt").value as Number?)?.toLong()
                                ?: System.currentTimeMillis()
                        )
                        lists.add(list)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        showToast("Error parsing list: ${e.localizedMessage}")
                    }
                }

                // Restore items
                val items = mutableListOf<TodoItem>()
                itemsSnapshot.children.forEach { itemSnapshot ->
                    try {
                        val item = TodoItem(
                            id = (itemSnapshot.child("id").value as Number).toLong(),
                            listId = (itemSnapshot.child("listId").value as Number).toLong(),
                            description = itemSnapshot.child("description").value as String,
                            isCompleted = itemSnapshot.child("isCompleted").value as Boolean,
                            position = (itemSnapshot.child("position").value as Number).toInt(),
                            createdAt = (itemSnapshot.child("createdAt").value as Number?)?.toLong()
                                ?: System.currentTimeMillis()
                        )
                        items.add(item)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        showToast("Error parsing item: ${e.localizedMessage}")
                    }
                }

                // Insert the restored data
                if (lists.isNotEmpty()) {
                    lists.forEach { list ->
                        todoDao.insertList(list)
                    }
                    items.forEach { item ->
                        todoDao.insertItem(item)
                    }
                    showToast("Restore successful: ${lists.size} lists and ${items.size} items")
                } else {
                    showToast("No valid lists found in backup")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Database error: ${e.localizedMessage}")
                throw e
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Restore failed: ${e.localizedMessage}")
            throw e
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