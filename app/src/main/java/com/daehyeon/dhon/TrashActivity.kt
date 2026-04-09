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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trash)

        folderName = intent.getStringExtra("folderName") ?: "work_report"

        // 반응형: 시스템 네비게이션 바 높이 자동 적용
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

    private fun restoreFile(file: File) {
        try {
            val restoreFolder = File(File(filesDir, folderName), "restored")
            if (!restoreFolder.exists()) restoreFolder.mkdirs()
            file.copyTo(File(restoreFolder, file.name), overwrite = true)
            file.delete()
            Toast.makeText(this, "복원했어요!", Toast.LENGTH_SHORT).show()
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
            fileName.endsWith(".docx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            fileName.endsWith(".xls", ignoreCase = true) -> "application/vnd.ms-excel"
            fileName.endsWith(".xlsx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            else -> "application/octet-stream"
        }
    }
}