package com.janithjayashan.todolistapp.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.janithjayashan.todolistapp.R
import com.janithjayashan.todolistapp.data.database.entities.TodoList
import java.text.SimpleDateFormat
import java.util.*

class TodoListsAdapter(
    private val onListClick: (TodoList) -> Unit,
    private val onDeleteClick: (TodoList) -> Unit,
    private val onEditClick: (TodoList) -> Unit
) : ListAdapter<TodoList, TodoListsAdapter.ViewHolder>(DiffCallback()) {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

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
        private val descriptionTextView: TextView = itemView.findViewById(R.id.tvDescription)
        private val dateTextView: TextView = itemView.findViewById(R.id.tvDate)
        private val timeTextView: TextView = itemView.findViewById(R.id.tvTime)
        private val editButton: ImageButton = itemView.findViewById(R.id.btnEdit)

        fun bind(todoList: TodoList) {
            titleTextView.text = todoList.title
            descriptionTextView.text = todoList.description
            dateTextView.text = dateFormat.format(todoList.selectedDate)
            timeTextView.text = todoList.selectedTime

            if (todoList.description.isEmpty()) {
                descriptionTextView.visibility = View.GONE
            } else {
                descriptionTextView.visibility = View.VISIBLE
            }

            itemView.setOnClickListener { onListClick(todoList) }
            editButton.setOnClickListener { onEditClick(todoList) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<TodoList>() {
        override fun areItemsTheSame(oldItem: TodoList, newItem: TodoList): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TodoList, newItem: TodoList): Boolean {
            return oldItem == newItem
        }
    }
}