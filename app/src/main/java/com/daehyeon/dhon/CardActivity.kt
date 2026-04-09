package com.daehyeon.dhon

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class CardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card)

        // 전체보기 버튼
        findViewById<LinearLayout>(R.id.btnAllCards).setOnClickListener {
            startCardList("전체보기")
        }

        // 명함 추가 버튼
        findViewById<LinearLayout>(R.id.btnAddCard).setOnClickListener {
            showCategoryDialog()
        }

        // 당사 버튼
        findViewById<LinearLayout>(R.id.btnDangsa).setOnClickListener {
            startCardList("당사")
        }

        // 원청 버튼
        findViewById<LinearLayout>(R.id.btnWonchung).setOnClickListener {
            startCardList("원청")
        }

        // 감리단 버튼
        findViewById<LinearLayout>(R.id.btnGamri).setOnClickListener {
            startCardList("감리단")
        }

        // 발주처 버튼
        findViewById<LinearLayout>(R.id.btnBaljucho).setOnClickListener {
            startCardList("발주처")
        }

        // 크레인 버튼
        findViewById<LinearLayout>(R.id.btnCrane).setOnClickListener {
            startCardList("크레인")
        }

        // 협력업체 버튼
        findViewById<LinearLayout>(R.id.btnPartner).setOnClickListener {
            startCardList("협력업체")
        }

        // 기타명함 버튼
        findViewById<LinearLayout>(R.id.btnEtcCard).setOnClickListener {
            startCardList("기타명함")
        }
    }

    private fun startCardList(category: String) {
        val intent = Intent(this, CardListActivity::class.java)
        intent.putExtra("category", category)
        startActivity(intent)
    }

    private fun showCategoryDialog() {
        val categories = arrayOf(
            "당사", "원청", "감리단", "발주처",
            "크레인", "협력업체", "기타명함"
        )
        android.app.AlertDialog.Builder(this)
            .setTitle("카테고리 선택")
            .setItems(categories) { _, which ->
                val intent = Intent(this, CardAddActivity::class.java)
                intent.putExtra("category", categories[which])
                startActivity(intent)
            }
            .setNegativeButton("취소", null)
            .show()
    }
}