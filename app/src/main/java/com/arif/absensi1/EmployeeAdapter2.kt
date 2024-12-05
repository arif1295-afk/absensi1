package com.arif.absensi1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class EmployeeAdapter2(
    private val employees: List<mengaturjadwal.Employee>,
    private val onItemClick: (mengaturjadwal.Employee) -> Unit
) : RecyclerView.Adapter<EmployeeAdapter2.EmployeeViewHolder>() {

    private val filteredEmployees = mutableListOf<mengaturjadwal.Employee>()

    init {
        // Initialize the filtered list with the full list of employees
        filteredEmployees.addAll(employees)
    }

    inner class EmployeeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val usernameTextView: TextView = itemView.findViewById(R.id.usernameTextView)
        val bagianTextView: TextView = itemView.findViewById(R.id.bagianTextView)
        val cardView: CardView = itemView.findViewById(R.id.cardView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmployeeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_employee2, parent, false)
        return EmployeeViewHolder(view)
    }

    override fun onBindViewHolder(holder: EmployeeViewHolder, position: Int) {
        val employee = filteredEmployees[position]
        holder.usernameTextView.text = "Nama: ${employee.username}"
        holder.bagianTextView.text = "Jabatan/Bagian: ${employee.bagian}"

        // Set the item click listener to invoke the callback when clicked
        holder.cardView.setOnClickListener {
            onItemClick(employee)
        }
    }

    override fun getItemCount(): Int = filteredEmployees.size

    // Function to filter employees based on the search query
    fun filter(query: String) {
        filteredEmployees.clear() // Clear previous filtered list
        if (query.isEmpty()) {
            // If query is empty, show all employees
            filteredEmployees.addAll(employees)
        } else {
            // Otherwise, filter based on the query
            filteredEmployees.addAll(employees.filter {
                it.username.contains(query, ignoreCase = true)
            })
        }
        notifyDataSetChanged() // Notify adapter to update the list
    }

}
