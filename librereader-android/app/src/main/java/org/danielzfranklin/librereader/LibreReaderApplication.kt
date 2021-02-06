package org.danielzfranklin.librereader

import android.app.Application
import org.danielzfranklin.librereader.repo.Repo
import timber.log.Timber

@Suppress("unused") // used in AndroidManifest.xml
class LibreReaderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Repo.initialize(this)
        Timber.plant(Timber.DebugTree())
    }
}