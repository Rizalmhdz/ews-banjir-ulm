package com.example.ewsbanjirulm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.ewsbanjirulm.databinding.FragmentMainBinding
import com.example.ewsbanjirulm.databinding.FragmentSimulatorBinding
import com.example.ewsbanjirulm.ui.main.MainFragment
import com.example.ewsbanjirulm.ui.main.MainViewModel
import java.text.DecimalFormat

class SimulatorFragment : Fragment() {
    private var _binding: FragmentSimulatorBinding? = null
    private val binding get() = _binding!!


    companion object {
        fun newInstance() = SimulatorFragment()
    }


    private lateinit var viewModel: MainViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        placeHolder(binding.valueSuhu)
        placeHolder(binding.valueKelembaban)
        placeHolder(binding.valueCurahHujan)
        placeHolder(binding.valueTinggiAir)

        binding.alarmStatus.setOnClickListener {
            // Memanggil fungsi pengambilan data dari ViewModel
            viewModel.getDataFromFirebase()
        }

        // Menambahkan fungsi untuk simulasi
        binding.simulasi.setOnClickListener {
            val suhu = binding.valueSuhu.text.toString()
            val kelembaban = binding.valueKelembaban.text.toString()
            val curahHujan = binding.valueCurahHujan.text.toString()
            val tinggiAir = binding.valueTinggiAir.text.toString()
            // Cek jika teks kosong
            if (suhu.isNotEmpty() && kelembaban.isNotEmpty() && curahHujan.isNotEmpty() && tinggiAir.isNotEmpty()) {
                val centroid = viewModel.getStatusBanjir(suhu.toDouble(), kelembaban.toDouble(), curahHujan.toDouble(), tinggiAir.toDouble())
                val label = viewModel.getLabelInput(suhu.toDouble(), kelembaban.toDouble(), curahHujan.toDouble(), tinggiAir.toDouble())
                binding.kelasSuhu.text = label[0]
                binding.kelasKelembaban.text = label[1]
                binding.kelasCurahHujan.text = label[2]
                binding.kelasTinggiAir.text = label[3]

                val status = viewModel.classifyOutput(centroid[2])
                binding.centroidPotensi.text = DecimalFormat("#.###").format(centroid[0]).toString()
                binding.centroidResiko.text = DecimalFormat("#.###").format(centroid[1]).toString()
                binding.centroidStatus.text = DecimalFormat("#.###").format(centroid[2]).toString()
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
            } else {
                // Tindakan yang akan dilakukan jika nilai EditText kosong
                Toast.makeText(requireContext(), "Nilai harus diisi!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun placeHolder(editText: EditText){
        editText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                editText.hint = ""
            } else {
                editText.hint = "Input"
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSimulatorBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}