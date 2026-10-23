package app.quiet

import android.app.Application
import app.quiet.data.Store

class QuietApp : Application() {
    lateinit var store: Store
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        store = Store(this)
    }

    companion object {
        lateinit var instance: QuietApp
            private set

        val store: Store get() = instance.store
    }
}
