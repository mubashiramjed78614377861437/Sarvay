package com.roadsurvey.manager.ui

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.roadsurvey.manager.R
import com.roadsurvey.manager.RoadSurveyApp
import com.roadsurvey.manager.data.Project
import com.roadsurvey.manager.data.Repository
import com.roadsurvey.manager.data.SurveyFile
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProjectDetailActivity : AppCompatActivity() {

    private lateinit var repo: Repository
    private var projectId: Long = 0
    private var project: Project? = null
    private lateinit var fileAdapter: FileAdapter
    private var currentFilter: String? = null // null = all

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project_detail)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        projectId = intent.getLongExtra("projectId", 0)
        if (projectId == 0L) {
            finish()
            return
        }
        repo = (application as RoadSurveyApp).repository

        fileAdapter = FileAdapter { openFile(it) }
        val recycler = findViewById<RecyclerView>(R.id.recyclerFiles)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = fileAdapter

        val tabs = findViewById<TabLayout>(R.id.tabs)
        tabs.addTab(tabs.newTab().setText("All"))
        tabs.addTab(tabs.newTab().setText("Photos"))
        tabs.addTab(tabs.newTab().setText("PDFs"))
        tabs.addTab(tabs.newTab().setText("DWG/CAD"))
        tabs.addTab(tabs.newTab().setText("Docs"))
        tabs.addTab(tabs.newTab().setText("Notes"))

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentFilter = when (tab?.position) {
                    1 -> "IMAGE"
                    2 -> "PDF"
                    3 -> "DWG"
                    4 -> "DOCUMENT"
                    5 -> "NOTES"
                    else -> null
                }
                loadFiles()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        lifecycleScope.launch {
            project = repo.getProject(projectId)
            project?.let { bindHeader(it) }
            loadFiles()
            updateCounts()
        }
    }

    private fun bindHeader(p: Project) {
        supportActionBar?.title = p.name
        findViewById<TextView>(R.id.txtRoad).text =
            listOf(p.roadCode, p.roadName).filter { it.isNotBlank() }.joinToString(" · ")
        findViewById<TextView>(R.id.txtLocation).text =
            listOf(p.location, p.date).filter { it.isNotBlank() }.joinToString(" · ")
        findViewById<TextView>(R.id.txtDesc).text = p.description.ifBlank { "No description" }
        if (p.isDemo) {
            findViewById<TextView>(R.id.txtDemoBadge).visibility = android.view.View.VISIBLE
        }
    }

    private fun loadFiles() {
        lifecycleScope.launch {
            if (currentFilter == "NOTES") {
                // Show notes as simple list via Toast for brevity — full note UI can expand
                repo.notesByProject(projectId).collectLatest { notes ->
                    fileAdapter.submitList(emptyList())
                    findViewById<TextView>(R.id.txtEmpty).apply {
                        visibility = if (notes.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                        text = if (notes.isEmpty()) "No notes yet"
                        else notes.joinToString("\n\n") { "• ${it.title}\n${it.content.take(120)}" }
                    }
                }
            } else {
                findViewById<TextView>(R.id.txtEmpty).visibility = android.view.View.GONE
                repo.filesByProject(projectId).collectLatest { all ->
                    val filtered = if (currentFilter == null) all
                    else all.filter { it.category == currentFilter }
                    fileAdapter.submitList(filtered)
                    findViewById<TextView>(R.id.txtEmpty).apply {
                        visibility = if (filtered.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                        text = getString(R.string.empty_files)
                    }
                }
            }
        }
    }

    private suspend fun updateCounts() {
        val counts = repo.projectCounts(projectId)
        findViewById<TextView>(R.id.txtCounts).text =
            "PDFs: ${counts["PDF"]}  ·  Photos: ${counts["IMAGE"]}  ·  CAD: ${counts["DWG"]}  ·  Notes: ${counts["NOTES"]}"
    }

    private fun openFile(f: SurveyFile) {
        startActivity(Intent(this, FileDetailActivity::class.java).putExtra("fileId", f.id))
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
