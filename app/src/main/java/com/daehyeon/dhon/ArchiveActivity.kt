package com.daehyeon.dhon

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class ArchiveActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fileAdapter: FileAdapter
    private lateinit var tvTitle: TextView
    private val fileList = mutableListOf<File>()
    private var archivePath = ""
    private var restorePath = ""
    private var folderName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_archive)

        folderName = intent.getStringExtra("folderName") ?: ""
        archivePath = intent.getStringExtra("archivePath") ?: ""
        restorePath = intent.getStringExtra("restorePath") ?: ""

        tvTitle = findViewById(R.id.tvTitle)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.navBarSpacer)) { view, insets ->
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val params = view.layoutParams
            params.height = navBarHeight
            view.layoutParams = params
            insets
        }

        findViewById<LinearLayout>(R.id.btnHome).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        findViewById<LinearLayout>(R.id.btnBack).setOnClickListener {
            finish()
        }

        recyclerView = findViewById(R.id.recyclerFiles)
        recyclerView.layoutManager = LinearLayoutManager(this)

        fileAdapter = FileAdapter(
            fileList,
            onClick = { file -> openFile(file) },
            onLongClick = { file -> showFileOptions(file) }
        )
        recyclerView.adapter = fileAdapter

        loadAllFiles()
    }

    private fun loadAllFiles() {
        val archiveFolder = File(archivePath)
        fileList.clear()
        if (archiveFolder.exists()) {
            archiveFolder.walkTopDown()
                .filter { it.isFile }
                .sortedByDescending { it.lastModified() }
                .let { fileList.addAll(it) }
        }
        tvTitle.text = "지난서류 보관함 (${fileList.size}개)"
        fileAdapter.notifyDataSetChanged()
        if (fileList.isEmpty()) {
            Toast.makeText(this, "지난서류 보관함이 비어있어요", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFileOptions(file: File) {
        val options = arrayOf("공유하기", "복원하기", "완전 삭제")
        AlertDialog.Builder(this)
            .setTitle(file.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> shareFile(file)
                    1 -> restoreFile(file)
                    2 -> deleteFile(file)
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun restoreFile(file: File) {
        try {
            val baseRestoreFolder = if (restorePath.isNotEmpty()) {
                File(restorePath)
            } else {
                File(filesDir, folderName)
            }

            val parentName = file.parentFile?.name ?: ""
            val grandParentName = file.parentFile?.parentFile?.name ?: ""

            val targetFolder = when {
                // 수동이동: archive/2026년 4월/파일.pdf
                parentName.contains("년") && parentName.contains("월") -> {
                    File(baseRestoreFolder, parentName)
                }
                // 자동이동: archive/2026년/4월/파일.pdf
                parentName.contains("월") && grandParentName.contains("년") -> {
                    val yearStr = grandParentName.replace("년", "").trim()
                    val monthStr = parentName.replace("월", "").trim()
                    File(baseRestoreFolder, "${yearStr}년 ${monthStr}월")
                }
                else -> baseRestoreFolder
            }

            if (!targetFolder.exists()) targetFolder.mkdirs()
            file.copyTo(File(targetFolder, file.name), overwrite = true)
            file.delete()
            Toast.makeText(this, "원래 폴더로 복원됐어요!", Toast.LENGTH_SHORT).show()

            // 복원 후 WorkReportActivity 로 이동해서 목록 새로고침
            val intent = Intent(this, WorkReportActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()

        } catch (e: Exception) {
            Toast.makeText(this, "복원 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteFile(file: File) {
        AlertDialog.Builder(this)
            .setTitle("완전 삭제")
            .setMessage("${file.name} 을 완전히 삭제할까요?")
            .setPositiveButton("삭제") { _, _ ->
                file.delete()
                fileList.remove(file)
                fileAdapter.notifyDataSetChanged()
                tvTitle.text = "지난서류 보관함 (${fileList.size}개)"
                Toast.makeText(this, "삭제됐어요!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun openFile(file: File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this, "${packageName}.provider", file)
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, getMimeType(file.name))
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(android.content.Intent.createChooser(intent, "파일 열기"))
        } catch (e: Exception) {
            Toast.makeText(this, "파일을 열 수 없어요: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareFile(file: File) {
        try {
            val mimeType = getMimeType(file.name)
            val fileName = file.name
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = contentResolver
                val uri = resolver.insert(
                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { output ->
                        file.inputStream().use { input -> input.copyTo(output) }
                    }
                    contentValues.clear()
                    contentValues.put(android.provider.MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = mimeType
                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(android.content.Intent.createChooser(shareIntent, "파일 공유"))
                }
            } else {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val destFile = File(downloadsDir, fileName)
                file.copyTo(destFile, overwrite = true)
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    this, "${packageName}.provider", destFile)
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(android.content.Intent.createChooser(shareIntent, "파일 공유"))
            }
        } catch (e: Exception) {
            Toast.makeText(this, "공유 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
            fileName.endsWith(".doc", ignoreCase = true) -> "application/msword"
            fileName.endsWith(".docx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            fileName.endsWith(".xls", ignoreCase = true) -> "application/vnd.ms-excel"
            fileName.endsWith(".xlsx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            else -> "application/octet-stream"
        }
    }
}