package com.roadsurvey.manager.ui

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.roadsurvey.manager.R

class ImageViewerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = intent.getStringExtra("title") ?: "Image"

        val uriStr = intent.getStringExtra("uri")
        if (uriStr != null) {
            Glide.with(this)
                .load(Uri.parse(uriStr))
                .into(findViewById(R.id.imageView))
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
