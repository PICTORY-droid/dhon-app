package com.daehyeon.dhon

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class TrashActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fileAdapter: FileAdapter
    private val fileList = mutableListOf<File>()
    private var folderName = "work_report"
    // ✅ 복원 경로 추가 (WorkReportActivity에서 전달받음)
    private var restorePath = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trash)

        folderName = intent.getStringExtra("folderName") ?: "work_report"
        // ✅ restorePath 받기
        restorePath = intent.getStringExtra("restorePath") ?: ""

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.navBarSpacer)) { view, insets ->
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val params = view.layoutParams
            params.height = navBarHeight
            view.layoutParams = params
            insets
        }

        // 홈 버튼
        findViewById<LinearLayout>(R.id.btnHome).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        // 전체비우기 버튼
        findViewById<LinearLayout>(R.id.btnEmptyTrash).setOnClickListener {
            confirmEmptyTrash()
        }

        recyclerView = findViewById(R.id.recyclerFiles)
        recyclerView.layoutManager = LinearLayoutManager(this)

        fileAdapter = FileAdapter(
            fileList,
            onClick = { file -> openFile(file) },
            onLongClick = { file -> showFileOptions(file) }
        )
        recyclerView.adapter = fileAdapter

        loadFiles()
    }

    // ✅ onResume: 복원 후 돌아왔을 때 목록 자동 갱신
    override fun onResume() {
        super.onResume()
        loadFiles()
    }

    private fun loadFiles() {
        val trashFolder = File(File(filesDir, folderName), "trash")
        fileList.clear()
        if (trashFolder.exists()) {
            trashFolder.walkTopDown()
                .filter { it.isFile }
                .sortedByDescending { it.lastModified() }
                .let { fileList.addAll(it) }
        }
        fileAdapter.notifyDataSetChanged()
    }

    private fun confirmEmptyTrash() {
        if (fileList.isEmpty()) {
            Toast.makeText(this, "휴지통이 비어있어요!", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("전체 비우기")
            .setMessage("휴지통을 모두 비울까요?\n복구할 수 없어요!")
            .setPositiveButton("비우기") { _, _ -> emptyTrash() }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun emptyTrash() {
        val trashFolder = File(File(filesDir, folderName), "trash")
        if (trashFolder.exists()) {
            trashFolder.walkTopDown()
                .filter { it.isFile }
                .forEach { it.delete() }
        }
        fileList.clear()
        fileAdapter.notifyDataSetChanged()
        Toast.makeText(this, "휴지통을 비웠어요!", Toast.LENGTH_SHORT).show()
    }

    private fun showFileOptions(file: File) {
        val options = arrayOf("복원하기", "완전 삭제")
        AlertDialog.Builder(this)
            .setTitle(file.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> restoreFile(file)
                    1 -> deleteFile(file)
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // ✅ 핵심 수정: 원래 월별 폴더에 복원
    private fun restoreFile(file: File) {
        try {
            // 복원 기본 경로 결정
            val baseRestoreFolder = if (restorePath.isNotEmpty()) {
                File(restorePath)
            } else {
                File(filesDir, folderName)
            }

            // ✅ 휴지통 안에서 파일의 부모 폴더명 확인
            // 예: trash/2026년 4월/파일명 → 부모 = "2026년 4월"
            val parentName = file.parentFile?.name ?: ""
            val grandParentName = file.parentFile?.parentFile?.name ?: ""

            val targetFolder = when {
                // 월별 폴더 안에 있는 파일: "2026년 4월" 형식
                parentName.matches(Regex("\\d{4}년 \\d{1,2}월")) -> {
                    File(baseRestoreFolder, parentName)
                }
                // 연도/월 2단계 폴더 구조: "2026년/4월" 형식
                parentName.contains("월") && grandParentName.contains("년") -> {
                    val yearStr = grandParentName.replace("년", "").trim()
                    val monthStr = parentName.replace("월", "").trim()
                    File(baseRestoreFolder, "${yearStr}년 ${monthStr}월")
                }
                // 그 외: 기본 복원 폴더로
                else -> baseRestoreFolder
            }

            if (!targetFolder.exists()) targetFolder.mkdirs()
            file.copyTo(File(targetFolder, file.name), overwrite = true)
            file.delete()
            Toast.makeText(this, "'${file.name}' 을 복원했어요!", Toast.LENGTH_SHORT).show()
            // ✅ 복원 후 휴지통 목록 갱신
            loadFiles()

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
                Toast.makeText(this, "삭제됐어요!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun openFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, getMimeType(file.name))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "파일 열기"))
        } catch (e: Exception) {
            Toast.makeText(this, "파일을 열 수 없어요: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
            fileName.endsWith(".doc", ignoreCase = true) -> "application/msword"
            fileName.endsWith(".docx", ignoreCase = true) ->
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            fileName.endsWith(".xls", ignoreCase = true) -> "application/vnd.ms-excel"
            fileName.endsWith(".xlsx", ignoreCase = true) ->
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            else -> "application/octet-stream"
        }
    }
}
