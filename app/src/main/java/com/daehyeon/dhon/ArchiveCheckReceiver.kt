package com.daehyeon.dhon

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.io.File
import java.util.Calendar

class ArchiveCheckReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        checkAndArchiveOldFiles(context, "work_report")
    }

    private fun checkAndArchiveOldFiles(context: Context, folderName: String) {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1

        val baseFolder = File(context.filesDir, folderName)
        if (!baseFolder.exists()) return

        for (year in 2020..currentYear) {
            for (month in 1..12) {
                if (year == currentYear && month >= currentMonth) continue

                val monthFolder = File(baseFolder, "${year}년 ${month}월")
                if (monthFolder.exists() && !monthFolder.listFiles().isNullOrEmpty()) {
                    val archiveFolder = File(
                        File(File(baseFolder, "archive"), "${year}년"),
                        "${month}월"
                    )
                    if (!archiveFolder.exists()) {
                        archiveFolder.mkdirs()
                        monthFolder.listFiles()?.forEach { file ->
                            file.copyTo(File(archiveFolder, file.name), overwrite = true)
                            file.delete()
                        }
                        monthFolder.delete()

                        NotificationHelper.sendNotification(
                            context,
                            "📦 지난서류 자동이동",
                            "${year}년 ${month}월 출력일보가 지난서류 보관함으로 이동되었습니다"
                        )
                    }
                }
            }
        }
    }
}