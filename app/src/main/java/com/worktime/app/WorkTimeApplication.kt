package com.worktime.app

import android.app.Application
import com.worktime.app.widget.observeForWidget

class WorkTimeApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        observeForWidget(this, container.workEntryRepository)
    }
}
