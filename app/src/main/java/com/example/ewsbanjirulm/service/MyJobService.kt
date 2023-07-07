package com.example.ewsbanjirulm.service

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.util.Log
import com.example.ewsbanjirulm.ui.main.MainViewModel
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.DataSnapshot

class MyJobService() : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        // Jalankan tugas Anda di sini
        // Misalnya, panggil fungsi atau metode yang perlu dijalankan secara terjadwal

        // Selesaikan tugas dan kembalikan false jika tugas selesai
        // Jika ada tugas yang berlanjut, kembalikan true dan panggil jobFinished() saat tugas selesai
        try {
            getAlarmBG(applicationContext)
        } catch (e: Exception){
            Log.e("JobService", "${e.message}")
        }

        jobFinished(params, false) // Contoh: Menyelesaikan tugas dan menghentikan Job

        return false
    }

    override fun onStopJob(params: JobParameters): Boolean {
        // Panggil ini jika Anda ingin menghentikan tugas yang sedang berjalan

        return false // Kembalikan false jika tugas tidak perlu di-restart
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
                        Log.d("JobService","status banjir : AMAN (${centroid[2]}))")
                    }
                    "sedang" -> {
                        Log.d("JobService","status banjir : WASPADA (${centroid[2]}))")
                    }
                    "tinggi" -> {
                        Log.d("JobService","status banjir : BAHAYA (${centroid[2]}))")
                        val judulNotif = "STATUS BANJIR BAHAYA"
                        val isiNotif = "Status Banjir  BAHAYA"
                        viewModel.showNotification(context, judulNotif, isiNotif)
                    }
                    else -> {
                        Log.e("JobService","status banjir : ERROR")
                    }
                }
            }
            .addOnFailureListener { exception ->
                // Penanganan kesalahan jika terjadi
                Log.e("JobService", "Error getting data: ${exception.message}")
            }
    }
}
