package com.arif.absensi1

import java.text.SimpleDateFormat
import java.util.*
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.*

class gajikaryawan : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var database: DatabaseReference
    private lateinit var selectedMonth: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gajikaryawan)

        // Ambil SharedPreferences dengan nama yang sama seperti di loginpage1
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)

        // Mengambil userId dan username dari SharedPreferences
        val userId = sharedPreferences.getString("userId", null)
        val username = sharedPreferences.getString("username", null)

        if (userId == null || username == null) {
            // Jika userId atau username tidak ditemukan, tampilkan pesan error
            Toast.makeText(this, "Gagal memuat data pengguna. Silakan login kembali.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val userIdTextView = findViewById<TextView>(R.id.userIdTextView)
        userIdTextView.text = "USERNAME: $username"

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance("https://penggajian-b318f-default-rtdb.asia-southeast1.firebasedatabase.app/").reference

        // Set up month spinner with dynamic months
        val monthSpinner = findViewById<Spinner>(R.id.monthSpinner)
        val months = generateMonthList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, months)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        monthSpinner.adapter = adapter

        // Spinner item selection listener
        monthSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                selectedMonth = months[position]
                fetchAttendanceData(userId, selectedMonth) // Fetch data for selected month
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun generateMonthList(): List<String> {
        val calendar = Calendar.getInstance() // Current date
        val year = calendar.get(Calendar.YEAR)

        // Generate months for the current year
        val months = mutableListOf<String>()
        val monthNames = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )

        // Add months for the current year
        for (i in monthNames.indices) {
            months.add("${monthNames[i]}-$year")
        }

        // Add 3 months from the next year
        for (i in 0..2) {
            months.add("${monthNames[i]}-${year + 1}")
        }

        return months
    }
    private fun fetchAttendanceData(userId: String, month: String) {
        // Check if userId is valid
        val attendanceRef = database.child("users").child(userId).child("datatabel").child(month)

        attendanceRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalDays = 0
                var totalLateness: Long = 0

                // Loop through each day of the month
                for (day in snapshot.children) {
                    val attendanceData = day.child("attendance").value as? Map<*, *>
                    if (attendanceData != null) {
                        val attendance1 = attendanceData["attendance1"] as? Map<*, *>
                        val scan1DoneAttendance1 = attendance1?.get("scan1_done") as? Boolean ?: false
                        val scanTime1 = attendance1?.get("scanTime") as? String ?: ""

                        if (scan1DoneAttendance1) {
                            totalDays++

                            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            val scanTimeDate = sdf.parse(scanTime1)

                            // Define shift start times
                            val shift1StartTime = sdf.parse("${scanTime1.substring(0, 10)} 07:00:00")
                            val shift2StartTime = sdf.parse("${scanTime1.substring(0, 10)} 15:00:00")

                            // Check lateness for active shift
                            when {
                                scanTimeDate.after(shift1StartTime) && scanTimeDate.before(shift2StartTime) -> {
                                    // Shift 1 (07:00)
                                    val lateness = (scanTimeDate.time - shift1StartTime.time) / 60000 // Convert to minutes
                                    totalLateness += lateness
                                }
                                scanTimeDate.after(shift2StartTime) -> {
                                    // Shift 2 (15:00)
                                    val lateness = (scanTimeDate.time - shift2StartTime.time) / 60000 // Convert to minutes
                                    totalLateness += lateness
                                }
                            }
                        }
                    }
                }

                // Calculate salary
                val salary: Long = totalDays * 100000L

                // Calculate lateness fine
                val dendaTelat: Long = totalLateness / 30
                val uangDenda: Long = dendaTelat * 7000L

                // Display lateness fine
                val uangDendaTextView = findViewById<TextView>(R.id.totalLatenessTextView)
                uangDendaTextView.text = "Total Denda Keterlambatan Rp: $uangDenda "

                // Display salary
                val salaryTextView = findViewById<TextView>(R.id.salaryTextView)
                salaryTextView.text = "Gaji: Rp ${salary}"

                // Calculate tax and BPJS
                val pph21: Long = (salary * 0.05).toLong()
                val bpjsKesehatan: Long = (salary * 0.03).toLong()
                val netIncome: Long = salary - pph21 - bpjsKesehatan - uangDenda

                // Display tax and BPJS
                val pph21TextView = findViewById<TextView>(R.id.pph21TextView)
                pph21TextView.text = "PPh 21: Rp $pph21"

                val bpjsKesehatanTextView = findViewById<TextView>(R.id.bpjsKesehatanTextView)
                bpjsKesehatanTextView.text = "BPJS Kesehatan: Rp $bpjsKesehatan"

                // Display net income
                val netIncomeTextView = findViewById<TextView>(R.id.netIncomeTextView)
                netIncomeTextView.text = "Penerimaan Bersih: Rp $netIncome"

                // Update Firebase with the calculated salary data
                val salaryData = mapOf(
                    "salary" to salary,
                    "totalLateness" to totalLateness,
                    "pph21" to pph21,
                    "bpjsKesehatan" to bpjsKesehatan,
                    "netIncome" to netIncome
                )

                database.child("users").child(userId).child("salary")
                    .child(month)
                    .setValue(salaryData)
                    .addOnSuccessListener {
                        Log.d("Firebase", "Salary data updated successfully for month: $month")
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firebase", "Error updating salary data: ${e.message}", e)
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@gajikaryawan, "Gagal memuat data", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
