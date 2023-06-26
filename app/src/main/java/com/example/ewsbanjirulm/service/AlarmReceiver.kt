package com.example.ewsbanjirulm.service;

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.MutableLiveData
import com.example.ewsbanjirulm.ui.main.MainViewModel
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class AlarmReceiver : BroadcastReceiver()  {

    private val viewModel = MainViewModel()
    var dataBG = arrayOf("", "", "", "", "")

    override fun onReceive(context: Context, intent: Intent) {
        getAlarmBG(context)
    }

    private fun getAlarmBG(context: Context){

        val dataTasks = mutableListOf<Task<DataSnapshot>>()

        // Mengambil data dari setiap path secara asynchronous
        dataTasks.add(viewModel.path0Ref.get())
        dataTasks.add(viewModel.path1Ref.get())
        dataTasks.add(viewModel.path2Ref.get())
        dataTasks.add(viewModel.path3Ref.get())
        dataTasks.add(viewModel.path4Ref.get())

        // Menggunakan Task.whenAllSuccess() untuk menunggu hingga semua tugas selesai
        Tasks.whenAllSuccess<DataSnapshot>(dataTasks)
            .addOnSuccessListener { snapshots ->
                var i = 0
                for (snapshot in snapshots) {
                    val value = snapshot.getValue(String::class.java)
                    if (value != null) {
                        dataBG[i] = value
                        i++
                    }
                }

                val centroid = viewModel.getStatusBanjir(dataBG[1].toDouble(), dataBG[2].toDouble(), dataBG[3].toDouble(), dataBG[4].toDouble())
                val status = viewModel.classifyOutput(centroid[2])

                when (status) {
                    "rendah" -> {
                        Log.d("getAlarmBG","status banjir : AMAN (${centroid[2]}))")
                    }
                    "sedang" -> {
                        Log.d("getAlarmBG","status banjir : WASPADA (${centroid[2]}))")
                    }
                    "tinggi" -> {
                        Log.d("getAlarmBG","status banjir : BAHAYA (${centroid[2]}))")
                        val judulNotif = "STATUS BANJIR BAHAYA"
                        val isiNotif = "Status Banjir  BAHAYA"
                        viewModel.showNotification(context, judulNotif, isiNotif)
                    }
                    else -> {
                        Log.e("getAlarmBG","status banjir : ERROR")
                    }
                }
            }
            .addOnFailureListener { exception ->
                // Penanganan kesalahan jika terjadi
                Log.e("getAlarmBG", "Error getting data: ${exception.message}")
            }
    }

}

