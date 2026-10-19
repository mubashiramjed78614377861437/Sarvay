package com.roadsurvey.manager.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.roadsurvey.manager.R
import com.roadsurvey.manager.RoadSurveyApp
import com.roadsurvey.manager.data.Repository
import com.roadsurvey.manager.data.SurveyFile
import kotlinx.coroutines.launch
import java.io.File

class FileDetailActivity : AppCompatActivity() {

    private lateinit var repo: Repository
    private var fileId: Long = 0
    private var file: SurveyFile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_detail)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "File"

        fileId = intent.getLongExtra("fileId", 0)
        repo = (application as RoadSurveyApp).repository

        lifecycleScope.launch {
            file = repo.getFile(fileId)
            file?.let { bind(it) } ?: finish()
        }

        findViewById<MaterialButton>(R.id.btnOpen).setOnClickListener { openWith() }
        findViewById<MaterialButton>(R.id.btnShare).setOnClickListener { share() }
        findViewById<MaterialButton>(R.id.btnFavorite).setOnClickListener { toggleFavorite() }
        findViewById<MaterialButton>(R.id.btnDelete).setOnClickListener { confirmDelete() }
        findViewById<MaterialButton>(R.id.btnPreview).setOnClickListener { preview() }
    }

    private fun bind(f: SurveyFile) {
        findViewById<TextView>(R.id.txtCustomName).text = f.customName
        findViewById<TextView>(R.id.txtOriginal).text = "Original: ${f.originalName}"
        findViewById<TextView>(R.id.txtCategory).text = f.category
        findViewById<TextView>(R.id.txtMeta).text = buildString {
            append("Size: ${formatSize(f.fileSize)}\n")
            append("Date: ${f.date}\n")
            append("Storage: ${if (f.isDemo) "DEMO (no real file)" else if (f.isCopied) "Copied to app storage" else "Linked to original"}\n")
            if (!f.description.isBlank()) append("\n${f.description}")
            if (!f.tags.isBlank()) append("\nTags: ${f.tags}")
        }
        findViewById<MaterialButton>(R.id.btnFavorite).text =
            if (f.isFavorite) "★ Favorited" else "☆ Favorite"

        if (f.isDemo) {
            findViewById<TextView>(R.id.txtDemo).visibility = android.view.View.VISIBLE
            findViewById<MaterialButton>(R.id.btnOpen).isEnabled = false
            findViewById<MaterialButton>(R.id.btnPreview).isEnabled = false
            findViewById<MaterialButton>(R.id.btnShare).isEnabled = false
        }
    }

    private fun formatSize(b: Long): String {
        if (b <= 0) return "—"
        if (b < 1024) return "$b B"
        if (b < 1024 * 1024) return "${b / 1024} KB"
        return String.format("%.1f MB", b / (1024.0 * 1024.0))
    }

    private fun resolveUri(): Uri? {
        val f = file ?: return null
        if (f.isDemo) return null
        f.localPath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                return FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    file
                )
            }
        }
        f.originalUri?.let { return Uri.parse(it) }
        return null
    }

    private fun openWith() {
        val uri = resolveUri() ?: run {
            Toast.makeText(this, "File not available (demo or missing)", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, file?.mimeType ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.open_with)))
        } catch (e: Exception) {
            Toast.makeText(this, "No app can open this file type", Toast.LENGTH_LONG).show()
        }
    }

    private fun share() {
        val uri = resolveUri() ?: run {
            Toast.makeText(this, "File not available", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = file?.mimeType ?: "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }

    private fun preview() {
        val f = file ?: return
        when (f.category) {
            "IMAGE" -> {
                val uri = resolveUri()
                if (uri != null) {
                    startActivity(
                        Intent(this, ImageViewerActivity::class.java)
                            .putExtra("uri", uri.toString())
                            .putExtra("title", f.customName)
                    )
                } else Toast.makeText(this, "Image not available", Toast.LENGTH_SHORT).show()
            }
            "PDF" -> {
                val path = f.localPath
                if (path != null && File(path).exists()) {
                    startActivity(
                        Intent(this, PdfViewerActivity::class.java)
                            .putExtra("path", path)
                            .putExtra("title", f.customName)
                    )
                } else {
                    // fallback open with
                    openWith()
                }
            }
            else -> openWith()
        }
    }

    private fun toggleFavorite() {
        val f = file ?: return
        lifecycleScope.launch {
            val updated = f.copy(isFavorite = !f.isFavorite, updatedAt = System.currentTimeMillis())
            repo.saveFile(updated)
            file = updated
            bind(updated)
        }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_delete)
            .setMessage(R.string.confirm_delete_message)
            .setPositiveButton(R.string.delete) { _, _ ->
                lifecycleScope.launch {
                    repo.deleteFile(fileId)
                    Toast.makeText(this@FileDetailActivity, "Deleted", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
