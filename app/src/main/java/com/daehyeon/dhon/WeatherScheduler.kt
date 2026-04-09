package com.daehyeon.dhon

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object WeatherScheduler {

    private const val MORNING_REQUEST_CODE = 3001  // 아침 5시
    private const val EVENING_REQUEST_CODE = 3002  // 저녁 8시

    // 알람 2개 등록 (아침 5시 + 저녁 8시)
    fun scheduleWeatherAlarms(context: Context) {
        scheduleMorningAlarm(context)
        scheduleEveningAlarm(context)
    }

    // 아침 5시 알람
    private fun scheduleMorningAlarm(context: Context) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 5)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // 이미 지난 시간이면 다음날로
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        setAlarm(context, calendar.timeInMillis, MORNING_REQUEST_CODE)
    }

    // 저녁 8시 알람
    private fun scheduleEveningAlarm(context: Context) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        setAlarm(context, calendar.timeInMillis, EVENING_REQUEST_CODE)
    }

    // 알람 등록
    private fun setAlarm(context: Context, timeInMillis: Long, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, WeatherAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 매일 반복
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    // 알람 취소
    fun cancelWeatherAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        listOf(MORNING_REQUEST_CODE, EVENING_REQUEST_CODE).forEach { requestCode ->
            val intent = Intent(context, WeatherAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}