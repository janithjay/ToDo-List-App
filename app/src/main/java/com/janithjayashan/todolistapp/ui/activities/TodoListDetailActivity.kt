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
            private var startPosition = -1
            private var endPosition = -1

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.bindingAdapterPosition
                val toPosition = target.bindingAdapterPosition

                if (startPosition == -1) {
                    startPosition = fromPosition
                }
                endPosition = toPosition

                val currentList = adapter.currentList.toMutableList()
                val item = currentList[fromPosition]
                currentList.removeAt(fromPosition)
                currentList.add(toPosition, item)
                adapter.submitList(currentList)

                return true
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)

                if (startPosition != -1 && endPosition != -1 && startPosition != endPosition) {
                    val currentList = adapter.currentList
                    val updates = currentList.mapIndexed { index, todoItem ->
                        todoItem.id to index
                    }
                    viewModel.updateItemPositions(updates)
                }

                // Reset positions
                startPosition = -1
                endPosition = -1
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Not used
            }

            override fun isLongPressDragEnabled(): Boolean {
                return true
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
        // Only observe the manual ordering
        viewModel.getItemsByListId(listId).observe(this) { items ->
            adapter.submitList(items)
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
                    // Shift all existing items' positions down by 1
                    val currentList = adapter.currentList
                    currentList.forEachIndexed { index, item ->
                        viewModel.updateItemPosition(item.id, index + 1)
                    }

                    // Add new item at position 0 (top)
                    val todoItem = TodoItem(
                        id = 0, // AutoGenerate will handle this
                        listId = listId,
                        title = title,
                        description = description,
                        dueDate = selectedDate,
                        dueTime = selectedTime,
                        position = 0, // Place at the top
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
        val formattedDate = dateFormat.format(date)
        val text = getString(R.string.due_date_time, formattedDate, time)
        textView.text = text
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
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}