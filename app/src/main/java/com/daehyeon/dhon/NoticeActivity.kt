package com.daehyeon.dhon

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions

class NoticeActivity : AppCompatActivity() {

    private lateinit var etKorean: EditText
    private lateinit var tvVietnamese: TextView
    private lateinit var tvChinese: TextView
    private lateinit var btnTranslate: LinearLayout
    private lateinit var btnKakao: LinearLayout
    private lateinit var btnZalo: LinearLayout
    private lateinit var btnFacebook: LinearLayout
    private lateinit var btnWechat: LinearLayout
    private lateinit var btnSms: LinearLayout

    private var translatedVietnamese = ""
    private var translatedChinese = ""
    private var isModelReady = false

    private var koToViTranslator = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.KOREAN)
            .setTargetLanguage(TranslateLanguage.VIETNAMESE)
            .build()
    )
    private var koToZhTranslator = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.KOREAN)
            .setTargetLanguage(TranslateLanguage.CHINESE)
            .build()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notice)

        etKorean = findViewById(R.id.etKorean)
        tvVietnamese = findViewById(R.id.tvVietnamese)
        tvChinese = findViewById(R.id.tvChinese)
        btnTranslate = findViewById(R.id.btnTranslate)
        btnKakao = findViewById(R.id.btnKakao)
        btnZalo = findViewById(R.id.btnZalo)
        btnFacebook = findViewById(R.id.btnFacebook)
        btnWechat = findViewById(R.id.btnWechat)
        btnSms = findViewById(R.id.btnSms)

        tvVietnamese.text = "번역 모델 준비 중... (인터넷 필요)"
        downloadModels()

        etKorean.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(etKorean.windowToken, 0)
            }
        }

        // 홈으로 가기
        findViewById<LinearLayout>(R.id.btnHome).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        // 번역 버튼
        btnTranslate.setOnClickListener {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(etKorean.windowToken, 0)
            val korean = etKorean.text.toString().trim()
            if (korean.isEmpty()) {
                Toast.makeText(this, "공지 내용을 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!isModelReady) {
                Toast.makeText(this, "번역 모델 준비 중이에요. 잠깐 기다려주세요", Toast.LENGTH_SHORT).show()
                downloadModels()
                return@setOnClickListener
            }
            translateText(korean)
        }

        // 카카오톡 - 한국어 + 베트남어 + 중국어 모두 발송
        btnKakao.setOnClickListener {
            val korean = etKorean.text.toString().trim()
            if (korean.isEmpty()) {
                Toast.makeText(this, "공지 내용을 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendMessage("com.kakao.talk", buildFullMessage(korean))
        }

        // Zalo - 한국어 + 베트남어 + 중국어 모두 발송
        btnZalo.setOnClickListener {
            val korean = etKorean.text.toString().trim()
            if (korean.isEmpty()) {
                Toast.makeText(this, "공지 내용을 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (translatedVietnamese.isEmpty()) {
                Toast.makeText(this, "먼저 번역하기 버튼을 눌러주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendMessage("com.zing.zalo", buildFullMessage(korean))
        }

        // Facebook - 한국어 + 베트남어 + 중국어 모두 발송
        btnFacebook.setOnClickListener {
            val korean = etKorean.text.toString().trim()
            if (korean.isEmpty()) {
                Toast.makeText(this, "공지 내용을 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (translatedVietnamese.isEmpty()) {
                Toast.makeText(this, "먼저 번역하기 버튼을 눌러주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendMessage("com.facebook.orca", buildFullMessage(korean))
        }

        // WeChat - 한국어 + 베트남어 + 중국어 모두 발송
        btnWechat.setOnClickListener {
            val korean = etKorean.text.toString().trim()
            if (korean.isEmpty()) {
                Toast.makeText(this, "공지 내용을 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (translatedChinese.isEmpty()) {
                Toast.makeText(this, "먼저 번역하기 버튼을 눌러주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendMessage("com.tencent.mm", buildFullMessage(korean))
        }

        // SMS - 한국어 + 베트남어 + 중국어 모두 발송
        btnSms.setOnClickListener {
            val korean = etKorean.text.toString().trim()
            if (korean.isEmpty()) {
                Toast.makeText(this, "공지 내용을 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendSms(buildFullMessage(korean))
        }
    }

    // 한국어 + 베트남어 + 중국어 합쳐서 메시지 만들기
    private fun buildFullMessage(korean: String): String {
        return buildString {
            append("📢 공지사항\n")
            append("━━━━━━━━━━━━━━━\n")
            append("🇰🇷 한국어\n")
            append("$korean\n")
            if (translatedVietnamese.isNotEmpty()) {
                append("\n🇻🇳 베트남어\n")
                append("$translatedVietnamese\n")
            }
            if (translatedChinese.isNotEmpty()) {
                append("\n🇨🇳 중국어\n")
                append("$translatedChinese\n")
            }
            append("━━━━━━━━━━━━━━━")
        }
    }

    private fun downloadModels() {
        koToViTranslator.downloadModelIfNeeded()
            .addOnSuccessListener {
                koToZhTranslator.downloadModelIfNeeded()
                    .addOnSuccessListener {
                        isModelReady = true
                        tvVietnamese.text = ""
                        Toast.makeText(this, "번역 준비 완료!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        tvVietnamese.text = "인터넷 연결을 확인해주세요"
                        Toast.makeText(this, "중국어 모델 다운로드 실패", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener {
                tvVietnamese.text = "인터넷 연결을 확인해주세요"
                Toast.makeText(this, "베트남어 모델 다운로드 실패", Toast.LENGTH_LONG).show()
            }
    }

    private fun translateText(korean: String) {
        tvVietnamese.text = "번역 중..."
        tvChinese.text = "번역 중..."

        koToViTranslator.translate(korean)
            .addOnSuccessListener { vi ->
                translatedVietnamese = vi
                tvVietnamese.text = vi
            }
            .addOnFailureListener {
                tvVietnamese.text = "번역 실패"
            }

        koToZhTranslator.translate(korean)
            .addOnSuccessListener { zh ->
                translatedChinese = zh
                tvChinese.text = zh
            }
            .addOnFailureListener {
                tvChinese.text = "번역 실패"
            }
    }

    private fun sendMessage(packageName: String, message: String) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                setPackage(packageName)
            }
            startActivity(shareIntent)
        } catch (e: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            startActivity(Intent.createChooser(shareIntent, "발송하기"))
        }
    }

    private fun sendSms(message: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("smsto:")
            putExtra("sms_body", message)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        koToViTranslator.close()
        koToZhTranslator.close()
        super.onDestroy()
    }
}