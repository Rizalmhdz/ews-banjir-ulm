package com.example.ewsbanjirulm.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Buat PendingIntent untuk memulai Service
            val serviceIntent = Intent(context, MyService(context)::class.java)
            val pendingIntent = PendingIntent.getService(
                context,
                0,
                serviceIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            // Atur alarm dengan interval waktu tertentu menggunakan AlarmManager
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intervalMillis : Long= 5 * 1 * 1000 // Interval 5 menit dalam milidetik
            val triggerAtMillis = System.currentTimeMillis() + intervalMillis
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                intervalMillis,
                pendingIntent
            )
            Log.d("ServiceBG", "Service dimulai")
        } else {
            Log.v("ServiceBG", "Service gagal dimulai")
        }

        // Buat PendingIntent untuk memulai Service
        val serviceIntent = Intent(context, MyService(context)::class.java)
        val pendingIntent = PendingIntent.getService(
            context,
            0,
            serviceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Atur alarm dengan interval waktu tertentu menggunakan AlarmManager
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intervalMillis : Long= 5 * 1 * 1000 // Interval 5 menit dalam milidetik
        val triggerAtMillis = System.currentTimeMillis() + intervalMillis
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            intervalMillis,
            pendingIntent
        )
        Log.d("ServiceBG", "Service dimulai")
    }
}
