package com.ossadkowski.crm.callhistory

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.CallLog
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.ossadkowski.crm.callhistory.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val callAdapter = CallAdapter()
    private var allCalls = listOf<CallItem>()
    private var activeFilter = "all"
    private var searchQuery = ""

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupFilters()
        setupSearch()

        binding.btnGrant.setOnClickListener {
            requestCallLogPermission()
        }

        checkAndLoad()
    }

    private fun checkAndLoad() {
        if (hasCallLogPermission()) {
            binding.permissionCard.visibility = View.GONE
            binding.statsContainer.visibility = View.VISIBLE
            binding.searchBar.visibility = View.VISIBLE
            loadCalls()
        } else {
            binding.permissionCard.visibility = View.VISIBLE
            binding.statsContainer.visibility = View.GONE
            binding.searchBar.visibility = View.GONE
            binding.emptyText.visibility = View.GONE
        }
    }

    private fun hasCallLogPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCallLogPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_CALL_LOG),
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
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkAndLoad()
            } else {
                Toast.makeText(this, "Brak uprawnień do rejestru połączeń", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = callAdapter
    }

    private fun loadCalls() {
        binding.progressBar.visibility = View.VISIBLE
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
            Toast.makeText(this, "Błąd zabezpieczeń przy czytaniu rejestru", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Błąd wczytywania połączeń: ${e.message}", Toast.LENGTH_LONG).show()
        }

        allCalls = callsList
        binding.progressBar.visibility = View.GONE

        calculateStats()
        applyFiltersAndSearch()
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

        // Filter by tab
        filteredList = when (activeFilter) {
            "incoming" -> filteredList.filter { it.type == CallLog.Calls.INCOMING_TYPE }
            "outgoing" -> filteredList.filter { it.type == CallLog.Calls.OUTGOING_TYPE }
            "missed" -> filteredList.filter { it.type == CallLog.Calls.MISSED_TYPE || it.type == CallLog.Calls.REJECTED_TYPE }
            else -> filteredList
        }

        // Search filter
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
}
