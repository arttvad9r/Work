package com.worktime.app.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppDestination : NavKey {
    @Serializable
    data object Calendar : AppDestination

    @Serializable
    data object Settings : AppDestination

    @Serializable
    data object YearSummary : AppDestination
}
