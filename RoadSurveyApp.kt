package com.roadsurvey.manager

import android.app.Application
import com.roadsurvey.manager.data.Repository

class RoadSurveyApp : Application() {
    lateinit var repository: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = Repository(this)
    }
}
