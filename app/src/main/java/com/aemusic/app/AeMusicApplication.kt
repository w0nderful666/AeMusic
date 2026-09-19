package com.aemusic.app

import android.app.Application

class AeMusicApplication : Application() {
    val container: AppContainer by lazy(LazyThreadSafetyMode.NONE) {
        AppContainer(this)
    }

    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { container.diagnosticEventStore.recordCrash(thread, error) }
            previous?.uncaughtException(thread, error)
        }
    }
}
