package com.worktime.app

import android.app.Application
import com.worktime.app.widget.ensureWidgetObservation

class WorkTimeApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ensureWidgetObservation(
            context = this,
            workEntryRepository = container.workEntryRepository,
            userPreferencesRepository = container.userPreferencesRepository,
        )
    }
}
