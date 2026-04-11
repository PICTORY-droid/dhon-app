package com.daehyeon.dhon

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
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

class TrashActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fileAdapter: FileAdapter
    private lateinit var tvTitle: TextView
    private val fileList = mutableListOf<File>()
    private var folderName = "work_report"
    private var trashPath = ""
    private var restorePath = ""

    // ✅ 현재 보고 있는 폴더 (null이면 휴지통 최상위)
    private var currentViewFolder: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trash)

        folderName = intent.getStringExtra("folderName") ?: "work_report"
        trashPath = intent.getStringExtra("trashPath") ?: ""
        restorePath = intent.getStringExtra("restorePath") ?: ""

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.navBarSpacer)) { view, insets ->
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val params = view.layoutParams
            params.height = navBarHeight
            view.layoutParams = params
            insets
        }

        tvTitle = findViewById(R.id.tvTitle)

        findViewById<LinearLayout>(R.id.btnHome).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        findViewById<LinearLayout>(R.id.btnEmptyTrash).setOnClickListener {
            confirmEmptyTrash()
        }

        recyclerView = findViewById(R.id.recyclerFiles)
        recyclerView.layoutManager = LinearLayoutManager(this)

        fileAdapter = FileAdapter(
            fileList,
            onClick = { file ->
                if (file.isDirectory) {
                    // ✅ 폴더 클릭하면 안으로 들어가기
                    currentViewFolder = file
                    loadFilesInFolder(file)
                } else {
                    openFile(file)
                }
            },
            onLongClick = { file -> showItemOptions(file) }
        )
        recyclerView.adapter = fileAdapter

        loadFiles()
    }

    override fun onResume() {
        super.onResume()
        loadFiles()
    }

    // ✅ 뒤로가기 누르면 폴더 안에 있으면 상위로, 아니면 Activity 닫기
    override fun onBackPressed() {
        if (currentViewFolder != null) {
            currentViewFolder = null
            loadFiles()
        } else {
            super.onBackPressed()
        }
    }

    private fun loadFiles() {
        val trashFolder = if (trashPath.isNotEmpty()) {
            File(trashPath)
        } else {
            File(File(filesDir, folderName), "trash")
        }
        fileList.clear()
        if (trashFolder.exists()) {
            trashFolder.listFiles()
                ?.sortedWith(compareByDescending<File> { it.isDirectory }.thenByDescending { it.lastModified() })
                ?.let { fileList.addAll(it) }
        }
        tvTitle.text = "${fileList.size}개"
        fileAdapter.notifyDataSetChanged()
    }

    private fun loadFilesInFolder(folder: File) {
        fileList.clear()
        if (folder.exists()) {
            folder.listFiles()
                ?.sortedByDescending { it.lastModified() }
                ?.let { fileList.addAll(it) }
        }
        tvTitle.text = "${folder.name} (${fileList.size}개)"
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
        val trashFolder = if (trashPath.isNotEmpty()) {
            File(trashPath)
        } else {
            File(File(filesDir, folderName), "trash")
        }
        if (trashFolder.exists()) {
            trashFolder.listFiles()?.forEach { it.deleteRecursively() }
        }
        currentViewFolder = null
        fileList.clear()
        tvTitle.text = "0개"
        fileAdapter.notifyDataSetChanged()
        Toast.makeText(this, "휴지통을 비웠어요!", Toast.LENGTH_SHORT).show()
    }

    private fun showItemOptions(file: File) {
        AlertDialog.Builder(this)
            .setTitle(file.name)
            .setItems(arrayOf("복원하기", "완전 삭제")) { _, which ->
                when (which) {
                    0 -> restoreItem(file)
                    1 -> deleteItem(file)
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun restoreItem(file: File) {
        try {
            val baseRestoreFolder = if (restorePath.isNotEmpty()) {
                File(restorePath)
            } else {
                File(filesDir, folderName)
            }

            if (file.isDirectory) {
                val targetFolder = File(baseRestoreFolder, file.name)
                targetFolder.mkdirs()
                file.walkTopDown().forEach { f ->
                    if (f.isFile) {
                        val relativePath = f.relativeTo(file).path
                        val destFile = File(targetFolder, relativePath)
                        destFile.parentFile?.mkdirs()
                        f.copyTo(destFile, overwrite = true)
                    }
                }
                file.walkTopDown().filter { it.isFile }.forEach { it.delete() }
                file.walkBottomUp().filter { it.isDirectory && it != file }.forEach { it.delete() }
                file.delete()
                Toast.makeText(this, "'${file.name}' 폴더를 복원했어요!", Toast.LENGTH_SHORT).show()
            } else {
                val parentName = file.parentFile?.name ?: ""
                val targetFolder = when {
                    parentName.matches(Regex("\\d{4}년 \\d{1,2}월")) -> File(baseRestoreFolder, parentName)
                    else -> baseRestoreFolder
                }
                if (!targetFolder.exists()) targetFolder.mkdirs()
                file.copyTo(File(targetFolder, file.name), overwrite = true)
                file.delete()
                Toast.makeText(this, "'${file.name}' 을 복원했어요!", Toast.LENGTH_SHORT).show()
            }

            currentViewFolder = null
            loadFiles()

        } catch (e: Exception) {
            Toast.makeText(this, "복원 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteItem(file: File) {
        AlertDialog.Builder(this)
            .setTitle("완전 삭제")
            .setMessage("${file.name} 을 완전히 삭제할까요?")
            .setPositiveButton("삭제") { _, _ ->
                if (file.isDirectory) file.deleteRecursively() else file.delete()
                fileList.remove(file)
                tvTitle.text = "${fileList.size}개"
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
            fileName.endsWith(".jpg", ignoreCase = true) ||
                    fileName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
            fileName.endsWith(".png", ignoreCase = true) -> "image/png"
            else -> "application/octet-stream"
        }
    }
}
