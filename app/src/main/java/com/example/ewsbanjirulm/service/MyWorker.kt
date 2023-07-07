package com.example.ewsbanjirulm.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.ewsbanjirulm.ui.main.MainViewModel
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.DataSnapshot

class MyWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Implementasikan tugas yang ingin Anda jalankan di latar belakang
        // di sini. Tugas ini akan dijalankan setiap 3 menit.

        try {
            getAlarmBG(applicationContext)
        } catch (e: Exception){
            Log.e("MyWorker", "${e.message}")
        }
        // Contoh: Menampilkan pesan log
        Log.d("MyWorker", "Tugas dijalankan di latar belakang")

        // Mengembalikan Result.success() jika tugas berhasil
        return Result.success()
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
                        Log.d("MyWorker","status banjir : AMAN (${centroid[2]}))")
                    }
                    "sedang" -> {
                        Log.d("MyWorker","status banjir : WASPADA (${centroid[2]}))")
                    }
                    "tinggi" -> {
                        Log.d("MyWorker","status banjir : BAHAYA (${centroid[2]}))")
                        val judulNotif = "STATUS BANJIR BAHAYA"
                        val isiNotif = "Status Banjir  BAHAYA"
                        viewModel.showNotification(context, judulNotif, isiNotif)
                    }
                    else -> {
                        Log.e("MyWorker","status banjir : ERROR")
                    }
                }
            }
            .addOnFailureListener { exception ->
                // Penanganan kesalahan jika terjadi
                Log.e("ServiceBG", "Error getting data: ${exception.message}")
            }
    }
}
