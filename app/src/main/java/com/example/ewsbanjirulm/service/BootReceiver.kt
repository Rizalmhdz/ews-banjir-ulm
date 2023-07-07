package com.example.ewsbanjirulm.service

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val JOB_ID = 113
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

            val jobInfo = JobInfo.Builder(JOB_ID, ComponentName(context, MyJobService()::class.java))
                .setPeriodic(3 * 60 * 1000) // Interval 5 menit
                .setPersisted(true) // Bertahan bahkan setelah reboot
                .build()

            Log.d("JobService", "Job dimulai")

            jobScheduler.schedule(jobInfo)
        }
    }
}
