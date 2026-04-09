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

    // true = 최신순(내림차순), false = 오래된순(오름차순)
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

        // 파일 아이콘
        holder.tvFileIcon.text = when {
            file.name.endsWith(".pdf", ignoreCase = true) -> "📕"
            file.name.endsWith(".doc", ignoreCase = true) ||
                    file.name.endsWith(".docx", ignoreCase = true) -> "📘"
            file.name.endsWith(".xls", ignoreCase = true) ||
                    file.name.endsWith(".xlsx", ignoreCase = true) -> "📗"
            else -> "📄"
        }

        // 파일명
        holder.tvFileName.text = file.name

        // 날짜 표시 (00년 00월 00일 형식)
        holder.tvFileDate.text = formatDate(file.lastModified())

        holder.itemView.setOnClickListener { onClick(file) }
        holder.itemView.setOnLongClickListener {
            onLongClick(file)
            true
        }
    }

    override fun getItemCount() = files.size

    // 날짜 포맷 변환
    private fun formatDate(timeMillis: Long): String {
        return try {
            val sdf = SimpleDateFormat("yy년 MM월 dd일", Locale.KOREA)
            sdf.format(Date(timeMillis))
        } catch (e: Exception) {
            ""
        }
    }

    // 정렬 함수
    fun sortFiles() {
        if (isDescending) {
            files.sortByDescending { it.lastModified() }
        } else {
            files.sortBy { it.lastModified() }
        }
        notifyDataSetChanged()
    }
}