package com.arif.absensi1

import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class Registerpage1 : AppCompatActivity() {
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registerpage1)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val etrpw1 = findViewById<EditText>(R.id.etrpw1)
        val etrname1 = findViewById<EditText>(R.id.etrname1)
        val btnReg1 = findViewById<Button>(R.id.btnReg1)
        val spinnerBagian = findViewById<Spinner>(R.id.spinnerBagian)

        // Toggle password visibility

        // Registration logic
        btnReg1.setOnClickListener {
            val username = etrname1.text.toString().trim()
            val password = etrpw1.text.toString().trim()
            val bagian = spinnerBagian.selectedItem.toString() // Get selected "bagian"

            if (username.isEmpty() || password.isEmpty() || bagian.isEmpty()) {
                Toast.makeText(this, "Mohon isi semua data", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val database = FirebaseDatabase.getInstance("https://penggajian-b318f-default-rtdb.asia-southeast1.firebasedatabase.app/")
            val userRef = database.getReference("users")
            val gajianBagianRef = database.getReference("gajiBagian")

            // Check if username is already taken
            userRef.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        Toast.makeText(this@Registerpage1, "Nama Sudah Ada", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(this@Registerpage1, "Berhasil Menambahkan Data", Toast.LENGTH_SHORT).show()
                            finish() // Close the registration activity
                        }.addOnFailureListener {
                            Toast.makeText(this@Registerpage1, "Gagal Menambahkan Data", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@Registerpage1, "Registrasi gagal", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}