package com.arif.absensi1

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class tabeljadwal : AppCompatActivity() {
    private lateinit var tableLayout: TableLayout
    private lateinit var saveButton: Button
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tabeljadwal)

        // Initialize Firebase with custom URL
        database = FirebaseDatabase.getInstance("https://penggajian-b318f-default-rtdb.asia-southeast1.firebasedatabase.app/")
        databaseReference = database.reference

        // Retrieve data from previous activity
        val username = intent.getStringExtra("username") ?: "Unknown"
        val bagian = intent.getStringExtra("bagian") ?: "Unknown"
        val idUser = intent.getStringExtra("iduser") ?: "Unknown"

        // Set up UI elements
        val usernameTextView = findViewById<TextView>(R.id.usernameTextView)
        val bagianTextView = findViewById<TextView>(R.id.bagianTextView)
        usernameTextView.text = "Nama Pegawai : $username"
        bagianTextView.text = "Bagian/Jabatan : $bagian"

        val monthSpinner: Spinner = findViewById(R.id.monthSpinner)
        val months = arrayOf("January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December")

        val monthAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, months)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        monthSpinner.adapter = monthAdapter

        val yearSpinner: Spinner = findViewById(R.id.yearSpinner)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (currentYear - 10..currentYear + 10).toList().map { it.toString() }

        val yearAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, years)
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        yearSpinner.adapter = yearAdapter

        val calendar = Calendar.getInstance()
        monthSpinner.setSelection(calendar.get(Calendar.MONTH))
        yearSpinner.setSelection(years.indexOf(currentYear.toString()))

        tableLayout = findViewById(R.id.tableLayout)

        monthSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selectedMonth = months[position]
                val selectedYear = yearSpinner.selectedItem.toString().toInt()
                updateTable(selectedMonth, selectedYear)
                loadTableData(idUser, selectedMonth, selectedYear)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        yearSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selectedMonth = monthSpinner.selectedItem.toString()
                val selectedYear = years[position].toInt()
                updateTable(selectedMonth, selectedYear)
                loadTableData(idUser, selectedMonth, selectedYear)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        saveButton = findViewById(R.id.saveButton)
        saveButton.setOnClickListener {
            saveTableData(username, monthSpinner.selectedItem.toString(), yearSpinner.selectedItem.toString().toInt(), idUser)
        }
    }

    private fun updateTable(selectedMonth: String, selectedYear: Int) {
        tableLayout.removeAllViews()
        val monthIndex = arrayOf("January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December").indexOf(selectedMonth)

        val calendar = Calendar.getInstance()
        calendar.set(selectedYear, monthIndex, 1)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val headerRow = TableRow(this)
        val headers = arrayOf("Tgl", "Shift 1", "Shift 2") // Updated headers
        for (header in headers) {
            val headerText = TextView(this)
            headerText.text = header
            headerText.setPadding(16, 16, 16, 16)
            headerText.textSize = 16f
            headerText.setBackgroundColor(Color.LTGRAY)
            headerText.gravity = android.view.Gravity.CENTER
            headerText.setTextColor(Color.BLACK)

            // Set weight for equal column width
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
            dateCell.gravity = android.view.Gravity.CENTER

            // Set weight for equal column width
            val dateLayoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
            dateCell.layoutParams = dateLayoutParams
            dataRow.addView(dateCell)

            val checkboxes = mutableListOf<CheckBox>()
            for (j in 1..2) { // Now only two shifts
                val frameLayout = FrameLayout(this)
                val checkBox = CheckBox(this)
                checkBox.gravity = android.view.Gravity.CENTER

                // Tambahkan pengaturan layout params untuk frameLayout
                val frameLayoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT, 1f)
                frameLayout.layoutParams = frameLayoutParams

                // Tambahkan checkbox ke dalam frameLayout
                frameLayout.addView(checkBox)

                // Pastikan checkbox ditengahkan di frameLayout
                val checkBoxParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                checkBoxParams.gravity = android.view.Gravity.CENTER
                checkBox.layoutParams = checkBoxParams

                checkboxes.add(checkBox)

                checkBox.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        for (otherCheckBox in checkboxes) {
                            if (otherCheckBox != checkBox) {
                                otherCheckBox.isChecked = false
                            }
                        }
                    }
                }
                if (calendar.before(Calendar.getInstance()) || calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    checkBox.isEnabled = false
                }

                // Tambahkan frameLayout ke dataRow
                dataRow.addView(frameLayout)
            }
            tableLayout.addView(dataRow)
        }
    }

    private fun loadTableData(idUser: String, selectedMonth: String, selectedYear: Int) {
        val userRef = databaseReference.child("users").child(idUser).child("datatabel").child("$selectedMonth-$selectedYear")
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (i in 1 until tableLayout.childCount) {
                    val row = tableLayout.getChildAt(i) as TableRow
                    val dateText = (row.getChildAt(0) as TextView).text.toString().split(",")[1].trim()
                    val daySnapshot = snapshot.child(dateText)
                    if (daySnapshot.exists()) {
                        for (j in 1..2) { // Now only two shifts
                            val checkBox = (row.getChildAt(j) as FrameLayout).getChildAt(0) as CheckBox
                            val value = when (j) {
                                1 -> "Shift 1"
                                else -> "Shift 2"
                            }
                            checkBox.isChecked = daySnapshot.child(value).getValue(Boolean::class.java) ?: false
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("tabeljadwal", "Failed to load data: ${error.message}")
            }
        })
    }

    private fun saveTableData(username: String, selectedMonth: String, selectedYear: Int, idUser: String) {
        val tableData = mutableMapOf<String, Any>()
        for (i in 1 until tableLayout.childCount) {
            val row = tableLayout.getChildAt(i) as TableRow
            val dateText = (row.getChildAt(0) as TextView).text.toString().split(",")[1].trim()
            val attendanceData = mutableMapOf<String, Boolean>()
            for (j in 1 until row.childCount) {
                val checkBox = (row.getChildAt(j) as FrameLayout).getChildAt(0) as CheckBox
                val value = when (j) {
                    1 -> "Shift 1"
                    else -> "Shift 2"
                }
                attendanceData[value] = checkBox.isChecked
            }
            tableData[dateText] = attendanceData
        }
        val userRef = databaseReference.child("users").child(idUser).child("datatabel")
            .child("$selectedMonth-$selectedYear")
        userRef.setValue(tableData).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Data saved successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to save data", Toast.LENGTH_SHORT).show()
            }
        }
    }
}