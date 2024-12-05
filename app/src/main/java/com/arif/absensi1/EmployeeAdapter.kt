package com.arif.absensi1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DatabaseReference

class EmployeeAdapter(
    private val employees: MutableList<daftarpegawaipenggajian.Employee>,
    private val database: DatabaseReference
) : RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder>() {

    private val filteredEmployees = mutableListOf<daftarpegawaipenggajian.Employee>()
    private var selectedPosition: Int? = null // Menyimpan posisi item yang dipilih

    init {
        filteredEmployees.addAll(employees)
    }

    inner class EmployeeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val usernameTextView: TextView = itemView.findViewById(R.id.usernameTextView)
        val bagianTextView: TextView = itemView.findViewById(R.id.bagianTextView)
        val passwordTextView: TextView = itemView.findViewById(R.id.passwordTextView)
        val deleteIcon: ImageView = itemView.findViewById(R.id.deleteIcon)
        val editIcon: ImageView = itemView.findViewById(R.id.editIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmployeeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_employee, parent, false)
        return EmployeeViewHolder(view)
    }

    override fun onBindViewHolder(holder: EmployeeViewHolder, position: Int) {
        val employee = filteredEmployees[holder.adapterPosition]
        holder.usernameTextView.text = "Nama : ${employee.username}"
        holder.bagianTextView.text = "Bagian/Jabatan : ${employee.bagian}"
        holder.passwordTextView.text = "Password Akun : ${employee.password}"

        // Tampilkan atau sembunyikan ikon edit dan hapus berdasarkan posisi yang dipilih
        val isSelected = selectedPosition == holder.adapterPosition
        holder.editIcon.visibility = if (isSelected) View.VISIBLE else View.GONE
        holder.deleteIcon.visibility = if (isSelected) View.VISIBLE else View.GONE

        // Tekan lama untuk memilih item
        holder.itemView.setOnLongClickListener {
            val previousPosition = selectedPosition
            selectedPosition = holder.adapterPosition

            // Perbarui tampilan untuk item sebelumnya dan item yang dipilih
            if (previousPosition != null && previousPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(previousPosition)
            }
            notifyItemChanged(holder.adapterPosition)
            true
        }
        holder.deleteIcon.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                val context = holder.itemView.context

                // Tampilkan dialog konfirmasi hapus
                AlertDialog.Builder(context)
                    .setTitle("Hapus Pegawai")
                    .setMessage("Apakah Anda yakin ingin menghapus data pegawai ini?")
                    .setPositiveButton("Hapus") { _, _ ->
                        // Hapus data di Firebase
                        database.child("users").child(employee.id).removeValue()
                            .addOnSuccessListener {
                                Toast.makeText(context, "Data Pegawai Berhasil Dihapus", Toast.LENGTH_SHORT).show()
                                filteredEmployees.removeAt(currentPosition)
                                notifyItemRemoved(currentPosition) // Perbarui RecyclerView
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Gagal Menghapus Data", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }
        }

        // Klik pada ikon edit untuk membuka dialog edit
        holder.editIcon.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                val context = holder.itemView.context

                // Tampilkan dialog edit
                val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_employee, null)
                val usernameEditText = dialogView.findViewById<EditText>(R.id.usernameEditText)
                val passwordEditText = dialogView.findViewById<EditText>(R.id.passwordEditText)
                passwordEditText.setText(employee.password) // Password ditampilkan langsung
                val bagianSpinner = dialogView.findViewById<Spinner>(R.id.bagianSpinner)

                usernameEditText.setText(employee.username)
                passwordEditText.setText(employee.password)
                // Inisialisasi Spinner
                val bagianArray = context.resources.getStringArray(R.array.bagian_array)
                val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, bagianArray)
                bagianSpinner.adapter = adapter

                // Atur nilai spinner ke posisi yang sesuai
                val bagianPosition = bagianArray.indexOf(employee.bagian)
                if (bagianPosition >= 0) {
                    bagianSpinner.setSelection(bagianPosition)
                }

                AlertDialog.Builder(context)
                    .setTitle("Edit Data Pegawai")
                    .setView(dialogView)
                    .setPositiveButton("Simpan") { _, _ ->
                        val newUsername = usernameEditText.text.toString()
                        val newPassword = passwordEditText.text.toString()
                        val newBagian = bagianSpinner.selectedItem.toString()

                        // Update data di Firebase
                        database.child("users").child(employee.id)
                            .updateChildren(mapOf("username" to newUsername,"password" to newPassword, "bagian" to newBagian))
                            .addOnSuccessListener {
                                Toast.makeText(context, "Data Pegawai Berhasil diperbarui", Toast.LENGTH_SHORT).show()
                                employee.username = newUsername
                                employee.password = newPassword
                                employee.bagian = newBagian
                                notifyItemChanged(currentPosition) // Update item di RecyclerView
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Gagal Memperbarui Data", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }
        }


        // Klik pada item di luar ikon untuk menghilangkan ikon edit dan hapus
        holder.itemView.setOnClickListener {
            if (selectedPosition == holder.adapterPosition) {
                selectedPosition = null // Reset pilihan
                notifyItemChanged(holder.adapterPosition)
            }
        }
    }

    override fun getItemCount(): Int = filteredEmployees.size

    // Fungsi untuk memfilter data
    fun filter(query: String) {
        filteredEmployees.clear()
        if (query.isEmpty()) {
            filteredEmployees.addAll(employees)
        } else {
            filteredEmployees.addAll(employees.filter {
                it.username.contains(query, ignoreCase = true)
            })
        }
        notifyDataSetChanged()
    }
}
