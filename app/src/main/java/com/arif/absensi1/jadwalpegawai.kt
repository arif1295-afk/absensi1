package com.arif.absensi1

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class jadwalpegawai : AppCompatActivity() {
    private lateinit var tableLayout: TableLayout
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_jadwalpegawai)

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance("https://penggajian-b318f-default-rtdb.asia-southeast1.firebasedatabase.app/")
        databaseReference = database.reference

        // Get the user ID and role from SharedPreferences
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val userId = sharedPreferences.getString("userId", null)
        val userRole = sharedPreferences.getString("userRole", null)
        val username = sharedPreferences.getString("username", "Guest")

        // Initialize UI components
        tableLayout = findViewById(R.id.tableLayout)

        // Set up the month and year selection logic
        val monthSpinner: Spinner = findViewById(R.id.monthSpinner)
        val yearSpinner: Spinner = findViewById(R.id.yearSpinner)
        setupSpinners(monthSpinner, yearSpinner)

        // Load data when month or year is selected
        monthSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selectedMonth = monthSpinner.selectedItem.toString()
                val selectedYear = yearSpinner.selectedItem.toString().toInt()
                updateTable(selectedMonth, selectedYear)
                loadTableData(userId, selectedMonth, selectedYear)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        yearSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selectedMonth = monthSpinner.selectedItem.toString()
                val selectedYear = yearSpinner.selectedItem.toString().toInt()
                updateTable(selectedMonth, selectedYear)
                loadTableData(userId, selectedMonth, selectedYear)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // Setup spinners for month and year
    private fun setupSpinners(monthSpinner: Spinner, yearSpinner: Spinner) {
        val months = arrayOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        val monthAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, months)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        monthSpinner.adapter = monthAdapter

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (currentYear - 10..currentYear + 10).toList().map { it.toString() }
        val yearAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, years)
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        yearSpinner.adapter = yearAdapter

        val calendar = Calendar.getInstance()
        monthSpinner.setSelection(calendar.get(Calendar.MONTH))
        yearSpinner.setSelection(years.indexOf(currentYear.toString()))
    }

    // Update table based on selected month and year
    private fun updateTable(selectedMonth: String, selectedYear: Int) {
        tableLayout.removeAllViews()

        val monthIndex = arrayOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December").indexOf(selectedMonth)
        val calendar = Calendar.getInstance()
        calendar.set(selectedYear, monthIndex, 1)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val headerRow = TableRow(this)
        val headers = arrayOf("Tgl", "Shift 1", "Shift 2") // Header for Shift1 and Shift2
        for (header in headers) {
            val headerText = TextView(this)
            headerText.text = header
            headerText.setPadding(16, 16, 16, 16)
            headerText.textSize = 16f
            headerText.setBackgroundColor(Color.LTGRAY)
            headerText.gravity = Gravity.CENTER
            headerText.setTextColor(Color.BLACK)

            val layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
            headerText.layoutParams = layoutParams
            headerRow.addView(headerText)
        }
        tableLayout.addView(headerRow)

        val dateFormat = SimpleDateFormat("EEE, d", Locale.getDefault())
        for (day in 1..daysInMonth) {
            val dataRow = TableRow(this)
            calendar.set(Calendar.DAY_OF_MONTH, day)

            val dateCell = TextView(this)
            dateCell.text = dateFormat.format(calendar.time)
            dateCell.setPadding(16, 16, 16, 16)
            dateCell.textSize = 14f
            dateCell.setBackgroundResource(R.drawable.table_cell_border)
            dateCell.gravity = Gravity.CENTER

            val dateLayoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
            dateCell.layoutParams = dateLayoutParams
            dataRow.addView(dateCell)

            for (j in 1..2) { // Hanya 2 shift: shift1 dan shift2
                val frameLayout = FrameLayout(this)

                // Tambahkan margin pada FrameLayout untuk memberi ruang
                val frameLayoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT, 1f)
                frameLayoutParams.setMargins(8, 8, 8, 8) // Menambahkan margin
                frameLayout.layoutParams = frameLayoutParams

                val checkBox = CheckBox(this)
                checkBox.isEnabled = false  // Nonaktifkan checkbox
                checkBox.isChecked = false // Default tidak tercentang

                // Atur tata letak agar checkbox berada di tengah
                val checkBoxParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                checkBoxParams.gravity = Gravity.CENTER  // Letakkan di tengah
                checkBox.layoutParams = checkBoxParams

                // Tambahkan checkbox ke FrameLayout
                frameLayout.addView(checkBox)

                // Tambahkan FrameLayout ke baris tabel
                dataRow.addView(frameLayout)
            }
            tableLayout.addView(dataRow)
        }
    }

    // Load data from Firebase based on selected user ID, month, and year
    private fun loadTableData(userId: String?, selectedMonth: String, selectedYear: Int) {
        if (userId != null && userId != "Unknown") {
            val userRef = databaseReference.child("users").child(userId).child("datatabel").child("$selectedMonth-$selectedYear")
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (i in 1 until tableLayout.childCount) {
                            val row = tableLayout.getChildAt(i) as TableRow
                            val dateText = (row.getChildAt(0) as TextView).text.toString().split(",")[1].trim()  // Extract day
                            val daySnapshot = snapshot.child(dateText)  // Get the snapshot for that day
                            if (daySnapshot.exists()) {
                                // Update checkboxes based on "Shift 1" and "Shift 2" values
                                for (j in 1..2) {
                                    val checkBox = (row.getChildAt(j) as FrameLayout).getChildAt(0) as CheckBox
                                    val shiftValue = when (j) {
                                        1 -> "Shift 1"
                                        else -> "Shift 2"
                                    }
                                    val isChecked = daySnapshot.child(shiftValue).getValue(Boolean::class.java) ?: false
                                    checkBox.isChecked = isChecked
                                }
                            }
                        }
                    } else {
                        Log.d("JadwalPegawai", "No data found for $selectedMonth-$selectedYear")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("JadwalPegawai", "Failed to load data: ${error.message}")
                    Toast.makeText(this@jadwalpegawai, "Failed to load data", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
        }
    }
} 