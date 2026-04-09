package com.daehyeon.dhon

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class CardAdapter(
    private val cards: MutableList<File>,
    private val onClick: (File) -> Unit
) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    inner class CardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvCompany: TextView = view.findViewById(R.id.tvCompany)
        val imgCard: ImageView = view.findViewById(R.id.imgCard)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val cardFolder = cards[position]

        // 정보 파일 읽기
        val infoFile = File(cardFolder, "info.txt")
        if (infoFile.exists()) {
            val lines = infoFile.readLines()
            holder.tvName.text = lines.getOrElse(0) { "" }
                .replace("이름:", "").trim()
            holder.tvCompany.text = lines.getOrElse(1) { "" }
                .replace("회사:", "").trim()
        }

        // 명함 사진 표시
        val photoFile = File(cardFolder, "card.jpg")
        if (photoFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
            holder.imgCard.setImageBitmap(bitmap)
        } else {
            holder.imgCard.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        holder.itemView.setOnClickListener {
            onClick(cardFolder)
        }
    }

    override fun getItemCount() = cards.size
}