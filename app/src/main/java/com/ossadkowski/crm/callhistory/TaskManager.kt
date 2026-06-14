package com.ossadkowski.crm.callhistory

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class TaskManager(context: Context) {
    private val prefs = context.getSharedPreferences("call_tasks_prefs", Context.MODE_PRIVATE)

    fun getTasks(): List<TaskItem> {
        val jsonStr = prefs.getString("tasks", "[]") ?: "[]"
        val tasks = mutableListOf<TaskItem>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                tasks.add(
                    TaskItem(
                        id = obj.getString("id"),
                        contactName = obj.getString("contactName"),
                        phoneNumber = obj.getString("phoneNumber"),
                        taskTitle = obj.getString("taskTitle"),
                        isCompleted = obj.getBoolean("isCompleted"),
                        completedAt = obj.getLong("completedAt")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return tasks
    }

    fun saveTasks(tasks: List<TaskItem>) {
        val arr = JSONArray()
        for (task in tasks) {
            val obj = JSONObject().apply {
                put("id", task.id)
                put("contactName", task.contactName)
                put("phoneNumber", task.phoneNumber)
                put("taskTitle", task.taskTitle)
                put("isCompleted", task.isCompleted)
                put("completedAt", task.completedAt)
            }
            arr.put(obj)
        }
        prefs.edit().putString("tasks", arr.toString()).apply()
    }

    fun addTask(contactName: String, phoneNumber: String, taskTitle: String): TaskItem {
        val tasks = getTasks().toMutableList()
        val newTask = TaskItem(
            id = UUID.randomUUID().toString(),
            contactName = contactName,
            phoneNumber = phoneNumber,
            taskTitle = taskTitle
        )
        tasks.add(newTask)
        saveTasks(tasks)
        return newTask
    }

    fun deleteTask(id: String) {
        val tasks = getTasks().filter { it.id != id }
        saveTasks(tasks)
    }

    fun checkAndCompleteTask(incomingOrOutgoingNumber: String, cachedNameFromPhone: String = ""): Boolean {
        if (incomingOrOutgoingNumber.isBlank()) return false
        val tasks = getTasks().toMutableList()
        var updated = false
        var found = false

        for (task in tasks) {
            if (isSamePhoneNumber(task.phoneNumber, incomingOrOutgoingNumber)) {
                found = true
                if (!task.isCompleted) {
                    task.isCompleted = true
                    task.completedAt = System.currentTimeMillis()
                    updated = true
                }
            }
        }

        if (!found) {
            val contactName = if (cachedNameFromPhone.isNotBlank()) cachedNameFromPhone else "Nowy kontrahent"
            val newTask = TaskItem(
                id = UUID.randomUUID().toString(),
                contactName = contactName,
                phoneNumber = incomingOrOutgoingNumber,
                taskTitle = if (cachedNameFromPhone.isNotBlank()) "Dodaj kontrahenta: $cachedNameFromPhone" else "Dodaj kontrahenta: $incomingOrOutgoingNumber",
                isCompleted = false,
                completedAt = 0L
            )
            tasks.add(newTask)
            updated = true
        }

        if (updated) {
            saveTasks(tasks)
        }
        return updated
    }

    private fun isSamePhoneNumber(num1: String, num2: String): Boolean {
        val clean1 = num1.filter { it.isDigit() }
        val clean2 = num2.filter { it.isDigit() }
        if (clean1.isEmpty() || clean2.isEmpty()) return false
        
        val len1 = clean1.length
        val len2 = clean2.length
        val suffix1 = if (len1 >= 9) clean1.substring(len1 - 9) else clean1
        val suffix2 = if (len2 >= 9) clean2.substring(len2 - 9) else clean2
        return suffix1 == suffix2
    }
}
