package com.janithjayashan.todolistapp.ui.adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.janithjayashan.todolistapp.R
import com.janithjayashan.todolistapp.data.database.entities.TodoItem
import java.text.SimpleDateFormat
import java.util.*

class TodoItemsAdapter(
    private val onItemClick: (TodoItem) -> Unit,
    private val onDeleteClick: (TodoItem) -> Unit,
    private val onEditClick: (TodoItem) -> Unit
) : ListAdapter<TodoItem, TodoItemsAdapter.ViewHolder>(DiffCallback()) {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_todo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkBoxCompleted)
        private val titleTextView: TextView = itemView.findViewById(R.id.tvTitle)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.tvDescription)
        private val dateTimeTextView: TextView = itemView.findViewById(R.id.tvDateTime)
        private val editButton: ImageButton = itemView.findViewById(R.id.btnEdit)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(todoItem: TodoItem) {
            checkBox.isChecked = todoItem.completed
            titleTextView.text = todoItem.title
            descriptionTextView.text = todoItem.description
            dateTimeTextView.text = "Due: ${dateFormat.format(todoItem.dueDate)} at ${todoItem.dueTime}"

            // Strike through text for completed items
            val paintFlags = if (todoItem.completed) {
                Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                0
            }
            titleTextView.paintFlags = paintFlags
            descriptionTextView.paintFlags = paintFlags
            dateTimeTextView.paintFlags = paintFlags

            // Set click listeners
            checkBox.setOnClickListener { onItemClick(todoItem) }
            editButton.setOnClickListener { onEditClick(todoItem) }
            deleteButton.setOnClickListener { onDeleteClick(todoItem) }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<TodoItem>() {
        override fun areItemsTheSame(oldItem: TodoItem, newItem: TodoItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TodoItem, newItem: TodoItem): Boolean {
            return oldItem == newItem
        }
    }
}