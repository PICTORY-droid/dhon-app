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

    // ─────────────────────────────────────────────────────
    // onCreate
    // ─────────────────────────────────────────────────────
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

        // ── 홈 버튼 ──────────────────────────────────────
        findViewById<LinearLayout>(R.id.btnHome).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        // ── 전체 비우기 버튼 ──────────────────────────────
        findViewById<LinearLayout>(R.id.btnEmptyTrash).setOnClickListener {
            showDeleteAllDialog()
        }

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        fileAdapter = FileAdapter(
            fileList,
            onClick = { file ->
                if (file.isDirectory) {
                    loadFilesInFolder(file)
                } else {
                    openFile(file)
                }
            },
            onLongClick = { file -> showFileOptions(file) }
        )
        recyclerView.adapter = fileAdapter

        loadFiles()
    }

    // ─────────────────────────────────────────────────────
    // 휴지통 파일/폴더 목록 로드
    // ─────────────────────────────────────────────────────
    private fun loadFiles() {
        val trashFolder = getTrashFolder()
        fileList.clear()

        if (trashFolder.exists()) {
            val items = trashFolder.listFiles()
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
            fileList.addAll(items)
        }

        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)
        tvEmpty.visibility = if (fileList.isEmpty()) View.VISIBLE else View.GONE
        fileAdapter.notifyDataSetChanged()
    }

    // ─────────────────────────────────────────────────────
    // 날짜폴더 안 파일 목록 로드 (폴더 클릭 시)
    // ─────────────────────────────────────────────────────
    private fun loadFilesInFolder(folder: File) {
        fileList.clear()
        if (folder.exists()) {
            val items = folder.listFiles()
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
            fileList.addAll(items)
        }
        fileAdapter.notifyDataSetChanged()
    }

    // ─────────────────────────────────────────────────────
    // 휴지통 폴더 경로 반환
    // → docs_manage/trash/category/subItem/
    // ─────────────────────────────────────────────────────
    private fun getTrashFolder(): File {
        val baseFolder = File(filesDir, "docs_manage")
        return if (subItem.isNotEmpty()) {
            File(baseFolder, "trash/$category/$subItem")
        } else {
            File(baseFolder, "trash/$category")
        }
    }

    // ─────────────────────────────────────────────────────
    // 파일/폴더 옵션 메뉴 (롱클릭)
    // ─────────────────────────────────────────────────────
    private fun showFileOptions(file: File) {
        val label = if (file.isDirectory) "폴더: ${file.name}" else file.name
        val options = arrayOf("복원하기", "완전 삭제")
        AlertDialog.Builder(this)
            .setTitle(label)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> restoreItem(file)
                    1 -> deleteItemPermanently(file)
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // ─────────────────────────────────────────────────────
    // 파일/폴더 복원
    // ─────────────────────────────────────────────────────
    private fun restoreItem(item: File) {
        try {
            val baseFolder = File(filesDir, "docs_manage")
            val restoreBaseFolder = if (subItem.isNotEmpty()) {
                File(baseFolder, "$category/$subItem")
            } else {
                File(baseFolder, category)
            }
            if (!restoreBaseFolder.exists()) restoreBaseFolder.mkdirs()

            if (item.isDirectory) {
                val destFolder = File(restoreBaseFolder, item.name)
                if (!destFolder.exists()) destFolder.mkdirs()
                item.walkTopDown().forEach { f ->
                    if (f.isFile) {
                        val rel = f.relativeTo(item).path
                        val dest = File(destFolder, rel)
                        dest.parentFile?.mkdirs()
                        f.copyTo(dest, overwrite = true)
                    }
                }
                item.deleteRecursively()
                Toast.makeText(this, "폴더 '${item.name}' 을 복원했어요!", Toast.LENGTH_SHORT).show()
            } else {
                item.copyTo(File(restoreBaseFolder, item.name), overwrite = true)
                item.delete()
                Toast.makeText(this, "'${item.name}' 을 복원했어요!", Toast.LENGTH_SHORT).show()
            }
            loadFiles()
        } catch (e: Exception) {
            Toast.makeText(this, "복원 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ─────────────────────────────────────────────────────
    // 파일/폴더 완전 삭제
    // ─────────────────────────────────────────────────────
    private fun deleteItemPermanently(item: File) {
        val label = if (item.isDirectory) "폴더 '${item.name}'" else "'${item.name}'"
        AlertDialog.Builder(this)
            .setTitle("완전 삭제")
            .setMessage("$label 을 완전히 삭제할까요?\n복원할 수 없어요!")
            .setPositiveButton("삭제") { _, _ ->
                if (item.isDirectory) item.deleteRecursively() else item.delete()
                Toast.makeText(this, "완전히 삭제했어요!", Toast.LENGTH_SHORT).show()
                loadFiles()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // ─────────────────────────────────────────────────────
    // 휴지통 전체 비우기
    // ─────────────────────────────────────────────────────
    private fun showDeleteAllDialog() {
        if (fileList.isEmpty()) {
            Toast.makeText(this, "휴지통이 비어있어요!", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("휴지통 비우기")
            .setMessage("휴지통의 항목 ${fileList.size}개를 모두 삭제할까요?\n복원할 수 없어요!")
            .setPositiveButton("전체 삭제") { _, _ ->
                val trashFolder = getTrashFolder()
                trashFolder.listFiles()?.forEach { item ->
                    if (item.isDirectory) item.deleteRecursively() else item.delete()
                }
                Toast.makeText(this, "휴지통을 비웠어요!", Toast.LENGTH_SHORT).show()
                loadFiles()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // ─────────────────────────────────────────────────────
    // 파일 열기 (미리보기)
    // ─────────────────────────────────────────────────────
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

    // ─────────────────────────────────────────────────────
    // MIME 타입 반환
    // ─────────────────────────────────────────────────────
    private fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".pdf", ignoreCase = true)  -> "application/pdf"
            fileName.endsWith(".doc", ignoreCase = true)  -> "application/msword"
            fileName.endsWith(".docx", ignoreCase = true) ->
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            fileName.endsWith(".xls", ignoreCase = true)  -> "application/vnd.ms-excel"
            fileName.endsWith(".xlsx", ignoreCase = true) ->
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            else -> "application/octet-stream"
        }
    }
}
