package com.daehyeon.dhon

import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import kotlin.math.sqrt

class PhotoDetailActivity : AppCompatActivity() {

    private lateinit var imgPhoto: ImageView
    private lateinit var tvFileName: TextView
    private lateinit var tvTag: TextView
    private lateinit var tvMemo: TextView
    private var photoPath = ""
    private var folderName = ""

    // 핀치줌 관련 변수
    private val matrix = Matrix()
    private var lastScale = 1f
    private var lastX = 0f
    private var lastY = 0f
    private var startDist = 0f
    private var isDragging = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_detail)

        photoPath = intent.getStringExtra("photoPath") ?: ""
        folderName = intent.getStringExtra("folderName") ?: ""

        imgPhoto = findViewById(R.id.imgPhoto)
        tvFileName = findViewById(R.id.tvFileName)
        tvTag = findViewById(R.id.tvTag)
        tvMemo = findViewById(R.id.tvMemo)

        imgPhoto.scaleType = ImageView.ScaleType.MATRIX

        loadPhoto()
        setupPinchZoom()

        // 공유하기 버튼
        findViewById<LinearLayout>(R.id.btnShare).setOnClickListener {
            sharePhoto()
        }

        // 휴지통으로 이동 버튼
        findViewById<LinearLayout>(R.id.btnTrash).setOnClickListener {
            moveToTrash()
        }
    }

    // 핀치줌 (두 손가락으로 확대/축소)
    private fun setupPinchZoom() {
        imgPhoto.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.x
                    lastY = event.y
                    isDragging = true
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (event.pointerCount == 2) {
                        startDist = getDistance(event)
                        isDragging = false
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (event.pointerCount == 2) {
                        // 핀치줌
                        val newDist = getDistance(event)
                        if (startDist > 0) {
                            val scale = newDist / startDist
                            val newScale = (lastScale * scale).coerceIn(0.5f, 5f)
                            matrix.reset()
                            matrix.postScale(newScale, newScale,
                                imgPhoto.width / 2f, imgPhoto.height / 2f)
                            imgPhoto.imageMatrix = matrix
                        }
                    } else if (isDragging && lastScale > 1f) {
                        // 드래그 이동 (확대된 상태에서만)
                        val dx = event.x - lastX
                        val dy = event.y - lastY
                        matrix.postTranslate(dx, dy)
                        imgPhoto.imageMatrix = matrix
                        lastX = event.x
                        lastY = event.y
                    }
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    if (event.pointerCount == 2) {
                        lastScale = getCurrentScale()
                        startDist = 0f
                        isDragging = true
                    }
                }
                MotionEvent.ACTION_UP -> {
                    isDragging = false
                }
            }
            true
        }
    }

    private fun getDistance(event: MotionEvent): Float {
        val dx = event.getX(0) - event.getX(1)
        val dy = event.getY(0) - event.getY(1)
        return sqrt(dx * dx + dy * dy)
    }

    private fun getCurrentScale(): Float {
        val values = FloatArray(9)
        matrix.getValues(values)
        return values[Matrix.MSCALE_X]
    }

    private fun loadPhoto() {
        val file = File(photoPath)
        if (!file.exists()) {
            Toast.makeText(this, "사진을 찾을 수 없어요", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val bitmap = BitmapFactory.decodeFile(photoPath)
        imgPhoto.setImageBitmap(bitmap)

        val fileName = file.name.replace(".jpg", "")
        val parts = fileName.split("_")

        tvFileName.text = file.name

        val tag = if (parts.size >= 3) parts[2] else "없음"
        val memo = if (parts.size >= 4) parts.subList(3, parts.size).joinToString("_") else "없음"

        tvTag.text = "공정: $tag"
        tvMemo.text = "메모: $memo"
    }

    private fun sharePhoto() {
        try {
            val file = File(photoPath)
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

    private fun moveToTrash() {
        AlertDialog.Builder(this)
            .setTitle("휴지통으로 이동")
            .setMessage("이 사진을 휴지통으로 이동할까요?")
            .setPositiveButton("이동") { _, _ ->
                try {
                    val file = File(photoPath)
                    val trashFolder = File(File(filesDir, folderName), "trash")
                    if (!trashFolder.exists()) trashFolder.mkdirs()
                    file.copyTo(File(trashFolder, file.name), overwrite = true)
                    file.delete()
                    Toast.makeText(this, "휴지통으로 이동했어요!", Toast.LENGTH_SHORT).show()
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this, "이동 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }
}