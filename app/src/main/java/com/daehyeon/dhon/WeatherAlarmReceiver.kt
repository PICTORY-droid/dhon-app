package com.daehyeon.dhon

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WeatherAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // 백그라운드에서 날씨 정보 가져온 후 알림 표시
        CoroutineScope(Dispatchers.IO).launch {
            val message = WeatherService.getWeatherMessage(context)
            WeatherNotificationHelper.showWeatherNotification(context, message)
        }
    }
}