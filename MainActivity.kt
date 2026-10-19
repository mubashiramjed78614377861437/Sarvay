package com.mubashir.roadsurvey

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.util.Base64
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private var chooserCallback: ValueCallback<Array<Uri>>? = null
    private val pickerRequest = 4217
    private val backupRequest = 4218
    private var pendingBackupBytes: ByteArray? = null
    private val prefs by lazy { getSharedPreferences("stored_imports", MODE_PRIVATE) }
    private val importedDir by lazy { File(filesDir, "imported").apply { mkdirs() } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true
        webView.settings.databaseEnabled = true
        webView.settings.javaScriptCanOpenWindowsAutomatically = false
        webView.addJavascriptInterface(NativeFiles(), "AndroidFiles")
        webView.webViewClient = object : WebViewClient() {}
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView?, callback: ValueCallback<Array<Uri>>?, params: FileChooserParams?
            ): Boolean {
                chooserCallback?.onReceiveValue(null)
                chooserCallback = callback
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                }
                return try { startActivityForResult(intent, pickerRequest); true }
                catch (e: Exception) {
                    chooserCallback?.onReceiveValue(null); chooserCallback = null
                    Toast.makeText(this@MainActivity, "Phone file picker nahi khul saka: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    false
                }
            }
        }
        webView.loadUrl("file:///android_asset/index.html")
    }

    @Deprecated("Deprecated Android callback retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == backupRequest) {
            val bytes = pendingBackupBytes
            pendingBackupBytes = null
            if (resultCode == RESULT_OK && data?.data != null && bytes != null) {
                try {
                    contentResolver.openOutputStream(data.data!!, "w").use { out ->
                        requireNotNull(out) { "Backup destination cannot be opened" }
                        out.write(bytes); out.flush()
                    }
                    Toast.makeText(this, "Backup phone storage mein save ho gayi.", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Backup save fail hui: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
            return
        }
        if (requestCode != pickerRequest) return
        val callback = chooserCallback ?: return
        if (resultCode != RESULT_OK || data == null) {
            callback.onReceiveValue(null); chooserCallback = null
            return
        }
        val uris = mutableListOf<Uri>()
        data.clipData?.let { clip -> for (i in 0 until clip.itemCount) uris.add(clip.getItemAt(i).uri) }
        if (uris.isEmpty()) data.data?.let { uris.add(it) }
        val nativeMap = JSONArray()
        val failures = JSONArray()
        uris.forEach { uri ->
            try {
                try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) { }
                val info = copyUriIntoApp(uri)
                nativeMap.put(JSONObject().put("name", info.first).put("size", info.second).put("id", info.third).put("mime", info.fourth))
            } catch (e: Exception) {
                failures.put(JSONObject().put("uri", uri.toString()).put("error", e.localizedMessage ?: "Copy failed"))
            }
        }
        // WebView's File input still receives the selected content URIs, while native copies provide durable local files.
        webView.evaluateJavascript("window.__nativeImportMap = ${nativeMap}; window.__nativeImportErrors = ${failures};", null)
        callback.onReceiveValue(uris.toTypedArray())
        chooserCallback = null
        if (failures.length() > 0) Toast.makeText(this, "Kuch files ki local copy nahi ban saki. Storage space aur file permission check karein.", Toast.LENGTH_LONG).show()
    }

    private fun queryName(uri: Uri): String {
        var name = "imported_file"
        val cursor: Cursor? = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        cursor?.use { if (it.moveToFirst()) name = it.getString(0) ?: name }
        if (name.isBlank()) name = "imported_file"
        return name.replace('/', '_').replace('\\', '_').filter { it >= ' ' }
    }

    private fun copyUriIntoApp(uri: Uri): Quad {
        val name = queryName(uri)
        val id = UUID.randomUUID().toString()
        val dest = File(importedDir, id + "_" + name)
        val input = contentResolver.openInputStream(uri) ?: throw IllegalStateException("Android could not read this file")
        input.use { stream ->
            FileOutputStream(dest).use { output -> stream.copyTo(output, 1024 * 128); output.fd.sync() }
        }
        val mime = contentResolver.getType(uri) ?: mimeFor(name)
        prefs.edit().putString("path:$id", dest.absolutePath).putString("name:$id", name).putString("mime:$id", mime).apply()
        return Quad(name, dest.length(), id, mime)
    }
    private data class Quad(val first: String, val second: Long, val third: String, val fourth: String)

    private fun mimeFor(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "pdf" -> "application/pdf"; "jpg", "jpeg" -> "image/jpeg"; "png" -> "image/png"; "gif" -> "image/gif"; "webp" -> "image/webp"
        "doc" -> "application/msword"; "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xls" -> "application/vnd.ms-excel"; "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "dwg" -> "application/acad"; "dxf" -> "application/dxf"; "txt" -> "text/plain"; "zip" -> "application/zip"
        else -> "application/octet-stream"
    }

    inner class NativeFiles {
        @JavascriptInterface fun openImportedFile(id: String): String = openOrShare(id, false)
        @JavascriptInterface fun shareImportedFile(id: String): String = openOrShare(id, true)
        @JavascriptInterface fun hasNativeFile(id: String): Boolean = !storedFile(id).isNullOrEmpty()
        @JavascriptInterface fun saveBackupBase64(base64: String, filename: String) {
            try {
                pendingBackupBytes = Base64.decode(base64, Base64.DEFAULT)
                runOnUiThread {
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "application/json"
                        putExtra(Intent.EXTRA_TITLE, filename.replace('/', '_').replace('\\', '_'))
                    }
                    try { startActivityForResult(intent, backupRequest) }
                    catch (e: Exception) {
                        pendingBackupBytes = null
                        Toast.makeText(this@MainActivity, "Backup save picker nahi khula: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Backup data prepare nahi ho saka: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }

        private fun openOrShare(id: String, share: Boolean): String {
            return try {
                val path = storedFile(id) ?: return "MISSING"
                val file = File(path)
                if (!file.exists()) return "MISSING"
                val uri = FileProvider.getUriForFile(this@MainActivity, "$packageName.fileprovider", file)
                val mime = prefs.getString("mime:$id", "application/octet-stream") ?: "application/octet-stream"
                val intent = if (share) Intent(Intent.ACTION_SEND) else Intent(Intent.ACTION_VIEW)
                if (share) intent.type = mime else { intent.setDataAndType(uri, mime) }
                if (share) intent.putExtra(Intent.EXTRA_STREAM, uri) else intent.data = uri
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.clipData = ClipData.newUri(contentResolver, file.name, uri)
                val chooser = Intent.createChooser(intent, if (share) "Share with" else "Open with")
                chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                runOnUiThread { startActivity(chooser) }
                "OK"
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Compatible app nahi mili ya file open nahi hui: ${e.localizedMessage}", Toast.LENGTH_LONG).show() }
                "ERROR"
            }
        }
    }
    private fun storedFile(id: String): String? = prefs.getString("path:$id", null)

    override fun onBackPressed() { if (::webView.isInitialized && webView.canGoBack()) webView.goBack() else super.onBackPressed() }
}
