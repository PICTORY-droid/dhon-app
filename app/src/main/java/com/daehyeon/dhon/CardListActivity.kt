package com.daehyeon.dhon

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class CardListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvTitle: TextView
    private lateinit var tvToggleIcon: TextView
    private val cardList = mutableListOf<File>()
    private var category = ""
    private var isGridView = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_list)

        category = intent.getStringExtra("category") ?: ""

        tvTitle = findViewById(R.id.tvTitle)
        tvToggleIcon = findViewById(R.id.tvToggleIcon)
        recyclerView = findViewById(R.id.recyclerCards)

        tvTitle.text = "$category 명함"

        // 처음엔 리스트 보기
        setListView()

        // 홈으로 가기
        findViewById<LinearLayout>(R.id.btnHome).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        // 명함 추가 버튼
        findViewById<LinearLayout>(R.id.btnAddCard).setOnClickListener {
            val intent = Intent(this, CardAddActivity::class.java)
            intent.putExtra("category", category)
            startActivity(intent)
        }

        // 그리드/리스트 전환 버튼
        findViewById<LinearLayout>(R.id.btnToggleView).setOnClickListener {
            isGridView = !isGridView
            if (isGridView) {
                setGridView()
                tvToggleIcon.text = "☰"
            } else {
                setListView()
                tvToggleIcon.text = "⊞"
            }
        }

        loadCards()
    }

    override fun onResume() {
        super.onResume()
        loadCards()
    }

    private fun setListView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = CardListAdapter(cardList) { file ->
            openCardDetail(file)
        }
    }

    private fun setGridView() {
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        recyclerView.adapter = CardGridAdapter(cardList) { file ->
            openCardDetail(file)
        }
    }

    private fun openCardDetail(file: File) {
        val intent = Intent(this, CardDetailActivity::class.java)
        intent.putExtra("cardPath", file.absolutePath)
        intent.putExtra("category", category)
        startActivity(intent)
    }

    private fun loadCards() {
        cardList.clear()

        if (category == "전체보기") {
            val baseFolder = File(filesDir, "business_cards")
            if (baseFolder.exists()) {
                baseFolder.listFiles()
                    ?.filter { it.isDirectory }
                    ?.forEach { categoryFolder ->
                        categoryFolder.listFiles()
                            ?.filter { it.isDirectory }
                            ?.sortedByDescending { it.lastModified() }
                            ?.let { cardList.addAll(it) }
                    }
            }
        } else {
            val folder = File(File(filesDir, "business_cards"), category)
            if (folder.exists()) {
                folder.listFiles()
                    ?.filter { it.isDirectory }
                    ?.sortedByDescending { it.lastModified() }
                    ?.let { cardList.addAll(it) }
            }
        }

        if (cardList.isEmpty()) {
            tvTitle.text = "$category 명함 (없음)"
        } else {
            tvTitle.text = "$category 명함 (${cardList.size}개)"
        }

        if (isGridView) setGridView() else setListView()
    }

    // 리스트 어댑터
    inner class CardListAdapter(
        private val cards: MutableList<File>,
        private val onClick: (File) -> Unit
    ) : RecyclerView.Adapter<CardListAdapter.ListViewHolder>() {

        inner class ListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imgCard: ImageView = view.findViewById(R.id.imgCard)
            val tvName: TextView = view.findViewById(R.id.tvName)
            val tvCompany: TextView = view.findViewById(R.id.tvCompany)
            val tvPhone: TextView = view.findViewById(R.id.tvPhone)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_card_list, parent, false)
            return ListViewHolder(view)
        }

        override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
            val cardFolder = cards[position]
            val infoFile = File(cardFolder, "info.txt")
            if (infoFile.exists()) {
                val lines = infoFile.readLines()
                var name = ""; var company = ""; var phone = ""
                for (line in lines) {
                    when {
                        line.startsWith("이름:") -> name = line.replace("이름:", "").trim()
                        line.startsWith("회사:") -> company = line.replace("회사:", "").trim()
                        line.startsWith("전화:") -> phone = line.replace("전화:", "").trim()
                    }
                }
                holder.tvName.text = name
                holder.tvCompany.text = company
                holder.tvPhone.text = phone
            }
            val photoFile = File(cardFolder, "card.jpg")
            if (photoFile.exists()) {
                holder.imgCard.setImageBitmap(BitmapFactory.decodeFile(photoFile.absolutePath))
            } else {
                holder.imgCard.setImageResource(android.R.drawable.ic_menu_gallery)
            }
            holder.itemView.setOnClickListener { onClick(cardFolder) }
        }

        override fun getItemCount() = cards.size
    }

    // 그리드 어댑터
    inner class CardGridAdapter(
        private val cards: MutableList<File>,
        private val onClick: (File) -> Unit
    ) : RecyclerView.Adapter<CardGridAdapter.GridViewHolder>() {

        inner class GridViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imgCard: ImageView = view.findViewById(R.id.imgCard)
            val tvName: TextView = view.findViewById(R.id.tvName)
            val tvCompany: TextView = view.findViewById(R.id.tvCompany)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_card_grid, parent, false)
            return GridViewHolder(view)
        }

        override fun onBindViewHolder(holder: GridViewHolder, position: Int) {
            val cardFolder = cards[position]
            val infoFile = File(cardFolder, "info.txt")
            if (infoFile.exists()) {
                val lines = infoFile.readLines()
                var name = ""; var company = ""
                for (line in lines) {
                    when {
                        line.startsWith("이름:") -> name = line.replace("이름:", "").trim()
                        line.startsWith("회사:") -> company = line.replace("회사:", "").trim()
                    }
                }
                holder.tvName.text = name
                holder.tvCompany.text = company
            }
            val photoFile = File(cardFolder, "card.jpg")
            if (photoFile.exists()) {
                holder.imgCard.setImageBitmap(BitmapFactory.decodeFile(photoFile.absolutePath))
            } else {
                holder.imgCard.setImageResource(android.R.drawable.ic_menu_gallery)
            }
            holder.itemView.setOnClickListener { onClick(cardFolder) }
        }

        override fun getItemCount() = cards.size
    }
}