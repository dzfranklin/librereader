package org.danielzfranklin.librereader

import android.app.Application
import timber.log.Timber

@Suppress("unused") // used in AndroidManifest.xml
class LibreReaderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}