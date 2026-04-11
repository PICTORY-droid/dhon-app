package com.daehyeon.dhon

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PhotoActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fileAdapter: FileAdapter
    private lateinit var tvTitle: TextView
    private lateinit var tvSortIcon: TextView
    private lateinit var tvPhotoCount: TextView
    private lateinit var tvViewToggleIcon: TextView
    private val photoList = mutableListOf<File>()
    private var currentPhotoFile: File? = null
    private var selectedTag = "기타"
    private val folderName = "site_photos"
    private var isGridView = false
    private var currentViewFolder: File? = null

    private var isOldFoldersExpanded = false
    private val allMonthFolders = mutableListOf<File>()

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentPhotoFile?.let { file -> showTagDialog(file) }
        } else {
            Toast.makeText(this, "촬영이 취소되었어요", Toast.LENGTH_SHORT).show()
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
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
                Toast.makeText(this, "20장 초과! 앞의 20장만 불러올게요.", Toast.LENGTH_SHORT).show()
                uris.take(20)
            } else {
                uris
            }
            if (finalUris.size == 1) {
                savePhotoFromUri(finalUris[0], System.currentTimeMillis())
                    ?.let { showTagDialog(it) }
            } else if (finalUris.size > 1) {
                var successCount = 0
                finalUris.forEachIndexed { index, uri ->
                    val uniqueTime = System.currentTimeMillis() + index
                    savePhotoFromUri(uri, uniqueTime)?.let { file ->
                        savePhotoWithTag(file, "기타", "")
                        successCount++
                    }
                }
                Toast.makeText(this, "${successCount}장 추가 완료!", Toast.LENGTH_SHORT).show()
                loadMonthFolders()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.navBarSpacer)) { view, insets ->
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val params = view.layoutParams
            params.height = navBarHeight
            view.layoutParams = params
            insets
        }

        tvTitle = findViewById(R.id.tvTitle)
        tvSortIcon = findViewById(R.id.tvSortIcon)
        tvPhotoCount = findViewById(R.id.tvPhotoCount)
        tvViewToggleIcon = findViewById(R.id.tvViewToggleIcon)
        recyclerView = findViewById(R.id.recyclerPhotos)
        recyclerView.layoutManager = LinearLayoutManager(this)

        tvViewToggleIcon.text = "⊞"

        fileAdapter = FileAdapter(
            photoList,
            onClick = { file ->
                when {
                    file.name.startsWith("▶") || file.name.startsWith("▲") -> {
                        isOldFoldersExpanded = !isOldFoldersExpanded
                        applyToggleToFileList()
                    }
                    file.isDirectory -> {
                        currentViewFolder = file
                        loadPhotosInFolder(file)
                    }
                    else -> openPhotoDetail(file)
                }
            },
            onLongClick = { file ->
                if (!file.name.startsWith("▶") && !file.name.startsWith("▲")) {
                    if (file.isDirectory) showFolderOptions(file)
                    else showPhotoOptions(file)
                }
            }
        )
        recyclerView.adapter = fileAdapter

        fixDuplicateMonthFolder()
        deleteOldEmptyMonthFolders()
        createMonthFoldersIfNotExist()
        loadMonthFolders()

        findViewById<LinearLayout>(R.id.btnViewToggle).setOnClickListener {
            isGridView = !isGridView
            updateViewMode()
        }

        findViewById<LinearLayout>(R.id.btnHome).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        findViewById<LinearLayout>(R.id.btnAddPhoto).setOnClickListener {
            showPhotoSourceDialog()
        }

        findViewById<LinearLayout>(R.id.btnImportant).setOnClickListener {
            val importantPath = File(File(filesDir, folderName), "important").absolutePath
            val restorePath = File(filesDir, folderName).absolutePath
            val intent = Intent(this, ImportantDocsActivity::class.java)
            intent.putExtra("folderName", folderName)
            intent.putExtra("importantPath", importantPath)
            intent.putExtra("restorePath", restorePath)
            startActivity(intent)
        }

        findViewById<LinearLayout>(R.id.btnArchive).setOnClickListener {
            val archivePath = File(File(filesDir, folderName), "archive").absolutePath
            val restorePath = File(filesDir, folderName).absolutePath
            val intent = Intent(this, ArchiveActivity::class.java)
            intent.putExtra("folderName", folderName)
            intent.putExtra("archivePath", archivePath)
            intent.putExtra("restorePath", restorePath)
            startActivity(intent)
        }

        findViewById<LinearLayout>(R.id.btnTrash).setOnClickListener {
            val trashPath = File(File(filesDir, folderName), "trash").absolutePath
            val restorePath = File(filesDir, folderName).absolutePath
            val intent = Intent(this, TrashActivity::class.java)
            intent.putExtra("folderName", folderName)
            intent.putExtra("trashPath", trashPath)
            intent.putExtra("restorePath", restorePath)
            startActivity(intent)
        }

        findViewById<LinearLayout>(R.id.btnSort).setOnClickListener {
            fileAdapter.isDescending = !fileAdapter.isDescending
            tvSortIcon.text = if (fileAdapter.isDescending) "↓" else "↑"
            fileAdapter.sortFiles()
        }
    }

    override fun onResume() {
        super.onResume()
        if (currentViewFolder != null) loadPhotosInFolder(currentViewFolder!!)
        else loadMonthFolders()
    }

    override fun onBackPressed() {
        if (currentViewFolder != null) {
            currentViewFolder = null
            loadMonthFolders()
        } else {
            super.onBackPressed()
        }
    }

    private fun updateViewMode() {
        if (isGridView) {
            recyclerView.layoutManager = GridLayoutManager(this, 3)
            tvViewToggleIcon.text = "☰"
            fileAdapter.isGridView = true
        } else {
            recyclerView.layoutManager = LinearLayoutManager(this)
            tvViewToggleIcon.text = "⊞"
            fileAdapter.isGridView = false
        }
        fileAdapter.notifyDataSetChanged()
    }

    private fun fixDuplicateMonthFolder() {
        val baseFolder = File(filesDir, folderName)
        if (!baseFolder.exists()) return
        baseFolder.listFiles()
            ?.filter { it.isDirectory && it.name.matches(Regex("\\d{4}년 \\d{2}월")) }
            ?.forEach { folder ->
                val regex = Regex("(\\d{4})년 (\\d{2})월")
                val match = regex.find(folder.name) ?: return@forEach
                val year = match.groupValues[1]
                val month = match.groupValues[2].toInt()
                val correctName = "${year}년 ${month}월"
                val correctFolder = File(baseFolder, correctName)
                if (folder.name != correctName) {
                    if (!correctFolder.exists()) correctFolder.mkdirs()
                    folder.walkTopDown().forEach { file ->
                        if (file.isFile) {
                            val relativePath = file.relativeTo(folder).path
                            val destFile = File(correctFolder, relativePath)
                            destFile.parentFile?.mkdirs()
                            file.copyTo(destFile, overwrite = true)
                        }
                    }
                    deleteFolderCompletely(folder)
                }
            }
    }

    private fun createMonthFoldersIfNotExist() {
        val prefs = getSharedPreferences("photo_prefs", MODE_PRIVATE)
        val now = Calendar.getInstance()
        val currentYear = now.get(Calendar.YEAR)
        val currentMonth = now.get(Calendar.MONTH) + 1
        val currentTotal = currentYear * 12 + currentMonth
        val baseFolder = File(filesDir, folderName)
        for (offset in -3..2) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, offset)
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH) + 1
            val monthFolder = File(baseFolder, "${y}년 ${m}월")
            if (!monthFolder.exists()) monthFolder.mkdirs()
        }
        if (prefs.getInt("last_created_total", -1) != currentTotal) {
            prefs.edit().putInt("last_created_total", currentTotal).apply()
        }
    }

    private fun deleteOldEmptyMonthFolders() {
        val baseFolder = File(filesDir, folderName)
        if (!baseFolder.exists()) return
        val now = Calendar.getInstance()
        val currentTotal = now.get(Calendar.YEAR) * 12 + (now.get(Calendar.MONTH) + 1)
        baseFolder.listFiles()
            ?.filter { it.isDirectory && it.name.matches(Regex("\\d{4}년 \\d{1,2}월")) }
            ?.forEach { folder ->
                val regex = Regex("(\\d{4})년 (\\d{1,2})월")
                val match = regex.find(folder.name) ?: return@forEach
                val folderYear = match.groupValues[1].toInt()
                val folderMonth = match.groupValues[2].toInt()
                val folderTotal = folderYear * 12 + folderMonth
                val monthsAgo = currentTotal - folderTotal
                if (monthsAgo > 3) {
                    val hasFiles = folder.walkTopDown().any { it.isFile }
                    if (!hasFiles) deleteFolderCompletely(folder)
                }
            }
    }

    private fun shouldHideInToggle(folderName: String): Boolean {
        return try {
            val regex = Regex("(\\d{4})년 (\\d{1,2})월")
            val match = regex.find(folderName) ?: return false
            val folderYear = match.groupValues[1].toInt()
            val folderMonth = match.groupValues[2].toInt()
            val folderTotal = folderYear * 12 + folderMonth
            val now = Calendar.getInstance()
            val currentTotal = now.get(Calendar.YEAR) * 12 + (now.get(Calendar.MONTH) + 1)
            folderTotal < currentTotal || folderTotal > currentTotal + 2
        } catch (e: Exception) {
            false
        }
    }

    private fun applyToggleToFileList() {
        photoList.clear()
        val visibleFolders = allMonthFolders.filter { !shouldHideInToggle(it.name) }
        val hiddenFolders = allMonthFolders.filter { shouldHideInToggle(it.name) }
        photoList.addAll(visibleFolders)
        if (hiddenFolders.isNotEmpty()) {
            val toggleLabel = if (isOldFoldersExpanded)
                "▲ 이전 기록 닫기 (${hiddenFolders.size}개)"
            else
                "▶ 이전 기록 보기 (${hiddenFolders.size}개)"
            val baseFolder = File(filesDir, folderName)
            val toggleFile = File(baseFolder, toggleLabel)
            photoList.add(toggleFile)
            if (isOldFoldersExpanded) photoList.addAll(hiddenFolders)
        }
        fileAdapter.notifyDataSetChanged()
    }

    private fun loadMonthFolders() {
        val baseFolder = File(filesDir, folderName)
        allMonthFolders.clear()
        if (baseFolder.exists()) {
            val folders = baseFolder.listFiles()
                ?.filter { it.isDirectory && it.name.matches(Regex("\\d{4}년 \\d{1,2}월")) }
                ?.sortedByDescending { it.name }
                ?: emptyList()
            allMonthFolders.addAll(folders)
        }
        currentViewFolder = null
        val totalPhotoCount = countAllFiles(File(filesDir, folderName))
        tvTitle.text = "현장 사진"
        tvPhotoCount.text = "현장 사진 (${totalPhotoCount}장)"
        applyToggleToFileList()
    }

    private fun loadPhotosInFolder(folder: File) {
        photoList.clear()
        if (folder.exists()) {
            folder.walkTopDown()
                .filter { it.isFile && it.name.endsWith(".jpg") }
                .sortedByDescending { it.lastModified() }
                .forEach { photoList.add(it) }
        }
        tvTitle.text = folder.name
        tvPhotoCount.text = "${folder.name} (${photoList.size}장)"
        fileAdapter.notifyDataSetChanged()
    }

    private fun countAllFiles(folder: File): Int {
        if (!folder.exists()) return 0
        return folder.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".jpg") }
            .filter { !it.absolutePath.contains("trash") }
            .filter { !it.absolutePath.contains("archive") }
            .filter { !it.absolutePath.contains("important") }
            .count()
    }

    private fun deleteFolderCompletely(folder: File): Boolean {
        if (!folder.exists()) return true
        try {
            folder.walkTopDown().filter { it.isFile }.forEach { it.delete() }
            folder.walkBottomUp()
                .filter { it.isDirectory && it.absolutePath != folder.absolutePath }
                .forEach { it.delete() }
            val result = folder.delete()
            if (!result && folder.exists()) {
                Runtime.getRuntime().exec("rm -rf ${folder.absolutePath}")
                Thread.sleep(100)
            }
            return !folder.exists()
        } catch (e: Exception) {
            return false
        }
    }

    private fun showFolderOptions(folder: File) {
        val options = arrayOf("중요 보관함으로 이동", "지난사진으로 이동", "휴지통으로 이동")
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
            val importantFolder = File(File(filesDir, folderName), "important/${folder.name}")
            importantFolder.mkdirs()
            folder.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val relativePath = file.relativeTo(folder).path
                    val destFile = File(importantFolder, relativePath)
                    destFile.parentFile?.mkdirs()
                    file.copyTo(destFile, overwrite = true)
                }
            }
            deleteFolderCompletely(folder)
            createMonthFoldersIfNotExist()
            Toast.makeText(this, "중요 보관함으로 이동했어요!", Toast.LENGTH_SHORT).show()
            loadMonthFolders()
        } catch (e: Exception) {
            Toast.makeText(this, "이동 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun moveFolderToArchive(folder: File) {
        try {
            val archiveFolder = File(File(filesDir, folderName), "archive/${folder.name}")
            archiveFolder.mkdirs()
            folder.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val relativePath = file.relativeTo(folder).path
                    val destFile = File(archiveFolder, relativePath)
                    destFile.parentFile?.mkdirs()
                    file.copyTo(destFile, overwrite = true)
                }
            }
            deleteFolderCompletely(folder)
            createMonthFoldersIfNotExist()
            Toast.makeText(this, "지난사진 보관함으로 이동했어요!", Toast.LENGTH_SHORT).show()
            loadMonthFolders()
        } catch (e: Exception) {
            Toast.makeText(this, "이동 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun moveFolderToTrash(folder: File) {
        try {
            val trashFolder = File(File(filesDir, folderName), "trash/${folder.name}")
            trashFolder.mkdirs()
            folder.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val relativePath = file.relativeTo(folder).path
                    val destFile = File(trashFolder, relativePath)
                    destFile.parentFile?.mkdirs()
                    file.copyTo(destFile, overwrite = true)
                }
            }
            deleteFolderCompletely(folder)
            createMonthFoldersIfNotExist()
            Toast.makeText(this, "휴지통으로 이동했어요!", Toast.LENGTH_SHORT).show()
            loadMonthFolders()
        } catch (e: Exception) {
            Toast.makeText(this, "이동 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openPhotoDetail(file: File) {
        val intent = Intent(this, PhotoDetailActivity::class.java)
        intent.putExtra("photoPath", file.absolutePath)
        intent.putExtra("folderName", folderName)
        startActivity(intent)
    }

    private fun showPhotoSourceDialog() {
        val options = arrayOf("카메라로 촬영", "갤러리에서 선택 (최대 20장)")
        AlertDialog.Builder(this)
            .setTitle("사진 추가")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermission()
                    1 -> openGalleryMultiple()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun openGalleryMultiple() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        galleryLauncher.launch(intent)
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
        } else {
            openCamera()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            Toast.makeText(this, "카메라 권한이 필요해요!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCamera() {
        val photoFile = createPhotoFile()
        currentPhotoFile = photoFile
        val uri = FileProvider.getUriForFile(this, "${packageName}.provider", photoFile)
        cameraLauncher.launch(uri)
    }

    private fun createPhotoFile(): File {
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA).format(Date())
        val now = Calendar.getInstance()
        val y = now.get(Calendar.YEAR)
        val m = now.get(Calendar.MONTH) + 1
        val folder = File(File(filesDir, folderName), "${y}년 ${m}월")
        if (!folder.exists()) folder.mkdirs()
        return File(folder, "PHOTO_${dateStr}_임시.jpg")
    }

    private fun getCurrentMonthFolder(): String {
        val now = Calendar.getInstance()
        val y = now.get(Calendar.YEAR)
        val m = now.get(Calendar.MONTH) + 1
        return "${y}년 ${m}월"
    }

    private fun savePhotoFromUri(uri: Uri, uniqueTime: Long = System.currentTimeMillis()): File? {
        return try {
            val folder = File(File(filesDir, folderName), getCurrentMonthFolder())
            if (!folder.exists()) folder.mkdirs()
            val file = File(folder, "PHOTO_${uniqueTime}_임시.jpg")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            }
            file
        } catch (e: Exception) { null }
    }

    private fun showTagDialog(photoFile: File) {
        val tags = arrayOf(
            "기초공사", "골조공사", "철근공사",
            "콘크리트타설", "방수공사", "마감공사",
            "안전점검", "장비작업", "기타"
        )
        val btnLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(24, 8, 24, 16)
        }
        val btnCancel = Button(this).apply {
            text = "취소"
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ).also { it.marginEnd = 8 }
            setBackgroundColor(android.graphics.Color.parseColor("#E2E8F0"))
            setTextColor(android.graphics.Color.parseColor("#475569"))
        }
        val btnSaveNoTag = Button(this).apply {
            text = "태그 없이 저장"
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ).also { it.marginStart = 8 }
            setBackgroundColor(android.graphics.Color.parseColor("#1E3A8A"))
            setTextColor(android.graphics.Color.WHITE)
        }
        btnLayout.addView(btnCancel)
        btnLayout.addView(btnSaveNoTag)

        val dialog = AlertDialog.Builder(this)
            .setTitle("공정 태그 선택")
            .setItems(tags) { _, which ->
                selectedTag = tags[which]
                showMemoDialog(photoFile, selectedTag)
            }
            .setView(btnLayout)
            .create()

        btnCancel.setOnClickListener {
            if (photoFile.exists()) photoFile.delete()
            dialog.dismiss()
        }
        btnSaveNoTag.setOnClickListener {
            savePhotoWithTag(photoFile, "기타", "")
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showMemoDialog(photoFile: File, tag: String) {
        val editText = android.widget.EditText(this).apply {
            hint = "메모 입력 (선택사항)"
            setTextColor(android.graphics.Color.BLACK)
            setHintTextColor(android.graphics.Color.GRAY)
            setPadding(32, 24, 32, 24)
        }
        AlertDialog.Builder(this)
            .setTitle("메모 입력")
            .setView(editText)
            .setPositiveButton("저장") { _, _ ->
                savePhotoWithTag(photoFile, tag, editText.text.toString())
            }
            .setNegativeButton("메모 없이 저장") { _, _ ->
                savePhotoWithTag(photoFile, tag, "")
            }
            .show()
    }

    private fun savePhotoWithTag(photoFile: File, tag: String, memo: String) {
        try {
            val uniqueTime = System.currentTimeMillis()
            val folder = photoFile.parentFile ?: return
            val newName = when {
                tag == "기타" && memo.isEmpty() -> "PHOTO_${uniqueTime}.jpg"
                memo.isNotEmpty() -> "PHOTO_${uniqueTime}_${tag}_${memo.trim().take(20)}.jpg"
                else -> "PHOTO_${uniqueTime}_${tag}.jpg"
            }
            val newFile = File(folder, newName)
            if (photoFile.exists()) photoFile.renameTo(newFile)
            loadMonthFolders()
        } catch (e: Exception) {
            Toast.makeText(this, "저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPhotoOptions(file: File) {
        val options = arrayOf("공유하기", "중요 보관함으로 이동", "지난사진으로 이동", "휴지통으로 이동")
        AlertDialog.Builder(this)
            .setTitle(file.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> sharePhoto(file)
                    1 -> moveToImportant(file)
                    2 -> moveToArchive(file)
                    3 -> moveToTrash(file)
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun moveToImportant(file: File) {
        try {
            val importantFolder = File(File(filesDir, folderName), "important")
            if (!importantFolder.exists()) importantFolder.mkdirs()
            file.copyTo(File(importantFolder, file.name), overwrite = true)
            file.delete()
            Toast.makeText(this, "중요 보관함으로 이동했어요!", Toast.LENGTH_SHORT).show()
            if (currentViewFolder != null) loadPhotosInFolder(currentViewFolder!!)
            else loadMonthFolders()
        } catch (e: Exception) {
            Toast.makeText(this, "이동 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun moveToArchive(file: File) {
        try {
            val monthFolderName = file.parentFile?.name ?: ""
            val archiveFolder = if (monthFolderName.matches(Regex("\\d{4}년 \\d{1,2}월"))) {
                File(File(filesDir, folderName), "archive/$monthFolderName")
            } else {
                File(File(filesDir, folderName), "archive")
            }
            if (!archiveFolder.exists()) archiveFolder.mkdirs()
            file.copyTo(File(archiveFolder, file.name), overwrite = true)
            file.delete()
            Toast.makeText(this, "지난사진 보관함으로 이동했어요!", Toast.LENGTH_SHORT).show()
            if (currentViewFolder != null) loadPhotosInFolder(currentViewFolder!!)
            else loadMonthFolders()
        } catch (e: Exception) {
            Toast.makeText(this, "이동 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun moveToTrash(file: File) {
        try {
            val trashFolder = File(File(filesDir, folderName), "trash")
            if (!trashFolder.exists()) trashFolder.mkdirs()
            file.copyTo(File(trashFolder, file.name), overwrite = true)
            file.delete()
            Toast.makeText(this, "휴지통으로 이동했어요!", Toast.LENGTH_SHORT).show()
            if (currentViewFolder != null) loadPhotosInFolder(currentViewFolder!!)
            else loadMonthFolders()
        } catch (e: Exception) {
            Toast.makeText(this, "이동 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sharePhoto(file: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "사진 공유"))
        } catch (e: Exception) {
            Toast.makeText(this, "공유 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndDeleteOldTrash() {
        val trashFolder = File(File(filesDir, folderName), "trash")
        if (!trashFolder.exists()) return
        val oneYearAgo = System.currentTimeMillis() - (365L * 24 * 60 * 60 * 1000)
        trashFolder.walkTopDown()
            .filter { it.isFile }
            .sortedBy { it.lastModified() }
            .filter { it.lastModified() < oneYearAgo }
            .forEach { it.delete() }
    }
}
