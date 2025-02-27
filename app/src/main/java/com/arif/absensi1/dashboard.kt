
package com.arif.absensi1

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
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

class dashboard : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private var userRole: String? = null // Variable to store the user role
    private var userId: String? = null // User ID to check the datatabel
    private var bagian: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        // Initialize Firebase Database, point to the correct path
        database = FirebaseDatabase.getInstance("https://penggajian-b318f-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .reference // Root path, then child based on your needs


        // Get role and userId from SharedPreferences
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        userRole = sharedPref.getString("userRole", "user") // Default to "user" if not found
        userId = sharedPref.getString("userId", "Unknown") // Default to "Unknown" if not found
        bagian = sharedPref.getString("bagian", "user")


        val cardLogout = findViewById<CardView>(R.id.cv3)
        cardLogout.setOnClickListener {
            val sharedPreferences: SharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                clear() // Hapus semua data yang disimpan
                apply()
            }
            val intent = Intent(this, loginpage1::class.java)
            startActivity(intent)
            finish() // Tutup halaman dashboard
        }



        // Log role for debugging
        Log.d("Dashboard", "User Role: $userRole, UserId: $userId")

        // Initialize CardViews
        val cardViews = listOf(
            findViewById<CardView>(R.id.cv1),
            findViewById<CardView>(R.id.cv2),
            findViewById<CardView>(R.id.cv3),
            findViewById<CardView>(R.id.cv4),
            findViewById<CardView>(R.id.cv6),
            findViewById<CardView>(R.id.cv7),
            findViewById<CardView>(R.id.cv8),
            findViewById<CardView>(R.id.cv9),
            findViewById<CardView>(R.id.cv10)
        )



        // Adjust CardView visibility based on user role
        adjustCardViewVisibility(cardViews)

        // Card View for scanning QR Code
        cardViews[0].setOnClickListener {
            val intent = Intent(this, qrscanenr::class.java)
            startActivity(intent)
        }


        // Other card view actions
        cardViews[1].setOnClickListener {
            val intent = Intent(this, jadwalpegawai::class.java)
            startActivity(intent)
        }
        cardViews[3].setOnClickListener {
            userId?.let { id ->
                showGajiKaryawanDialog(id)
            }

        }
        cardViews[4].setOnClickListener {
            showProfileDialog()
        }

        // Card View to go to MainActivity
        cardViews[5].setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // Card View to go to Registerpage1
        cardViews[6].setOnClickListener {
            showRegisterDialog()
        }

        // Card View to go to Mengaturjadwal
        cardViews[7].setOnClickListener {
            val intent = Intent(this, mengaturjadwal::class.java)
            startActivity(intent)
        }
        cardViews[8].setOnClickListener {
            val intent = Intent(this, daftarpegawaipenggajian::class.java)
            startActivity(intent)
        }

        // Handle edge-to-edge window insets for padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // Adjust the visibility of CardViews based on user role
    private fun adjustCardViewVisibility(cardViews: List<CardView>) {
        if (userRole == "admin") {
            // Show all CardViews for admin
            cardViews.forEach { it.visibility = View.VISIBLE }
            cardViews[0].visibility = View.GONE  // CV1
            cardViews[1].visibility = View.GONE  // CV2
            cardViews[3].visibility = View.GONE  // CV4
            cardViews[4].visibility = View.GONE  // CV5
        } else if (userRole == "user") {
            // Hide some CardViews for regular users (example: hide cv2, cv4, and cv5)
            cardViews[5].visibility = View.GONE  // CV7]
            cardViews[6].visibility = View.GONE  // CV8
            cardViews[7].visibility = View.GONE  // CV9
            cardViews[8].visibility = View.GONE  // CV10
        }
    }
    private fun getServerMonth(callback: (String) -> Unit) {
        val serverTimeRef = database.child(".info/serverTimeOffset")
        serverTimeRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val offset = snapshot.getValue(Long::class.java) ?: 0L
                val serverTime = System.currentTimeMillis() + offset
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar.timeInMillis = serverTime
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH) // 0-based index
                val monthNames = arrayOf(
                    "January", "February", "March", "April", "May", "June",
                    "July", "August", "September", "October", "November", "December"
                )
                val currentMonth = "${monthNames[month]}-$year"
                callback(currentMonth)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@dashboard, "Gagal memuat bulan server.", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun showProfileDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.profile_dialog, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val usernameTextView = dialogView.findViewById<TextView>(R.id.usernameTextView)
        val bagianTextView = dialogView.findViewById<TextView>(R.id.bagianTextView)

        // Fetch the current user ID and other details from SharedPreferences
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val userId = sharedPref.getString("userId", "Unknown") // Get the user ID
        val username = sharedPref.getString("username", "Unknown")
        val bagian = sharedPref.getString("bagian", "Unknown") // Get the section (bagian)

        // Set the username and bagian text
        usernameTextView.text = "Nama Pegawai : $username"
        bagianTextView.text = "Bagian/Jabatan : $bagian"

        // Fetch the first day worked and update the dialog

        dialog.show()
    }


    private fun showGajiKaryawanDialog(userId: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.activity_gajikaryawan, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val usernameTextView = dialogView.findViewById<TextView>(R.id.userIdTextView)
        val bagianTextView = dialogView.findViewById<TextView>(R.id.bagianTextView)
        val monthSpinner = dialogView.findViewById<Spinner>(R.id.monthSpinner)
        val salaryTextView = dialogView.findViewById<TextView>(R.id.salaryTextView)
        val pph21TextView = dialogView.findViewById<TextView>(R.id.pph21TextView)
        val bpjsTextView = dialogView.findViewById<TextView>(R.id.bpjsKesehatanTextView)
        val netIncomeTextView = dialogView.findViewById<TextView>(R.id.netIncomeTextView)
        val latenessTextView = dialogView.findViewById<TextView>(R.id.totalLatenessTextView)

        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val username = sharedPref.getString("username", "Unknown")
        val bagian = sharedPref.getString("bagian", "Unknown") // Get the section (bagian)
        usernameTextView.text = "Nama Pegawai : $username"
        bagianTextView.text = "Bagian/Jabatan : $bagian"

        // Get the current server month first
        getServerMonth { currentMonth ->
            val months = generateMonthList()

            // Move the current month to the first item in the list
            val monthList = months.toMutableList()
            monthList.remove(currentMonth)
            monthList.add(0, currentMonth)

            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, monthList)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            monthSpinner.adapter = adapter

            // Set the current month as the selected item
            val currentMonthPosition = monthList.indexOf(currentMonth)
            monthSpinner.setSelection(currentMonthPosition)

            monthSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                    val selectedMonth = monthList[position]
                    fetchAttendanceData(userId, selectedMonth) { salaryData ->
                        salaryTextView.text = "Gaji Pokok : Rp ${salaryData.salary}"
                        pph21TextView.text = "PPh 21 : Rp ${salaryData.pph21}"
                        bpjsTextView.text = "BPJS Kesehatan : Rp ${salaryData.bpjsKesehatan}"
                        netIncomeTextView.text = "Gaji Bersih : Rp ${salaryData.netIncome}"
                        latenessTextView.text = "Total Denda Keterlambatan : Rp ${salaryData.latenessFine}"
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

            dialog.show()
        }
    }

    private fun generateMonthList(): List<String> {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val monthNames = arrayOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        return (0..11).map { "${monthNames[it]}-$year" }
    }

    data class SalaryData(val salary: Long, val pph21: Long, val bpjsKesehatan: Long, val netIncome: Long, val latenessFine: Long)

    private fun fetchAttendanceData(userId: String, month: String, callback: (SalaryData) -> Unit) {
        val userRef = database.child("users").child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(userSnapshot: DataSnapshot) {
                val bagian = userSnapshot.child("bagian").getValue(String::class.java) ?: "Unknown"

                val gajiBagianRef = database.child("gajiBagian").child(bagian)
                gajiBagianRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(gajiSnapshot: DataSnapshot) {
                        val dailySalary = gajiSnapshot.getValue(Long::class.java) ?: 100000L // Default salary if not found

                        val ref = userRef.child("datatabel").child(month)
                        ref.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                var totalDays = 0
                                var totalLateness: Long = 0

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

                                            val shift1StartTime = sdf.parse("${scanTime1.substring(0, 10)} 07:00:00")
                                            val shift2StartTime = sdf.parse("${scanTime1.substring(0, 10)} 15:00:00")

                                            when {
                                                scanTimeDate.after(shift1StartTime) && scanTimeDate.before(shift2StartTime) -> {
                                                    val lateness = (scanTimeDate.time - shift1StartTime.time) / 60000 // In minutes
                                                    totalLateness += lateness
                                                }
                                                scanTimeDate.after(shift2StartTime) -> {
                                                    val lateness = (scanTimeDate.time - shift2StartTime.time) / 60000 // In minutes
                                                    totalLateness += lateness
                                                }
                                            }
                                        }
                                    }
                                }

                                val salary = totalDays * dailySalary
                                val latenessFine: Long = (totalLateness / 30) * 7000L
                                val pph21: Long = (salary * 0.05).toLong()
                                val bpjsKesehatan: Long = (salary * 0.03).toLong()
                                val netIncome: Long = salary - pph21 - bpjsKesehatan - latenessFine

                                callback(SalaryData(salary, pph21, bpjsKesehatan, netIncome, latenessFine))

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
                                Toast.makeText(this@dashboard, "Failed to load attendance data.", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@dashboard, "Failed to load salary data.", Toast.LENGTH_SHORT).show()
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@dashboard, "Failed to load user data.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showRegisterDialog() {
        val dialogView = layoutInflater.inflate(R.layout.activity_registerpage1, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView) // Use the view for the registration form
            .setCancelable(true) // Make dialog non-cancelable
            .create()

        // Find the button in the dialog layout
        val btnReg1 = dialogView.findViewById<Button>(R.id.btnReg1)

        // Handle registration logic when the button is clicked
        btnReg1.setOnClickListener {
            val etrname1 = dialogView.findViewById<EditText>(R.id.etrname1)
            val etrpw1 = dialogView.findViewById<EditText>(R.id.etrpw1)
            val spinnerBagian = dialogView.findViewById<Spinner>(R.id.spinnerBagian)

            val username = etrname1.text.toString().trim()
            val password = etrpw1.text.toString().trim()
            val bagian = spinnerBagian.selectedItem.toString() // Get selected "bagian"

            if (username.isEmpty() || password.isEmpty() || bagian.isEmpty()) {
                Toast.makeText(this, "Mohon isi semua field", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val database = FirebaseDatabase.getInstance("https://penggajian-b318f-default-rtdb.asia-southeast1.firebasedatabase.app/")
            val userRef = database.getReference("users")
            val gajiBagianRef = database.getReference("gajiBagian")

            // Check if username is already taken
            userRef.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        Toast.makeText(
                            this@dashboard,
                            "Username sudah digunakan",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        // Register new user
                        val newUser = userRef.push()
                        val user = mapOf(
                            "username" to username,
                            "password" to password,
                            "role" to "user", // Automatically assigning role here
                            "bagian" to bagian // Add the selected bagian to the database
                        )
                        newUser.setValue(user).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val gajiEntry = gajiBagianRef.child(username)
                                val salary = when (bagian) {
                                    "HR" -> 200000L
                                    "Supervisor" -> 110000L
                                    "Operator Jahit" -> 100000L
                                    "Quality Control" -> 100000L
                                    "Packing" -> 100000L
                                    "Admin" -> 100000L
                                    "Sales Garment" -> 100000L
                                    else -> 100000L // Default jika bagian tidak dikenal
                                }
                                val gajiData = mapOf(
                                    "username" to username,
                                    "bagian" to bagian,
                                    "salary" to salary
                                )
                                gajiEntry.setValue(gajiData).addOnCompleteListener { gajiTask ->
                                    if (gajiTask.isSuccessful) {
                                        Toast.makeText(
                                            this@dashboard,
                                            "Berhasil Menambahkan Data",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        finish()
                                    } else {
                                        Toast.makeText(
                                            this@dashboard,
                                            "Gagal Menambahkan Data",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } else {
                                Toast.makeText(
                                    this@dashboard,
                                    "Gagal Menambahkan Data",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@dashboard, "Registrasi gagal", Toast.LENGTH_SHORT).show()
                }
            })
        }

        dialog.show() // Show the dialog
    }
}