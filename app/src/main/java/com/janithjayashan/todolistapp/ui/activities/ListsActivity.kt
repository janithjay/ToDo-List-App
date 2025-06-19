package com.janithjayashan.todolistapp.ui.activities

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
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
import java.text.SimpleDateFormat
import java.util.*

class ListsActivity : AppCompatActivity() {

    private lateinit var viewModel: TodoViewModel
    private lateinit var adapter: TodoListsAdapter
    private lateinit var backupManager: FirebaseBackupManager
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

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
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_list, null)
        val titleEdit = dialogView.findViewById<EditText>(R.id.etTitle)
        val descriptionEdit = dialogView.findViewById<EditText>(R.id.etDescription)
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

        // Set initial date/time
        updateSelectedDateTime(tvSelectedDateTime, selectedDate, selectedTime)

        AlertDialog.Builder(this, R.style.NeonDialog)
            .setTitle("Add New List")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val title = titleEdit.text.toString().trim()
                val description = descriptionEdit.text.toString().trim()
                if (title.isNotEmpty()) {
                    val todoList = TodoList(
                        title = title,
                        description = description,
                        selectedDate = selectedDate,
                        selectedTime = selectedTime
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
        val descriptionEdit = dialogView.findViewById<EditText>(R.id.etDescription)
        val btnSelectDate = dialogView.findViewById<Button>(R.id.btnSelectDate)
        val btnSelectTime = dialogView.findViewById<Button>(R.id.btnSelectTime)
        val tvSelectedDateTime = dialogView.findViewById<TextView>(R.id.tvSelectedDateTime)

        titleEdit.setText(todoList.title)
        descriptionEdit.setText(todoList.description)

        var selectedDate = todoList.selectedDate
        var selectedTime = todoList.selectedTime

        btnSelectDate.setOnClickListener {
            val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
            showDatePicker(calendar) { date ->
                selectedDate = date
                updateSelectedDateTime(tvSelectedDateTime, selectedDate, selectedTime)
            }
        }

        btnSelectTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            showTimePicker(calendar) { time ->
                selectedTime = time
                updateSelectedDateTime(tvSelectedDateTime, selectedDate, selectedTime)
            }
        }

        // Set initial date/time
        updateSelectedDateTime(tvSelectedDateTime, selectedDate, selectedTime)

        AlertDialog.Builder(this, R.style.NeonDialog)
            .setTitle("Edit List")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val title = titleEdit.text.toString().trim()
                val description = descriptionEdit.text.toString().trim()
                if (title.isNotEmpty()) {
                    val updatedList = todoList.copy(
                        title = title,
                        description = description,
                        selectedDate = selectedDate,
                        selectedTime = selectedTime
                    )
                    viewModel.updateList(updatedList)
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
        textView.text = "Selected: ${dateFormat.format(date)} at $time"
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