package com.daehyeon.dhon

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileAdapter(
    private val files: MutableList<File>,
    private val onClick: (File) -> Unit,
    private val onLongClick: (File) -> Unit
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    var isDescending = true

    inner class FileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFileIcon: TextView = view.findViewById(R.id.tvFileIcon)
        val tvFileName: TextView = view.findViewById(R.id.tvFileName)
        val tvFileDate: TextView = view.findViewById(R.id.tvFileDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = files[position]

        if (file.isDirectory) {
            val fileCount = file.walkTopDown().filter { it.isFile }.count()
            holder.tvFileIcon.text = "📁"
            holder.tvFileName.text = file.name
            holder.tvFileName.setTextColor(android.graphics.Color.BLACK)
            holder.tvFileName.textSize = 15f
            holder.tvFileDate.text = "${fileCount}개"
        } else {
            holder.tvFileIcon.text = when {
                file.name.endsWith(".pdf", ignoreCase = true) -> "📕"
                file.name.endsWith(".doc", ignoreCase = true) ||
                        file.name.endsWith(".docx", ignoreCase = true) -> "📘"
                file.name.endsWith(".xls", ignoreCase = true) ||
                        file.name.endsWith(".xlsx", ignoreCase = true) -> "📗"
                else -> "📄"
            }
            holder.tvFileName.text = file.name
            holder.tvFileName.setTextColor(android.graphics.Color.BLACK)
            holder.tvFileName.textSize = 15f
            holder.tvFileDate.text = formatDate(file.lastModified())
        }

        holder.itemView.setOnClickListener { onClick(file) }
        holder.itemView.setOnLongClickListener {
            onLongClick(file)
            true
        }
    }

    override fun getItemCount() = files.size

    private fun formatDate(timeMillis: Long): String {
        return try {
            SimpleDateFormat("yy년 MM월 dd일", Locale.KOREA).format(Date(timeMillis))
        } catch (e: Exception) {
            ""
        }
    }

    fun sortFiles() {
        if (isDescending) {
            files.sortWith(compareBy<File> { it.isFile }.thenByDescending { it.name })
        } else {
            files.sortWith(compareBy<File> { it.isFile }.thenBy { it.name })
        }
        notifyDataSetChanged()
    }

    // ✅ updateDisplayList는 그냥 notifyDataSetChanged 호출
    fun updateDisplayList() {
        notifyDataSetChanged()
    }
}
