package com.daehyeon.dhon

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
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
    var isGridView = false

    companion object {
        const val VIEW_TYPE_LIST = 0
        const val VIEW_TYPE_GRID = 1
    }

    inner class FileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFileIcon: TextView = view.findViewById(R.id.tvFileIcon)
        val tvFileName: TextView = view.findViewById(R.id.tvFileName)
        val tvFileDate: TextView = view.findViewById(R.id.tvFileDate)
    }

    override fun getItemViewType(position: Int): Int {
        return if (isGridView) VIEW_TYPE_GRID else VIEW_TYPE_LIST
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val layoutId = if (viewType == VIEW_TYPE_GRID) {
            R.layout.item_file_grid
        } else {
            R.layout.item_file
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = files[position]

        // ✅ 토글 더미 항목 처리 (▶ 또는 ▲ 로 시작하는 항목)
        if (file.name.startsWith("▶") || file.name.startsWith("▲")) {
            holder.tvFileIcon.text = ""
            holder.tvFileName.text = file.name
            holder.tvFileName.setTextColor(android.graphics.Color.parseColor("#374151"))
            holder.tvFileName.textSize = 14f
            holder.tvFileDate.text = ""
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
            holder.tvFileDate.text = "${fileCount}개"
            holder.tvFileName.setTextColor(android.graphics.Color.BLACK)

            if (isGridView) {
                // ✅ 그리드 보기: "26년\n6월" 형식으로 표시
                val displayName = convertToGridFolderName(file.name)
                holder.tvFileName.text = displayName
                holder.tvFileName.textSize = 12f
            } else {
                // ✅ 리스트 보기: "2026년 6월" 그대로 표시
                holder.tvFileName.text = file.name
                holder.tvFileName.textSize = 15f
            }
        } else {
            holder.tvFileIcon.text = when {
                file.name.endsWith(".pdf", ignoreCase = true) -> "📕"
                file.name.endsWith(".doc", ignoreCase = true) ||
                        file.name.endsWith(".docx", ignoreCase = true) -> "📘"
                file.name.endsWith(".xls", ignoreCase = true) ||
                        file.name.endsWith(".xlsx", ignoreCase = true) -> "📗"
                else -> "📄"
            }
            holder.tvFileName.setTextColor(android.graphics.Color.BLACK)
            holder.tvFileDate.text = formatDate(file.lastModified())

            if (isGridView) {
                holder.tvFileName.text = file.name
                holder.tvFileName.textSize = 11f
            } else {
                holder.tvFileName.text = file.name
                holder.tvFileName.textSize = 15f
            }
        }

        // ✅ 짧게 클릭
        holder.itemView.setOnClickListener {
            if (file.isDirectory) {
                onClick(file)
            } else {
                openFileViewer(holder.itemView.context, file)
            }
        }

        // ✅ 길게 클릭
        holder.itemView.setOnLongClickListener {
            onLongClick(file)
            true
        }
    }

    override fun getItemCount() = files.size

    // ✅ "2026년 6월" → "26년\n6월" 변환
    private fun convertToGridFolderName(folderName: String): String {
        return try {
            val regex = Regex("(\\d{4})년 (\\d{1,2})월")
            val match = regex.find(folderName) ?: return folderName
            val year = match.groupValues[1].takeLast(2)
            val month = match.groupValues[2]
            "${year}년\n${month}월"
        } catch (e: Exception) {
            folderName
        }
    }

    // ✅ 파일 크게보기 (뷰어로 열기)
    private fun openFileViewer(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val mimeType = getMimeType(file.name)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "파일 열기"))
        } catch (e: Exception) {
            Toast.makeText(context, "❌ 파일을 열 수 없어요\n뷰어 앱을 설치해 주세요", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
            fileName.endsWith(".doc", ignoreCase = true) -> "application/msword"
            fileName.endsWith(".docx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            fileName.endsWith(".xls", ignoreCase = true) -> "application/vnd.ms-excel"
            fileName.endsWith(".xlsx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            fileName.endsWith(".jpg", ignoreCase = true) ||
                    fileName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
            fileName.endsWith(".png", ignoreCase = true) -> "image/png"
            else -> "*/*"
        }
    }

    private fun formatDate(timeMillis: Long): String {
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
