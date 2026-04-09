package com.daehyeon.dhon

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class WeatherAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "WeatherAlarmReceiver"
        private const val MORNING_REQUEST_CODE = 3001
        private const val EVENING_REQUEST_CODE = 3002
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive 호출됨: action=${intent.action}")

        // ── 부팅 완료 시 알람 재등록 ──────────────────────
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "BOOT_COMPLETED → 날씨 알람 재등록")
            WeatherScheduler.scheduleWeatherAlarms(context)
            return
        }

        // ── 날씨 알람 실행 ────────────────────────────────
        val requestCode = intent.getIntExtra("requestCode", -1)
        Log.d(TAG, "날씨 알람 실행: requestCode=$requestCode")

        // 백그라운드에서 날씨 정보 가져와서 알림 표시
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val message = WeatherService.getWeatherMessage(context)
                WeatherNotificationHelper.showWeatherNotification(context, message)
                Log.d(TAG, "날씨 알림 표시 완료")
            } catch (e: Exception) {
                Log.e(TAG, "날씨 알림 실패: ${e.message}")
                WeatherNotificationHelper.showWeatherNotification(
                    context, "날씨 정보를 불러올 수 없어요. 인터넷 연결을 확인해주세요."
                )
            }
        }

        // ── 다음날 동일 시간에 알람 재등록 ───────────────
        scheduleNextAlarm(context, requestCode)
    }

    private fun scheduleNextAlarm(context: Context, requestCode: Int) {
        val hour = when (requestCode) {
            MORNING_REQUEST_CODE -> 5
            EVENING_REQUEST_CODE -> 20
            else -> return
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1)
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val nextIntent = Intent(context, WeatherAlarmReceiver::class.java).apply {
            action = "com.daehyeon.dhon.WEATHER_ALARM"
            putExtra("requestCode", requestCode)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "다음날 알람 재등록 완료: requestCode=$requestCode, time=${calendar.time}")
        } catch (e: SecurityException) {
            Log.e(TAG, "알람 재등록 실패 (권한 없음): ${e.message}")
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }
}
