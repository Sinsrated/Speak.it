package com.example.speakit

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Gravity
import android.widget.*
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.*
import java.io.File
import java.util.*

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    private var editText: EditText? = null
    private var textToSpeech: TextToSpeech? = null
    private val mainScope = CoroutineScope(Dispatchers.Main)

    companion object {
        private const val PDF_REQUEST_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = ScrollView(this)
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 60, 40, 40)
            setBackgroundColor(0xFFDFFFEF.toInt()) // Mint green
        }
        scrollView.addView(rootLayout)

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val speakerIcon = ImageView(this).apply {
            setImageResource(R.drawable.speaker)
        }

        val title = TextView(this).apply {
            text = "Text to Speech"
            textSize = 22f
            setTextColor(0xFF404040.toInt())
            setPadding(20, 0, 0, 0)
        }

        header.addView(speakerIcon, LinearLayout.LayoutParams(100, 100))
        header.addView(title)
        rootLayout.addView(header)

        // EditText
        editText = EditText(this).apply {
            hint = "Type something..."
            textSize = 18f
            setBackgroundColor(0xFFFFFFFF.toInt())
            setPadding(30, 30, 30, 30)
            setTextColor(0xFF404040.toInt())
        }
        rootLayout.addView(editText, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 300
        ))

        // Speak Button
        val speakBtn = Button(this).apply {
            text = "🔊 Speak"
            setBackgroundColor(0xFFC8FFD4.toInt())
            setTextColor(0xFF404040.toInt())
        }
        rootLayout.addView(speakBtn)

        // PDF Section
        val pdfText = TextView(this).apply {
            text = "Upload PDF"
            textSize = 22f
            setTextColor(0xFF404040.toInt())
            setPadding(0, 40, 0, 20)
        }
        rootLayout.addView(pdfText)

        val uploadBtn = Button(this).apply {
            text = "📄 Select PDF"
            setBackgroundColor(0xFFC8FFD4.toInt())
            setTextColor(0xFF404040.toInt())
        }
        rootLayout.addView(uploadBtn)

        setContentView(scrollView)

        // Initialize TTS
        textToSpeech = TextToSpeech(applicationContext, this)

        speakBtn.setOnClickListener {
            val text = editText?.text.toString()
            if (text.isNotEmpty()) {
                textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            } else {
                Toast.makeText(this, "No text to speak", Toast.LENGTH_SHORT).show()
            }
        }

        uploadBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "application/pdf"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(intent, PDF_REQUEST_CODE)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.UK)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Language not supported", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "TTS Initialization failed", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Deprecated in API 33")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PDF_REQUEST_CODE && resultCode == RESULT_OK) {
            val uri = data?.data
            if (uri != null) {
                parsePdf(uri)
            } else {
                Toast.makeText(this, "No PDF selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun parsePdf(uri: Uri) {
        mainScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        PDDocument.load(inputStream).use { doc ->
                            PDFTextStripper().getText(doc)
                        }
                    }?: throw Exception("Unable to open PDF")
               }

                editText?.setText(text)
                textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)

            } catch (e: Exception) {
                Log.e("PDF", "Error reading PDF", e)
                Toast.makeText(this@MainActivity, "Failed to read PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech?.shutdown()
    }
}
