package com.arif.absensi1

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class daftarpegawaipenggajian : AppCompatActivity() {

    // Model Employee
    data class Employee(
        val id: String,
        var username: String,
        var bagian: String,
        var password: String,
    )

    private lateinit var database: DatabaseReference
    private lateinit var adapter: EmployeeAdapter
    private val employees = mutableListOf<Employee>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_daftarpegawaipenggajian)

        val searchEditText = findViewById<EditText>(R.id.searchEditText)
        val recyclerView = findViewById<RecyclerView>(R.id.employeesRecyclerView)

        // Inisialisasi Firebase Database
        database = FirebaseDatabase.getInstance("https://penggajian-b318f-default-rtdb.asia-southeast1.firebasedatabase.app/").reference

        // Inisialisasi Adapter
        adapter = EmployeeAdapter(employees, database)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // TextWatcher untuk pencarian
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                adapter.filter(s.toString()) // Filter data berdasarkan input pengguna
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        fetchEmployees() // Ambil data dari Firebase
    }

    private fun fetchEmployees() {
        database.child("users").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                employees.clear()
                if (snapshot.exists()) {
                    for (userSnapshot in snapshot.children) {
                        val iduser = userSnapshot.key
                        val username = userSnapshot.child("username").getValue(String::class.java) ?: "N/A"
                        val bagian = userSnapshot.child("bagian").getValue(String::class.java) ?: "N/A"
                        val password = userSnapshot.child("password").getValue(String::class.java) ?: "N/A"
                        val role = userSnapshot.child("role").getValue(String::class.java)

                        if (role == "user" && iduser != null) {
                            employees.add(Employee(iduser, username, bagian,password))
                        }
                    }
                    adapter.filter("") // Menampilkan semua data saat pertama kali dimuat
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error fetching user data: ${error.message}")
                Toast.makeText(this@daftarpegawaipenggajian, "Gagal memuat data karyawan", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
