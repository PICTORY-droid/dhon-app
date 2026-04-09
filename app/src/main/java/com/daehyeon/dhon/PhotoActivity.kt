package com.daehyeon.dhon

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PhotoActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var photoAdapter: PhotoAdapter
    private lateinit var tvTitle: TextView
    private lateinit var tvSortIcon: TextView
    private val photoList = mutableListOf<File>()
    private var currentPhotoFile: File? = null
    private var selectedTag = "기타"
    private val folderName = "site_photos"

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
                // 1장이면 태그 선택 가능
                savePhotoFromUri(finalUris[0], System.currentTimeMillis())
                    ?.let { showTagDialog(it) }
            } else if (finalUris.size > 1) {
                // 여러 장이면 태그 없이 자동저장 (파일명 겹침 방지: 인덱스 추가)
                var successCount = 0
                finalUris.forEachIndexed { index, uri ->
                    val uniqueTime = System.currentTimeMillis() + index
                    savePhotoFromUri(uri, uniqueTime)?.let { file ->
                        savePhotoWithTag(file, "기타", "")
                        successCount++
                    }
                }
                Toast.makeText(this, "${successCount}장 추가 완료!", Toast.LENGTH_SHORT).show()
                loadPhotos()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo)

        tvTitle = findViewById(R.id.tvTitle)
        tvSortIcon = findViewById(R.id.tvSortIcon)
        recyclerView = findViewById(R.id.recyclerPhotos)
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        photoAdapter = PhotoAdapter(
            photoList,
            onClick = { file -> openPhotoDetail(file) },
            onLongClick = { file -> showPhotoOptions(file) }
        )
        recyclerView.adapter = photoAdapter

        checkAndArchiveOldPhotos()
        checkAndDeleteOldTrash()

        findViewById<LinearLayout>(R.id.btnHome).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        findViewById<LinearLayout>(R.id.btnArchive).setOnClickListener {
            showArchiveDialog()
        }

        findViewById<LinearLayout>(R.id.btnTrash).setOnClickListener {
            val intent = Intent(this, TrashActivity::class.java)
            intent.putExtra("folderName", folderName)
            startActivity(intent)
        }

        findViewById<LinearLayout>(R.id.btnAddPhoto).setOnClickListener {
            showPhotoSourceDialog()
        }

        findViewById<LinearLayout>(R.id.btnSort).setOnClickListener {
            photoAdapter.isDescending = !photoAdapter.isDescending
            tvSortIcon.text = if (photoAdapter.isDescending) "↓" else "↑"
            photoAdapter.sortPhotos()
        }
    }

    override fun onResume() {
        super.onResume()
        loadPhotos()
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
        val folder = File(File(filesDir, folderName), getCurrentMonthFolder())
        if (!folder.exists()) folder.mkdirs()
        return File(folder, "PHOTO_${dateStr}_임시.jpg")
    }

    private fun getCurrentMonthFolder(): String {
        return SimpleDateFormat("yyyy년 MM월", Locale.KOREA).format(Date())
    }

    // uniqueTime 파라미터로 파일명 겹침 방지!
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
            loadPhotos()
        } catch (e: Exception) {
            Toast.makeText(this, "저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPhotoOptions(file: File) {
        val options = arrayOf("공유하기", "지난사진으로 이동", "휴지통으로 이동")
        AlertDialog.Builder(this)
            .setTitle(file.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> sharePhoto(file)
                    1 -> showMoveToArchiveMonthPicker(file)
                    2 -> moveToTrash(file)
                }
            }
            .setNegativeButton("취소", null)
            .show()
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
        val scrollOld = dialogView.findViewById<android.widget.ScrollView>(R.id.scrollOld)
        val archiveDialog = AlertDialog.Builder(this)
            .setTitle("지난사진 보관함 - 월 선택")
            .setView(dialogView)
            .setNegativeButton("취소", null)
            .create()
        recentMonths.forEach { month ->
            val btn = Button(this).apply {
                text = month; textSize = 15f
                setTextColor(android.graphics.Color.WHITE)
                backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#1E3A8A"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(0, 0, 0, 16) }
            }
            btn.setOnClickListener { moveToArchive(file, month); archiveDialog.dismiss() }
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
                ).also { it.setMargins(0, 0, 0, 16) }
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
            val archiveFolder = File(File(File(filesDir, folderName), "archive"), monthStr)
            if (!archiveFolder.exists()) archiveFolder.mkdirs()
            file.copyTo(File(archiveFolder, file.name), overwrite = true)
            file.delete()
            Toast.makeText(this, "지난사진 보관함으로 이동했어요!", Toast.LENGTH_SHORT).show()
            loadPhotos()
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
            loadPhotos()
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

    private fun loadPhotos() {
        val baseFolder = File(filesDir, folderName)
        photoList.clear()
        if (baseFolder.exists()) {
            baseFolder.walkTopDown()
                .filter { it.isFile && it.name.endsWith(".jpg") }
                .filter { !it.absolutePath.contains("trash") }
                .filter { !it.absolutePath.contains("archive") }
                .sortedByDescending { it.lastModified() }
                .let { photoList.addAll(it) }
        }
        tvTitle.text = "현장 사진 (${photoList.size}장)"
        photoAdapter.notifyDataSetChanged()
    }

    private fun showArchiveDialog() {
        val archiveFolder = File(File(filesDir, folderName), "archive")
        if (!archiveFolder.exists() || archiveFolder.listFiles().isNullOrEmpty()) {
            Toast.makeText(this, "지난사진 보관함이 비어있어요", Toast.LENGTH_SHORT).show()
            return
        }
        val years = archiveFolder.listFiles()
            ?.filter { it.isDirectory }?.map { it.name }
            ?.sortedDescending()?.toTypedArray() ?: return
        AlertDialog.Builder(this)
            .setTitle("지난사진 보관함")
            .setItems(years) { _, which -> showArchiveMonthDialog(years[which]) }
            .setNegativeButton("닫기", null).show()
    }

    private fun showArchiveMonthDialog(year: String) {
        val archiveFolder = File(File(File(filesDir, folderName), "archive"), year)
        val months = archiveFolder.listFiles()
            ?.filter { it.isDirectory }?.map { it.name }
            ?.sortedDescending()?.toTypedArray() ?: return
        AlertDialog.Builder(this)
            .setTitle("$year 지난사진")
            .setItems(months) { _, which -> loadArchivePhotos(year, months[which]) }
            .setNegativeButton("뒤로", null).show()
    }

    private fun loadArchivePhotos(year: String, month: String) {
        val folder = File(File(File(File(filesDir, folderName), "archive"), year), month)
        photoList.clear()
        if (folder.exists()) {
            folder.walkTopDown()
                .filter { it.isFile && it.name.endsWith(".jpg") }
                .sortedByDescending { it.lastModified() }
                .let { photoList.addAll(it) }
        }
        tvTitle.text = "$year $month 보관함 (${photoList.size}장)"
        photoAdapter.notifyDataSetChanged()
    }

    private fun checkAndArchiveOldPhotos() {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val baseFolder = File(filesDir, folderName)
        if (!baseFolder.exists()) return
        baseFolder.listFiles()
            ?.filter { it.isDirectory && it.name.contains("년") }
            ?.forEach { monthFolder ->
                try {
                    val parts = monthFolder.name.replace("년 ", "-").replace("월", "").split("-")
                    val year = parts[0].trim().toInt()
                    val month = parts[1].trim().toInt()
                    if (year < currentYear || (year == currentYear && month < currentMonth)) {
                        val archiveFolder = File(
                            File(File(baseFolder, "archive"), "${year}년"), "${month}월"
                        )
                        if (!archiveFolder.exists()) {
                            archiveFolder.mkdirs()
                            monthFolder.listFiles()?.forEach { file ->
                                file.copyTo(File(archiveFolder, file.name), overwrite = true)
                                file.delete()
                            }
                            monthFolder.delete()
                        }
                    }
                } catch (e: Exception) { }
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