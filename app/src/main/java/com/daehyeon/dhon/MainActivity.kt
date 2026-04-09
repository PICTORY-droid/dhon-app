package com.daehyeon.dhon

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 앱 시작할 때 날씨 알람 자동 등록 (아침 5시 + 저녁 8시)
        WeatherScheduler.scheduleWeatherAlarms(this)

        findViewById<LinearLayout>(R.id.btnWorkReport).setOnClickListener {
            startActivity(Intent(this, WorkReportActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.btnDocsManage).setOnClickListener {
            startActivity(Intent(this, DocsManageActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.btnPhoto).setOnClickListener {
            startActivity(Intent(this, PhotoActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.btnCard).setOnClickListener {
            startActivity(Intent(this, CardActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.btnTranslate).setOnClickListener {
            startActivity(Intent(this, TranslateActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.btnNotice).setOnClickListener {
            startActivity(Intent(this, NoticeActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.btnSetting).setOnClickListener {
            startActivity(Intent(this, SettingActivity::class.java))
        }
    }
}