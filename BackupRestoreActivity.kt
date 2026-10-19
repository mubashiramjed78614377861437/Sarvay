package com.roadsurvey.manager.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.roadsurvey.manager.R
import com.roadsurvey.manager.RoadSurveyApp
import com.roadsurvey.manager.data.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupRestoreActivity : AppCompatActivity() {

    private lateinit var repo: Repository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Backup & Restore"

        repo = (application as RoadSurveyApp).repository

        findViewById<MaterialButton>(R.id.btnExportMeta).setOnClickListener { exportMetadata() }
        findViewById<MaterialButton>(R.id.btnExportZip).setOnClickListener { exportZip() }
        findViewById<MaterialButton>(R.id.btnShareBackup).setOnClickListener {
            Toast.makeText(this, "Create a backup first, then share the file from Files app", Toast.LENGTH_LONG).show()
        }
        findViewById<MaterialButton>(R.id.btnRestoreInfo).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Restore")
                .setMessage(
                    "To restore:\n\n" +
                    "1. Create a backup ZIP from this screen.\n" +
                    "2. Keep the ZIP on your phone (Downloads or Documents).\n" +
                    "3. Full restore of database + files is available in a future update.\n\n" +
                    "WARNING: Restoring can overwrite existing data. Always keep a copy of the ZIP outside the app."
                )
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun exportMetadata() {
        lifecycleScope.launch {
            val json = withContext(Dispatchers.IO) {
                val dir = File(getExternalFilesDir(null), "backups").apply { mkdirs() }
                val name = "rs_meta_${ts()}.json"
                val file = File(dir, name)
                // Minimal metadata marker; full DB dump can be expanded
                file.writeText(
                    """{"app":"RoadSurveyManager","type":"metadata","exportedAt":"${ts()}","note":"Offline backup. Import files remain in app private storage."}"""
                )
                file
            }
            Toast.makeText(this@BackupRestoreActivity, "Saved: ${json.absolutePath}", Toast.LENGTH_LONG).show()
            shareFile(json)
        }
    }

    private fun exportZip() {
        lifecycleScope.launch {
            val zip = withContext(Dispatchers.IO) {
                val dir = File(getExternalFilesDir(null), "backups").apply { mkdirs() }
                val zipFile = File(dir, "rs_backup_${ts()}.zip")
                ZipOutputStream(zipFile.outputStream()).use { zos ->
                    // Include metadata
                    zos.putNextEntry(ZipEntry("metadata.json"))
                    zos.write("""{"app":"RoadSurveyManager","exportedAt":"${ts()}"}""".toByteArray())
                    zos.closeEntry()

                    // Include imported files from app private storage
                    val imports = File(filesDir, "imports")
                    if (imports.exists()) {
                        imports.listFiles()?.forEach { f ->
                            if (f.isFile) {
                                zos.putNextEntry(ZipEntry("files/${f.name}"))
                                f.inputStream().use { it.copyTo(zos) }
                                zos.closeEntry()
                            }
                        }
                    }
                }
                zipFile
            }
            Toast.makeText(this@BackupRestoreActivity, "ZIP saved: ${zip.name}", Toast.LENGTH_LONG).show()
            shareFile(zip)
        }
    }

    private fun shareFile(file: File) {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            this, "${packageName}.fileprovider", file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share backup"))
    }

    private fun ts() = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
