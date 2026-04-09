package com.daehyeon.dhon

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherDetailActivity : AppCompatActivity() {

    private lateinit var tvWeatherDetail: TextView
    private lateinit var tvLastUpdate: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather_detail)

        tvWeatherDetail = findViewById(R.id.tvWeatherDetail)
        tvLastUpdate = findViewById(R.id.tvLastUpdate)

        // 알림에서 전달된 날씨 메시지 받기
        val weatherMessage = intent.getStringExtra("weather_message") ?: ""

        if (weatherMessage.isNotEmpty()) {
            tvWeatherDetail.text = weatherMessage
            val now = SimpleDateFormat("MM월 dd일 HH시 mm분", Locale.KOREA).format(Date())
            tvLastUpdate.text = "마지막 업데이트: $now"
        } else {
            loadWeather()
        }

        // 홈으로 가기
        findViewById<LinearLayout>(R.id.btnHome).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        // 새로고침 버튼
        findViewById<LinearLayout>(R.id.btnRefresh).setOnClickListener {
            loadWeather()
        }
    }

    private fun loadWeather() {
        tvWeatherDetail.text = "날씨 정보 가져오는 중..."
        tvLastUpdate.text = ""

        CoroutineScope(Dispatchers.IO).launch {
            val message = WeatherService.getWeatherMessage(this@WeatherDetailActivity)
            withContext(Dispatchers.Main) {
                tvWeatherDetail.text = message
                val now = SimpleDateFormat("MM월 dd일 HH시 mm분", Locale.KOREA).format(Date())
                tvLastUpdate.text = "마지막 업데이트: $now"
            }
        }
    }
}