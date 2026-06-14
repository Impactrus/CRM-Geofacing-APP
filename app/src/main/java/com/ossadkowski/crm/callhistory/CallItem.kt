package com.ossadkowski.crm.callhistory

data class CallItem(
    val id: String,
    val name: String?,
    val number: String,
    val type: Int, // android.provider.CallLog.Calls.INCOMING_TYPE, etc.
    val durationSeconds: Long,
    val timestamp: Long
)
