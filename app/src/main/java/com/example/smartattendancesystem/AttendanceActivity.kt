package com.example.smartattendancesystem

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartattendancesystem.adapter.AttendanceAdapter
import com.example.smartattendancesystem.databinding.ActivityAttendanceBinding
import com.example.smartattendancesystem.model.AttendanceModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AttendanceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAttendanceBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val attendanceList = mutableListOf<AttendanceModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAttendanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        firestore.collection("attendance")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                attendanceList.clear()
                for (doc in result) {
                    val item = doc.toObject(AttendanceModel::class.java)
                    attendanceList.add(item)
                }
                binding.recyclerView.adapter = AttendanceAdapter(attendanceList)
            }
            .addOnFailureListener {
                Log.e("AttendanceActivity", "Failed to fetch data: ${it.message}")
            }
    }
}
