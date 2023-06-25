package com.example.ewsbanjirulm.ui.main

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.os.Message
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Observer
import com.example.ewsbanjirulm.R
import com.example.ewsbanjirulm.SimulatorFragment
import com.example.ewsbanjirulm.databinding.FragmentMainBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!


    companion object {
        fun newInstance() = MainFragment()
    }


    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Observasi LiveData dari ViewModel untuk mendapatkan pembaruan data secara otomatis
        // Mendapatkan referensi ke NotificationManager

        viewModel.dataList.observe(viewLifecycleOwner, Observer { dataList ->
            binding.textUpdateAt.text = dataList[0]
            binding.valueSuhu.text = dataList[1]
            binding.valueKelembaban.text = dataList[2]
            binding.valueCurahHujan.text = dataList[3]
            binding.valueTinggiAir.text = dataList[4]
            val suhu = dataList[1].toDouble()
            val kelembaban = dataList[2].toDouble()
            val curahHujan = dataList[3].toDouble()
            val tinggiAir = dataList[4].toDouble()
            val centroid = viewModel.getStatusBanjir(suhu, kelembaban, curahHujan, tinggiAir)
            val status = viewModel.classifyOutput(centroid[2])
            when (status) {
                "rendah" -> binding.alarmStatus.setImageResource(R.drawable.status_aman)
                "sedang" -> binding.alarmStatus.setImageResource(R.drawable.status_waspada)
                "tinggi" -> {
                    val judulNotif = "STATUS BANJIR BAHAYA"
                    val isiNotif = "Resiko Banjir Cukup Membahayakan"
                    binding.alarmStatus.setImageResource(R.drawable.status_bahaya)
                    showNotification(requireContext(), judulNotif, isiNotif)

                }
                else -> { // Note the block
                    binding.alarmStatus.setImageResource(com.google.android.material.R.drawable.mtrl_ic_error)
                }
            }
        })

        binding.alarmStatus.setOnClickListener {
            // Memanggil fungsi pengambilan data dari ViewModel
            viewModel.getDataFromFirebase()
        }

        // Menambahkan listener untuk tombol refresh
        binding.simulasi.setOnClickListener {
            val fragmentSimulator = SimulatorFragment()
            val fragmentManager = requireActivity().supportFragmentManager
            fragmentManager.beginTransaction()
                .replace(R.id.container, fragmentSimulator)
                .addToBackStack(null) // Tambahkan ke back stack agar bisa kembali ke Fragment A
                .commit()
        }

        binding.rekap.setOnClickListener {
            val url = "https://riset-banjir-ulm-2023.tech/ews_banjir_ulm.php" // Ganti dengan URL yang diinginkan
            var intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Fungsi untuk membuat dan menampilkan notifikasi
    fun showNotification(context: Context, title: String, message: String) {
        // Buat ID unik untuk notifikasi
        val notificationId = 1

        // Buat instance dari NotificationManager
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Cek versi Android, karena konfigurasi notifikasi berbeda pada versi Android tertentu
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Buat channel untuk notifikasi (hanya perlu dilakukan sekali)
            val channelId = "my_channel_id"
            val channelName = "My Channel"
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        // Buat builder untuk notifikasi
        val builder = NotificationCompat.Builder(context, "my_channel_id")
            .setSmallIcon(R.drawable.icon_curah_hujan) // Ikon kecil notifikasi
            .setContentTitle(title) // Judul notifikasi
            .setContentText(message) // Isi notifikasi
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Prioritas notifikasi

        // Tampilkan notifikasi sebagai popup (heads-up notification) jika perangkat mendukung
        builder.setDefaults(NotificationCompat.DEFAULT_ALL)

        // Set efek dering pada notifikasi
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        builder.setSound(soundUri)

        // Tampilkan notifikasi
        notificationManager.notify(notificationId, builder.build())
    }
}