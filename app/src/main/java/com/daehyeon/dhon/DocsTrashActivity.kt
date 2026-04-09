package com.daehyeon.dhon

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class DocsTrashActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fileAdapter: FileAdapter
    private val fileList = mutableListOf<File>()
    private var category = ""
    private var subItem = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_docs_trash)

        category = intent.getStringExtra("category") ?: ""
        subItem = intent.getStringExtra("subItem") ?: ""

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

        // 전체 비우기 버튼
        findViewById<LinearLayout>(R.id.btnEmptyTrash).setOnClickListener {
            showDeleteAllDialog()
        }

        recyclerView = findViewById(R.id.recyclerView)
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
        val trashFolder = getTrashFolder()
        fileList.clear()
        if (trashFolder.exists()) {
            trashFolder.walkTopDown()
                .filter { it.isFile }
                .sortedByDescending { it.lastModified() }
                .let { fileList.addAll(it) }
        }

        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)
        tvEmpty.visibility = if (fileList.isEmpty()) View.VISIBLE else View.GONE

        fileAdapter.notifyDataSetChanged()
    }

    private fun getTrashFolder(): File {
        val baseFolder = File(filesDir, "docs_manage")
        return if (subItem.isNotEmpty()) {
            File(File(File(baseFolder, "trash"), category), subItem)
        } else {
            File(File(baseFolder, "trash"), category)
        }
    }

    private fun showFileOptions(file: File) {
        val options = arrayOf("복원하기", "완전 삭제")
        AlertDialog.Builder(this)
            .setTitle(file.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> restoreFile(file)
                    1 -> deleteFilePermanently(file)
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun restoreFile(file: File) {
        try {
            val baseFolder = File(filesDir, "docs_manage")
            val restoreFolder = if (subItem.isNotEmpty()) {
                File(File(baseFolder, category), subItem)
            } else {
                File(baseFolder, category)
            }
            if (!restoreFolder.exists()) restoreFolder.mkdirs()
            file.copyTo(File(restoreFolder, file.name), overwrite = true)
            file.delete()
            Toast.makeText(this, "복원했어요!", Toast.LENGTH_SHORT).show()
            loadFiles()
        } catch (e: Exception) {
            Toast.makeText(this, "복원 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteFilePermanently(file: File) {
        AlertDialog.Builder(this)
            .setTitle("완전 삭제")
            .setMessage("${file.name} 을 완전히 삭제할까요?\n복원할 수 없어요!")
            .setPositiveButton("삭제") { _, _ ->
                file.delete()
                Toast.makeText(this, "완전히 삭제했어요!", Toast.LENGTH_SHORT).show()
                loadFiles()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showDeleteAllDialog() {
        if (fileList.isEmpty()) {
            Toast.makeText(this, "휴지통이 비어있어요!", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("휴지통 비우기")
            .setMessage("휴지통의 파일 ${fileList.size}개를 모두 삭제할까요?\n복원할 수 없어요!")
            .setPositiveButton("전체 삭제") { _, _ ->
                val trashFolder = getTrashFolder()
                trashFolder.walkTopDown()
                    .filter { it.isFile }
                    .forEach { it.delete() }
                Toast.makeText(this, "휴지통을 비웠어요!", Toast.LENGTH_SHORT).show()
                loadFiles()
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