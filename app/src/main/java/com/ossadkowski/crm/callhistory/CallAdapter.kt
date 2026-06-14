package com.ossadkowski.crm.callhistory

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class CallAdapter : ListAdapter<CallItem, CallAdapter.ViewHolder>(DiffCallback) {

    companion object DiffCallback : DiffUtil.ItemCallback<CallItem>() {
        override fun areItemsTheSame(oldItem: CallItem, newItem: CallItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: CallItem, newItem: CallItem) = oldItem == newItem
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: TextView = view.findViewById(R.id.avatar_circle)
        val name: TextView = view.findViewById(R.id.caller_name)
        val info: TextView = view.findViewById(R.id.call_info)
        val badge: TextView = view.findViewById(R.id.call_badge)
        val time: TextView = view.findViewById(R.id.call_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_call, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val context = holder.itemView.context

        // Name / Number
        val displayName = if (!item.name.isNullOrBlank()) item.name else item.number
        holder.name.text = displayName

        // Initials Avatar
        val initials = if (!item.name.isNullOrBlank()) {
            val parts = item.name.split(" ")
            if (parts.size > 1) {
                "${parts[0].firstOrNull() ?: ""}${parts[1].firstOrNull() ?: ""}".uppercase()
            } else {
                (item.name.firstOrNull()?.toString() ?: "?").uppercase()
            }
        } else {
            "?"
        }
        holder.avatar.text = initials

        // Date time format
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val dateString = sdf.format(Date(item.timestamp))
        holder.time.text = dateString

        // Duration format
        val durationText = if (item.durationSeconds < 60) {
            context.getString(R.string.call_duration_seconds, item.durationSeconds)
        } else {
            val mins = item.durationSeconds / 60
            val secs = item.durationSeconds % 60
            context.getString(R.string.call_duration_minutes, mins, secs)
        }

        // Call type styling and texts
        // 1 = Incoming, 2 = Outgoing, 3 = Missed, 5 = Rejected
        val (typeText, badgeBg, badgeTextColor) = when (item.type) {
            1 -> Triple(
                context.getString(R.string.call_incoming),
                R.drawable.bg_tab_active,
                R.color.status_success_text
            )
            2 -> Triple(
                context.getString(R.string.call_outgoing),
                R.drawable.bg_tab_inactive,
                R.color.crm_blue
            )
            3, 5 -> Triple(
                context.getString(R.string.call_missed),
                R.drawable.bg_tab_inactive,
                R.color.status_error_text
            )
            else -> Triple(
                "Inne",
                R.drawable.bg_tab_inactive,
                R.color.crm_secondary
            )
        }

        holder.info.text = "$typeText • $durationText"
        holder.badge.text = typeText
        holder.badge.setBackgroundResource(badgeBg)
        holder.badge.setTextColor(ContextCompat.getColor(context, badgeTextColor))
    }
}
