package com.daehyeon.dhon

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class DocsSubActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_docs_sub)

        val category = intent.getStringExtra("category") ?: ""
        val title = intent.getStringExtra("title") ?: "서류"

        // 화면 제목 설정
        findViewById<TextView>(R.id.tvTitle).text = "📁 $title"

        // 하위 항목 목록 가져오기
        val subItems = getSubItems(category)

        // 버튼을 동적으로 생성해서 containerSubItems에 추가
        val container = findViewById<LinearLayout>(R.id.containerSubItems)

        subItems.forEach { itemName ->
            // CardView 생성
            val cardView = CardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = dpToPx(10) }
                radius = dpToPx(12).toFloat()
                cardElevation = dpToPx(2).toFloat()
                setCardBackgroundColor(Color.WHITE)
            }

            // 안쪽 LinearLayout
            val innerLayout = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(56)
                )
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dpToPx(20), 0, dpToPx(20), 0)
            }

            // 항목 이름 TextView
            val tvName = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                text = itemName
                textSize = 15f
                setTextColor(Color.parseColor("#0F172A"))
            }

            // 화살표 TextView
            val tvArrow = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text = "›"
                textSize = 22f
                setTextColor(Color.parseColor("#94A3B8"))
            }

            innerLayout.addView(tvName)
            innerLayout.addView(tvArrow)
            cardView.addView(innerLayout)

            // 클릭 이벤트 - subItem 키 이름 통일!
            cardView.setOnClickListener {
                val intent = Intent(this, DocsFileActivity::class.java)
                intent.putExtra("category", category)
                intent.putExtra("subItem", itemName)  // subCategory → subItem 으로 수정!
                intent.putExtra("title", itemName)
                startActivity(intent)
            }

            container.addView(cardView)
        }
    }

    // dp를 px로 변환하는 함수
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun getSubItems(category: String): List<String> {
        return when (category) {
            "safety" -> listOf(
                "TBM일지", "안전교육일지", "위험성평가서",
                "장비작업계획서", "동바리 구조검토서", "보호구 지급대장"
            )
            "quality" -> listOf(
                "콘크리트 타설일지", "레미콘 납품서",
                "철근 시험성적서", "검측 요청서"
            )
            "labor" -> listOf(
                "근로계약서", "근로자 명부", "임금대장",
                "4대보험 신고서류", "외국인 체류자격 확인서"
            )
            "admin" -> listOf(
                "하도급 계약서", "공사일보", "기성 청구서류", "자재 반입 기록부"
            )
            else -> emptyList()
        }
    }
}