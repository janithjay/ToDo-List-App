package com.janithjayashan.todolistapp.ui.activities

import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
        backupManager = FirebaseBackupManager(this)

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
        editText.setHintTextColor(ContextCompat.getColor(this, R.color.neon_blue))
        editText.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
        editText.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.neon_blue))

        AlertDialog.Builder(this, R.style.NeonDialog)
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
        menuInflater.inflate(R.menu.lists_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_logout -> {
                FirebaseAuth.getInstance().signOut()
                // Navigate back to MainActivity (login screen)
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                true
            }
            R.id.action_backup -> {
                CoroutineScope(Dispatchers.Main).launch {
                    backupManager.backupToFirebase()
                }
                true
            }
            R.id.action_restore -> {
                CoroutineScope(Dispatchers.Main).launch {
                    backupManager.restoreFromFirebase()
                }
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