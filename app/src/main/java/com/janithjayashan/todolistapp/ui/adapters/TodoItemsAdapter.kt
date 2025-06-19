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

class TodoItemsAdapter(
    private val onItemClick: (TodoItem) -> Unit,
    private val onDeleteClick: (TodoItem) -> Unit,
    private val onEditClick: (TodoItem) -> Unit
) : ListAdapter<TodoItem, TodoItemsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_todo_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkBoxItem)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.textViewDescription)
        private val editButton: ImageButton = itemView.findViewById(R.id.buttonEditItem)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.buttonDeleteItem)

        fun bind(todoItem: TodoItem) {
            checkBox.isChecked = todoItem.completed
            descriptionTextView.text = todoItem.description

            // Strike through completed items
            if (todoItem.completed) {
                descriptionTextView.paintFlags = descriptionTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                descriptionTextView.paintFlags = descriptionTextView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            checkBox.setOnClickListener { onItemClick(todoItem) }
            editButton.setOnClickListener { onEditClick(todoItem) }
            deleteButton.setOnClickListener { onDeleteClick(todoItem) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<TodoItem>() {
        override fun areItemsTheSame(oldItem: TodoItem, newItem: TodoItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TodoItem, newItem: TodoItem): Boolean {
            return oldItem == newItem
        }
    }
}