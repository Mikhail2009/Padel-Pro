package com.padelpro.app

import android.app.Application
import com.padelpro.app.di.AppContainer

class PadelProApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
