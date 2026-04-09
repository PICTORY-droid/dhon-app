package com.daehyeon.dhon

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WeatherService {

    private const val API_KEY = "b517cc914d4b54b0bcf8493fedf8babd"
    private const val BASE_URL = "https://api.openweathermap.org/data/2.5"

    fun getWeatherMessage(context: Context): String {
        return try {
            val prefs = context.getSharedPreferences("dhon_setting", Context.MODE_PRIVATE)
            val region = prefs.getString("region", "") ?: ""

            if (region.isEmpty()) {
                return "⚠️ 현장 설정에서 지역을 먼저 설정해주세요!"
            }

            // 현재 날씨 가져오기
            val locationUrl = "$BASE_URL/weather?q=${encodeRegion(region)}&appid=$API_KEY&units=metric&lang=kr"
            val locationData = fetchJson(locationUrl) ?: return "날씨 정보를 가져올 수 없어요 (인터넷 확인)"

            val lat = locationData.getJSONObject("coord").getDouble("lat")
            val lon = locationData.getJSONObject("coord").getDouble("lon")

            val currentTemp = locationData.getJSONObject("main").getDouble("temp").toInt()
            val maxTemp = locationData.getJSONObject("main").getDouble("temp_max").toInt()
            val minTemp = locationData.getJSONObject("main").getDouble("temp_min").toInt()
            val windSpeed = locationData.getJSONObject("wind").getDouble("speed")
            val weatherDesc = locationData.getJSONArray("weather")
                .getJSONObject(0).getString("description")

            // 1시간 단위 예보 가져오기 (cnt=24 → 24시간치)
            val forecastUrl = "$BASE_URL/forecast?lat=$lat&lon=$lon&appid=$API_KEY&units=metric&lang=kr&cnt=24"
            val forecastData = fetchJson(forecastUrl)

            val rainInfo = StringBuilder()
            val snowInfo = StringBuilder()
            var hasRain = false
            var hasSnow = false

            forecastData?.getJSONArray("list")?.let { list ->
                for (i in 0 until list.length()) {
                    val item = list.getJSONObject(i)
                    val timeStr = item.getString("dt_txt")
                    val time = parseTimeKorean(timeStr)  // 한국어 형식으로 변환

                    // 강수량 확인
                    if (item.has("rain")) {
                        val rain = item.getJSONObject("rain")
                        // 3시간 단위 데이터를 1시간으로 나누기
                        val amount3h = if (rain.has("3h")) rain.getDouble("3h") else 0.0
                        val amount1h = amount3h / 3.0  // 3시간 → 1시간 평균
                        if (amount1h > 0) {
                            hasRain = true
                            rainInfo.append("  $time ${String.format("%.1f", amount1h)}mm\n")
                        }
                    }

                    // 적설량 확인
                    if (item.has("snow")) {
                        val snow = item.getJSONObject("snow")
                        val amount3h = if (snow.has("3h")) snow.getDouble("3h") else 0.0
                        val amount1h = amount3h / 3.0
                        if (amount1h > 0) {
                            hasSnow = true
                            snowInfo.append("  $time ${String.format("%.1f", amount1h)}cm\n")
                        }
                    }
                }
            }

            // 경보 확인
            val alertUrl = "https://api.openweathermap.org/data/3.0/onecall?lat=$lat&lon=$lon&appid=$API_KEY&exclude=minutely,hourly,daily,current&lang=kr"
            val alertData = fetchJson(alertUrl)
            val alertText = StringBuilder()
            alertData?.optJSONArray("alerts")?.let { alerts ->
                for (i in 0 until alerts.length()) {
                    val alert = alerts.getJSONObject(i)
                    val event = alert.optString("event", "")
                    if (event.isNotEmpty()) {
                        alertText.append("⚠️ $event\n")
                    }
                }
            }

            // 메시지 조합
            val sb = StringBuilder()
            sb.append("[OpenWeatherMap]\n")
            sb.append("📍 $region\n")
            sb.append("🌡 현재 ${currentTemp}°C / 최고 ${maxTemp}°C / 최저 ${minTemp}°C\n")
            sb.append("☁️ $weatherDesc\n")

            if (hasRain) {
                sb.append("🌧 강수예보 (1시간 기준)\n")
                sb.append(rainInfo)
            } else {
                sb.append("🌧 강수: 없음\n")
            }

            if (hasSnow) {
                sb.append("❄️ 적설예보 (1시간 기준)\n")
                sb.append(snowInfo)
            } else {
                sb.append("❄️ 눈: 없음\n")
            }

            sb.append("💨 바람: ${String.format("%.1f", windSpeed)}m/s\n")

            if (alertText.isNotEmpty()) {
                sb.append("\n$alertText")
            }

            sb.toString().trimEnd()

        } catch (e: Exception) {
            "날씨 정보 오류: ${e.message}"
        }
    }

    // 지역명 → 영문 변환
    private fun encodeRegion(region: String): String {
        val map = mapOf(
            "서울" to "Seoul,KR",
            "부산" to "Busan,KR",
            "대구" to "Daegu,KR",
            "인천" to "Incheon,KR",
            "광주" to "Gwangju,KR",
            "대전" to "Daejeon,KR",
            "울산" to "Ulsan,KR",
            "세종" to "Sejong,KR",
            "경기" to "Gyeonggi-do,KR",
            "강원" to "Gangwon,KR",
            "충북" to "Chungcheongbuk-do,KR",
            "충남" to "Chungcheongnam-do,KR",
            "전북" to "Jeollabuk-do,KR",
            "전남" to "Jeollanam-do,KR",
            "경북" to "Gyeongsangbuk-do,KR",
            "경남" to "Gyeongsangnam-do,KR",
            "제주" to "Jeju,KR",
            "수원" to "Suwon,KR",
            "성남" to "Seongnam,KR",
            "고양" to "Goyang,KR",
            "용인" to "Yongin,KR",
            "창원" to "Changwon,KR",
            "청주" to "Cheongju,KR",
            "전주" to "Jeonju,KR",
            "천안" to "Cheonan,KR",
            "안산" to "Ansan,KR",
            "안양" to "Anyang,KR",
            "김해" to "Gimhae,KR",
            "포항" to "Pohang,KR",
            "의정부" to "Uijeongbu,KR"
        )
        for ((korean, english) in map) {
            if (region.contains(korean)) return english
        }
        return "Seoul,KR"
    }

    // 날짜 형식 변환 → "4월 9일 오전 3시" 형식
    private fun parseTimeKorean(timeStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA)
            val date = sdf.parse(timeStr) ?: return timeStr

            val cal = java.util.Calendar.getInstance()
            cal.time = date

            val month = cal.get(java.util.Calendar.MONTH) + 1
            val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
            val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)

            val ampm = if (hour < 12) "오전" else "오후"
            val displayHour = when {
                hour == 0 -> 12
                hour <= 12 -> hour
                else -> hour - 12
            }

            "${month}월 ${day}일 $ampm ${displayHour}시"
        } catch (e: Exception) {
            timeStr
        }
    }

    // HTTP GET 요청
    private fun fetchJson(urlString: String): JSONObject? {
        return try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.connect()

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                JSONObject(response)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}