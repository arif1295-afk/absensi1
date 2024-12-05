package com.arif.absensi1

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class qrscanenr : AppCompatActivity() {
    private lateinit var database: DatabaseReference
    private var userId: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_qrscanenr)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        database = FirebaseDatabase.getInstance(
            "https://penggajian-b318f-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).reference

        // Retrieve userId from SharedPreferences
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        userId = sharedPref.getString("userId", "Unknown")

        // Start QR Scanner
        initiateQRScanner()
    }
    private fun initiateQRScanner() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setBeepEnabled(true)
        integrator.setPrompt("Arahkan ke QR Code untuk Scan")
        integrator.setOrientationLocked(false)
        integrator.initiateScan()
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val result: IntentResult? = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents != null && result.contents.isNotEmpty()) {
            val scannedContent = result.contents
            Log.d("QRCodeResult", "Scanned data: $scannedContent")
            validateQRCode(scannedContent)
        } else {
            Toast.makeText(this, "Pemindaian dibatalkan atau hasilnya kosong", Toast.LENGTH_SHORT).show()
            navigateToDashboard()
        }
    }
    private fun validateQRCode(scannedData: String) {
        Log.d("validateQRCode", "Scanning data: $scannedData")

        try {
            // Query Firebase to check if scanned data matches any record in QRData
            database.child("QRData").orderByChild("data").equalTo(scannedData)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            Log.d("FirebaseQuery", "Snapshot found: ${snapshot.value}")
                            // Now there's no check for attendance, simply perform the action
                            performQRCodeAction(scannedData, true)
                        } else {
                            Toast.makeText(this@qrscanenr, "Token Invalid", Toast.LENGTH_SHORT).show()
                            navigateToDashboard()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("FirebaseQuery", "Error verifying QR code: ${error.message}")
                        Toast.makeText(this@qrscanenr, "Gagal memverifikasi QR code", Toast.LENGTH_SHORT).show()
                        navigateToDashboard()
                    }
                })
        } catch (e: Exception) {
            Log.e("QRCodeValidation", "Error during QR code validation: ${e.message}")
            Toast.makeText(this@qrscanenr, "Error occurred while validating the QR code", Toast.LENGTH_SHORT).show()
            navigateToDashboard()
        }
    }

    private fun performQRCodeAction(scannedData: String, isValid: Boolean) {
        if (isValid) {
            val currentDate = SimpleDateFormat("d", Locale.getDefault()).format(Date())
            val currentMonth = SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())
            val currentYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())

            val userRef = database.child("users").child(userId!!).child("datatabel")
                .child("$currentMonth-$currentYear")
                .child(currentDate)

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val shift1 = snapshot.child("Shift 1").getValue(Boolean::class.java) ?: false
                    val shift2 = snapshot.child("Shift 2").getValue(Boolean::class.java) ?: false

                    // Mendapatkan waktu sekarang dalam WIB
                    val wibTimeZone = TimeZone.getTimeZone("Asia/Jakarta")
                    val currentTime = Calendar.getInstance(wibTimeZone)
                    val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)

                    val timePeriod = when {
                        shift1 && currentHour >= 6 -> "shift 1"
                        shift2 && currentHour >= 14 -> "shift 2"
                        else -> null
                    }

                    if (timePeriod == null) {
                        Toast.makeText(
                            this@qrscanenr,
                            "Belum memasuki jam shift atau tidak ada jadwal shift untuk hari ini",
                            Toast.LENGTH_SHORT
                        ).show()
                        navigateToDashboard()
                    } else {
                        handleScan(scannedData, timePeriod)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("QRCodeAction", "Error checking shift: ${error.message}")
                    Toast.makeText(this@qrscanenr, "Gagal memeriksa jadwal shift", Toast.LENGTH_SHORT).show()
                    navigateToDashboard()
                }
            })
        } else {
            Toast.makeText(this, "Token Tidak Valid", Toast.LENGTH_SHORT).show()
            navigateToDashboard()
        }
    }


    // Modifikasi handleScan untuk menerima timePeriod
    private fun handleScan(scannedData: String, timePeriod: String) {
        val currentDate = SimpleDateFormat("d", Locale.getDefault()).format(Date())
        val currentMonth = SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())
        val currentYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())

        // Membuat SimpleDateFormat untuk WIB (zona waktu Asia/Jakarta)
        val wibTimeZone = TimeZone.getTimeZone("Asia/Jakarta")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        dateFormat.timeZone = wibTimeZone // Set zona waktu ke WIB

        // Mendapatkan waktu saat ini dalam WIB
        val currentTimeWIB = dateFormat.format(Date())
        val calendarWIB = Calendar.getInstance(wibTimeZone)
        val currentHour = calendarWIB.get(Calendar.HOUR_OF_DAY)

        // Cek apakah waktu sudah melewati batas shift
        val isLate = when (timePeriod) {
            "shift 1" -> currentHour >= 15 // Shift 1 selesai pukul 15:00 WIB
            "shift 2" -> currentHour >= 23 // Shift 2 selesai pukul 23:00 WIB
            else -> false
        }

        if (isLate) {
            val lateMessage = when (timePeriod) {
                "shift 1" -> "Kamu sudah melewati jam shift 1, tidak bisa scan lagi."
                "shift 2" -> "Kamu sudah melewati jam shift 2, tidak bisa scan lagi."
                else -> "Kamu sudah melewati jam shift, tidak bisa scan lagi."
            }
            Toast.makeText(this@qrscanenr, lateMessage, Toast.LENGTH_SHORT).show()
            navigateToDashboard()
            return
        }


        val userRef = database.child("users").child(userId!!).child("datatabel")
            .child("$currentMonth-$currentYear").child(currentDate)

        // Mengecek jumlah scan yang sudah tercatat untuk hari ini
        userRef.child("attendance").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val scanCount = snapshot.childrenCount.toInt()

                when (scanCount) {
                    0 -> saveScanEvent(userRef, "attendance1", scannedData, currentTimeWIB, timePeriod, "scan1_done")
                    1 -> saveScanEvent(userRef, "attendance2", scannedData, currentTimeWIB, timePeriod, "scan2_done")
                    else -> Toast.makeText(this@qrscanenr, "Batas scan tercapai untuk hari ini", Toast.LENGTH_SHORT).show()
                }
                    navigateToDashboard()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("QRCodeAction", "Error retrieving scan count: ${error.message}")
                Toast.makeText(this@qrscanenr, "Gagal mengecek batas scan", Toast.LENGTH_SHORT).show()
                navigateToDashboard()
            }
        })
    }
    private fun saveScanEvent(
        userRef: DatabaseReference,
        attendanceLabel: String,
        scannedData: String,
        currentTimeWIB: String,
        timePeriod: String,
        scanStatus: String
    ) {
        val scanEvent = hashMapOf(
            "scannedData" to scannedData,
            "scanTime" to currentTimeWIB, // Menyimpan waktu scan dalam WIB
            "timePeriod" to timePeriod, // Menyimpan periode waktu (pagi, siang, malam)
            scanStatus to true // Menandakan bahwa scan pertama atau kedua sudah selesai
        )

        userRef.child("attendance").child(attendanceLabel).setValue(scanEvent)
            .addOnSuccessListener {
                Toast.makeText(this@qrscanenr, "$attendanceLabel berhasil tercatat pada $currentTimeWIB ", Toast.LENGTH_SHORT).show()
//                    navigateToDashboard()
            }
            .addOnFailureListener { e ->
                Log.e("QRCodeAction", "Error saving $attendanceLabel: ${e.message}")
                Toast.makeText(this@qrscanenr, "Gagal menyimpan $attendanceLabel", Toast.LENGTH_SHORT).show()
//                    navigateToDashboard()
            }
        navigateToDashboard()
    }
    private fun navigateToDashboard() {
        val intent = Intent(this, dashboard::class.java)
        startActivity(intent)
        finish()
    }
}