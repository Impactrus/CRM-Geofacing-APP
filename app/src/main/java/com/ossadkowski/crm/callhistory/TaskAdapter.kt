package com.ossadkowski.crm.callhistory

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class TaskAdapter(
    private val onDeleteClick: (TaskItem) -> Unit,
    private val onCallClick: (TaskItem) -> Unit
) : ListAdapter<TaskItem, TaskAdapter.ViewHolder>(DiffCallback) {

    companion object DiffCallback : DiffUtil.ItemCallback<TaskItem>() {
        override fun areItemsTheSame(oldItem: TaskItem, newItem: TaskItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: TaskItem, newItem: TaskItem) = oldItem == newItem
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: TextView = view.findViewById(R.id.task_avatar_circle)
        val title: TextView = view.findViewById(R.id.task_title)
        val info: TextView = view.findViewById(R.id.task_contact_info)
        val badge: TextView = view.findViewById(R.id.task_badge)
        val completedTime: TextView = view.findViewById(R.id.task_completed_time)
        val callBtn: ImageView = view.findViewById(R.id.btn_call_task)
        val deleteBtn: ImageView = view.findViewById(R.id.btn_delete_task)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val context = holder.itemView.context

        holder.title.text = item.taskTitle
        
        val contactInfo = if (item.contactName.isNotBlank()) {
            "${item.contactName} • ${item.phoneNumber}"
        } else {
            item.phoneNumber
        }
        holder.info.text = contactInfo

        val firstChar = if (item.contactName.isNotBlank()) {
            item.contactName.first().uppercase()
        } else {
            "?"
        }
        holder.avatar.text = firstChar

        if (item.isCompleted) {
            holder.avatar.setBackgroundResource(R.drawable.bg_initials_circle)
            holder.avatar.setTextColor(ContextCompat.getColor(context, R.color.status_success_text))
            
            holder.badge.text = "Wykonane"
            holder.badge.setBackgroundResource(R.drawable.bg_tab_active)
            holder.badge.setTextColor(ContextCompat.getColor(context, R.color.status_success_text))
            
            if (item.completedAt > 0L) {
                val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                val completedString = "Zakończono: " + sdf.format(Date(item.completedAt))
                holder.completedTime.text = completedString
                holder.completedTime.visibility = View.VISIBLE
            } else {
                holder.completedTime.visibility = View.GONE
            }
        } else {
            holder.avatar.setBackgroundResource(R.drawable.bg_tab_inactive)
            holder.avatar.setTextColor(ContextCompat.getColor(context, R.color.crm_secondary))
            
            holder.badge.text = "W toku"
            holder.badge.setBackgroundResource(R.drawable.bg_tab_inactive)
            holder.badge.setTextColor(ContextCompat.getColor(context, R.color.status_warning_text))
            holder.completedTime.visibility = View.GONE
        }

        holder.callBtn.setOnClickListener {
            onCallClick(item)
        }

        holder.deleteBtn.setOnClickListener {
            onDeleteClick(item)
        }
    }
}
