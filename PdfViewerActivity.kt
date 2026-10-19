package com.roadsurvey.manager.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.barteksc.pdfviewer.PDFView
import com.roadsurvey.manager.R
import java.io.File

class PdfViewerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_viewer)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = intent.getStringExtra("title") ?: "PDF"

        val path = intent.getStringExtra("path")
        if (path.isNullOrBlank()) {
            Toast.makeText(this, "No PDF path", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val file = File(path)
        if (!file.exists()) {
            Toast.makeText(this, "PDF file missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        findViewById<PDFView>(R.id.pdfView).fromFile(file)
            .enableSwipe(true)
            .swipeHorizontal(false)
            .enableDoubletap(true)
            .load()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
