package com.ossadkowski.crm.callhistory

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.CallLog
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import android.net.Uri
import com.ossadkowski.crm.callhistory.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val callAdapter = CallAdapter()
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var taskManager: TaskManager
    
    private var allCalls = listOf<CallItem>()
    private var activeFilter = "all"
    private var searchQuery = ""
    private var activeTab = "history"

    // References to the currently open add-task dialog fields
    private var dialogNameEdit: EditText? = null
    private var dialogPhoneEdit: EditText? = null

    private val contactPickerLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val contactUri = result.data?.data ?: return@registerForActivityResult
            val projection = arrayOf(
                android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER,
                android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            )
            try {
                contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val numberIndex = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                        val nameIndex = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        val number = cursor.getString(numberIndex)
                        val name = cursor.getString(nameIndex)
                        
                        dialogPhoneEdit?.setText(number)
                        dialogNameEdit?.setText(name)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Błąd odczytu kontaktu", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val refreshTasksReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            runOnUiThread {
                loadTasks()
                loadCalls()
            }
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_STATE
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        taskManager = TaskManager(this)

        setupRecyclerViews()
        setupFilters()
        setupSearch()
        setupNavigation()

        binding.btnGrant.setOnClickListener {
            requestPermissions()
        }

        binding.btnAddTask.setOnClickListener {
            showAddTaskDialog()
        }

        checkAndLoad()
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter("com.ossadkowski.crm.callhistory.REFRESH_TASKS")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(refreshTasksReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(refreshTasksReceiver, filter)
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(refreshTasksReceiver)
    }

    override fun onResume() {
        super.onResume()
        if (hasPermissions()) {
            checkRecentCallsForTasks()
            loadCalls()
            loadTasks()
        }
    }

    private fun checkAndLoad() {
        if (hasPermissions()) {
            binding.permissionCard.visibility = View.GONE
            binding.statsContainer.visibility = View.VISIBLE
            binding.searchBar.visibility = View.VISIBLE
            switchTab(activeTab)
        } else {
            binding.permissionCard.visibility = View.VISIBLE
            binding.statsContainer.visibility = View.GONE
            binding.searchBar.visibility = View.GONE
            binding.emptyText.visibility = View.GONE
        }
    }

    private fun hasPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            REQUIRED_PERMISSIONS,
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                checkAndLoad()
            } else {
                Toast.makeText(this, "Wymagane są uprawnienia do Rejestru i Stanu Telefonu", Toast.LENGTH_LONG).show()
                checkAndLoad()
            }
        }
    }

    private fun setupRecyclerViews() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = callAdapter

        taskAdapter = TaskAdapter(
            onDeleteClick = { task ->
                taskManager.deleteTask(task.id)
                loadTasks()
            },
            onCallClick = { task ->
                try {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${task.phoneNumber}"))
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Nie można otworzyć dialera", Toast.LENGTH_SHORT).show()
                }
            }
        )
        binding.tasksRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.tasksRecyclerView.adapter = taskAdapter
    }

    private fun setupNavigation() {
        binding.navHistory.setOnClickListener {
            switchTab("history")
        }
        binding.navTasks.setOnClickListener {
            switchTab("tasks")
        }
    }

    private fun switchTab(tab: String) {
        activeTab = tab
        if (tab == "history") {
            binding.layoutHistoryContainer.visibility = View.VISIBLE
            binding.layoutTasksContainer.visibility = View.GONE
            
            binding.navHistoryIcon.setColorFilter(ContextCompat.getColor(this, R.color.crm_primary))
            binding.navHistoryText.setTextColor(ContextCompat.getColor(this, R.color.crm_primary))
            
            binding.navTasksIcon.setColorFilter(ContextCompat.getColor(this, R.color.crm_secondary))
            binding.navTasksText.setTextColor(ContextCompat.getColor(this, R.color.crm_secondary))
            
            loadCalls()
        } else {
            binding.layoutHistoryContainer.visibility = View.GONE
            binding.layoutTasksContainer.visibility = View.VISIBLE

            binding.navHistoryIcon.setColorFilter(ContextCompat.getColor(this, R.color.crm_secondary))
            binding.navHistoryText.setTextColor(ContextCompat.getColor(this, R.color.crm_secondary))

            binding.navTasksIcon.setColorFilter(ContextCompat.getColor(this, R.color.crm_primary))
            binding.navTasksText.setTextColor(ContextCompat.getColor(this, R.color.crm_primary))
            
            loadTasks()
        }
    }

    private fun loadCalls() {
        if (!hasPermissions()) return
        binding.progressBar.visibility = View.VISIBLE
        Thread {
            val callsList = mutableListOf<CallItem>()

            val projection = arrayOf(
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DURATION,
                CallLog.Calls.DATE
            )

            try {
                val cursor: Cursor? = contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${CallLog.Calls.DATE} DESC"
                )

                cursor?.use {
                    val idCol = it.getColumnIndexOrThrow(CallLog.Calls._ID)
                    val numCol = it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                    val nameCol = it.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
                    val typeCol = it.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                    val durCol = it.getColumnIndexOrThrow(CallLog.Calls.DURATION)
                    val dateCol = it.getColumnIndexOrThrow(CallLog.Calls.DATE)

                    while (it.moveToNext()) {
                        val id = it.getString(idCol)
                        val number = it.getString(numCol) ?: ""
                        val name = it.getString(nameCol)
                        val type = it.getInt(typeCol)
                        val duration = it.getLong(durCol)
                        val date = it.getLong(dateCol)

                        callsList.add(CallItem(id, name, number, type, duration, date))
                    }
                }
            } catch (e: SecurityException) {
                runOnUiThread {
                    Toast.makeText(this, "Błąd zabezpieczeń przy czytaniu rejestru", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Błąd wczytywania połączeń: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            runOnUiThread {
                allCalls = callsList
                binding.progressBar.visibility = View.GONE
                calculateStats()
                applyFiltersAndSearch()
            }
        }.start()
    }

    private fun loadTasks() {
        val tasks = taskManager.getTasks()
        taskAdapter.submitList(tasks.reversed())

        val pending = tasks.count { !it.isCompleted }
        val completed = tasks.count { it.isCompleted }

        binding.statTasksPending.text = pending.toString()
        binding.statTasksCompleted.text = completed.toString()
        
        binding.tasksEmptyText.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun checkRecentCallsForTasks() {
        if (!hasPermissions()) return
        Thread {
            val projection = arrayOf(
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME
            )
            try {
                val cursor: Cursor? = contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${CallLog.Calls.DATE} DESC LIMIT 15"
                )

                var hasUpdates = false
                cursor?.use {
                    val numCol = it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                    val nameCol = it.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
                    while (it.moveToNext()) {
                        val number = it.getString(numCol) ?: ""
                        val name = it.getString(nameCol) ?: ""
                        if (number.isNotBlank()) {
                            val tasks = taskManager.getTasks()
                            val cleanNumber = number.filter { c -> c.isDigit() }
                            val hasMatchingActiveTask = tasks.any { t -> 
                                !t.isCompleted && t.phoneNumber.filter { c -> c.isDigit() }.takeLast(9) == cleanNumber.takeLast(9) 
                            }
                            if (hasMatchingActiveTask) {
                                // Since checkAndCompleteTask with cachedName auto-creates tasks for unknown numbers,
                                // we ONLY call it for MATCHING active tasks to prevent duplicating old calls as new tasks.
                                val completed = taskManager.checkAndCompleteTask(number, name)
                                if (completed) {
                                    hasUpdates = true
                                }
                            }
                        }
                    }
                }
                if (hasUpdates) {
                    runOnUiThread {
                        loadTasks()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun calculateStats() {
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        val todayMs = today.timeInMillis

        val todayCalls = allCalls.filter { it.timestamp >= todayMs }
        val totalToday = todayCalls.size
        val missedToday = todayCalls.count { it.type == CallLog.Calls.MISSED_TYPE || it.type == CallLog.Calls.REJECTED_TYPE }

        binding.statTotalCount.text = totalToday.toString()
        binding.statMissedCount.text = missedToday.toString()
    }

    private fun setupFilters() {
        val tabViews = listOf(
            binding.tabAll to "all",
            binding.tabIncoming to "incoming",
            binding.tabOutgoing to "outgoing",
            binding.tabMissed to "missed"
        )

        tabViews.forEach { (tv, filterName) ->
            tv.setOnClickListener {
                selectFilter(filterName)
            }
        }
    }

    private fun selectFilter(filterName: String) {
        activeFilter = filterName
        val tabViews = listOf(
            binding.tabAll to "all",
            binding.tabIncoming to "incoming",
            binding.tabOutgoing to "outgoing",
            binding.tabMissed to "missed"
        )

        tabViews.forEach { (tv, name) ->
            if (name == filterName) {
                tv.setBackgroundResource(R.drawable.bg_tab_active)
                tv.setTextColor(ContextCompat.getColor(this, R.color.crm_heading))
            } else {
                tv.setBackgroundResource(R.drawable.bg_tab_inactive)
                tv.setTextColor(ContextCompat.getColor(this, R.color.crm_secondary))
            }
        }

        applyFiltersAndSearch()
    }

    private fun setupSearch() {
        binding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim() ?: ""
                applyFiltersAndSearch()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun applyFiltersAndSearch() {
        var filteredList = allCalls

        filteredList = when (activeFilter) {
            "incoming" -> filteredList.filter { it.type == CallLog.Calls.INCOMING_TYPE }
            "outgoing" -> filteredList.filter { it.type == CallLog.Calls.OUTGOING_TYPE }
            "missed" -> filteredList.filter { it.type == CallLog.Calls.MISSED_TYPE || it.type == CallLog.Calls.REJECTED_TYPE }
            else -> filteredList
        }

        if (searchQuery.isNotEmpty()) {
            filteredList = filteredList.filter {
                val nameMatch = it.name?.contains(searchQuery, ignoreCase = true) == true
                val numberMatch = it.number.contains(searchQuery, ignoreCase = true)
                nameMatch || numberMatch
            }
        }

        callAdapter.submitList(filteredList)
        binding.emptyText.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showAddTaskDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Dodaj zadanie")

        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)
        val etName = view.findViewById<EditText>(R.id.et_task_contact_name)
        val etPhone = view.findViewById<EditText>(R.id.et_task_phone)
        val etTitle = view.findViewById<EditText>(R.id.et_task_title)
        val btnContacts = view.findViewById<View>(R.id.btn_pick_contacts)
        val btnRecent = view.findViewById<View>(R.id.btn_pick_recent)

        dialogNameEdit = etName
        dialogPhoneEdit = etPhone

        btnContacts.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            contactPickerLauncher.launch(intent)
        }

        btnRecent.setOnClickListener {
            showRecentCallsPickerDialog()
        }

        builder.setView(view)
        builder.setPositiveButton("Dodaj") { dialog, _ ->
            val name = etName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val title = etTitle.text.toString().trim()

            if (phone.isEmpty() || title.isEmpty()) {
                Toast.makeText(this, "Numer telefonu i opis są wymagane", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            taskManager.addTask(name, phone, title)
            loadTasks()
            dialog.dismiss()
        }
        builder.setNegativeButton("Anuluj") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.setOnDismissListener {
            dialogNameEdit = null
            dialogPhoneEdit = null
        }
        dialog.show()
    }

    private fun showRecentCallsPickerDialog() {
        val uniqueRecentCalls = allCalls.distinctBy { it.number.filter { c -> c.isDigit() }.takeLast(9) }.take(15)
        if (uniqueRecentCalls.isEmpty()) {
            Toast.makeText(this, "Brak ostatnich połączeń na liście", Toast.LENGTH_SHORT).show()
            return
        }

        val displayItems = uniqueRecentCalls.map {
            if (!it.name.isNullOrBlank()) "${it.name} (${it.number})" else it.number
        }.toTypedArray()

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Wybierz z ostatnich połączeń")
        builder.setItems(displayItems) { _, which ->
            val selectedCall = uniqueRecentCalls[which]
            dialogPhoneEdit?.setText(selectedCall.number)
            if (!selectedCall.name.isNullOrBlank()) {
                dialogNameEdit?.setText(selectedCall.name)
            } else {
                dialogNameEdit?.setText("")
            }
        }
        builder.create().show()
    }
}
