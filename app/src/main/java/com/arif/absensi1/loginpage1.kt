package com.arif.absensi1

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class loginpage1 : AppCompatActivity() {
    private var isPasswordVisible = false
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPreferences: SharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)

        if (isLoggedIn) {
            val userRole = sharedPreferences.getString("userRole", null)
            val intent = if (userRole == "admin") {
                Intent(this, dashboard::class.java) // Redirect to admin dashboard
            } else {
                Intent(this, dashboard::class.java) // Redirect to regular user dashboard
            }
            startActivity(intent)
            finish() // Close the login page
        }

        setContentView(R.layout.activity_loginpage1)
        enableEdgeToEdge()
        database = FirebaseDatabase.getInstance("https://penggajian-b318f-default-rtdb.asia-southeast1.firebasedatabase.app/").reference

        // Apply padding for system bars (e.g., status bar, navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnLogin1 = findViewById<Button>(R.id.btnLogin1)
        val etrpw1 = findViewById<EditText>(R.id.etloginpw1)
        val etrname1 = findViewById<EditText>(R.id.etloginname1)
        val eyebutton = findViewById<Button>(R.id.eyebutton)

        // Handle login button click
        btnLogin1.setOnClickListener {
            val username = etrname1.text.toString().trim()
            val password = etrpw1.text.toString().trim()
            if (username.isNotEmpty() && password.isNotEmpty()) {
                // Check credentials in the database
                database.child("users").get().addOnSuccessListener { dataSnapshot ->
                    var userRole: String? = null
                    var userId: String? = null  // To store the user ID
                    var bagian: String? = null
                    var usernameExists = false
                    var passwordCorrect = false

                    for (userSnapshot in dataSnapshot.children) {
                        val dbUsername = userSnapshot.child("username").getValue(String::class.java)
                        val dbPassword = userSnapshot.child("password").getValue(String::class.java)
                        userRole = userSnapshot.child("role").getValue(String::class.java)
                        userId = userSnapshot.key  // Get the user ID (Firebase key)
                        bagian = userSnapshot.child("bagian").getValue(String::class.java)

                        if (dbUsername == username) {
                            usernameExists = true
                            if (dbPassword == password) {
                                passwordCorrect = true
                                break // Exit loop on successful login
                            }
                        }
                    }

                    when {
                        !usernameExists -> {
                            Toast.makeText(this, "Username tidak ditemukan", Toast.LENGTH_SHORT).show()
                        }
                        !passwordCorrect -> {
                            Toast.makeText(this, "Password salah", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            // Save user role, user ID, and login status in shared preferences
                            val sharedPreferences: SharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                            with(sharedPreferences.edit()) {
                                putString("userRole", userRole) // Save the retrieved role
                                putString("username", username) // Save the username
                                putString("userId", userId)     // Save the user ID
                                putString("bagian", bagian)
                                putBoolean("isLoggedIn", true) // Mark user as logged in
                                apply()
                            }


                            // Navigate based on user role
                            val intent = if (userRole == "admin") {
                                Intent(this, dashboard::class.java) // Redirect to dashboard for admin
                            } else {
                                Intent(this, dashboard::class.java) // Redirect to dashboard for regular users
                            }
                            startActivity(intent)
                            finish()
                        }
                    }
                }.addOnFailureListener {
                    Toast.makeText(this, "Login gagal", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Mohon isi Semua Data", Toast.LENGTH_SHORT).show()
            }
        }

        // Set password sebagai hidden secara default
        etrpw1.transformationMethod = PasswordTransformationMethod.getInstance()

// Set icon awal pada eyebutton
        eyebutton.setCompoundDrawablesWithIntrinsicBounds(
            0, 0, R.drawable.baseline_face_24, 0 // Icon untuk mode password hidden
        )

// Handle toggle password visibility
        eyebutton.setOnClickListener {
            if (isPasswordVisible) {
                // Sembunyikan password
                etrpw1.transformationMethod = PasswordTransformationMethod.getInstance()
                eyebutton.setCompoundDrawablesWithIntrinsicBounds(
                    0, 0, R.drawable.baseline_face_24, 0
                )
            } else {
                // Tampilkan password
                etrpw1.transformationMethod = null
                eyebutton.setCompoundDrawablesWithIntrinsicBounds(
                    0, 0, R.drawable.baseline_face_retouching_off_24, 0
                )
            }
            isPasswordVisible = !isPasswordVisible
            etrpw1.setSelection(etrpw1.text.length) // Pindahkan cursor ke akhir teks
        }
    }
}
