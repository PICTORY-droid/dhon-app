package com.daehyeon.dhon

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.EditText
import android.widget.ExpandableListView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var etSiteName: EditText
    private lateinit var tvSelectedRegion: TextView
    private lateinit var tvCurrentSetting: TextView
    private lateinit var tvSelectedTime1: TextView
    private lateinit var tvSelectedTime2: TextView
    private lateinit var tvAlarm2Status: TextView
    private lateinit var tvToggleAlarm2: TextView
    private lateinit var layoutAlarm2Time: LinearLayout

    private var selectedRegion = ""
    private var alarm1Hour = 20
    private var alarm1Minute = 0
    private var alarm2Hour = 5
    private var alarm2Minute = 0
    private var isAlarm2Enabled = false
    private var tempHour = 0
    private var tempMinute = 0

    private val regionData = linkedMapOf(
        "서울특별시" to linkedMapOf(
            "서울특별시" to listOf(
                "종로구","중구","용산구","성동구","광진구",
                "동대문구","중랑구","성북구","강북구","도봉구",
                "노원구","은평구","서대문구","마포구","양천구",
                "강서구","구로구","금천구","영등포구","동작구",
                "관악구","서초구","강남구","송파구","강동구"
            )
        ),
        "경기도" to linkedMapOf(
            "경기도" to listOf(
                "수원시","성남시","고양시","용인시","부천시",
                "안산시","안양시","남양주시","화성시","평택시",
                "의정부시","시흥시","파주시","광명시","김포시",
                "군포시","광주시","이천시","양주시","오산시",
                "구리시","안성시","포천시","의왕시","하남시",
                "여주시","동두천시","과천시","가평군","양평군","연천군"
            )
        ),
        "충청북도" to linkedMapOf(
            "충청북도" to listOf(
                "청주시","충주시","제천시","보은군","옥천군",
                "영동군","증평군","진천군","괴산군","음성군","단양군"
            ),
            "청주시" to listOf("오송읍")
        ),
        "충청남도" to linkedMapOf(
            "충청남도" to listOf(
                "천안시","공주시","보령시","아산시","서산시",
                "논산시","계룡시","당진시","금산군","부여군",
                "서천군","청양군","홍성군","예산군","태안군"
            )
        ),
        "세종특별자치시" to linkedMapOf(
            "세종특별자치시" to listOf("세종시")
        ),
        "전라북도" to linkedMapOf(
            "전라북도" to listOf(
                "전주시","군산시","익산시","정읍시","남원시",
                "김제시","완주군","진안군","무주군","장수군",
                "임실군","순창군","고창군","부안군"
            )
        ),
        "전라남도" to linkedMapOf(
            "전라남도" to listOf(
                "목포시","여수시","순천시","나주시","광양시",
                "담양군","곡성군","구례군","고흥군","보성군",
                "화순군","장흥군","강진군","해남군","영암군",
                "무안군","함평군","영광군","장성군","완도군",
                "진도군","신안군"
            )
        ),
        "경상북도" to linkedMapOf(
            "경상북도" to listOf(
                "포항시","경주시","김천시","안동시","구미시",
                "영주시","영천시","상주시","문경시","경산시",
                "군위군","의성군","청송군","영양군","영덕군",
                "청도군","고령군","성주군","칠곡군","예천군",
                "봉화군","울진군","울릉군"
            )
        ),
        "경상남도" to linkedMapOf(
            "경상남도" to listOf(
                "창원시","진주시","통영시","사천시","김해시",
                "밀양시","거제시","양산시","의령군","함안군",
                "창녕군","고성군","남해군","하동군","산청군",
                "함양군","거창군","합천군"
            )
        ),
        "부산광역시" to linkedMapOf(
            "부산광역시" to listOf(
                "중구","서구","동구","영도구","부산진구",
                "동래구","남구","북구","해운대구","사하구",
                "금정구","강서구","연제구","수영구","사상구","기장군"
            )
        ),
        "강원도" to linkedMapOf(
            "강원도" to listOf(
                "춘천시","원주시","강릉시","동해시","태백시",
                "속초시","삼척시","홍천군","횡성군","영월군",
                "평창군","정선군","철원군","화천군","양구군",
                "인제군","고성군","양양군"
            )
        ),
        "대구광역시" to linkedMapOf(
            "대구광역시" to listOf(
                "중구","동구","서구","남구","북구",
                "수성구","달서구","달성군"
            )
        ),
        "인천광역시" to linkedMapOf(
            "인천광역시" to listOf(
                "중구","동구","미추홀구","연수구","남동구",
                "부평구","계양구","서구","강화군","옹진군"
            )
        ),
        "광주광역시" to linkedMapOf(
            "광주광역시" to listOf(
                "동구","서구","남구","북구","광산구"
            )
        ),
        "대전광역시" to linkedMapOf(
            "대전광역시" to listOf(
                "동구","중구","서구","유성구","대덕구"
            )
        ),
        "울산광역시" to linkedMapOf(
            "울산광역시" to listOf(
                "중구","남구","동구","북구","울주군"
            )
        ),
        "제주특별자치도" to linkedMapOf(
            "제주특별자치도" to listOf("제주시","서귀포시")
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        prefs = getSharedPreferences("dhon_setting", MODE_PRIVATE)

        etSiteName = findViewById(R.id.etSiteName)
        tvSelectedRegion = findViewById(R.id.tvSelectedRegion)
        tvCurrentSetting = findViewById(R.id.tvCurrentSetting)
        tvSelectedTime1 = findViewById(R.id.tvSelectedTime1)
        tvSelectedTime2 = findViewById(R.id.tvSelectedTime2)
        tvAlarm2Status = findViewById(R.id.tvAlarm2Status)
        tvToggleAlarm2 = findViewById(R.id.tvToggleAlarm2)
        layoutAlarm2Time = findViewById(R.id.layoutAlarm2Time)

        loadSettings()

        // 홈으로 가기
        findViewById<LinearLayout>(R.id.btnHome).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        // 지역 선택
        findViewById<LinearLayout>(R.id.btnSelectRegion).setOnClickListener {
            showRegionDialog()
        }

        // 알림 시간 1
        findViewById<LinearLayout>(R.id.btnSelectTime1).setOnClickListener {
            showAlarmTimePicker(1)
        }

        // 알림 2 켜기/끄기
        findViewById<LinearLayout>(R.id.btnToggleAlarm2).setOnClickListener {
            isAlarm2Enabled = !isAlarm2Enabled
            updateAlarm2UI()
        }

        // 알림 시간 2
        findViewById<LinearLayout>(R.id.btnSelectTime2).setOnClickListener {
            showAlarmTimePicker(2)
        }

        // 🔔 테스트 알림 버튼
        findViewById<LinearLayout>(R.id.btnTestWeather).setOnClickListener {
            sendTestWeatherNotification()
        }

        // 저장
        findViewById<LinearLayout>(R.id.btnSave).setOnClickListener {
            saveSettings()
        }
    }

    // 테스트 알림 발송
    private fun sendTestWeatherNotification() {
        val region = prefs.getString("region", "") ?: ""
        if (region.isEmpty()) {
            Toast.makeText(this, "먼저 지역을 설정하고 저장해주세요!", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "날씨 정보 가져오는 중... (10~20초 소요)", Toast.LENGTH_LONG).show()

        CoroutineScope(Dispatchers.IO).launch {
            val message = WeatherService.getWeatherMessage(this@SettingActivity)
            withContext(Dispatchers.Main) {
                WeatherNotificationHelper.showWeatherNotification(
                    this@SettingActivity, message
                )
                Toast.makeText(
                    this@SettingActivity,
                    "테스트 알림이 발송됐어요! 상단을 확인해주세요 🔔",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadSettings() {
        val siteName = prefs.getString("site_name", "") ?: ""
        val region = prefs.getString("region", "") ?: ""
        alarm1Hour = prefs.getInt("alarm1_hour", 20)
        alarm1Minute = prefs.getInt("alarm1_minute", 0)
        alarm2Hour = prefs.getInt("alarm2_hour", 5)
        alarm2Minute = prefs.getInt("alarm2_minute", 0)
        isAlarm2Enabled = prefs.getBoolean("alarm2_enabled", false)

        etSiteName.setText(siteName)
        selectedRegion = region

        if (region.isEmpty()) {
            tvSelectedRegion.text = "지역을 선택하세요"
            tvSelectedRegion.setTextColor(resources.getColor(R.color.text_hint, null))
        } else {
            tvSelectedRegion.text = region
            tvSelectedRegion.setTextColor(resources.getColor(R.color.text_primary, null))
        }

        updateAlarmTime1()
        updateAlarmTime2()
        updateAlarm2UI()
        updateCurrentSetting()
    }

    private fun updateAlarm2UI() {
        if (isAlarm2Enabled) {
            tvAlarm2Status.text = "두 번째 알림 사용 중"
            tvAlarm2Status.setTextColor(resources.getColor(R.color.secondary, null))
            tvToggleAlarm2.text = "끄기"
            layoutAlarm2Time.visibility = View.VISIBLE
        } else {
            tvAlarm2Status.text = "두 번째 알림 사용 안함"
            tvAlarm2Status.setTextColor(resources.getColor(R.color.text_hint, null))
            tvToggleAlarm2.text = "켜기"
            layoutAlarm2Time.visibility = View.GONE
        }
    }

    private fun showRegionDialog() {
        val provinces = regionData.keys.toList()
        val dialogView = layoutInflater.inflate(R.layout.dialog_region_picker, null)
        val expandableList = dialogView.findViewById<ExpandableListView>(R.id.expandableList)
        val adapter = RegionExpandableAdapter(provinces)
        expandableList.setAdapter(adapter)

        val dialog = AlertDialog.Builder(this)
            .setTitle("현장 지역 선택")
            .setView(dialogView)
            .setNegativeButton("취소", null)
            .create()

        expandableList.setOnChildClickListener { _, _, groupPosition, childPosition, _ ->
            val province = provinces[groupPosition]
            val subData = regionData[province] ?: return@setOnChildClickListener false
            val mainList = subData[province] ?: return@setOnChildClickListener false
            val city = mainList[childPosition]
            val subCities = subData[city]
            if (subCities != null && subCities.isNotEmpty()) {
                showSubCityDialog(province, city, subCities, dialog)
            } else {
                selectedRegion = "$province $city"
                tvSelectedRegion.text = selectedRegion
                tvSelectedRegion.setTextColor(resources.getColor(R.color.text_primary, null))
                dialog.dismiss()
            }
            true
        }
        dialog.show()
    }

    private fun showSubCityDialog(
        province: String, city: String,
        subCities: List<String>, parentDialog: AlertDialog
    ) {
        val items = mutableListOf("$city (전체)") + subCities
        AlertDialog.Builder(this)
            .setTitle("$province $city")
            .setItems(items.toTypedArray()) { _, which ->
                selectedRegion = if (which == 0) "$province $city"
                else "$province $city ${subCities[which - 1]}"
                tvSelectedRegion.text = selectedRegion
                tvSelectedRegion.setTextColor(resources.getColor(R.color.text_primary, null))
                parentDialog.dismiss()
            }
            .setNegativeButton("뒤로", null)
            .show()
    }

    private fun showAlarmTimePicker(alarmNum: Int) {
        val hours = (0..23).map { h ->
            when {
                h == 0 -> "오전 12시"
                h < 12 -> "오전 ${h}시"
                h == 12 -> "오후 12시"
                else -> "오후 ${h - 12}시"
            }
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("알림 시간 $alarmNum 선택")
            .setItems(hours) { _, which ->
                tempHour = which
                showMinutePicker(alarmNum)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showMinutePicker(alarmNum: Int) {
        val minutes = arrayOf("00분", "30분")
        var selectedMinuteIndex = 0

        AlertDialog.Builder(this)
            .setTitle("분 선택")
            .setSingleChoiceItems(minutes, 0) { _, which ->
                selectedMinuteIndex = which
            }
            .setNegativeButton("취소", null)
            .setPositiveButton("확인") { _, _ ->
                tempMinute = if (selectedMinuteIndex == 0) 0 else 30
                if (alarmNum == 1) {
                    alarm1Hour = tempHour
                    alarm1Minute = tempMinute
                    updateAlarmTime1()
                } else {
                    alarm2Hour = tempHour
                    alarm2Minute = tempMinute
                    updateAlarmTime2()
                }
                updateCurrentSetting()
            }
            .show()
    }

    private fun updateAlarmTime1() {
        tvSelectedTime1.text = formatTime(alarm1Hour, alarm1Minute)
    }

    private fun updateAlarmTime2() {
        tvSelectedTime2.text = formatTime(alarm2Hour, alarm2Minute)
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val ampm = if (hour < 12) "오전" else "오후"
        val h = when {
            hour == 0 -> 12
            hour <= 12 -> hour
            else -> hour - 12
        }
        val m = if (minute == 0) "00" else "30"
        return "$ampm ${h}시 ${m}분"
    }

    private fun saveSettings() {
        val siteName = etSiteName.text.toString().trim()
        if (siteName.isEmpty()) {
            Toast.makeText(this, "현장명을 입력해주세요!", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedRegion.isEmpty()) {
            Toast.makeText(this, "지역을 선택해주세요!", Toast.LENGTH_SHORT).show()
            return
        }

        prefs.edit()
            .putString("site_name", siteName)
            .putString("region", selectedRegion)
            .putInt("alarm1_hour", alarm1Hour)
            .putInt("alarm1_minute", alarm1Minute)
            .putInt("alarm2_hour", alarm2Hour)
            .putInt("alarm2_minute", alarm2Minute)
            .putBoolean("alarm2_enabled", isAlarm2Enabled)
            .putInt("alarm_hour", alarm1Hour)
            .putInt("alarm_minute", alarm1Minute)
            .apply()

        WeatherScheduler.scheduleWeatherAlarms(this)
        updateCurrentSetting()
        Toast.makeText(this, "설정이 저장되었어요!", Toast.LENGTH_SHORT).show()
    }

    private fun updateCurrentSetting() {
        val siteName = prefs.getString("site_name", "미설정") ?: "미설정"
        val region = prefs.getString("region", "미설정") ?: "미설정"
        val time1 = formatTime(alarm1Hour, alarm1Minute)
        val time2 = if (isAlarm2Enabled) " / ${formatTime(alarm2Hour, alarm2Minute)}" else ""
        tvCurrentSetting.text = "$siteName  |  $region\n알림설정시간  $time1$time2"
    }

    inner class RegionExpandableAdapter(
        private val provinces: List<String>
    ) : BaseExpandableListAdapter() {

        override fun getGroupCount() = provinces.size
        override fun getChildrenCount(groupPosition: Int): Int {
            val province = provinces[groupPosition]
            val subData = regionData[province] ?: return 0
            return subData[province]?.size ?: 0
        }
        override fun getGroup(groupPosition: Int): Any = provinces[groupPosition]
        override fun getChild(groupPosition: Int, childPosition: Int): Any {
            val province = provinces[groupPosition]
            val subData = regionData[province] ?: return ""
            return subData[province]?.get(childPosition) ?: ""
        }
        override fun getGroupId(groupPosition: Int) = groupPosition.toLong()
        override fun getChildId(groupPosition: Int, childPosition: Int) = childPosition.toLong()
        override fun hasStableIds() = false
        override fun isChildSelectable(groupPosition: Int, childPosition: Int) = true

        override fun getGroupView(
            groupPosition: Int, isExpanded: Boolean,
            convertView: View?, parent: ViewGroup?
        ): View {
            val view = convertView ?: LayoutInflater.from(this@SettingActivity)
                .inflate(android.R.layout.simple_expandable_list_item_1, parent, false)
            val tv = view.findViewById<TextView>(android.R.id.text1)
            tv.text = provinces[groupPosition]
            tv.textSize = 16f
            tv.setTextColor(resources.getColor(R.color.text_primary, null))
            view.setBackgroundColor(resources.getColor(R.color.surface, null))
            tv.setPadding(32, 24, 32, 24)
            return view
        }

        override fun getChildView(
            groupPosition: Int, childPosition: Int, isLastChild: Boolean,
            convertView: View?, parent: ViewGroup?
        ): View {
            val view = convertView ?: LayoutInflater.from(this@SettingActivity)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            val tv = view.findViewById<TextView>(android.R.id.text1)
            tv.text = getChild(groupPosition, childPosition).toString()
            tv.textSize = 15f
            tv.setTextColor(resources.getColor(R.color.text_secondary, null))
            view.setBackgroundColor(resources.getColor(R.color.background, null))
            tv.setPadding(64, 20, 32, 20)
            return view
        }
    }
}