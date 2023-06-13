package com.example.ewsbanjirulm.ui.main

import android.content.ContentValues
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.os.Message
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.Observer
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

    private var dataList: List<String> = emptyList() // Variabel global untuk menyimpan dataList

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
            val status = viewModel.getStatusBanjir(suhu, kelembaban, curahHujan, tinggiAir)
            binding.titleStatus.text = status
        })
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

}