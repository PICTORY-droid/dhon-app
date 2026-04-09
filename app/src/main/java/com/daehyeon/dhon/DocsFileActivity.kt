package com.daehyeon.dhon

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

class DocsFileActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fileAdapter: FileAdapter
    private lateinit var tvTitle: TextView
    private lateinit var tvSortIcon: TextView
    private lateinit var tvFileCount: TextView
    private val fileList = mutableListOf<File>()

    private var category = ""
    private var subItem = ""
    private var currentFolder: File? = null
    private var monthPickerDialog: AlertDialog? = null
    private var currentViewFolder: File? = null

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            monthPickerDialog?.dismiss()
            monthPickerDialog = null

            val uris = mutableListOf<Uri>()
            val clipData = result.data?.clipData
            if (clipData != null) {
                for (i in 0 until clipData.itemCount) {
                    uris.add(clipData.getItemAt(i).uri)
                }
            } else {
                result.data?.data?.let { uris.add(it) }
            }

            val finalUris = if (uris.size > 20) {
                Toast.makeText(this, "20개 초과! 앞의 20개만 불러올게요.", Toast.LENGTH_SHORT).show()
                uris.take(20)
            } else {
                uris
            }

            var successCount = 0
            finalUris.forEach { uri ->
                try {
                    copyFileToInternal(uri)
                    successCount++
                } catch (e: Exception) { }
            }
            if (successCount > 0) {
                Toast.makeText(this, "${successCount}개 파일 추가 완료!", Toast.LENGTH_SHORT).show()
            }
        } else {
            monthPickerDialog?.dismiss()
            monthPickerDialog = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_docs_file)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.navBarSpacer)) { view, insets ->
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val params = view.layoutParams
            params.height = navBarHeight
            view.layoutParams = params
            insets
        }

        category = intent.getStringExtra("category") ?: ""
        subItem = intent.getStringExtra("subItem") ?: ""

        tvTitle = findViewById(R.id.tvTitle)
        tvSortIcon = findViewById(R.id.tvSortIcon)
        tvFileCount = findViewById(R.id.tvFileCount)
        recyclerView = findViewById(R.id.recyclerFiles)
        recyclerView.layoutManager = LinearLayoutManager(this)

        tvTitle.text = subItem

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

        findViewById<LinearLayout>(R.id.btnHome).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        findViewById<LinearLayout>(R.id.btnArchive).setOnClickListener {
            val archivePath = File(
                File(File(File(filesDir, "docs_manage"), "archive"), category), subItem
            ).absolutePath
            val intent = Intent(this, ArchiveActivity::class.java)
            intent.putExtra("folderName", "docs_manage")
            intent.putExtra("archivePath", archivePath)
            startActivity(intent)
        }

        findViewById<LinearLayout>(R.id.btnImportant).setOnClickListener {
            val intent = Intent(this, DocsImportantActivity::class.java)
            intent.putExtra("category", category)
            intent.putExtra("subItem", subItem)
            startActivity(intent)
        }

        findViewById<LinearLayout>(R.id.btnTrash).setOnClickListener {
            val intent = Intent(this, DocsTrashActivity::class.java)
            intent.putExtra("category", category)
            intent.putExtra("subItem", subItem)
            startActivity(intent)
        }

        findViewById<LinearLayout>(R.id.btnAddFile).setOnClickListener {
            showMonthPickerDialog()
        }

        findViewById<LinearLayout>(R.id.btnSort).setOnClickListener {
            fileAdapter.isDescending = !fileAdapter.isDescending
            tvSortIcon.text = if (fileAdapter.isDescending) "↓" else "↑"
            fileAdapter.sortFiles()
        }

        createMonthFoldersIfNotExist()
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

    private fun createMonthFoldersIfNotExist() {
        val baseFolder = File(filesDir, "docs_manage")
        val subItemFolder = File(File(baseFolder, category), subItem)
        for (i in 0..35) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -i)
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH) + 1
            val monthFolder = File(subItemFolder, "${y}년 ${m}월")
            if (!monthFolder.exists()) monthFolder.mkdirs()
        }
    }

    private fun loadFiles() {
        val baseFolder = File(filesDir, "docs_manage")
        val folder = File(File(baseFolder, category), subItem)
        fileList.clear()
        if (folder.exists()) {
            val subFolders = folder.listFiles()
                ?.filter { it.isDirectory }
                ?.sortedByDescending { it.name }
                ?: emptyList()
            fileList.addAll(subFolders)
        }
        currentViewFolder = null
        val totalFileCount = countAllFiles(folder)
        tvTitle.text = subItem
        tvFileCount.text = "${totalFileCount}개"
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
        tvTitle.text = "${subItem} > ${folder.name}"
        tvFileCount.text = "${fileList.size}개"
        fileAdapter.notifyDataSetChanged()
    }

    private fun countAllFiles(folder: File): Int {
        if (!folder.exists()) return 0
        return folder.walkTopDown().filter { it.isFile }.count()
    }

    private fun showFolderOptions(folder: File) {
        val options = arrayOf("중요 보관함으로 이동", "지난서류로 이동", "휴지통으로 이동")
        AlertDialog.Builder(this)
            .setTitle(folder.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> moveFolderToImportant(folder)
                    1 -> showFolderMoveToArchiveMonthPicker(folder)
                    2 -> moveFolderToTrash(folder)
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun moveFolderToImportant(folder: File) {
        try {
            val importantFolder = File(File(File(filesDir, "docs_manage"), "important"), category)
            val destFolder = File(importantFolder, "${subItem}/${folder.name}")
            if (!destFolder.exists()) destFolder.mkdirs()
            folder.listFiles()?.forEach { file ->
                if (file.isFile) {
                    file.copyTo(File(destFolder, file.name), overwrite = true)
                }
            }
            folder.deleteRecursively()
            createMonthFoldersIfNotExist()
            Toast.makeText(this, "중요 보관함으로 이동했어요!", Toast.LENGTH_SHORT).show()
            loadFiles()
        } catch (e: Exception) {
            Toast.makeText(this, "이동 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFolderMoveToArchiveMonthPicker(folder: File) {
        val recentMonths = mutableListOf<String>()
        for (i in -1..4) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -i + 1)
            recentMonths.add("${cal.get(Calendar.YEAR)}년 ${cal.get(Calendar.MONTH) + 1}월")
        }
        val oldMonths = mutableListOf<String>()
        for (i in 6..36) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -i + 1)
            oldMonths.add("${cal.get(Calendar.YEAR)}년 ${cal.get(Calendar.MONTH) + 1}월")
        }
        val dialogView = layoutInflater.inflate(R.layout.dialog_month_picker, null)
        val containerRecent = dialogView.findViewById<LinearLayout>(R.id.containerRecent)
        val containerOld = dialogView.findViewById<LinearLayout>(R.id.containerOld)
        val btnToggle = dialogView.findViewById<Button>(R.id.btnToggle)
        val scrollOld = dialogView.findViewById<ScrollView>(R.id.scrollOld)
        val archiveDialog = AlertDialog.Builder(this)
            .setTitle("지난서류 보관함 - 월 선택")
            .setView(dialogView)
            .setNegativeButton("취소", null)
            .create()
        recentMonths.forEach { month ->
            val btn = Button(this).apply {
                text = month; textSize = 15f
                setTextColor(android.graphics.Color.WHITE)
                backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1E3A8A"))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.setMargins(0,0,0,16) }
            }
            btn.setOnClickListener { moveFolderToArchive(folder, month); archiveDialog.dismiss() }
            containerRecent.addView(btn)
        }
        scrollOld.visibility = View.GONE
        oldMonths.forEach { month ->
            val btn = Button(this).apply {
                text = month; textSize = 15f
                setTextColor(android.graphics.Color.WHITE)
                backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1E3A8A"))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.setMargins(0,0,0,16) }
            }
            btn.setOnClickListener { moveFolderToArchive(folder, month); archiveDialog.dismiss() }
            containerOld.addView(btn)
        }
        var isOldVisible = false
        btnToggle.setOnClickListener {
            isOldVisible = !isOldVisible
            scrollOld.visibility = if (isOldVisible) View.VISIBLE else View.GONE
            btnToggle.text = if (isOldVisible) "▲ 이전 기록 닫기" else "▶ 이전 기록 보기"
        }
        archiveDialog.show()
    }

    private fun moveFolderToArchive(folder: File, monthStr: String) {
        try {
            val baseFolder = File(filesDir, "docs_manage")
            val archiveFolder = File(File(File(baseFolder, "archive"), category), "$subItem/$monthStr/${folder.name}")
            if (!archiveFolder.exists()) archiveFolder.mkdirs()
            folder.listFiles()?.forEach { file ->
                if (file.isFile) {
                    file.copyTo(File(archiveFolder, file.name), overwrite = true)
                }
            }
            folder.deleteRecursively()
            createMonthFoldersIfNotExist()
            Toast.makeText(this, "지난서류 보관함으로 이동했어요!", Toast.LENGTH_SHORT).show()
            loadFiles()
        } catch (e: Exception) {
            Toast.makeText(this, "이동 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun moveFolderToTrash(folder: File) {
        try {
            val trashFolder = File(File(File(filesDir, "docs_manage"), "trash"), category)
            val destFolder = File(trashFolder, "${subItem}/${folder.name}")
            if (!destFolder.exists()) destFolder.mkdirs()
            folder.listFiles()?.forEach { file ->
                if (file.isFile) {
                    file.copyTo(File(destFolder, file.name), overwrite = true)
                }
            }
            folder.deleteRecursively()
            createMonthFoldersIfNotExist()
            Toast.makeText(this, "휴지통으로 이동했어요!", Toast.LENGTH_SHORT).show()
            loadFiles()
        } catch (e: Exception) {
            Toast.makeText(this, "이동 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showMonthPickerDialog() {
        val recentMonths = mutableListOf<String>()
        for (i in -1..4) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -i + 1)
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH) + 1
            recentMonths.add("${y}년 ${m}월")
        }
        val oldMonths = mutableListOf<String>()
        for (i in 6..36) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -i + 1)
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH) + 1
            oldMonths.add("${y}년 ${m}월")
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_month_picker, null)
        val containerRecent = dialogView.findViewById<LinearLayout>(R.id.containerRecent)
        val containerOld = dialogView.findViewById<LinearLayout>(R.id.containerOld)
        val btnToggle = dialogView.findViewById<Button>(R.id.btnToggle)
        val scrollOld = dialogView.findViewById<ScrollView>(R.id.scrollOld)

        recentMonths.forEach { month ->
            val btn = Button(this).apply {
                text = month; textSize = 15f
                setTextColor(android.graphics.Color.WHITE)
                backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#1E3A8A"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 16) }
            }
            btn.setOnClickListener { saveToMonth(month) }
            containerRecent.addView(btn)
        }

        scrollOld.visibility = View.GONE
        oldMonths.forEach { month ->
            val btn = Button(this).apply {
                text = month; textSize = 15f
                setTextColor(android.graphics.Color.WHITE)
                backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#1E3A8A"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 16) }
            }
            btn.setOnClickListener { saveToMonth(month) }
            containerOld.addView(btn)
        }

        var isOldVisible = false
        btnToggle.setOnClickListener {
            isOldVisible = !isOldVisible
            scrollOld.visibility = if (isOldVisible) View.VISIBLE else View.GONE
            btnToggle.text = if (isOldVisible) "▲ 이전 기록 닫기" else "▶ 이전 기록 보기"
        }

        monthPickerDialog = AlertDialog.Builder(this)
            .setTitle("저장할 월을 선택해주세요")
            .setView(dialogView)
            .setNegativeButton("취소") { _, _ -> monthPickerDialog = null }
            .create()
        monthPickerDialog?.show()
    }

    private fun saveToMonth(monthStr: String) {
        val baseFolder = File(filesDir, "docs_manage")
        val folder = File(File(File(baseFolder, category), subItem), monthStr)
        if (!folder.exists()) folder.mkdirs()
        currentFolder = folder
        openFilePicker()
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "application/pdf", "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            ))
        }
        filePickerLauncher.launch(intent)
    }

    private fun copyFileToInternal(uri: Uri) {
        val fileName = getFileName(uri)
        val folder = currentFolder ?: return
        val destFile = File(folder, fileName)
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output -> input.copyTo(output) }
        }
        if (currentViewFolder != null) {
            loadFilesInFolder(currentViewFolder!!)
        } else {
            loadFiles()
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "file_${System.currentTimeMillis()}"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) name = cursor.getString(nameIndex)
        }
        return name
    }

    private fun showFileOptions(file: File) {
        val options = arrayOf("공유하기", "지난서류로 이동", "중요 보관함으로 이동", "휴지통으로 이동")
        AlertDialog.Builder(this)
            .setTitle(file.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> shareFile(file)
                    1 -> showMoveToArchiveMonthPicker(file)
                    2 -> moveToImportant(file)
                    3 -> moveToTrash(file)
                }
            }
            .setNegativeButton("취소", null).show()
    }

    private fun showMoveToArchiveMonthPicker(file: File) {
        val recentMonths = mutableListOf<String>()
        for (i in -1..4) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -i + 1)
            recentMonths.add("${cal.get(Calendar.YEAR)}년 ${cal.get(Calendar.MONTH) + 1}월")
        }
        val oldMonths = mutableListOf<String>()
        for (i in 6..36) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -i + 1)
            oldMonths.add("${cal.get(Calendar.YEAR)}년 ${cal.get(Calendar.MONTH) + 1}월")
        }
        val dialogView = layoutInflater.inflate(R.layout.dialog_month_picker, null)
        val containerRecent = dialogView.findViewById<LinearLayout>(R.id.containerRecent)
        val containerOld = dialogView.findViewById<LinearLayout>(R.id.containerOld)
        val btnToggle = dialogView.findViewById<Button>(R.id.btnToggle)
        val scrollOld = dialogView.findViewById<ScrollView>(R.id.scrollOld)
        val archiveDialog = AlertDialog.Builder(this)
            .setTitle("지난서류 보관함 - 월 선택")
            .setView(dialogView)
            .setNegativeButton("취소", null)
            .create()
        recentMonths.forEach { month ->
            val btn = Button(this).apply {
                text = month; textSize = 15f
                setTextColor(android.graphics.Color.WHITE)
                backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1E3A8A"))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.setMargins(0,0,0,16) }
            }
            btn.setOnClickListener { moveToArchive(file, month); archiveDialog.dismiss() }
            containerRecent.addView(btn)
        }
        scrollOld.visibility = View.GONE
        oldMonths.forEach { month ->
            val btn = Button(this).apply {
                text = month; textSize = 15f
                setTextColor(android.graphics.Color.WHITE)
                backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1E3A8A"))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.setMargins(0,0,0,16) }
            }
            btn.setOnClickListener { moveToArchive(file, month); archiveDialog.dismiss() }
            containerOld.addView(btn)
        }
        var isOldVisible = false
        btnToggle.setOnClickListener {
            isOldVisible = !isOldVisible
            scrollOld.visibility = if (isOldVisible) View.VISIBLE else View.GONE
            btnToggle.text = if (isOldVisible) "▲ 이전 기록 닫기" else "▶ 이전 기록 보기"
        }
        archiveDialog.show()
    }

    private fun moveToArchive(file: File, monthStr: String) {
        try {
            val baseFolder = File(filesDir, "docs_manage")
            val archiveFolder = File(File(File(baseFolder, "archive"), category), "$subItem/$monthStr")
            if (!archiveFolder.exists()) archiveFolder.mkdirs()
            file.copyTo(File(archiveFolder, file.name), overwrite = true)
            file.delete()
            Toast.makeText(this, "지난서류 보관함으로 이동했어요!", Toast.LENGTH_SHORT).show()
            if (currentViewFolder != null) {
                loadFilesInFolder(currentViewFolder!!)
            } else {
                loadFiles()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "이동 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun moveToImportant(file: File) {
        try {
            val importantFolder = File(File(File(filesDir, "docs_manage"), "important"), category)
            if (!importantFolder.exists()) importantFolder.mkdirs()
            file.copyTo(File(importantFolder, file.name), overwrite = true)
            file.delete()
            Toast.makeText(this, "중요 보관함으로 이동했어요!", Toast.LENGTH_SHORT).show()
            if (currentViewFolder != null) {
                loadFilesInFolder(currentViewFolder!!)
            } else {
                loadFiles()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "이동 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun moveToTrash(file: File) {
        try {
            val trashFolder = File(File(File(filesDir, "docs_manage"), "trash"), category)
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
            val mimeType = getMimeType(file.name)
            val fileName = file.name
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { output ->
                        file.inputStream().use { input -> input.copyTo(output) }
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                    contentResolver.update(uri, contentValues, null, null)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = mimeType
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, fileName)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(shareIntent, "파일 공유"))
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val destFile = File(downloadsDir, fileName)
                file.copyTo(destFile, overwrite = true)
                val uri = FileProvider.getUriForFile(this, "${packageName}.provider", destFile)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "파일 공유"))
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
