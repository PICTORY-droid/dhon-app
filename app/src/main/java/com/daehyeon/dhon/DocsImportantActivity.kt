package com.daehyeon.dhon

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class DocsImportantActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fileAdapter: FileAdapter
    private val fileList = mutableListOf<File>()

    private var category = ""
    private var subItem = ""
    private var currentViewFolder: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_docs_important)

        category = intent.getStringExtra("category") ?: ""
        subItem = intent.getStringExtra("subItem") ?: ""

        findViewById<android.widget.ImageButton>(R.id.btnBack).setOnClickListener {
            if (currentViewFolder != null) {
                currentViewFolder = null
                loadFiles()
            } else {
                finish()
            }
        }

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        fileAdapter = FileAdapter(
            fileList,
            onClick = { file ->
                if (file.isDirectory) {
                    currentViewFolder = file
                    loadFilesInFolder(file)
                } else {
                    openFile(file)
                }
            },
            onLongClick = { file ->
                if (file.isDirectory) {
                    showFolderOptions(file)
                } else {
                    showFileOptions(file)
                }
            }
        )
        recyclerView.adapter = fileAdapter

        loadFiles()
    }

    override fun onBackPressed() {
        if (currentViewFolder != null) {
            currentViewFolder = null
            loadFiles()
        } else {
            super.onBackPressed()
        }
    }

    private fun loadFiles() {
        val baseFolder = File(filesDir, "docs_manage")
        val importantFolder = File(File(File(baseFolder, "important"), category), subItem)

        fileList.clear()
        if (importantFolder.exists()) {
            val subFolders = importantFolder.listFiles()
                ?.filter { it.isDirectory }
                ?.sortedByDescending { it.name }
                ?: emptyList()
            val files = importantFolder.listFiles()
                ?.filter { it.isFile }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
            fileList.addAll(subFolders)
            fileList.addAll(files)
        }

        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)
        tvEmpty.visibility = if (fileList.isEmpty()) View.VISIBLE else View.GONE

        fileAdapter.notifyDataSetChanged()
    }

    private fun loadFilesInFolder(folder: File) {
        fileList.clear()
        if (folder.exists()) {
            val filesInFolder = folder.listFiles()
                ?.filter { it.isFile }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
            fileList.addAll(filesInFolder)
        }

        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)
        tvEmpty.visibility = if (fileList.isEmpty()) View.VISIBLE else View.GONE

        fileAdapter.notifyDataSetChanged()
    }

    private fun showFolderOptions(folder: File) {
        val options = arrayOf("복원하기", "휴지통으로 이동")
        AlertDialog.Builder(this)
            .setTitle(folder.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> restoreFolder(folder)
                    1 -> moveFolderToTrash(folder)
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun restoreFolder(folder: File) {
        try {
            val baseFolder = File(filesDir, "docs_manage")
            val restoreFolder = File(File(File(baseFolder, category), subItem), folder.name)
            if (!restoreFolder.exists()) restoreFolder.mkdirs()
            folder.listFiles()?.forEach { file ->
                if (file.isFile) {
                    file.copyTo(File(restoreFolder, file.name), overwrite = true)
                }
            }
            folder.deleteRecursively()
            Toast.makeText(this, "복원 완료!", Toast.LENGTH_SHORT).show()
            loadFiles()
        } catch (e: Exception) {
            Toast.makeText(this, "복원 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun moveFolderToTrash(folder: File) {
        try {
            val trashFolder = File(
                File(File(filesDir, "docs_manage"), "trash"),
                "중요보관함/${folder.name}"
            )
            if (!trashFolder.exists()) trashFolder.mkdirs()
            folder.listFiles()?.forEach { file ->
                if (file.isFile) {
                    file.copyTo(File(trashFolder, file.name), overwrite = true)
                }
            }
            folder.deleteRecursively()
            Toast.makeText(this, "휴지통으로 이동했어요!", Toast.LENGTH_SHORT).show()
            loadFiles()
        } catch (e: Exception) {
            Toast.makeText(this, "이동 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFileOptions(file: File) {
        val options = arrayOf("공유하기", "휴지통으로 이동")
        AlertDialog.Builder(this)
            .setTitle(file.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> shareFile(file)
                    1 -> moveToTrash(file)
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun moveToTrash(file: File) {
        try {
            val trashFolder = File(
                File(File(filesDir, "docs_manage"), "trash"),
                "중요보관함"
            )
            if (!trashFolder.exists()) trashFolder.mkdirs()
            file.copyTo(File(trashFolder, file.name), overwrite = true)
            file.delete()
            Toast.makeText(this, "휴지통으로 이동했어요!", Toast.LENGTH_SHORT).show()
            if (currentViewFolder != null) {
                loadFilesInFolder(currentViewFolder!!)
            } else {
                loadFiles()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "이동 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
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

    private fun shareFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = getMimeType(file.name)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "파일 공유"))
        } catch (e: Exception) {
            Toast.makeText(this, "공유 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".pdf") -> "application/pdf"
            fileName.endsWith(".doc") -> "application/msword"
            fileName.endsWith(".docx") -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            fileName.endsWith(".xls") -> "application/vnd.ms-excel"
            fileName.endsWith(".xlsx") -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            else -> "*/*"
        }
    }
}
