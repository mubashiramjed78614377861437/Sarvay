package com.roadsurvey.manager.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE deletedAt IS NULL ORDER BY updatedAt DESC LIMIT :limit")
    fun getRecent(limit: Int = 10): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE isFavorite = 1 AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getFavorites(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getById(id: Long): Project?

    @Query("""
        SELECT * FROM projects WHERE deletedAt IS NULL AND (
            name LIKE '%' || :q || '%' OR
            roadName LIKE '%' || :q || '%' OR
            roadCode LIKE '%' || :q || '%' OR
            location LIKE '%' || :q || '%' OR
            description LIKE '%' || :q || '%' OR
            tags LIKE '%' || :q || '%' OR
            client LIKE '%' || :q || '%' OR
            date LIKE '%' || :q || '%'
        ) ORDER BY updatedAt DESC
    """)
    suspend fun search(q: String): List<Project>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: Project): Long

    @Update
    suspend fun update(project: Project)

    @Query("UPDATE projects SET deletedAt = :ts WHERE id = :id")
    suspend fun softDelete(id: Long, ts: Long = System.currentTimeMillis())

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun hardDelete(id: Long)

    @Query("SELECT COUNT(*) FROM projects WHERE deletedAt IS NULL")
    suspend fun count(): Int
}

@Dao
interface FileDao {
    @Query("SELECT * FROM files WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<SurveyFile>>

    @Query("SELECT * FROM files WHERE deletedAt IS NULL ORDER BY updatedAt DESC LIMIT :limit")
    fun getRecent(limit: Int = 15): Flow<List<SurveyFile>>

    @Query("SELECT * FROM files WHERE isFavorite = 1 AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getFavorites(): Flow<List<SurveyFile>>

    @Query("SELECT * FROM files WHERE projectId = :projectId AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getByProject(projectId: Long): Flow<List<SurveyFile>>

    @Query("SELECT * FROM files WHERE category = :category AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getByCategory(category: String): Flow<List<SurveyFile>>

    @Query("SELECT * FROM files WHERE id = :id")
    suspend fun getById(id: Long): SurveyFile?

    @Query("""
        SELECT * FROM files WHERE deletedAt IS NULL AND (
            customName LIKE '%' || :q || '%' OR
            originalName LIKE '%' || :q || '%' OR
            description LIKE '%' || :q || '%' OR
            tags LIKE '%' || :q || '%' OR
            notes LIKE '%' || :q || '%' OR
            date LIKE '%' || :q || '%' OR
            category LIKE '%' || :q || '%'
        ) ORDER BY updatedAt DESC
    """)
    suspend fun search(q: String): List<SurveyFile>

    @Query("""
        SELECT * FROM files WHERE deletedAt IS NULL AND projectId = :projectId AND (
            customName LIKE '%' || :q || '%' OR
            originalName LIKE '%' || :q || '%' OR
            tags LIKE '%' || :q || '%'
        ) ORDER BY updatedAt DESC
    """)
    suspend fun searchInProject(projectId: Long, q: String): List<SurveyFile>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: SurveyFile): Long

    @Update
    suspend fun update(file: SurveyFile)

    @Query("UPDATE files SET deletedAt = :ts WHERE id = :id")
    suspend fun softDelete(id: Long, ts: Long = System.currentTimeMillis())

    @Query("DELETE FROM files WHERE id = :id")
    suspend fun hardDelete(id: Long)

    @Query("SELECT COUNT(*) FROM files WHERE projectId = :projectId AND category = :category AND deletedAt IS NULL")
    suspend fun countByProjectAndCategory(projectId: Long, category: String): Int

    @Query("SELECT COUNT(*) FROM files WHERE projectId = :projectId AND deletedAt IS NULL")
    suspend fun countByProject(projectId: Long): Int
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE projectId = :projectId AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getByProject(projectId: Long): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): Note?

    @Query("""
        SELECT * FROM notes WHERE deletedAt IS NULL AND (
            title LIKE '%' || :q || '%' OR
            content LIKE '%' || :q || '%' OR
            tags LIKE '%' || :q || '%'
        ) ORDER BY updatedAt DESC
    """)
    suspend fun search(q: String): List<Note>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Query("UPDATE notes SET deletedAt = :ts WHERE id = :id")
    suspend fun softDelete(id: Long, ts: Long = System.currentTimeMillis())

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun hardDelete(id: Long)

    @Query("SELECT COUNT(*) FROM notes WHERE projectId = :projectId AND deletedAt IS NULL")
    suspend fun countByProject(projectId: Long): Int
}

@Dao
interface CalcHistoryDao {
    @Query("SELECT * FROM calc_history ORDER BY createdAt DESC LIMIT 50")
    fun getRecent(): Flow<List<CalcHistory>>

    @Insert
    suspend fun insert(item: CalcHistory)

    @Query("DELETE FROM calc_history")
    suspend fun clear()
}
