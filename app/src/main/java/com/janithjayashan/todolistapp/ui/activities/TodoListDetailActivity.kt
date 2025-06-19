package com.janithjayashan.todolistapp.ui.activities

import android.app.AlertDialog
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.janithjayashan.todolistapp.R
import com.janithjayashan.todolistapp.data.database.entities.TodoItem
import com.janithjayashan.todolistapp.ui.adapters.TodoItemsAdapter
import com.janithjayashan.todolistapp.ui.viewmodels.TodoViewModel

class TodoListDetailActivity : AppCompatActivity() {

    private lateinit var viewModel: TodoViewModel
    private lateinit var adapter: TodoItemsAdapter
    private var listId: Long = -1
    private var listTitle: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_todo_list_detail)

        listId = intent.getLongExtra("LIST_ID", -1)
        listTitle = intent.getStringExtra("LIST_TITLE") ?: ""

        supportActionBar?.title = listTitle
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProvider(this)[TodoViewModel::class.java]

        setupRecyclerView()
        setupFab()
        observeItems()
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewItems)
        adapter = TodoItemsAdapter(
            onItemClick = { todoItem ->
                // Toggle completion status
                viewModel.updateItem(todoItem.copy(completed = !todoItem.completed))
            },
            onDeleteClick = { todoItem ->
                showDeleteConfirmation(todoItem)
            },
            onEditClick = { todoItem ->
                showEditItemDialog(todoItem)
            }
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Add drag and drop functionality
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition

                // Update positions in database
                val items = adapter.currentList
                if (fromPosition < items.size && toPosition < items.size) {
                    val item = items[fromPosition]
                    viewModel.updateItemPosition(item.id, toPosition)
                }

                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Not used
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun setupFab() {
        val fab = findViewById<FloatingActionButton>(R.id.fabAddItem)
        fab.setOnClickListener {
            showAddItemDialog()
        }
    }

    private fun observeItems() {
        viewModel.getItemsByListId(listId).observe(this) { items ->
            adapter.submitList(items)
        }
    }

    private fun showAddItemDialog() {
        val editText = EditText(this)
        editText.hint = "Enter item description"

        AlertDialog.Builder(this)
            .setTitle("Add New Item")
            .setView(editText)
            .setPositiveButton("Add") { _, _ ->
                val description = editText.text.toString().trim()
                if (description.isNotEmpty()) {
                    val position = adapter.itemCount
                    viewModel.insertItem(listId, description, position)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditItemDialog(todoItem: TodoItem) {
        val editText = EditText(this)
        editText.setText(todoItem.description)

        AlertDialog.Builder(this)
            .setTitle("Edit Item")
            .setView(editText)
            .setPositiveButton("Update") { _, _ ->
                val description = editText.text.toString().trim()
                if (description.isNotEmpty()) {
                    viewModel.updateItem(todoItem.copy(description = description))
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmation(todoItem: TodoItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete Item")
            .setMessage("Are you sure you want to delete '${todoItem.description}'?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteItem(todoItem)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}