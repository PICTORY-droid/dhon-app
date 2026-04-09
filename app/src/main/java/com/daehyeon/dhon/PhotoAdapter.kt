package com.daehyeon.dhon

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PhotoAdapter(
    private val photos: MutableList<File>,
    private val onClick: (File) -> Unit,
    private val onLongClick: (File) -> Unit
) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    // true = 최신순(내림차순), false = 오래된순(오름차순)
    var isDescending = true

    inner class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imgPhoto)
        val tvTag: TextView = view.findViewById(R.id.tvTag)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val file = photos[position]

        // 사진 표시
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        holder.imageView.setImageBitmap(bitmap)

        // 태그/메모 표시
        holder.tvTag.text = extractDisplayText(file.name)

        // 날짜 표시
        holder.tvDate.text = formatDate(file.lastModified())

        // 짧게 클릭 → 크게보기 (PhotoDetailActivity)
        holder.itemView.setOnClickListener { onClick(file) }

        // 길게 클릭 → 팝업 메뉴 (공유/이동/휴지통)
        holder.itemView.setOnLongClickListener {
            onLongClick(file)
            true
        }
    }

    override fun getItemCount() = photos.size

    private fun formatDate(timeMillis: Long): String {
        return try {
            val sdf = SimpleDateFormat("yy년 MM월 dd일", Locale.KOREA)
            sdf.format(Date(timeMillis))
        } catch (e: Exception) {
            ""
        }
    }

    private fun extractDisplayText(fileName: String): String {
        return try {
            val withoutExt = fileName.replace(".jpg", "")
            val parts = withoutExt.split("_")
            when {
                parts.size >= 5 -> parts.subList(4, parts.size).joinToString("_")
                parts.size >= 4 -> parts[3]
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    fun sortPhotos() {
        if (isDescending) {
            photos.sortByDescending { it.lastModified() }
        } else {
            photos.sortBy { it.lastModified() }
        }
        notifyDataSetChanged()
    }
}