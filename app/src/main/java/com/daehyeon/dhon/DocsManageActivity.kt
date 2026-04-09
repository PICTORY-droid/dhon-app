package com.daehyeon.dhon

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class DocsManageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_docs_manage)

        // 안전서류
        findViewById<LinearLayout>(R.id.btnSafety).setOnClickListener {
            val intent = Intent(this, DocsSubActivity::class.java)
            intent.putExtra("category", "safety")
            intent.putExtra("title", "안전서류")
            startActivity(intent)
        }

        // 품질서류
        findViewById<LinearLayout>(R.id.btnQuality).setOnClickListener {
            val intent = Intent(this, DocsSubActivity::class.java)
            intent.putExtra("category", "quality")
            intent.putExtra("title", "품질서류")
            startActivity(intent)
        }

        // 노무서류
        findViewById<LinearLayout>(R.id.btnLabor).setOnClickListener {
            val intent = Intent(this, DocsSubActivity::class.java)
            intent.putExtra("category", "labor")
            intent.putExtra("title", "노무서류")
            startActivity(intent)
        }

        // 행정/계약서류
        findViewById<LinearLayout>(R.id.btnAdmin).setOnClickListener {
            val intent = Intent(this, DocsSubActivity::class.java)
            intent.putExtra("category", "admin")
            intent.putExtra("title", "행정/계약서류")
            startActivity(intent)
        }
    }
}