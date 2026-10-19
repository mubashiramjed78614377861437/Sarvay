package com.roadsurvey.manager.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.roadsurvey.manager.R
import com.roadsurvey.manager.data.Project
import com.roadsurvey.manager.data.SearchResult
import com.roadsurvey.manager.data.SurveyFile

class ProjectAdapter(
    private val onClick: (Project) -> Unit
) : ListAdapter<Project, ProjectAdapter.VH>(Diff()) {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.txtName)
        val subtitle: TextView = v.findViewById(R.id.txtSubtitle)
        val meta: TextView = v.findViewById(R.id.txtMeta)
        val fav: ImageView = v.findViewById(R.id.imgFavorite)
        val demo: TextView = v.findViewById(R.id.txtDemo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_project, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = getItem(position)
        holder.name.text = p.name
        holder.subtitle.text = listOf(p.roadCode, p.roadName).filter { it.isNotBlank() }.joinToString(" · ")
        holder.meta.text = listOf(p.location, p.date).filter { it.isNotBlank() }.joinToString(" · ")
        holder.fav.visibility = if (p.isFavorite) View.VISIBLE else View.GONE
        holder.demo.visibility = if (p.isDemo) View.VISIBLE else View.GONE
        holder.itemView.setOnClickListener { onClick(p) }
    }

    class Diff : DiffUtil.ItemCallback<Project>() {
        override fun areItemsTheSame(a: Project, b: Project) = a.id == b.id
        override fun areContentsTheSame(a: Project, b: Project) = a == b
    }
}

class FileAdapter(
    private val onClick: (SurveyFile) -> Unit
) : ListAdapter<SurveyFile, FileAdapter.VH>(Diff()) {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icon: TextView = v.findViewById(R.id.txtCategoryIcon)
        val custom: TextView = v.findViewById(R.id.txtCustomName)
        val original: TextView = v.findViewById(R.id.txtOriginal)
        val meta: TextView = v.findViewById(R.id.txtMeta)
        val fav: ImageView = v.findViewById(R.id.imgFavorite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val f = getItem(position)
        holder.custom.text = f.customName
        holder.original.text = f.originalName
        val sizeStr = if (f.fileSize > 0) formatSize(f.fileSize) else ""
        val status = when {
            f.isDemo -> "DEMO"
            f.isCopied -> "Local"
            else -> "Linked"
        }
        holder.meta.text = listOf(f.category, sizeStr, f.date, status).filter { it.isNotBlank() }.joinToString(" · ")
        holder.fav.visibility = if (f.isFavorite) View.VISIBLE else View.GONE
        holder.icon.text = when (f.category) {
            "PDF" -> "PDF"
            "IMAGE" -> "IMG"
            "DWG" -> "CAD"
            "DOCUMENT" -> "DOC"
            else -> "FILE"
        }
        holder.icon.setBackgroundColor(
            when (f.category) {
                "PDF" -> Color.parseColor("#D32F2F")
                "IMAGE" -> Color.parseColor("#7B1FA2")
                "DWG" -> Color.parseColor("#00838F")
                "DOCUMENT" -> Color.parseColor("#1565C0")
                else -> Color.parseColor("#546E7A")
            }
        )
        holder.itemView.setOnClickListener { onClick(f) }
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        if (bytes < 1024 * 1024) return "${bytes / 1024} KB"
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }

    class Diff : DiffUtil.ItemCallback<SurveyFile>() {
        override fun areItemsTheSame(a: SurveyFile, b: SurveyFile) = a.id == b.id
        override fun areContentsTheSame(a: SurveyFile, b: SurveyFile) = a == b
    }
}

class SearchAdapter(
    private val onClick: (SearchResult) -> Unit
) : ListAdapter<SearchResult, SearchAdapter.VH>(Diff()) {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.txtCustomName)
        val subtitle: TextView = v.findViewById(R.id.txtOriginal)
        val meta: TextView = v.findViewById(R.id.txtMeta)
        val icon: TextView = v.findViewById(R.id.txtCategoryIcon)
        val fav: ImageView = v.findViewById(R.id.imgFavorite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = getItem(position)
        holder.title.text = r.title
        holder.subtitle.text = r.subtitle
        holder.meta.text = listOf(r.type, r.extra).filter { it.isNotBlank() }.joinToString(" · ")
        holder.icon.text = when (r.type) {
            "PROJECT" -> "PRJ"
            "FILE" -> "FILE"
            "NOTE" -> "NOTE"
            else -> "?"
        }
        holder.fav.visibility = if (r.isFavorite) View.VISIBLE else View.GONE
        holder.itemView.setOnClickListener { onClick(r) }
    }

    class Diff : DiffUtil.ItemCallback<SearchResult>() {
        override fun areItemsTheSame(a: SearchResult, b: SearchResult) = a.type == b.type && a.id == b.id
        override fun areContentsTheSame(a: SearchResult, b: SearchResult) = a == b
    }
}
