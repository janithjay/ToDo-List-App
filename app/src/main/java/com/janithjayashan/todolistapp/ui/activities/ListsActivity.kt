package com.janithjayashan.todolistapp.ui.activities

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.janithjayashan.todolistapp.R
import com.janithjayashan.todolistapp.data.database.entities.TodoList
import com.janithjayashan.todolistapp.ui.adapters.TodoListsAdapter
import com.janithjayashan.todolistapp.ui.viewmodels.TodoViewModel
import com.janithjayashan.todolistapp.utils.FirebaseBackupManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ListsActivity : AppCompatActivity() {

    private lateinit var viewModel: TodoViewModel
    private lateinit var adapter: TodoListsAdapter
    private lateinit var backupManager: FirebaseBackupManager
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private var allLists = listOf<TodoList>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lists)

        supportActionBar?.title = "Your Lists"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProvider(this)[TodoViewModel::class.java]
        backupManager = FirebaseBackupManager(this)

        setupRecyclerView()
        setupSearchView()
        setupFab()
        observeLists()

        // Handle back press with the new recommended way
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    private fun setupSearchView() {
        val searchView = findViewById<androidx.appcompat.widget.SearchView>(R.id.searchView)
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterLists(newText)
                return true
            }
        })
    }

    private fun filterLists(query: String?) {
        if (query.isNullOrBlank()) {
            adapter.submitList(allLists)
        } else {
            val filteredList = allLists.filter { todoList ->
                todoList.title.contains(query, ignoreCase = true)
            }
            adapter.submitList(filteredList)
        }
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewLists)
        adapter = TodoListsAdapter(
            onListClick = { todoList ->
                val intent = Intent(this, TodoListDetailActivity::class.java)
                intent.putExtra("LIST_ID", todoList.id)
                intent.putExtra("LIST_TITLE", todoList.title)
                startActivity(intent)
            },
            onDeleteClick = { todoList ->
                showDeleteConfirmation(todoList)
            },
            onEditClick = { todoList ->
                showEditListDialog(todoList)
            },
            getTotalTasks = { listId -> viewModel.getTotalTaskCount(listId) },
            getCompletedTasks = { listId -> viewModel.getCompletedTaskCount(listId) }
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupFab() {
        val fab = findViewById<FloatingActionButton>(R.id.fabAddList)
        fab.setOnClickListener {
            showAddListDialog()
        }
    }

    private fun observeLists() {
        viewModel.getAllLists().observe(this) { lists ->
            allLists = lists
            adapter.submitList(lists)
        }
    }

    private fun showAddListDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_list, null)
        val titleEdit = dialogView.findViewById<EditText>(R.id.etTitle)

        // Hide unnecessary views
        dialogView.findViewById<EditText>(R.id.etDescription)?.visibility = android.view.View.GONE
        dialogView.findViewById<Button>(R.id.btnSelectDate)?.visibility = android.view.View.GONE
        dialogView.findViewById<Button>(R.id.btnSelectTime)?.visibility = android.view.View.GONE
        dialogView.findViewById<TextView>(R.id.tvSelectedDateTime)?.visibility = android.view.View.GONE

        AlertDialog.Builder(this, R.style.NeonDialog)
            .setTitle("Add New List")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val title = titleEdit.text.toString().trim()
                if (title.isNotEmpty()) {
                    val todoList = TodoList(
                        title = title
                    )
                    viewModel.insertList(todoList)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditListDialog(todoList: TodoList) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_list, null)
        val titleEdit = dialogView.findViewById<EditText>(R.id.etTitle)

        // Hide unnecessary views
        dialogView.findViewById<EditText>(R.id.etDescription)?.visibility = android.view.View.GONE
        dialogView.findViewById<Button>(R.id.btnSelectDate)?.visibility = android.view.View.GONE
        dialogView.findViewById<Button>(R.id.btnSelectTime)?.visibility = android.view.View.GONE
        dialogView.findViewById<TextView>(R.id.tvSelectedDateTime)?.visibility = android.view.View.GONE

        titleEdit.setText(todoList.title)

        AlertDialog.Builder(this, R.style.NeonDialog)
            .setTitle("Edit List")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val title = titleEdit.text.toString().trim()
                if (title.isNotEmpty()) {
                    val updatedList = todoList.copy(
                        title = title
                    )
                    viewModel.updateList(updatedList)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun showDeleteConfirmation(todoList: TodoList) {
        AlertDialog.Builder(this)
            .setTitle("Delete List")
            .setMessage("Are you sure you want to delete '${todoList.title}'? This will also delete all items in this list.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteList(todoList)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.lists_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_logout -> {
                // Show confirmation dialog before logout
                AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Your data will be backed up before logging out. Do you want to continue?")
                    .setPositiveButton("Yes") { _, _ ->
                        CoroutineScope(Dispatchers.Main).launch {
                            // First backup the current data
                            backupManager.backupToFirebase()
                            // Clear local data
                            backupManager.clearLocalData()
                            // Logout from Firebase
                            FirebaseAuth.getInstance().signOut()
                            // Navigate back to MainActivity (login screen)
                            val intent = Intent(this@ListsActivity, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                true
            }
            R.id.action_backup -> {
                CoroutineScope(Dispatchers.Main).launch {
                    backupManager.backupToFirebase()
                }
                true
            }
            R.id.action_restore -> {
                // Show confirmation dialog before restore
                AlertDialog.Builder(this)
                    .setTitle("Restore Data")
                    .setMessage("This will restore your data from the last backup. Current data will be replaced. Continue?")
                    .setPositiveButton("Yes") { _, _ ->
                        CoroutineScope(Dispatchers.Main).launch {
                            backupManager.restoreUserData()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStart() {
        super.onStart()
        // Check if user is logged in, if not, redirect to login
        if (!backupManager.isUserLoggedIn()) {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}