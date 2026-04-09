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

        // ✅ 토글 더미 항목 처리 (▶ 또는 ▲ 로 시작하는 항목)
        // 날짜 표시 안 하고, 아이콘만 표시
        if (file.name.startsWith("▶") || file.name.startsWith("▲")) {
            holder.tvFileIcon.text = ""
            holder.tvFileName.text = file.name
            holder.tvFileName.setTextColor(android.graphics.Color.parseColor("#374151"))
            holder.tvFileName.textSize = 14f
            holder.tvFileDate.text = ""  // ✅ 날짜 완전히 숨김
            holder.itemView.setOnClickListener { onClick(file) }
            holder.itemView.setOnLongClickListener {
                onLongClick(file)
                true
            }
            return
        }

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
        // ✅ timeMillis가 0이면 빈 문자열 반환 (1970년 01월 01일 방지)
        if (timeMillis == 0L) return ""
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

    fun updateDisplayList() {
        notifyDataSetChanged()
    }
}
