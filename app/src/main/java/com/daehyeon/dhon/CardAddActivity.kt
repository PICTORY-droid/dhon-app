package com.daehyeon.dhon

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CardAddActivity : AppCompatActivity() {

    private lateinit var imgCard: ImageView
    private lateinit var etName: EditText
    private lateinit var etCompany: EditText
    private lateinit var etPhone: EditText
    private lateinit var etEmail: EditText
    private lateinit var etMemo: EditText

    private var category = ""
    private var cardPhotoFile: File? = null

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cardPhotoFile?.let { file ->
                showCardPhoto(file)
                runOCR(file)
            }
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val file = saveCardFromUri(it)
            file?.let { f ->
                cardPhotoFile = f
                showCardPhoto(f)
                runOCR(f)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_add)

        category = intent.getStringExtra("category") ?: ""

        imgCard = findViewById(R.id.imgCard)
        etName = findViewById(R.id.etName)
        etCompany = findViewById(R.id.etCompany)
        etPhone = findViewById(R.id.etPhone)
        etEmail = findViewById(R.id.etEmail)
        etMemo = findViewById(R.id.etMemo)

        // 홈으로 가기
        findViewById<LinearLayout>(R.id.btnHome).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        // 사진 촬영/선택 버튼
        findViewById<LinearLayout>(R.id.btnTakePhoto).setOnClickListener {
            showPhotoSourceDialog()
        }

        // 저장 버튼
        findViewById<LinearLayout>(R.id.btnSave).setOnClickListener {
            saveCard()
        }
    }

    private fun showPhotoSourceDialog() {
        val options = arrayOf("카메라로 촬영", "갤러리에서 선택")
        AlertDialog.Builder(this)
            .setTitle("명함 사진")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermission()
                    1 -> galleryLauncher.launch("image/*")
                }
            }
            .setNegativeButton("취소", null)
            .show()
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
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA).format(Date())
        val folder = File(filesDir, "business_cards_temp")
        if (!folder.exists()) folder.mkdirs()
        val file = File(folder, "CARD_${dateStr}.jpg")
        cardPhotoFile = file
        val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
        cameraLauncher.launch(uri)
    }

    private fun saveCardFromUri(uri: Uri): File? {
        return try {
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA).format(Date())
            val folder = File(filesDir, "business_cards_temp")
            if (!folder.exists()) folder.mkdirs()
            val file = File(folder, "CARD_${dateStr}.jpg")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            }
            file
        } catch (e: Exception) { null }
    }

    private fun showCardPhoto(file: File) {
        val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
        imgCard.setImageBitmap(bitmap)
    }

    private fun runOCR(file: File) {
        Toast.makeText(this, "전화번호/이메일 인식 중...", Toast.LENGTH_SHORT).show()
        try {
            val image = InputImage.fromFilePath(this, Uri.fromFile(file))
            val recognizer = TextRecognition.getClient(
                KoreanTextRecognizerOptions.Builder().build()
            )
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    val lines = result.text.split("\n").filter { it.isNotBlank() }
                    for (line in lines) {
                        val trimmed = line.trim()
                        if (trimmed.matches(Regex(".*\\d{2,4}[-. ]\\d{3,4}[-. ]\\d{4}.*"))
                            && etPhone.text.isEmpty()) {
                            etPhone.setText(cleanPhoneNumber(trimmed))
                        }
                    }
                    for (line in lines) {
                        val trimmed = line.trim()
                        if (trimmed.contains("@") && etEmail.text.isEmpty()) {
                            etEmail.setText(trimmed.trim())
                        }
                    }
                    Toast.makeText(this,
                        "인식 완료! 이름과 회사명은 직접 입력해주세요",
                        Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "인식 실패 - 직접 입력해주세요", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Toast.makeText(this, "직접 입력해주세요", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cleanPhoneNumber(raw: String): String {
        var cleaned = raw
            .replace(Regex("(?i)mobile\\.?\\s*"), "")
            .replace(Regex("(?i)tel\\.?\\s*"), "")
            .replace(Regex("(?i)fax\\.?\\s*"), "")
            .replace(Regex("(?i)hp\\.?\\s*"), "")
            .replace(Regex("(?i)m\\.?\\s*"), "")
            .trim()
        cleaned = cleaned.replace(Regex("[^0-9+\\-]"), "")
        return cleaned
    }

    private fun saveCard() {
        val name = etName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "이름을 입력해주세요!", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA).format(Date())
            val cardFolder = File(
                File(File(filesDir, "business_cards"), category),
                "${name}_${dateStr}"
            )
            cardFolder.mkdirs()

            cardPhotoFile?.let { photo ->
                if (photo.exists()) {
                    photo.copyTo(File(cardFolder, "card.jpg"), overwrite = true)
                }
            }

            val infoFile = File(cardFolder, "info.txt")
            infoFile.writeText(
                "이름:${etName.text}\n" +
                        "회사:${etCompany.text}\n" +
                        "전화:${etPhone.text}\n" +
                        "이메일:${etEmail.text}\n" +
                        "메모:${etMemo.text}\n" +
                        "카테고리:$category"
            )

            Toast.makeText(this, "명함이 저장되었어요!", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}