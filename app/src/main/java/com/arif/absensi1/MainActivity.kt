package com.arif.absensi1

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.FirebaseApp
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var qrCodeImageView: ImageView
    private val handler = Handler()
    private val updateInterval: Long = 10000 // 5 seconds
    private var currentQRCodeData: String? = null // Track current QR code data
    private lateinit var database: DatabaseReference // Firebase database reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        database = FirebaseDatabase.getInstance("https://penggajian-b318f-default-rtdb.asia-southeast1.firebasedatabase.app/").reference.child("QRData")

        qrCodeImageView = findViewById(R.id.qrCodeImageView)

        // Start updating QR code
        startUpdatingQRCode()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val backToDashboardButton = findViewById<ImageView>(R.id.back)
        backToDashboardButton.setOnClickListener {
            val intent = Intent(this, dashboard::class.java)
            startActivity(intent)
        }
    }

    private fun startUpdatingQRCode() {
        handler.post(object : Runnable {
            override fun run() {
                val randomData = generateRandomData() // Generate new data
                currentQRCodeData = randomData // Update current QR code data
                generateQRCode(randomData) // Generate QR code

                // Save QR code data to Firebase
                saveQRCodeToFirebase(randomData)

                handler.postDelayed(this, updateInterval) // Schedule next update
            }
        })
    }

    private fun saveQRCodeToFirebase(data: String) {
        val qrCodeEntry = mapOf(
            "timestamp" to System.currentTimeMillis(),
            "data" to data,
            "expiryTime" to (System.currentTimeMillis() + 10000)  // Expiry time 5 seconds from now
        )

        // Save data to Firebase and get a reference to the entry
        val qrCodeRef = database.push()
        qrCodeRef.setValue(qrCodeEntry)
            .addOnSuccessListener {
                Log.d("FirebaseSave", "QR code data saved successfully.")

                // Schedule deletion of the QR code entry after 5 seconds
                handler.postDelayed({
                    qrCodeRef.removeValue()
                        .addOnSuccessListener {
                            Log.d("FirebaseDelete", "QR code data deleted successfully after 5 seconds.")
                        }
                        .addOnFailureListener { exception ->
                            Log.e("FirebaseDelete", "Error deleting QR code data: ${exception.message}")
                        }
                }, 10000) // 10000 milliseconds = 10 seconds

            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseSave", "Error saving QR code data: ${exception.message}")
            }
    }

    private fun generateRandomData(): String {
        return UUID.randomUUID().toString()
    }

    private fun generateQRCode(data: String) {
        val qrCodeWriter = QRCodeWriter()
        try {
            val bitMatrix: BitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 200, 200)
            val bitmap: Bitmap = createBitmapFromBitMatrix(bitMatrix)
            qrCodeImageView.setImageBitmap(bitmap)
//
//            qrCodeImageView.setOnClickListener {
//                verifyAndOpenDashboard(data)
//            }
        } catch (e: WriterException) {
            e.printStackTrace()
        }
    }

    private fun createBitmapFromBitMatrix(bitMatrix: BitMatrix): Bitmap {
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) BLACK else WHITE)
            }
        }
        return bitmap
    }

    private fun verifyAndOpenDashboard(scannedData: String) {
        if (scannedData == currentQRCodeData) {
            val intent = Intent(this, dashboard::class.java)
            intent.putExtra("scannedData", scannedData)
            startActivity(intent)
        } else {
            Log.d("QRValidation", "QR Code tidak valid lagi.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null) // Clean up the handler
    }

    companion object {
        private val BLACK = -0x1000000 // Color black
        private val WHITE = -0x1 // Color white
    }
}
