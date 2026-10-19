package com.roadsurvey.manager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Project::class, SurveyFile::class, Note::class, CalcHistory::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun fileDao(): FileDao
    abstract fun noteDao(): NoteDao
    abstract fun calcHistoryDao(): CalcHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "road_survey.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Seed demo data on first create
                            CoroutineScope(Dispatchers.IO).launch {
                                getInstance(context).seedDemoData()
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    suspend fun seedDemoData() {
        val projectDao = projectDao()
        if (projectDao.count() > 0) return

        val projectId = projectDao.insert(
            Project(
                name = "Murree Road Survey",
                roadName = "Murree Road",
                roadCode = "MR-01",
                location = "Rawalpindi",
                date = "20-09-2026",
                client = "C&W Department",
                contractor = "",
                description = "Sample road survey project for demonstration. Import real files from your phone.",
                tags = "survey, demo, murree",
                isFavorite = true,
                isDemo = true
            )
        )

        val fileDao = fileDao()
        val demoFiles = listOf(
            SurveyFile(
                projectId = projectId,
                customName = "MR-01 Survey PDF",
                originalName = "demo_survey.pdf",
                category = "PDF",
                mimeType = "application/pdf",
                description = "Demo placeholder – import a real PDF",
                tags = "pdf, survey, demo",
                date = "20-09-2026",
                isDemo = true,
                isCopied = false,
                fileSize = 0
            ),
            SurveyFile(
                projectId = projectId,
                customName = "MR-01 Existing Road Photo",
                originalName = "demo_photo.jpg",
                category = "IMAGE",
                mimeType = "image/jpeg",
                description = "Demo placeholder – import a real photo",
                tags = "photo, existing, demo",
                date = "20-09-2026",
                isDemo = true,
                isCopied = false,
                fileSize = 0
            ),
            SurveyFile(
                projectId = projectId,
                customName = "MR-01 Culvert 01",
                originalName = "culvert01.jpg",
                category = "IMAGE",
                mimeType = "image/jpeg",
                description = "Demo placeholder",
                tags = "culvert, photo, demo",
                date = "20-09-2026",
                isDemo = true,
                isCopied = false,
                fileSize = 0
            ),
            SurveyFile(
                projectId = projectId,
                customName = "MR-01 DWG",
                originalName = "mr01.dwg",
                category = "DWG",
                mimeType = "application/acad",
                description = "Demo CAD placeholder – open with external CAD app after import",
                tags = "dwg, cad, demo",
                date = "20-09-2026",
                isDemo = true,
                isCopied = false,
                fileSize = 0
            ),
            SurveyFile(
                projectId = projectId,
                customName = "KM 2+500 Screenshot",
                originalName = "screenshot_2+500.png",
                category = "IMAGE",
                mimeType = "image/png",
                description = "Demo screenshot placeholder",
                tags = "screenshot, chainage, demo",
                date = "20-09-2026",
                isDemo = true,
                isCopied = false,
                fileSize = 0
            )
        )
        demoFiles.forEach { fileDao.insert(it) }

        noteDao().insert(
            Note(
                projectId = projectId,
                title = "Site visit notes",
                content = "Demo note: Check culvert at KM 2+500. Import real notes and files from phone storage.",
                tags = "site, demo",
                date = "20-09-2026",
                isDemo = true
            )
        )
    }
}
