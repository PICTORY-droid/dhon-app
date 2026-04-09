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

    private var isOldFoldersExpanded = false
    private val allMonthFolders = mutableListOf<File>()

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
                when {
                    file.name.startsWith("▶") || file.name.startsWith("▲") -> {
                        isOldFoldersExpanded = !isOldFoldersExpanded
                        applyToggleToFileList()
                    }
                    file.isDirectory -> {
                        currentViewFolder = file
                        loadFilesInFolder(file)
                    }
                    else -> openFile(file)
                }
            },
            onLongClick = { file ->
                if (!file.name.startsWith("▶") && !file.name.startsWith("▲")) {
                    if (file.isDirectory) showFolderOptions(file)
                    else showFileOptions(file)
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
            val restoreBasePath = File(
                File(File(filesDir, "docs_manage"), category), subItem
            ).absolutePath
            val intent = Intent(this, ArchiveActivity::class.java)
            intent.putExtra("folderName", "docs_manage")
            intent.putExtra("archivePath", archivePath)
            intent.putExtra("restorePath", restoreBasePath)
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

        // ✅ 순서 중요: 삭제 → 생성(3개만) → 로드
        deleteOldEmptyMonthFolders()
        createMonthFoldersIfNotExist()
        loadFiles()
    }

    override fun onResume() {
        super.onResume()
        if (currentViewFolder != null) loadFilesInFolder(currentViewFolder!!)
        else loadFiles()
    }

    override fun onBackPressed() {
        if (currentViewFolder != null) {
            currentViewFolder = null
            loadFiles()
        } else {
            super.onBackPressed()
        }
    }

    // ✅ 핵심: 현재월 포함 앞으로 2개월만 생성 (4월이면 4,5,6월만)
    // SharedPreferences로 월이 바뀔 때만 실행
    private fun createMonthFoldersIfNotExist() {
        val prefsKey = "docs_file_prefs_${category}_${subItem}"
        val prefs = getSharedPreferences(prefsKey, MODE_PRIVATE)
        val now = Calendar.getInstance()
        val currentYear = now.get(Calendar.YEAR)
        val currentMonth = now.get(Calendar.MONTH) + 1
        val currentTotal = currentYear * 12 + currentMonth
        val lastCreatedTotal = prefs.getInt("last_created_total", -1)
        val baseFolder = File(filesDir, "docs_manage")
        val subItemFolder = File(File(baseFolder, category), subItem)

        // 이번 달에 이미 생성했어도 폴더 존재 확인 후 없으면 재생성
        for (offset in 0..2) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, offset)
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH) + 1
            val monthFolder = File(subItemFolder, "${y}년 ${m}월")
            if (!monthFolder.exists()) monthFolder.mkdirs()
        }

        // 월이 바뀐 경우에만 기록 갱신
        if (lastCreatedTotal != currentTotal) {
            prefs.edit().putInt("last_created_total", currentTotal).apply()
        }
    }

    // ✅ 2025년 12월 이전 빈 폴더 삭제 (파일 있는 폴더는 보호)
    // 2026년 1,2,3월은 유지 (토글 안에 표시)
    private fun deleteOldEmptyMonthFolders() {
        val baseFolder = File(filesDir, "docs_manage")
        val subItemFolder = File(File(baseFolder, category), subItem)
        if (!subItemFolder.exists()) return

        val now = Calendar.getInstance()
        val currentYear = now.get(Calendar.YEAR)
        // 2026년 1월(currentYear * 12 + 1) 이전은 삭제 대상
        val keepFromTotal = currentYear * 12 + 1

        subItemFolder.listFiles()
            ?.filter { it.isDirectory && it.name.matches(Regex("\\d{4}년 \\d{1,2}월")) }
            ?.forEach { folder ->
                val regex = Regex("(\\d{4})년 (\\d{1,2})월")
                val match = regex.find(folder.name) ?: return@forEach
                val folderYear = match.groupValues[1].toInt()
                val folderMonth = match.groupValues[2].toInt()
                val folderTotal = folderYear * 12 + folderMonth
                // 2025년 12월 이전이고 파일 없으면 삭제
                if (folderTotal < keepFromTotal) {
                    val hasFiles = folder.walkTopDown().any { it.isFile }
                    if (!hasFiles) folder.delete()
                }
            }
    }

    // ✅ 4,5,6월 표시 / 나머지는 토글 숨김
    private fun shouldHideInToggle(folderName: String): Boolean {
        return try {
            val regex = Regex("(\\d{4})년 (\\d{1,2})월")
            val match = regex.find(folderName) ?: return false
            val folderYear = match.groupValues[1].toInt()
            val folderMonth = match.groupValues[2].toInt()
            val folderTotal = folderYear * 12 + folderMonth
            val now = Calendar.getInstance()
            val currentTotal = now.get(Calendar.YEAR) * 12 + (now.get(Calendar.MONTH) + 1)
            // currentTotal = 4월, currentTotal+1 = 5월, currentTotal+2 = 6월 → 표시
            // 나머지(1,2,3월 등) → 토글 숨김
            folderTotal < currentTotal || folderTotal > currentTotal + 2
        } catch (e: Exception) {
            false
        }
    }

    private fun applyToggleToFileList() {
        fileList.clear()
        val visibleFolders = allMonthFolders.filter { !shouldHideInToggle(it.name) }
        val hiddenFolders = allMonthFolders.filter { shouldHideInToggle(it.name) }
        fileList.addAll(visibleFolders)
        if (hiddenFolders.isNotEmpty()) {
            val toggleLabel = if (isOldFoldersExpanded)
                "▲ 이전 기록 닫기 (${hiddenFolders.size}개)"
            else
                "▶ 이전 기록 보기 (${hiddenFolders.size}개)"
            val subItemFolder = File(File(File(filesDir, "docs_manage"), category), subItem)
            val toggleFile = File(subItemFolder, toggleLabel)
            fileList.add(toggleFile)
            if (isOldFoldersExpanded) {
                fileList.addAll(hiddenFolders)
            }
        }
        fileAdapter.notifyDataSetChanged()
    }

    private fun loadFiles() {
        val baseFolder = File(filesDir, "docs_manage")
        val folder = File(File(baseFolder, category), subItem)
        allMonthFolders.clear()
        if (folder.exists()) {
            val subFolders = folder.listFiles()
                ?.filter { it.isDirectory && it.name.matches(Regex("\\d{4}년 \\d{1,2}월")) }
                ?.sortedByDescending { it.name }
                ?: emptyList()
            allMonthFolders.addAll(subFolders)
        }
        currentViewFolder = null
        val totalFileCount = countAllFiles(folder)
        tvTitle.text = subItem
        tvFileCount.text = "${totalFileCount}개"
        applyToggleToFileList()
    }

    private fun loadFilesInFolder(folder: File) {
        fileList.clear()
        if (folder.exists()) {
            folder.walkTopDown()
                .filter { it.isFile }
                .sortedByDescending { it.lastModified() }
                .forEach { fileList.add(it) }
        }
        tvTitle.text = buildTitlePath(folder)
        tvFileCount.text = "${fileList.size}개"
        fileAdapter.notifyDataSetChanged()
    }

    private fun buildTitlePath(folder: File): String {
        val parts = mutableListOf<String>()
        var f: File? = folder
        while (f != null && f.name != subItem) {
            parts.add(0, f.name)
            f = f.parentFile
        }
        return if (parts.isEmpty()) subItem else "$subItem > ${parts.joinToString(" > ")}"
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
                    1 -> moveFolderToArchive(folder)
                    2 -> moveFolderToTrash(folder)
                }
            }
            .setNegativeButton("취소", null).show()
    }

    private fun moveFolderToImportant(folder: File) {
        try {
            val importantFolder = File(
                File(filesDir, "docs_manage"),
                "important/$category/$subItem/${folder.name}"
            )
            if (!importantFolder.exists()) importantFolder.mkdirs()
            folder.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val relativePath = file.relativeTo(folder).path
                    val destFile = File(importantFolder, relativePath)
                    destFile.parentFile?.mkdirs()
                    file.copyTo(destFile, overwrite = true)
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

    private fun moveFolderToArchive(folder: File) {
        try {
            val folderMonthName = folder.name
            val archiveFolder = File(
                File(filesDir, "docs_manage"),
                "archive/$category/$subItem/$folderMonthName"
            )
            if (!archiveFolder.exists()) archiveFolder.mkdirs()
            folder.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val relativePath = file.relativeTo(folder).path
                    val destFile = File(archiveFolder, relativePath)
                    destFile.parentFile?.mkdirs()
                    file.copyTo(destFile, overwrite = true)
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
            val folderMonthName = folder.name
            val trashFolder = File(
                File(filesDir, "docs_manage"),
                "trash/$category/$subItem/$folderMonthName"
            )
            if (!trashFolder.exists()) trashFolder.mkdirs()
            folder.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val relativePath = file.relativeTo(folder).path
                    val destFile = File(trashFolder, relativePath)
                    destFile.parentFile?.mkdirs()
                    file.copyTo(destFile, overwrite = true)
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

    private fun showFileOptions(file: File) {
        val options = arrayOf("공유하기", "지난서류로 이동", "중요 보관함으로 이동", "휴지통으로 이동")
        AlertDialog.Builder(this)
            .setTitle(file.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> shareFile(file)
                    1 -> moveToArchive(file)
                    2 -> moveToImportant(file)
                    3 -> moveToTrash(file)
                }
            }
            .setNegativeButton("취소", null).show()
    }

    private fun moveToArchive(file: File) {
        try {
            val monthFolderName = file.parentFile?.name ?: ""
            val archiveFolder = if (monthFolderName.matches(Regex("\\d{4}년 \\d{1,2}월"))) {
                File(
                    File(filesDir, "docs_manage"),
                    "archive/$category/$subItem/$monthFolderName"
                )
            } else {
                File(
                    File(filesDir, "docs_manage"),
                    "archive/$category/$subItem"
                )
            }
            if (!archiveFolder.exists()) archiveFolder.mkdirs()
            file.copyTo(File(archiveFolder, file.name), overwrite = true)
            file.delete()
            Toast.makeText(this, "지난서류 보관함으로 이동했어요!", Toast.LENGTH_SHORT).show()
            if (currentViewFolder != null) loadFilesInFolder(currentViewFolder!!)
            else loadFiles()
        } catch (e: Exception) {
            Toast.makeText(this, "이동 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun moveToImportant(file: File) {
        try {
            val importantFolder = File(
                File(filesDir, "docs_manage"),
                "important/$category/$subItem"
            )
            if (!importantFolder.exists()) importantFolder.mkdirs()
            file.copyTo(File(importantFolder, file.name), overwrite = true)
            file.delete()
            Toast.makeText(this, "중요 보관함으로 이동했어요!", Toast.LENGTH_SHORT).show()
            if (currentViewFolder != null) loadFilesInFolder(currentViewFolder!!)
            else loadFiles()
        } catch (e: Exception) {
            Toast.makeText(this, "이동 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun moveToTrash(file: File) {
        try {
            val monthFolderName = file.parentFile?.name ?: ""
            val trashFolder = if (monthFolderName.matches(Regex("\\d{4}년 \\d{1,2}월"))) {
                File(
                    File(filesDir, "docs_manage"),
                    "trash/$category/$subItem/$monthFolderName"
                )
            } else {
                File(
                    File(filesDir, "docs_manage"),
                    "trash/$category/$subItem"
                )
            }
            if (!trashFolder.exists()) trashFolder.mkdirs()
            file.copyTo(File(trashFolder, file.name), overwrite = true)
            file.delete()
            Toast.makeText(this, "휴지통으로 이동했어요!", Toast.LENGTH_SHORT).show()
            if (currentViewFolder != null) loadFilesInFolder(currentViewFolder!!)
            else loadFiles()
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
                setTextColor(android.graphics.Color.parseColor("#374151"))
                backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#F3F4F6"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 16) }
            }
            btn.setOnClickListener {
                monthPickerDialog?.dismiss()
                saveToMonth(month)
            }
            containerRecent.addView(btn)
        }
        scrollOld.visibility = View.GONE
        oldMonths.forEach { month ->
            val btn = Button(this).apply {
                text = month; textSize = 15f
                setTextColor(android.graphics.Color.parseColor("#374151"))
                backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#F3F4F6"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 16) }
            }
            btn.setOnClickListener {
                monthPickerDialog?.dismiss()
                saveToMonth(month)
            }
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
        val monthFolder = File(
            File(filesDir, "docs_manage"),
            "$category/$subItem/$monthStr"
        )
        if (!monthFolder.exists()) monthFolder.mkdirs()
        currentFolder = monthFolder
        openFilePicker()
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "application/pdf",
                "application/msword",
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
        if (currentViewFolder != null) loadFilesInFolder(currentViewFolder!!)
        else loadFiles()
    }

    private fun getFileName(uri: Uri): String {
        var name = "file_${System.currentTimeMillis()}"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) name = cursor.getString(nameIndex)
        }
        return name
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
                val uri = contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
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
                val downloadsDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val destFile = File(downloadsDir, fileName)
                file.copyTo(destFile, overwrite = true)
                val uri = FileProvider.getUriForFile(
                    this, "${packageName}.provider", destFile)
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
            fileName.endsWith(".docx", ignoreCase = true) ->
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            fileName.endsWith(".xls", ignoreCase = true) -> "application/vnd.ms-excel"
            fileName.endsWith(".xlsx", ignoreCase = true) ->
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            else -> "application/octet-stream"
        }
    }
}
