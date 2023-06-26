package com.example.ewsbanjirulm.ui.main

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.example.ewsbanjirulm.R
import com.example.ewsbanjirulm.SimulatorFragment
import com.example.ewsbanjirulm.databinding.FragmentMainBinding
import com.example.ewsbanjirulm.service.AlarmReceiver
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.eventbus.Subscribe
import java.util.Calendar

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private val interval : Long = 3 * 60 * 1000

    private lateinit var alarmManager: AlarmManager
    private lateinit var pendingIntent: PendingIntent

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    fun repeater(){
        // Set up the repeating task
        println("mulai repeeater")
        runnable = object : Runnable {
            override fun run() {
                // Do the task that needs to be repeated
                viewModel.getDataFromFirebase()

                // Schedule the next execution after a delay (e.g., 3 minutes)
                handler.postDelayed(this, interval) // 3 minutes in milliseconds
                Log.d("repeater", "data baru coy")
            }
        }
        // Start the repeating task
        handler.postDelayed(runnable, 0)
    }
    companion object {
        fun newInstance() = MainFragment()
    }
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), AlarmReceiver::class.java)
        pendingIntent = PendingIntent.getBroadcast(requireContext(), 0, intent, 0)
        startAlarm()
        repeater()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Observasi LiveData dari ViewModel untuk mendapatkan pembaruan data secara otomatis
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
                    viewModel.showNotification(requireContext(), judulNotif, isiNotif)

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
        stopAlarm()
        handler.removeCallbacks(runnable)
        _binding = null
    }

    private fun startAlarm() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.add(Calendar.SECOND, 3)

        // Set alarm untuk berulang setiap 5 menit
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            interval,  // interva dalam milidetik
            pendingIntent
        )
    }

    private fun stopAlarm() {
        // Batalkan alarm
        alarmManager.cancel(pendingIntent)
    }
}