package com.janithjayashan.todolistapp.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.janithjayashan.todolistapp.R
import com.janithjayashan.todolistapp.data.database.entities.TodoList

class TodoListsAdapter(
    private val onListClick: (TodoList) -> Unit,
    private val onDeleteClick: (TodoList) -> Unit,
    private val onEditClick: (TodoList) -> Unit,
    private val getTotalTasks: (Long) -> LiveData<Int>,
    private val getCompletedTasks: (Long) -> LiveData<Int>
) : ListAdapter<TodoList, TodoListsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_todo_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.tvListTitle)
        private val statisticsTextView: TextView = itemView.findViewById(R.id.tvDate)
        private val editButton: ImageButton = itemView.findViewById(R.id.btnEdit)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.btnDelete)
        private val matchingTasksContainer: LinearLayout = itemView.findViewById(R.id.matchingTasksContainer)

        fun bind(todoList: TodoList) {
            titleTextView.text = todoList.title

            // Observe and display task statistics
            getTotalTasks(todoList.id).observeForever { totalTasks ->
                getCompletedTasks(todoList.id).observeForever { completedTasks ->
                    statisticsTextView.text = "$completedTasks/$totalTasks tasks completed"
                }
            }

            // Handle matching items display
            matchingTasksContainer.removeAllViews()
            if (todoList.matchingItems.isNotEmpty()) {
                matchingTasksContainer.visibility = View.VISIBLE
                todoList.matchingItems.forEach { item ->
                    val taskView = LayoutInflater.from(itemView.context)
                        .inflate(R.layout.item_matching_task, matchingTasksContainer, false)
                    val taskDescText = taskView.findViewById<TextView>(R.id.tvTaskDescription)
                    val itemText = if (item.title.isNotEmpty()) {
                        "• ${item.title} - ${item.description}"
                    } else {
                        "• ${item.description}"
                    }
                    taskDescText.text = itemText
                    matchingTasksContainer.addView(taskView)
                }
            } else {
                matchingTasksContainer.visibility = View.GONE
            }

            itemView.setOnClickListener { onListClick(todoList) }
            editButton.setOnClickListener { onEditClick(todoList) }
            deleteButton.setOnClickListener { onDeleteClick(todoList) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<TodoList>() {
        override fun areItemsTheSame(oldItem: TodoList, newItem: TodoList): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TodoList, newItem: TodoList): Boolean {
            return oldItem == newItem && oldItem.matchingItems == newItem.matchingItems
        }
    }
}