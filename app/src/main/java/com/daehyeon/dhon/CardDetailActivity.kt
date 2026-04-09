package com.daehyeon.dhon

import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class CardDetailActivity : AppCompatActivity() {

    private lateinit var imgCard: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvCompany: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvMemo: TextView

    private var cardPath = ""
    private var name = ""
    private var company = ""
    private var phone = ""
    private var email = ""
    private var memo = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_detail)

        cardPath = intent.getStringExtra("cardPath") ?: ""

        imgCard = findViewById(R.id.imgCard)
        tvName = findViewById(R.id.tvName)
        tvCompany = findViewById(R.id.tvCompany)
        tvPhone = findViewById(R.id.tvPhone)
        tvEmail = findViewById(R.id.tvEmail)
        tvMemo = findViewById(R.id.tvMemo)

        loadCard()

        // 홈으로 가기
        findViewById<LinearLayout>(R.id.btnHome).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        // 전화걸기
        findViewById<LinearLayout>(R.id.btnCall).setOnClickListener {
            val cleanPhone = cleanPhoneNumber(phone)
            if (cleanPhone.isEmpty()) {
                Toast.makeText(this, "전화번호가 없어요!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$cleanPhone")
            }
            startActivity(intent)
        }

        // 문자보내기
        findViewById<LinearLayout>(R.id.btnSms).setOnClickListener {
            val cleanPhone = cleanPhoneNumber(phone)
            if (cleanPhone.isEmpty()) {
                Toast.makeText(this, "전화번호가 없어요!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$cleanPhone")
            }
            startActivity(intent)
        }

        // 카카오톡
        findViewById<LinearLayout>(R.id.btnKakao).setOnClickListener {
            openKakaoTalk()
        }

        // 이메일
        findViewById<LinearLayout>(R.id.btnEmail).setOnClickListener {
            showEmailDialog()
        }

        // 삭제
        findViewById<LinearLayout>(R.id.btnDelete).setOnClickListener {
            showDeleteDialog()
        }
    }

    private fun loadCard() {
        val cardFolder = File(cardPath)
        if (!cardFolder.exists()) { finish(); return }

        val photoFile = File(cardFolder, "card.jpg")
        if (photoFile.exists()) {
            imgCard.setImageBitmap(BitmapFactory.decodeFile(photoFile.absolutePath))
        }

        val infoFile = File(cardFolder, "info.txt")
        if (infoFile.exists()) {
            for (line in infoFile.readLines()) {
                when {
                    line.startsWith("이름:") -> name = line.replace("이름:", "").trim()
                    line.startsWith("회사:") -> company = line.replace("회사:", "").trim()
                    line.startsWith("전화:") -> phone = line.replace("전화:", "").trim()
                    line.startsWith("이메일:") -> email = line.replace("이메일:", "").trim()
                    line.startsWith("메모:") -> memo = line.replace("메모:", "").trim()
                }
            }
        }

        tvName.text = name
        tvCompany.text = company
        tvPhone.text = if (phone.isEmpty()) "전화번호 없음" else "📞  $phone"
        tvEmail.text = if (email.isEmpty()) "이메일 없음" else "✉️  $email"
        tvMemo.text = if (memo.isEmpty()) "메모 없음" else memo
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

    private fun openKakaoTalk() {
        try {
            val intent = packageManager.getLaunchIntentForPackage("com.kakao.talk")
            if (intent != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "카카오톡이 설치되어 있지 않아요!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "카카오톡 실행 실패", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEmailDialog() {
        if (email.isEmpty()) {
            Toast.makeText(this, "이메일 주소가 없어요!", Toast.LENGTH_SHORT).show()
            return
        }
        val options = arrayOf("일반 이메일 발송", "파일 첨부해서 발송")
        AlertDialog.Builder(this)
            .setTitle("이메일 발송")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> sendEmail()
                    1 -> sendEmailWithAttachment()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun sendEmail() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, "다현건설 현장 관련")
        }
        startActivity(Intent.createChooser(intent, "이메일 앱 선택"))
    }

    private fun sendEmailWithAttachment() {
        val pickIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(Intent.createChooser(pickIntent, "첨부할 파일 선택"), 101)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data?.data != null) {
            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                putExtra(Intent.EXTRA_SUBJECT, "다현건설 현장 관련")
                putExtra(Intent.EXTRA_STREAM, data.data)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(emailIntent, "이메일 앱 선택"))
        }
    }

    private fun showDeleteDialog() {
        AlertDialog.Builder(this)
            .setTitle("명함 삭제")
            .setMessage("${name} 명함을 삭제할까요?")
            .setPositiveButton("삭제") { _, _ ->
                File(cardPath).deleteRecursively()
                Toast.makeText(this, "명함이 삭제되었어요!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton("취소", null)
            .show()
    }
}