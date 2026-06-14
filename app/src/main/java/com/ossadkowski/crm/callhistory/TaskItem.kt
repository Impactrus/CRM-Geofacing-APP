package com.ossadkowski.crm.callhistory

data class TaskItem(
    val id: String,
    val contactName: String,
    val phoneNumber: String,
    val taskTitle: String,
    var isCompleted: Boolean = false,
    var completedAt: Long = 0L
)
