package com.janithjayashan.todolistapp.ui.activities

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
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

class ListsActivity : AppCompatActivity() {

    private lateinit var viewModel: TodoViewModel
    private lateinit var adapter: TodoListsAdapter
    private lateinit var backupManager: FirebaseBackupManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lists)

        supportActionBar?.title = "Your Lists"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProvider(this)[TodoViewModel::class.java]
        backupManager = FirebaseBackupManager()

        setupRecyclerView()
        setupFab()
        observeLists()
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
            }
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
            adapter.submitList(lists)
        }
    }

    private fun showAddListDialog() {
        val editText = EditText(this)
        editText.hint = "Enter list title"

        AlertDialog.Builder(this)
            .setTitle("Add New List")
            .setView(editText)
            .setPositiveButton("Add") { _, _ ->
                val title = editText.text.toString().trim()
                if (title.isNotEmpty()) {
                    viewModel.insertList(title)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditListDialog(todoList: TodoList) {
        val editText = EditText(this)
        editText.setText(todoList.title)

        AlertDialog.Builder(this)
            .setTitle("Edit List")
            .setView(editText)
            .setPositiveButton("Update") { _, _ ->
                val title = editText.text.toString().trim()
                if (title.isNotEmpty()) {
                    viewModel.updateList(todoList.copy(title = title))
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
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_backup -> {
                backupManager.backupToFirebase(this)
                true
            }
            R.id.action_restore -> {
                backupManager.restoreFromFirebase(this)
                true
            }
            R.id.action_search -> {
                showSearchDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSearchDialog() {
        val editText = EditText(this)
        editText.hint = "Enter search term"

        AlertDialog.Builder(this)
            .setTitle("Search Lists")
            .setView(editText)
            .setPositiveButton("Search") { _, _ ->
                val query = editText.text.toString().trim()
                if (query.isNotEmpty()) {
                    viewModel.searchLists(query)
                    observeSearchResults()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeSearchResults() {
        viewModel.searchResults.observe(this) { results ->
            adapter.submitList(results)
        }
    }
}