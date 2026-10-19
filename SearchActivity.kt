package com.roadsurvey.manager.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.roadsurvey.manager.R
import com.roadsurvey.manager.RoadSurveyApp
import com.roadsurvey.manager.data.Repository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchActivity : AppCompatActivity() {

    private lateinit var repo: Repository
    private lateinit var adapter: SearchAdapter
    private var searchJob: Job? = null
    private var categoryFilter: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Search"

        repo = (application as RoadSurveyApp).repository
        categoryFilter = intent.getStringExtra("filterCategory")

        val edt = findViewById<EditText>(R.id.edtSearch)
        val recycler = findViewById<RecyclerView>(R.id.recyclerSearch)
        adapter = SearchAdapter { result ->
            when (result.type) {
                "PROJECT" -> startActivity(
                    Intent(this, ProjectDetailActivity::class.java).putExtra("projectId", result.id)
                )
                "FILE" -> startActivity(
                    Intent(this, FileDetailActivity::class.java).putExtra("fileId", result.id)
                )
                "NOTE" -> { /* open project notes later */ }
            }
        }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        if (categoryFilter != null) {
            edt.hint = "Filter: $categoryFilter — type to search"
            // preload category
            lifecycleScope.launch {
                repo.filesByCategory(categoryFilter!!).collect { list ->
                    adapter.submitList(list.map {
                        com.roadsurvey.manager.data.SearchResult(
                            "FILE", it.id, it.customName, it.originalName, it.date, it.isFavorite, it.isDemo
                        )
                    })
                }
            }
        }

        edt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(200) // debounce
                    val q = s?.toString()?.trim().orEmpty()
                    if (q.isEmpty() && categoryFilter == null) {
                        adapter.submitList(emptyList())
                        return@launch
                    }
                    val results = repo.search(q)
                    val filtered = if (categoryFilter != null && q.isNotEmpty()) {
                        results.filter { it.type == "FILE" }
                    } else results
                    adapter.submitList(filtered)
                }
            }
        })
        edt.requestFocus()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
