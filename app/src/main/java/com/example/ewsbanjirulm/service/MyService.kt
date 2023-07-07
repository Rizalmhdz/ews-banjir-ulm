package com.example.ewsbanjirulm.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.util.Log
import com.example.ewsbanjirulm.ui.main.MainViewModel
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.DataSnapshot
import java.lang.Error

class MyService(context: Context) : Service() {
    private val handler = Handler()
    private val runnable = object : Runnable {
        override fun run() {
            // Lakukan tugas yang diinginkan di background
            // Misalnya, menjalankan perintah jaringan, pengolahan data, dll.
            try {
                getAlarmBG(context)
            } catch (e: Exception){
                Log.e("ServiceBG", "${e.message}")
            }

            // Jadwalkan kembali tugas setelah 4 menit
            handler.postDelayed(this, 4 * 60 * 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        // Jadwalkan tugas pertama kali saat Service dibuat
        handler.post(runnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Return START_STICKY agar Service akan direstart jika dihentikan secara tidak sengaja
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Return null karena kita tidak menggunakan bound service.
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        // Hentikan penjadwalan tugas saat Service dihancurkan
        handler.removeCallbacks(runnable)
    }

    private val viewModel = MainViewModel()
    var dataBG = arrayOf("", "", "", "", "")

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
                        Log.d("ServiceBG","status banjir : AMAN (${centroid[2]}))")
                    }
                    "sedang" -> {
                        Log.d("ServiceBG","status banjir : WASPADA (${centroid[2]}))")
                    }
                    "tinggi" -> {
                        Log.d("ServiceBG","status banjir : BAHAYA (${centroid[2]}))")
                        val judulNotif = "STATUS BANJIR BAHAYA"
                        val isiNotif = "Status Banjir  BAHAYA"
                        viewModel.showNotification(context, judulNotif, isiNotif)
                    }
                    else -> {
                        Log.e("ServiceBG","status banjir : ERROR")
                    }
                }
            }
            .addOnFailureListener { exception ->
                // Penanganan kesalahan jika terjadi
                Log.e("ServiceBG", "Error getting data: ${exception.message}")
            }
    }
}
