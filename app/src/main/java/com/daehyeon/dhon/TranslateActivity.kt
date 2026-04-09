package com.daehyeon.dhon

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.Locale

class TranslateActivity : AppCompatActivity() {

    private lateinit var tvKorean: TextView
    private lateinit var tvTranslated: TextView
    private lateinit var tvLanguageLabel: TextView
    private lateinit var tvForeignBtnLabel: TextView
    private lateinit var btnVietnamese: LinearLayout
    private lateinit var btnChinese: LinearLayout
    private lateinit var btnSpeakKorean: LinearLayout
    private lateinit var btnSpeakForeign: LinearLayout

    private var currentLanguage = "vi"
    private var isKoreanSpeaking = true

    // TTS (음성 출력)
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val correctionMap = mapOf(
        "함마" to "함마", "빠루" to "빠루", "가사" to "가새",
        "도베" to "되베", "시마이" to "마무리", "뗑깡" to "억지",
        "단도리" to "준비", "뽀루" to "볼트", "기스" to "흠집",
        "데모도" to "보조공", "오야" to "반장", "조공" to "보조공",
        "노가다" to "현장", "함바" to "현장식당", "아다리" to "맞춤",
        "기리" to "절단"
    )

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
    private var viToKoTranslator = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.VIETNAMESE)
            .setTargetLanguage(TranslateLanguage.KOREAN)
            .build()
    )
    private var zhToKoTranslator = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.CHINESE)
            .setTargetLanguage(TranslateLanguage.KOREAN)
            .build()
    )

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull() ?: return@registerForActivityResult

            if (isKoreanSpeaking) {
                val corrected = applyCorrections(spokenText)
                tvKorean.text = corrected
                translateKoreanToForeign(corrected)
            } else {
                tvTranslated.text = spokenText
                translateForeignToKorean(spokenText)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_translate)

        tvKorean = findViewById(R.id.tvKorean)
        tvTranslated = findViewById(R.id.tvTranslated)
        tvLanguageLabel = findViewById(R.id.tvLanguageLabel)
        tvForeignBtnLabel = findViewById(R.id.tvForeignBtnLabel)
        btnVietnamese = findViewById(R.id.btnVietnamese)
        btnChinese = findViewById(R.id.btnChinese)
        btnSpeakKorean = findViewById(R.id.btnSpeakKorean)
        btnSpeakForeign = findViewById(R.id.btnSpeakForeign)

        // TTS 초기화
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
            }
        }

        downloadModels()

        // 홈으로 가기
        findViewById<LinearLayout>(R.id.btnHome).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        // 베트남어 탭 - 클릭하면 파랗게
        btnVietnamese.setOnClickListener {
            currentLanguage = "vi"
            updateLanguageUI()
        }

        // 중국어 탭 - 클릭하면 파랗게
        btnChinese.setOnClickListener {
            currentLanguage = "zh"
            updateLanguageUI()
        }

        // 한국어로 말하기
        btnSpeakKorean.setOnClickListener {
            isKoreanSpeaking = true
            startSpeechRecognition("ko-KR")
        }

        // 외국어로 말하기
        btnSpeakForeign.setOnClickListener {
            isKoreanSpeaking = false
            val locale = if (currentLanguage == "vi") "vi-VN" else "zh-CN"
            startSpeechRecognition(locale)
        }

        // 처음 시작시 베트남어 선택된 상태로 표시
        updateLanguageUI()
    }

    // 언어 탭 버튼 색상 업데이트
    private fun updateLanguageUI() {
        val primaryColor = resources.getColor(R.color.primary, null)
        val surfaceColor = resources.getColor(R.color.surface_variant, null)
        val onPrimaryColor = resources.getColor(R.color.on_primary, null)
        val textPrimaryColor = resources.getColor(R.color.text_primary, null)

        if (currentLanguage == "vi") {
            // 베트남어 버튼 파랗게
            (btnVietnamese.parent as? androidx.cardview.widget.CardView)
                ?.setCardBackgroundColor(primaryColor)
            btnVietnamese.getChildAt(0)?.let {
                (it as? TextView)?.setTextColor(onPrimaryColor)
            }
            // 중국어 버튼 회색으로
            (btnChinese.parent as? androidx.cardview.widget.CardView)
                ?.setCardBackgroundColor(surfaceColor)
            btnChinese.getChildAt(0)?.let {
                (it as? TextView)?.setTextColor(textPrimaryColor)
            }
            tvLanguageLabel.text = "🇻🇳  베트남어"
            tvForeignBtnLabel.text = "베트남어로 말하기"
        } else {
            // 중국어 버튼 파랗게
            (btnChinese.parent as? androidx.cardview.widget.CardView)
                ?.setCardBackgroundColor(primaryColor)
            btnChinese.getChildAt(0)?.let {
                (it as? TextView)?.setTextColor(onPrimaryColor)
            }
            // 베트남어 버튼 회색으로
            (btnVietnamese.parent as? androidx.cardview.widget.CardView)
                ?.setCardBackgroundColor(surfaceColor)
            btnVietnamese.getChildAt(0)?.let {
                (it as? TextView)?.setTextColor(textPrimaryColor)
            }
            tvLanguageLabel.text = "🇨🇳  중국어"
            tvForeignBtnLabel.text = "중국어로 말하기"
        }

        tvKorean.text = ""
        tvTranslated.text = ""
    }

    private fun startSpeechRecognition(locale: String) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
            putExtra(RecognizerIntent.EXTRA_PROMPT,
                if (locale == "ko-KR") "한국어로 말씀해 주세요" else "말씀해 주세요")
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "음성 인식을 사용할 수 없어요", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyCorrections(text: String): String {
        var result = text
        correctionMap.forEach { (wrong, correct) ->
            result = result.replace(wrong, correct, ignoreCase = true)
        }
        return result
    }

    private fun translateKoreanToForeign(text: String) {
        tvTranslated.text = "번역 중..."
        val translator = if (currentLanguage == "vi") koToViTranslator else koToZhTranslator
        translator.translate(text)
            .addOnSuccessListener { translated ->
                tvTranslated.text = translated
                // 번역 완료 후 음성 출력
                speakText(translated, currentLanguage)
            }
            .addOnFailureListener {
                tvTranslated.text = "번역 실패"
                Toast.makeText(this, "번역 실패. 인터넷을 확인해주세요", Toast.LENGTH_SHORT).show()
            }
    }

    private fun translateForeignToKorean(text: String) {
        tvKorean.text = "번역 중..."
        val translator = if (currentLanguage == "vi") viToKoTranslator else zhToKoTranslator
        translator.translate(text)
            .addOnSuccessListener { translated ->
                tvKorean.text = translated
                // 번역 완료 후 한국어 음성 출력
                speakText(translated, "ko")
            }
            .addOnFailureListener {
                tvKorean.text = "번역 실패"
                Toast.makeText(this, "번역 실패. 인터넷을 확인해주세요", Toast.LENGTH_SHORT).show()
            }
    }

    // TTS 음성 출력
    private fun speakText(text: String, language: String) {
        if (!isTtsReady) {
            Toast.makeText(this, "음성 출력 준비 중이에요", Toast.LENGTH_SHORT).show()
            return
        }
        val locale = when (language) {
            "vi" -> Locale("vi", "VN")
            "zh" -> Locale.CHINESE
            else -> Locale.KOREAN
        }
        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Toast.makeText(this, "해당 언어 음성이 지원되지 않아요", Toast.LENGTH_SHORT).show()
            return
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun downloadModels() {
        koToViTranslator.downloadModelIfNeeded()
        koToZhTranslator.downloadModelIfNeeded()
        viToKoTranslator.downloadModelIfNeeded()
        zhToKoTranslator.downloadModelIfNeeded()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        koToViTranslator.close()
        koToZhTranslator.close()
        viToKoTranslator.close()
        zhToKoTranslator.close()
        super.onDestroy()
    }
}