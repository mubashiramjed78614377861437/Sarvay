package com.roadsurvey.manager.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Repository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val projectDao = db.projectDao()
    private val fileDao = db.fileDao()
    private val noteDao = db.noteDao()
    private val calcDao = db.calcHistoryDao()

    // ---- Projects ----
    fun projects(): Flow<List<Project>> = projectDao.getAll()
    fun recentProjects(limit: Int = 8): Flow<List<Project>> = projectDao.getRecent(limit)
    fun favoriteProjects(): Flow<List<Project>> = projectDao.getFavorites()
    suspend fun getProject(id: Long) = projectDao.getById(id)
    suspend fun saveProject(p: Project): Long {
        return if (p.id == 0L) projectDao.insert(p) else {
            projectDao.update(p.copy(updatedAt = System.currentTimeMillis()))
            p.id
        }
    }
    suspend fun deleteProject(id: Long) = projectDao.softDelete(id)

    // ---- Files ----
    fun files(): Flow<List<SurveyFile>> = fileDao.getAll()
    fun recentFiles(limit: Int = 12): Flow<List<SurveyFile>> = fileDao.getRecent(limit)
    fun favoriteFiles(): Flow<List<SurveyFile>> = fileDao.getFavorites()
    fun filesByProject(projectId: Long): Flow<List<SurveyFile>> = fileDao.getByProject(projectId)
    fun filesByCategory(category: String): Flow<List<SurveyFile>> = fileDao.getByCategory(category)
    suspend fun getFile(id: Long) = fileDao.getById(id)
    suspend fun saveFile(f: SurveyFile): Long {
        return if (f.id == 0L) fileDao.insert(f) else {
            fileDao.update(f.copy(updatedAt = System.currentTimeMillis()))
            f.id
        }
    }
    suspend fun deleteFile(id: Long) = fileDao.softDelete(id)

    // ---- Notes ----
    fun notesByProject(projectId: Long): Flow<List<Note>> = noteDao.getByProject(projectId)
    suspend fun getNote(id: Long) = noteDao.getById(id)
    suspend fun saveNote(n: Note): Long {
        return if (n.id == 0L) noteDao.insert(n) else {
            noteDao.update(n.copy(updatedAt = System.currentTimeMillis()))
            n.id
        }
    }
    suspend fun deleteNote(id: Long) = noteDao.softDelete(id)

    // ---- Search (offline) ----
    suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val q = query.trim()
        val results = mutableListOf<SearchResult>()

        projectDao.search(q).forEach {
            results.add(
                SearchResult(
                    type = "PROJECT",
                    id = it.id,
                    title = it.name,
                    subtitle = listOf(it.roadCode, it.roadName, it.location).filter { s -> s.isNotBlank() }.joinToString(" · "),
                    extra = it.date,
                    isFavorite = it.isFavorite,
                    isDemo = it.isDemo
                )
            )
        }
        fileDao.search(q).forEach {
            results.add(
                SearchResult(
                    type = "FILE",
                    id = it.id,
                    title = it.customName,
                    subtitle = "${it.category} · ${it.originalName}",
                    extra = it.date,
                    isFavorite = it.isFavorite,
                    isDemo = it.isDemo
                )
            )
        }
        noteDao.search(q).forEach {
            results.add(
                SearchResult(
                    type = "NOTE",
                    id = it.id,
                    title = it.title,
                    subtitle = it.content.take(80),
                    extra = it.date,
                    isFavorite = it.isFavorite,
                    isDemo = it.isDemo
                )
            )
        }
        results
    }

    // ---- File import via SAF ----
    /**
     * Copy selected URI into app private files dir and create DB record.
     * Returns the new SurveyFile id.
     */
    suspend fun importUri(
        uri: Uri,
        customName: String,
        projectId: Long?,
        category: String,
        description: String = "",
        tags: String = "",
        notes: String = ""
    ): Long = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val originalName = queryDisplayName(uri) ?: "imported_file"
        val mime = resolver.getType(uri) ?: guessMime(originalName)
        val size = querySize(uri)

        // Destination in app private storage
        val dir = File(context.filesDir, "imports").apply { mkdirs() }
        val safeName = "${System.currentTimeMillis()}_$originalName".replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val dest = File(dir, safeName)

        var copied = false
        try {
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            }
            copied = dest.exists() && dest.length() > 0
        } catch (e: Exception) {
            // Fall back to link-only
            copied = false
        }

        val dateStr = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        val file = SurveyFile(
            projectId = projectId,
            customName = customName.ifBlank { originalName },
            originalName = originalName,
            category = category.ifBlank { categoryFromMime(mime, originalName) },
            mimeType = mime,
            description = description,
            tags = tags,
            date = dateStr,
            notes = notes,
            fileSize = if (copied) dest.length() else size,
            localPath = if (copied) dest.absolutePath else null,
            originalUri = uri.toString(),
            isCopied = copied
        )
        fileDao.insert(file)
    }

    private fun queryDisplayName(uri: Uri): String? {
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) return c.getString(idx)
        }
        return uri.lastPathSegment
    }

    private fun querySize(uri: Uri): Long {
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.SIZE)
            if (idx >= 0 && c.moveToFirst()) return c.getLong(idx)
        }
        return 0L
    }

    private fun guessMime(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.endsWith(".pdf") -> "application/pdf"
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".dwg") -> "application/acad"
            lower.endsWith(".dxf") -> "image/vnd.dxf"
            lower.endsWith(".doc") -> "application/msword"
            lower.endsWith(".docx") -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            lower.endsWith(".xls") -> "application/vnd.ms-excel"
            lower.endsWith(".xlsx") -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            lower.endsWith(".txt") -> "text/plain"
            lower.endsWith(".zip") -> "application/zip"
            else -> "application/octet-stream"
        }
    }

    fun categoryFromMime(mime: String, name: String): String {
        val lower = name.lowercase()
        return when {
            mime.startsWith("image/") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") -> "IMAGE"
            mime == "application/pdf" || lower.endsWith(".pdf") -> "PDF"
            lower.endsWith(".dwg") || lower.endsWith(".dxf") || mime.contains("acad") || mime.contains("dxf") -> "DWG"
            mime.startsWith("text/") || lower.endsWith(".doc") || lower.endsWith(".docx") || lower.endsWith(".xls") || lower.endsWith(".xlsx") || lower.endsWith(".txt") -> "DOCUMENT"
            else -> "OTHER"
        }
    }

    // ---- Counts for project page ----
    suspend fun projectCounts(projectId: Long): Map<String, Int> = withContext(Dispatchers.IO) {
        mapOf(
            "PDF" to fileDao.countByProjectAndCategory(projectId, "PDF"),
            "IMAGE" to fileDao.countByProjectAndCategory(projectId, "IMAGE"),
            "DWG" to fileDao.countByProjectAndCategory(projectId, "DWG"),
            "DOCUMENT" to fileDao.countByProjectAndCategory(projectId, "DOCUMENT"),
            "OTHER" to fileDao.countByProjectAndCategory(projectId, "OTHER"),
            "NOTES" to noteDao.countByProject(projectId),
            "TOTAL" to fileDao.countByProject(projectId)
        )
    }

    // ---- Calculator history ----
    fun calcHistory(): Flow<List<CalcHistory>> = calcDao.getRecent()
    suspend fun addCalc(expr: String, result: String) = calcDao.insert(CalcHistory(expression = expr, result = result))
    suspend fun clearCalcHistory() = calcDao.clear()

    // ---- Backup metadata as JSON-like text (simple) ----
    suspend fun exportMetadataJson(): String = withContext(Dispatchers.IO) {
        val projects = projectDao.getAll().let { /* collect once */ emptyList<Project>() } // simplified
        // Full implementation would serialize all tables; for demo we write a marker
        """{"app":"RoadSurveyManager","version":1,"exportedAt":${System.currentTimeMillis()},"note":"Open Backup & Restore in app for full ZIP export"}"""
    }
}
