package com.example.smartattendancesystem.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.smartattendancesystem.databinding.ItemAttendanceBinding
import com.example.smartattendancesystem.model.AttendanceModel
import java.text.SimpleDateFormat
import java.util.*

class AttendanceAdapter(private val list: List<AttendanceModel>) :
    RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val binding = ItemAttendanceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AttendanceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount() = list.size

    inner class AttendanceViewHolder(private val binding: ItemAttendanceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AttendanceModel) {
            binding.textEmail.text = item.email
            binding.textStatus.text = item.status

            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val date = Date(item.timestamp)
            binding.textTime.text = sdf.format(date)

            if (item.imageUrl.isNotEmpty()) {
                Glide.with(binding.imageView.context)
                    .load(item.imageUrl)
                    .into(binding.imageView)
            }
        }
    }
}
