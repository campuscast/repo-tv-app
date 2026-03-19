package com.campuscast.tvplayer.app

import android.app.Application

class CampusCastTvApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
