package com.roadsurvey.manager.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.roadsurvey.manager.R
import com.roadsurvey.manager.RoadSurveyApp
import com.roadsurvey.manager.data.Project
import com.roadsurvey.manager.data.Repository
import com.roadsurvey.manager.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repo: Repository
    private lateinit var projectAdapter: ProjectAdapter
    private lateinit var fileAdapter: FileAdapter
    private lateinit var favProjectAdapter: ProjectAdapter

    private var pendingUris: List<Uri> = emptyList()
    private var projectsCache: List<Project> = emptyList()

    private val openMultiple = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNullOrEmpty()) return@registerForActivityResult
        pendingUris = uris
        // Take persistable permission when possible
        uris.forEach { uri ->
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) { /* some providers don't allow */ }
        }
        showImportDialogForNext()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        repo = (application as RoadSurveyApp).repository

        projectAdapter = ProjectAdapter { openProject(it) }
        fileAdapter = FileAdapter { openFile(it) }
        favProjectAdapter = ProjectAdapter { openProject(it) }

        binding.recyclerProjects.layoutManager = LinearLayoutManager(this)
        binding.recyclerProjects.adapter = projectAdapter
        binding.recyclerFiles.layoutManager = LinearLayoutManager(this)
        binding.recyclerFiles.adapter = fileAdapter
        binding.recyclerFavorites.layoutManager = LinearLayoutManager(this)
        binding.recyclerFavorites.adapter = favProjectAdapter

        binding.searchCard.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        binding.searchHint.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }

        binding.btnAddProject.setOnClickListener { showAddProjectDialog() }
        binding.btnImport.setOnClickListener { launchFilePicker() }
        binding.btnCalculator.setOnClickListener {
            startActivity(Intent(this, CalculatorActivity::class.java))
        }
        binding.btnBackup.setOnClickListener {
            startActivity(Intent(this, BackupRestoreActivity::class.java))
        }

        binding.chipPdf.setOnClickListener { openCategory("PDF") }
        binding.chipImages.setOnClickListener { openCategory("IMAGE") }
        binding.chipDwg.setOnClickListener { openCategory("DWG") }
        binding.chipDocs.setOnClickListener { openCategory("DOCUMENT") }
        binding.chipOther.setOnClickListener { openCategory("OTHER") }

        binding.fabQuickAdd.setOnClickListener { showQuickAddSheet() }

        observeData()
    }

    private fun observeData() {
        lifecycleScope.launch {
            repo.recentProjects(10).collectLatest {
                projectsCache = it
                projectAdapter.submitList(it)
            }
        }
        lifecycleScope.launch {
            repo.recentFiles(15).collectLatest { fileAdapter.submitList(it) }
        }
        lifecycleScope.launch {
            repo.favoriteProjects().collectLatest { favProjectAdapter.submitList(it) }
        }
    }

    private fun launchFilePicker() {
        // SAF — user can pick from Downloads, Documents, Pictures, etc.
        openMultiple.launch(arrayOf(
            "application/pdf",
            "image/*",
            "application/acad",
            "image/vnd.dxf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain",
            "application/zip",
            "*/*"
        ))
    }

    private fun showImportDialogForNext() {
        if (pendingUris.isEmpty()) {
            Toast.makeText(this, "Import finished", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = pendingUris.first()
        pendingUris = pendingUris.drop(1)

        val view = layoutInflater.inflate(R.layout.dialog_import_file, null)
        val edtCustom = view.findViewById<EditText>(R.id.edtCustomName)
        val edtCategory = view.findViewById<EditText>(R.id.edtCategory)
        val edtProject = view.findViewById<EditText>(R.id.edtProject)
        val edtDesc = view.findViewById<EditText>(R.id.edtDescription)
        val edtTags = view.findViewById<EditText>(R.id.edtTags)
        val txtOrig = view.findViewById<android.widget.TextView>(R.id.txtOriginalName)

        val nameGuess = uri.lastPathSegment?.substringAfterLast('/') ?: "file"
        txtOrig.text = "Original: $nameGuess"
        edtCustom.setText(nameGuess)
        val cat = repo.categoryFromMime(contentResolver.getType(uri) ?: "", nameGuess)
        edtCategory.setText(cat)

        var selectedProjectId: Long? = null
        edtProject.setOnClickListener {
            val names = projectsCache.map { it.name }.toTypedArray()
            if (names.isEmpty()) {
                Toast.makeText(this, "Create a project first (optional)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            AlertDialog.Builder(this)
                .setTitle("Assign to project")
                .setItems(names) { _, which ->
                    selectedProjectId = projectsCache[which].id
                    edtProject.setText(names[which])
                }
                .show()
        }

        val cats = arrayOf("PDF", "IMAGE", "DWG", "DOCUMENT", "OTHER")
        edtCategory.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Category")
                .setItems(cats) { _, which -> edtCategory.setText(cats[which]) }
                .show()
        }

        AlertDialog.Builder(this)
            .setTitle("Import file")
            .setView(view)
            .setPositiveButton("Import") { _, _ ->
                lifecycleScope.launch {
                    try {
                        repo.importUri(
                            uri = uri,
                            customName = edtCustom.text.toString().trim(),
                            projectId = selectedProjectId,
                            category = edtCategory.text.toString().ifBlank { cat },
                            description = edtDesc.text.toString(),
                            tags = edtTags.text.toString()
                        )
                        Toast.makeText(this@MainActivity, "Imported (copied to app storage when possible)", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                    showImportDialogForNext()
                }
            }
            .setNegativeButton("Skip") { _, _ -> showImportDialogForNext() }
            .setNeutralButton("Cancel all") { _, _ -> pendingUris = emptyList() }
            .setCancelable(false)
            .show()
    }

    private fun showAddProjectDialog(existing: Project? = null) {
        val view = layoutInflater.inflate(R.layout.dialog_add_project, null)
        val edtName = view.findViewById<EditText>(R.id.edtName)
        val edtRoad = view.findViewById<EditText>(R.id.edtRoadName)
        val edtCode = view.findViewById<EditText>(R.id.edtRoadCode)
        val edtLoc = view.findViewById<EditText>(R.id.edtLocation)
        val edtDate = view.findViewById<EditText>(R.id.edtDate)
        val edtClient = view.findViewById<EditText>(R.id.edtClient)
        val edtContractor = view.findViewById<EditText>(R.id.edtContractor)
        val edtDesc = view.findViewById<EditText>(R.id.edtDescription)
        val edtTags = view.findViewById<EditText>(R.id.edtTags)

        if (existing != null) {
            edtName.setText(existing.name)
            edtRoad.setText(existing.roadName)
            edtCode.setText(existing.roadCode)
            edtLoc.setText(existing.location)
            edtDate.setText(existing.date)
            edtClient.setText(existing.client)
            edtContractor.setText(existing.contractor)
            edtDesc.setText(existing.description)
            edtTags.setText(existing.tags)
        } else {
            edtDate.setText(SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date()))
        }

        AlertDialog.Builder(this)
            .setTitle(if (existing == null) "New Project" else "Edit Project")
            .setView(view)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = edtName.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Name required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    val p = (existing ?: Project(name = name)).copy(
                        name = name,
                        roadName = edtRoad.text.toString().trim(),
                        roadCode = edtCode.text.toString().trim(),
                        location = edtLoc.text.toString().trim(),
                        date = edtDate.text.toString().trim(),
                        client = edtClient.text.toString().trim(),
                        contractor = edtContractor.text.toString().trim(),
                        description = edtDesc.text.toString().trim(),
                        tags = edtTags.text.toString().trim(),
                        updatedAt = System.currentTimeMillis()
                    )
                    val id = repo.saveProject(p)
                    Toast.makeText(this@MainActivity, "Saved", Toast.LENGTH_SHORT).show()
                    if (existing == null) {
                        openProject(p.copy(id = id))
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showQuickAddSheet() {
        val dialog = BottomSheetDialog(this)
        val sheet = layoutInflater.inflate(android.R.layout.simple_list_item_1, null)
        // Simple options via AlertDialog for reliability
        dialog.dismiss()
        AlertDialog.Builder(this)
            .setTitle("Quick Add")
            .setItems(arrayOf("Add Project", "Import File(s)", "Add Photo", "Open Calculator")) { _, which ->
                when (which) {
                    0 -> showAddProjectDialog()
                    1 -> launchFilePicker()
                    2 -> launchFilePicker() // image filter still works via */*
                    3 -> startActivity(Intent(this, CalculatorActivity::class.java))
                }
            }
            .show()
    }

    private fun openProject(p: Project) {
        startActivity(Intent(this, ProjectDetailActivity::class.java).putExtra("projectId", p.id))
    }

    private fun openFile(f: com.roadsurvey.manager.data.SurveyFile) {
        startActivity(Intent(this, FileDetailActivity::class.java).putExtra("fileId", f.id))
    }

    private fun openCategory(cat: String) {
        startActivity(
            Intent(this, SearchActivity::class.java)
                .putExtra("filterCategory", cat)
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 0, "Search").setIcon(android.R.drawable.ic_menu_search)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.add(0, 2, 0, "Backup & Restore")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            1 -> startActivity(Intent(this, SearchActivity::class.java))
            2 -> startActivity(Intent(this, BackupRestoreActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }
}
