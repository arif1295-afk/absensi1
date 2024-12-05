package com.arif.absensi1

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
class mengaturjadwal : AppCompatActivity() {

    data class Employee(
        val id: String,
        val username: String,
        val bagian: String,
        val role: String
    )

    private lateinit var database: DatabaseReference
    private lateinit var adapter: EmployeeAdapter2
    private val employees = mutableListOf<Employee>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mengaturjadwal)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val searchEditText = findViewById<EditText>(R.id.editTextSearch)
        database = FirebaseDatabase.getInstance("https://penggajian-b318f-default-rtdb.asia-southeast1.firebasedatabase.app/").reference

        adapter = EmployeeAdapter2(employees) { employee ->
            val intent = Intent(this, tabeljadwal::class.java)
            intent.putExtra("iduser", employee.id)
            intent.putExtra("username", employee.username)
            intent.putExtra("bagian", employee.bagian)
            intent.putExtra("role", employee.role)
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                adapter.filter(s.toString()) // Filter data berdasarkan input pengguna
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        fetchEmployees()

        // Handle Edge-to-Edge insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun fetchEmployees() {
        database.child("users").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                employees.clear()
                if (snapshot.exists()) {
                    for (userSnapshot in snapshot.children) {
                        val id = userSnapshot.key ?: continue
                        val username = userSnapshot.child("username").getValue(String::class.java) ?: "N/A"
                        val bagian = userSnapshot.child("bagian").getValue(String::class.java) ?: "N/A"
                        val role = userSnapshot.child("role").getValue(String::class.java) ?: "N/A"

                        if (role == "user") {
                            employees.add(Employee(id, username, bagian, role))
                        }
                    }
                    adapter.filter("")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error fetching user data: ${error.message}")
            }
        })
    }
}
