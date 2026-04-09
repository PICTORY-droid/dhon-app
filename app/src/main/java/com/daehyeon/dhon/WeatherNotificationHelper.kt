package com.daehyeon.dhon

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

object WeatherNotificationHelper {

    private const val CHANNEL_ID = "weather_channel_v3"
    private const val CHANNEL_NAME = "날씨 알림"
    private const val NOTIFICATION_ID = 2001
    private const val TAG = "WeatherNotification"

    fun showWeatherNotification(context: Context, message: String) {
        Log.d(TAG, "showWeatherNotification 시작")

        createNotificationChannel(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        if (!notificationManager.areNotificationsEnabled()) {
            Log.e(TAG, "알림 권한이 없습니다!")
            return
        }

        // 알림 클릭 시 WeatherDetailActivity 열기 + 날씨 메시지 전달
        val intent = Intent(context, WeatherDetailActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("weather_message", message)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 상단 요약 텍스트
        val summaryLine = message.lines()
            .firstOrNull { it.contains("°C") } ?: "날씨 알림이 도착했어요"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🌤 DH On 날씨 알림")
            .setContentText(summaryLine)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
                    .setBigContentTitle("🌤 DH On 날씨 알림")
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
        Log.d(TAG, "notify 호출 완료!")
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

            notificationManager.deleteNotificationChannel("weather_channel")
            notificationManager.deleteNotificationChannel("weather_channel_v2")

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "매일 날씨 알림"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "새 채널 생성 완료: $CHANNEL_ID")
        }
    }
}