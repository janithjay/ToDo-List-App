package com.janithjayashan.todolistapp.ui.activities

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
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
import java.text.SimpleDateFormat
import java.util.*

class TodoListDetailActivity : AppCompatActivity() {

    private lateinit var viewModel: TodoViewModel
    private lateinit var adapter: TodoItemsAdapter
    private var listId: Long = -1
    private var listTitle: String = ""
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

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
        // First, observe items directly
        viewModel.getItemsByListId(listId).observe(this) { items ->
            adapter.submitList(items)
        }

        // Then observe sort order changes
        viewModel.currentItemSortOrder.observe(this) {
            if (it != null) {  // Only change sort if an order is explicitly selected
                viewModel.getItemsByCurrentSort(listId).observe(this) { items ->
                    adapter.submitList(items)
                }
            }
        }
    }

    private fun showAddItemDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_task, null)
        val titleEdit = dialogView.findViewById<EditText>(R.id.etTaskTitle)
        val descriptionEdit = dialogView.findViewById<EditText>(R.id.etTaskDescription)
        val btnSelectDate = dialogView.findViewById<Button>(R.id.btnSelectDate)
        val btnSelectTime = dialogView.findViewById<Button>(R.id.btnSelectTime)
        val tvSelectedDateTime = dialogView.findViewById<TextView>(R.id.tvSelectedDateTime)

        val calendar = Calendar.getInstance()
        var selectedDate = calendar.timeInMillis
        var selectedTime = timeFormat.format(calendar.time)

        btnSelectDate.setOnClickListener {
            showDatePicker(calendar) { date ->
                selectedDate = date
                updateSelectedDateTime(tvSelectedDateTime, selectedDate, selectedTime)
            }
        }

        btnSelectTime.setOnClickListener {
            showTimePicker(calendar) { time ->
                selectedTime = time
                updateSelectedDateTime(tvSelectedDateTime, selectedDate, selectedTime)
            }
        }

        updateSelectedDateTime(tvSelectedDateTime, selectedDate, selectedTime)

        AlertDialog.Builder(this)
            .setTitle("Add New Task")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val title = titleEdit.text.toString().trim()
                val description = descriptionEdit.text.toString().trim()
                if (title.isNotEmpty()) {
                    val position = adapter.currentList.size  // Set position to end of list
                    val todoItem = TodoItem(
                        id = 0, // AutoGenerate will handle this
                        listId = listId,
                        title = title,
                        description = description,
                        dueDate = selectedDate,
                        dueTime = selectedTime,
                        position = position,
                        completed = false
                    )
                    viewModel.insertItem(todoItem)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditItemDialog(todoItem: TodoItem) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_task, null)
        val titleEdit = dialogView.findViewById<EditText>(R.id.etTaskTitle)
        val descriptionEdit = dialogView.findViewById<EditText>(R.id.etTaskDescription)
        val btnSelectDate = dialogView.findViewById<Button>(R.id.btnSelectDate)
        val btnSelectTime = dialogView.findViewById<Button>(R.id.btnSelectTime)
        val tvSelectedDateTime = dialogView.findViewById<TextView>(R.id.tvSelectedDateTime)

        titleEdit.setText(todoItem.title)
        descriptionEdit.setText(todoItem.description)

        var selectedDate = todoItem.dueDate
        var selectedTime = todoItem.dueTime

        btnSelectDate.setOnClickListener {
            val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
            showDatePicker(calendar) { date ->
                selectedDate = date
                updateSelectedDateTime(tvSelectedDateTime, selectedDate, selectedTime)
            }
        }

        btnSelectTime.setOnClickListener {
            showTimePicker(Calendar.getInstance()) { time ->
                selectedTime = time
                updateSelectedDateTime(tvSelectedDateTime, selectedDate, selectedTime)
            }
        }

        updateSelectedDateTime(tvSelectedDateTime, selectedDate, selectedTime)

        AlertDialog.Builder(this)
            .setTitle("Edit Task")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val title = titleEdit.text.toString().trim()
                val description = descriptionEdit.text.toString().trim()
                if (title.isNotEmpty()) {
                    val updatedItem = todoItem.copy(
                        title = title,
                        description = description,
                        dueDate = selectedDate,
                        dueTime = selectedTime
                    )
                    viewModel.updateItem(updatedItem)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDatePicker(calendar: Calendar, onDateSelected: (Long) -> Unit) {
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                onDateSelected(calendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker(calendar: Calendar, onTimeSelected: (String) -> Unit) {
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                onTimeSelected(timeFormat.format(calendar.time))
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun updateSelectedDateTime(textView: TextView, date: Long, time: String) {
        textView.text = "Due: ${dateFormat.format(date)} at $time"
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.sort_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.sort_created_newest -> {
                item.isChecked = true
                viewModel.setItemSortOrder(TodoViewModel.ItemSortOrder.NEWEST_FIRST)
                true
            }
            R.id.sort_created_oldest -> {
                item.isChecked = true
                viewModel.setItemSortOrder(TodoViewModel.ItemSortOrder.OLDEST_FIRST)
                true
            }
            R.id.sort_due_earliest -> {
                item.isChecked = true
                viewModel.setItemSortOrder(TodoViewModel.ItemSortOrder.DUE_EARLIEST)
                true
            }
            R.id.sort_due_latest -> {
                item.isChecked = true
                viewModel.setItemSortOrder(TodoViewModel.ItemSortOrder.DUE_LATEST)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}