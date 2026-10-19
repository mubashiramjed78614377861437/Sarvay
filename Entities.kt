package com.roadsurvey.manager.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "projects",
    indices = [
        Index(value = ["name"]),
        Index(value = ["roadName"]),
        Index(value = ["roadCode"]),
        Index(value = ["location"]),
        Index(value = ["isFavorite"]),
        Index(value = ["updatedAt"])
    ]
)
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val roadName: String = "",
    val roadCode: String = "",
    val location: String = "",
    val date: String = "",           // ISO or display date
    val client: String = "",
    val contractor: String = "",
    val description: String = "",
    val tags: String = "",           // comma-separated
    val isFavorite: Boolean = false,
    val isDemo: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null      // soft delete
)

@Entity(
    tableName = "files",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["projectId"]),
        Index(value = ["customName"]),
        Index(value = ["originalName"]),
        Index(value = ["category"]),
        Index(value = ["tags"]),
        Index(value = ["isFavorite"]),
        Index(value = ["updatedAt"]),
        Index(value = ["mimeType"])
    ]
)
data class SurveyFile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long? = null,
    val customName: String,
    val originalName: String,
    val category: String,            // PDF, IMAGE, DWG, DOCUMENT, OTHER
    val mimeType: String = "",
    val description: String = "",
    val tags: String = "",
    val date: String = "",
    val notes: String = "",
    val fileSize: Long = 0,
    val localPath: String? = null,   // path inside app storage (copied)
    val originalUri: String? = null, // content:// URI if only linked
    val isCopied: Boolean = true,    // true = in app storage
    val isFavorite: Boolean = false,
    val isDemo: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["projectId"]),
        Index(value = ["title"]),
        Index(value = ["tags"])
    ]
)
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val title: String,
    val content: String = "",
    val tags: String = "",
    val date: String = "",
    val isFavorite: Boolean = false,
    val isDemo: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

@Entity(tableName = "calc_history")
data class CalcHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expression: String,
    val result: String,
    val createdAt: Long = System.currentTimeMillis()
)

/** Simple search result wrapper */
data class SearchResult(
    val type: String,   // PROJECT, FILE, NOTE
    val id: Long,
    val title: String,
    val subtitle: String,
    val extra: String = "",
    val isFavorite: Boolean = false,
    val isDemo: Boolean = false
)
