package com.example.ewsbanjirulm.ui.main

import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class MainViewModel : ViewModel() {

    private val database = Firebase.database.reference

    private val _dataList = MutableLiveData<List<String>>()
    val dataList: LiveData<List<String>>
        get() = _dataList

    init {
        getDataFromFirebase()
    }

    fun getDataFromFirebase() {

        val path0Ref = database.child("Realtime/lokasi_1/update_at")
        val path1Ref = database.child("Realtime/lokasi_1/dht22/temperature")
        val path2Ref = database.child("Realtime/lokasi_1/dht22/kelembaban")
        val path3Ref = database.child("Realtime/lokasi_1/total_tip")
        val path4Ref = database.child("Realtime/lokasi_1/tinggi_air_sungai")

        val dataTasks = mutableListOf<Task<DataSnapshot>>()

        // Mengambil data dari setiap path secara asynchronous

        dataTasks.add(path0Ref.get())
        dataTasks.add(path1Ref.get())
        dataTasks.add(path2Ref.get())
        dataTasks.add(path3Ref.get())
        dataTasks.add(path4Ref.get())

        // Menggunakan Task.whenAllSuccess() untuk menunggu hingga semua tugas selesai
        Tasks.whenAllSuccess<DataSnapshot>(dataTasks)
            .addOnSuccessListener { snapshots ->
                val dataList = mutableListOf<String>()

                // Mengambil nilai dari setiap DataSnapshot dan menyimpannya dalam dataList
                for (snapshot in snapshots) {
                    val value = snapshot.getValue(String::class.java)
                    if (value != null) {
                        dataList.add(value)
                    }
                }

                _dataList.value = dataList
            }
            .addOnFailureListener { exception ->
                // Penanganan kesalahan jika terjadi
                Log.e("ExampleViewModel", "Error getting data: ${exception.message}")
            }
    }
    fun getStatusBanjir(suhu: Double, kelembaban:Double, curahHujan: Double, tinggiAir: Double): String{
        val rules = rulesPotensi(suhu, kelembaban)
        val centroid = evaluateRules(rules)

        val rules1 = rulesResiko(centroid, curahHujan)
        val centroid1 = evaluateRules(rules1)

        val rules2 = rulesStatus(centroid1, tinggiAir)
        val centroid2 = evaluateRules(rules2)
        val labelStatus = classifyOutput(centroid2)


        return labelStatus
    }

    fun classifyOutput(centroid: Double): String{
        val rendah = fuzzifyClassifyRendah(centroid)
        val sedang = fuzzifyClassifySedang(centroid)
        val tinggi = fuzzifyClassifyTinggi(centroid)
        val kategori = maxOf(rendah, sedang, tinggi)
        return when(kategori){
            rendah -> "rendah"
            sedang -> "sedang"
            tinggi -> "tinggi"
            else -> "notDefined"
        }
    }

    fun getLabelInput1(a:Double=0.0, b:Double=0.0, c:Double=0.0, d:Double=0.0): Array<String>{
        val suhu = a
        val kelembaban = b
        val curahHujan = c
        val tinggiAir = d

        val suhuRendah = fuzzifySuhuRendah(suhu)
        val suhuSedang = fuzzifySuhuSedang(suhu)
        val suhuTinggi = fuzzifySuhuTinggi(suhu)
        val fSuhuMax= maxOf(suhuRendah, suhuSedang, suhuTinggi)

        val kelembabanRendah = fuzzifyKelembabanRendah(kelembaban)
        val kelembabanSedang = fuzzifyKelembabanSedang(kelembaban)
        val kelembabanTinggi = fuzzifyKelembabanTinggi(kelembaban)
        val fKelembabanMax = maxOf(kelembabanRendah, kelembabanSedang, kelembabanTinggi)

        val curahHujanRendah = fuzzifyCurahHujanRendah(curahHujan)
        val curahHujanSedang = fuzzifyCurahHujanSedang(curahHujan)
        val curahHujanTinggi = fuzzifyCurahHujanTinggi(curahHujan)
        val fCurahHujanMax = maxOf(curahHujanRendah, curahHujanSedang, curahHujanTinggi)

        val tinggiAirRendah = fuzzifyTinggiAirRendah(tinggiAir)
        val tinggiAirSedang = fuzzifyTinggiAirSedang(tinggiAir)
        val tinggiAirTinggi = fuzzifyTinggiAirTinggi(tinggiAir)
        val fTinggiAirMax = maxOf(tinggiAirRendah, tinggiAirSedang, tinggiAirTinggi)


        val label1 = when(fSuhuMax) {
            suhuRendah -> "rendah"
            suhuSedang -> "sedang"
            suhuTinggi -> "tinggi"
            else -> "notDefined"
        }

        val label2 = when(fKelembabanMax) {
            kelembabanRendah -> "rendah"
            kelembabanSedang -> "sedang"
            kelembabanTinggi -> "tinggi"
            else -> "notDefined"
        }

        val label3 = when(fCurahHujanMax) {
            curahHujanRendah -> "rendah"
            curahHujanSedang -> "sedang"
            curahHujanTinggi -> "tinggi"
            else -> "notDefined"
        }

        val label4 = when(fTinggiAirMax) {
            tinggiAirRendah -> "rendah"
            tinggiAirSedang -> "sedang"
            tinggiAirTinggi -> "tinggi"
            else -> "notDefined"
        }


        return arrayOf(label1, label2, label3, label4)

    }


    // =========================================== EVALUATE RULES ============================================
    private fun evaluateRules(rules: Array<DoubleArray>): Double{
        val aggregateValues = fisAggregation(rules,
            fuzzifyOutputRendah(),
            fuzzifyOutputSedang(),
            fuzzifyOutputTinggi())
        return getCentroid(aggregateValues)
    }
    // =========================================== DEFINE RULES ============================================
    private fun rulesPotensi(suhu: Double, kelembaban: Double): Array<DoubleArray> {
        val rules = Array(9) { DoubleArray(3) }

        val suhuRendah = fuzzifySuhuRendah(suhu)
        val suhuSedang = fuzzifySuhuSedang(suhu)
        val suhuTinggi = fuzzifySuhuTinggi(suhu)

        val kelembabanRendah = fuzzifyKelembabanRendah(kelembaban)
        val kelembabanSedang = fuzzifyKelembabanSedang(kelembaban)
        val kelembabanTinggi = fuzzifyKelembabanTinggi(kelembaban)

        // RULE 1 = Potensi Rendah
        rules[0][0] = minOf(suhuRendah, kelembabanRendah)
        // RULE 2 = Potensi Rendah
        rules[1][0] = minOf(suhuRendah, kelembabanSedang)
        // RULE 3 = Potensi Sedang
        rules[2][1] = minOf(suhuRendah, kelembabanTinggi)
        // RULE 4 = Potensi Rendah
        rules[3][0] = minOf(suhuSedang, kelembabanRendah)
        // RULE 5 = Potensi Sedang
        rules[4][1] = minOf(suhuSedang, kelembabanSedang)
        // RULE 6 = Potensi Tinggi
        rules[5][2] = minOf(suhuSedang, kelembabanTinggi)
        // RULE 7 = Potensi Sedang
        rules[6][1] = minOf(suhuTinggi, kelembabanRendah)
        // RULE 8 = Potensi Tinggi
        rules[7][2] = minOf(suhuTinggi, kelembabanSedang)
        // RULE 9 = Potensi Tinggi
        rules[8][2] = minOf(suhuTinggi, kelembabanTinggi)

        return rules
    }

    private fun rulesResiko(potensi: Double, curahHujan: Double): Array<DoubleArray> {
        val rules = Array(9) { DoubleArray(3) }

        val potensiRendah = fuzzifyClassifyRendah(potensi)
        val potensiSedang = fuzzifyClassifySedang(potensi)
        val potensiTinggi = fuzzifyClassifyTinggi(potensi)

        val curahHujanRendah = fuzzifyCurahHujanRendah(curahHujan)
        val curahHujanSedang = fuzzifyCurahHujanSedang(curahHujan)
        val curahHujanTinggi = fuzzifyCurahHujanTinggi(curahHujan)

        // RULE 1 = Resiko Rendah
        rules[0][0] = minOf(potensiRendah, curahHujanRendah)
        // RULE 2 = Resiko Sedang
        rules[1][1] = minOf(potensiRendah, curahHujanSedang)
        // RULE 3 = Resiko Tinggi
        rules[2][2] = minOf(potensiRendah, curahHujanTinggi)
        // RULE 4 = Resiko Rendah
        rules[3][0] = minOf(potensiSedang, curahHujanRendah)
        // RULE 5 = Resiko Tinggi
        rules[4][2] = minOf(potensiSedang, curahHujanSedang)
        // RULE 6 = Resiko Tinggi
        rules[5][2] = minOf(potensiSedang, curahHujanTinggi)
        // RULE 7 = Resiko Sedang
        rules[6][1] = minOf(potensiTinggi, curahHujanRendah)
        // RULE 8 = Resiko Tinggi
        rules[7][2] = minOf(potensiTinggi, curahHujanSedang)
        // RULE 9 = Resiko Tinggi
        rules[8][2] = minOf(potensiTinggi, curahHujanTinggi)

        return rules
    }

    private  fun rulesStatus(resiko: Double, tinggiAir: Double): Array<DoubleArray> {
        val rules = Array(9) { DoubleArray(3) }

        val resikoRendah = fuzzifyClassifyRendah(resiko)
        val resikoSedang = fuzzifyClassifySedang(resiko)
        val resikoTinggi = fuzzifyClassifyTinggi(resiko)

        val tinggiAirRendah = fuzzifyTinggiAirRendah(tinggiAir)
        val tinggiAirSedang = fuzzifyTinggiAirSedang(tinggiAir)
        val tinggiAirTinggi = fuzzifyTinggiAirTinggi(tinggiAir)

        // RULE 1 = Status Aman
        rules[0][0] = minOf(resikoRendah, tinggiAirRendah)
        // RULE 2 = Status Aman
        rules[1][0] = minOf(resikoRendah, tinggiAirSedang)
        // RULE 3 = Status Bahaya
        rules[2][2] = minOf(resikoRendah, tinggiAirTinggi)
        // RULE 4 = Status Waspada
        rules[3][1] = minOf(resikoSedang, tinggiAirRendah)
        // RULE 5 = Status Waspada
        rules[4][1] = minOf(resikoSedang, tinggiAirSedang)
        // RULE 6 = Status Bahaya
        rules[5][2] = minOf(resikoSedang, tinggiAirTinggi)
        // RULE 7 = Status Waspada
        rules[6][1] = minOf(resikoTinggi, tinggiAirRendah)
        // RULE 8 = Status Bahaya
        rules[7][2] = minOf(resikoTinggi, tinggiAirSedang)
        // RULE 9 = Status Bahaya
        rules[8][2] = minOf(resikoTinggi, tinggiAirTinggi)

        return rules
    }

    // ========================================= FUZZIFIKASI ==================================
// ================================= INPUT ======================================
// Fuzzifikasi Variabel suhu
    private fun fuzzifySuhuRendah(suhu: Double): Double {
        return trapmf(suhu, listOf(0.0, 0.0, 20.0, 25.0))
    }

    private fun fuzzifySuhuSedang(suhu: Double): Double {
        return trimf(suhu, listOf(23.0, 27.0, 32.0))
    }

    private fun fuzzifySuhuTinggi(suhu: Double): Double {
        return trapmf(suhu, listOf(30.0, 34.0, 50.0, 50.0))
    }

    // Fuzzifikasi Variabel Kelembaban
    private fun fuzzifyKelembabanRendah(kelembaban: Double): Double {
        return trapmf(kelembaban, listOf(0.0, 0.0, 50.0, 80.0))
    }

    private fun fuzzifyKelembabanSedang(kelembaban: Double): Double {
        return trimf(kelembaban, listOf(78.0, 83.0, 88.0))
    }

    private fun fuzzifyKelembabanTinggi(kelembaban: Double): Double {
        return trapmf(kelembaban, listOf(86.0, 100.0, 100.0, 100.0))
    }

    // Fuzzifikasi Variabel Curah Hujan
    private fun fuzzifyCurahHujanRendah(curahHujan: Double): Double {
        return trapmf(curahHujan, listOf(0.0, 0.0, 0.0, 50.0))
    }

    private fun fuzzifyCurahHujanSedang(curahHujan: Double): Double {
        return trapmf(curahHujan, listOf(30.0, 60.0, 75.0, 100.0))
    }

    private fun fuzzifyCurahHujanTinggi(curahHujan: Double): Double {
        return trapmf(curahHujan, listOf(75.0, 150.0, 250.0, 250.0))
    }

    // Fuzzifikasi Variabel Tinggi Air
    private fun fuzzifyTinggiAirRendah(tinggiAir: Double): Double {
        return trapmf(tinggiAir, listOf(0.0, 0.0, 0.0, 100.0))
    }

    private fun fuzzifyTinggiAirSedang(tinggiAir: Double): Double {
        return trimf(tinggiAir, listOf(50.0, 100.0, 150.0))
    }

    private fun fuzzifyTinggiAirTinggi(tinggiAir: Double): Double {
        return trapmf(tinggiAir, listOf(100.0, 200.0, 200.0, 200.0))
    }

    // fuzzifikasi Output untuk kategori
    private fun fuzzifyClassifyRendah(centroid: Double): Double {
        return trapmf(centroid, listOf(0.0, 0.0, 0.0, 3.0))
    }

    private fun fuzzifyClassifySedang(centroid: Double): Double {
        return trimf(centroid, listOf(2.0, 3.0, 4.0))
    }

    private fun fuzzifyClassifyTinggi(centroid: Double): Double {
        return trapmf(centroid, listOf(3.0, 5.0, 5.0, 5.0))
    }

//=============================== OUTPUT =================================

    // Fuzzifikasi Output Potensi Rendah
    private fun fuzzifyOutputRendah(): MutableList<Double> {
        return getTrapmfPlots(0, 5, listOf(0.0, 0.0, 0.0, 3.0), "left")
    }

    private fun fuzzifyOutputSedang(): MutableList<Double> {
        return getTrimfPlots(0, 5, listOf(2.0, 3.0, 4.0))
    }

    private fun fuzzifyOutputTinggi(): MutableList<Double> {
        return getTrapmfPlots(0, 5, listOf(3.0, 5.0, 5.0, 5.0), "right")
    }


    // ============================================= Aggregasi =====================================================
    private fun fisAggregation(rules: Array<DoubleArray>, rendah: MutableList<Double>, sedang: MutableList<Double>, tinggi: MutableList<Double>): DoubleArray {
        val result = DoubleArray(5)
        for (rule in rules.indices) {
            for (i in 0 until 5) {
                if (rules[rule][0] > 0 && i < 3) {
                    result[i] = maxOf(rules[rule][0], rendah[i])
                }
                if (rules[rule][1] > 0 && i > 2 && i < 4) {
                    result[i] = maxOf(rules[rule][1], sedang[i])
                }
                if (rules[rule][2] > 0 && i > 3 && i < 5) {
                    result[i] = maxOf(rules[rule][2], tinggi[i])
                }
            }
        }
        return result
    }


    // ============================================= Fuzzy Logic ==================================================
    private fun trimf(x: Double, points: List<Double>): Double {
        val pointA = points[0]
        val pointB = points[1]
        val pointC = points[2]
        val slopeAB = getSlope(pointA, 0.0, pointB, 1.0)
        val slopeBC = getSlope(pointB, 1.0, pointC, 0.0)
        var result = 0.0
        if (x >= pointA && x <= pointB) {
            result = slopeAB * x + getYIntercept(pointA, 0.0, pointB, 1.0)
        } else if (x >= pointB && x <= pointC) {
            result = slopeBC * x + getYIntercept(pointB, 1.0, pointC, 0.0)
        }
        return result
    }

    fun trapmf(x: Double, points: List<Double>): Double {
        val pointA = points[0]
        val pointB = points[1]
        val pointC = points[2]
        val pointD = points[3]
        val slopeAB = getSlope(pointA, 0.0, pointB, 1.0)
        val slopeCD = getSlope(pointC, 1.0, pointD, 0.0)
        val yInterceptAB = getYIntercept(pointA, 0.0, pointB, 1.0)
        val yInterceptCD = getYIntercept(pointC, 1.0, pointD, 0.0)
        var result = 0.0
        if (x > pointA && x < pointB) {
            result = slopeAB * x + yInterceptAB
        } else if (x >= pointB && x <= pointC) {
            result = 1.0
        } else if (x > pointC && x < pointD) {
            result = slopeCD * x + yInterceptCD
        }
        return result
    }

    fun getSlope(x1: Double, y1: Double, x2: Double, y2: Double): Double {
        // Avoid zero division error of vertical line for shouldered trapmf
        var slope: Double
        try {
            slope = (y2 - y1) / (x2 - x1)
        } catch (e: ArithmeticException) {
            slope = 0.0
        }
        return slope
    }

    fun getYIntercept(x1: Double, y1: Double, x2: Double, y2: Double): Double {
        val m = getSlope(x1, y1, x2, y2)
        val y: Double
        val x: Double
        if (y1 < y2) {
            y = y2
            x = x2
        } else {
            y = y1
            x = x1
        }
        return y - m * x
    }

    fun getTrimfPlots(start: Int, end: Int, points: List<Double>): MutableList<Double> {
        val plots = MutableList(end - start + 1) { 0.0 }
        val pointA = points[0]
        val pointB = points[1]
        val pointC = points[2]
        val slopeAB = getSlope(pointA, 0.0, pointB, 1.0)
        val slopeBC = getSlope(pointB, 1.0, pointC, 0.0)
        val yInterceptAB = getYIntercept(pointA, 0.0, pointB, 1.0)
        val yInterceptBC = getYIntercept(pointB, 1.0, pointC, 0.0)
        for (i in pointA.toInt() until pointB.toInt()) {
            plots[i] = slopeAB * i + yInterceptAB
        }
        for (i in pointB.toInt() until pointC.toInt()) {
            plots[i] = slopeBC * i + yInterceptBC
        }
        return plots
    }

    fun getTrapmfPlots(start: Int, end: Int, points: List<Double>, shoulder: String?): MutableList<Double> {
        val plots = MutableList(end - start + 1) { 0.0 }
        val pointA = points[0]
        val pointB = points[1]
        val pointC = points[2]
        val pointD = points[3]
        val slopeAB = getSlope(pointA, 0.0, pointB, 1.0)
        val slopeCD = getSlope(pointC, 1.0, pointD, 0.0)
        val yInterceptAB = getYIntercept(pointA, 0.0, pointB, 1.0)
        val yInterceptCD = getYIntercept(pointC, 1.0, pointD, 0.0)
        if (shoulder == "left") {
            for (i in start until pointA.toInt()) {
                plots[i] = 1.0
            }
        } else if (shoulder == "right") {
            for (i in pointD.toInt() until end) {
                plots[i] = 1.0
            }
        }
        for (i in pointA.toInt() until pointB.toInt()) {
            plots[i] = slopeAB * i + yInterceptAB
        }
        for (i in pointB.toInt() until pointC.toInt()) {
            plots[i] = 1.0
        }
        for (i in pointC.toInt() until pointD.toInt()) {
            plots[i] = slopeCD * i + yInterceptCD
        }
        return plots
    }

    fun getCentroid(aggregatedPlots: DoubleArray): Double {
        val n = aggregatedPlots.size
        val xAxis = DoubleArray(n) { it.toDouble() }
        var centroidNum = 0.0
        var centroidDenum = 0.0
        for (i in 0 until n) {
            centroidNum += xAxis[i] * aggregatedPlots[i]
            centroidDenum += aggregatedPlots[i]
        }

        return if (centroidDenum >= 0.0) {
            (centroidNum / centroidDenum)
        } else {
            0.0 // Atau nilai default lainnya sesuai kebutuhan
        }
    }

// ======================================================== RUN RULE TESTING ==========================================


}