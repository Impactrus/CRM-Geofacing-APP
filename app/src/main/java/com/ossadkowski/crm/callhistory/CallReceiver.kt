package com.ossadkowski.crm.callhistory

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.provider.CallLog
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast

class CallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            if (state == TelephonyManager.EXTRA_STATE_IDLE) {
                Thread {
                    try {
                        Thread.sleep(1500)
                        checkLastCall(context)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }.start()
            }
        }
    }

    private fun checkLastCall(context: Context) {
        val contentResolver = context.contentResolver
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
                "${CallLog.Calls.DATE} DESC LIMIT 1"
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val number = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)) ?: ""
                    val cachedName = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)) ?: ""
                    
                    val taskManager = TaskManager(context)
                    val wasCompleted = taskManager.checkAndCompleteTask(number, cachedName)
                    if (wasCompleted) {
                        val handler = android.os.Handler(context.mainLooper)
                        handler.post {
                            Toast.makeText(
                                context,
                                "Połączenie z $number ($cachedName) zaliczyło zadanie!",
                                Toast.LENGTH_LONG
                            ).show()
                            
                            context.sendBroadcast(Intent("com.ossadkowski.crm.callhistory.REFRESH_TASKS"))
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("CallReceiver", "Brak uprawnień do rejestru przy weryfikacji ostatniego połączenia")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
