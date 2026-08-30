package com.worktime.app.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.metadata
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.worktime.app.AppContainer
import com.worktime.app.R
import com.worktime.app.ui.backup.BackupOperationError
import com.worktime.app.ui.backup.BackupViewModel
import com.worktime.app.ui.backup.rememberBackupDocumentActions
import com.worktime.app.ui.calendar.CalendarScreen
import com.worktime.app.ui.calendar.CalendarViewModel
import com.worktime.app.ui.navigation.AppDestination
import com.worktime.app.ui.preferences.PreferencesViewModel
import com.worktime.app.ui.settings.SettingsScreen
import com.worktime.app.ui.theme.WorkTimeTheme
import com.worktime.app.ui.yearsummary.YearSummaryScreen
import com.worktime.app.ui.yearsummary.YearSummaryViewModel
import java.time.LocalDate

@Composable
fun WorkTimeApp(
    container: AppContainer,
    openTodayRequest: Long = 0L,
) {
    val viewModel: CalendarViewModel = viewModel(
        factory = CalendarViewModel.factory(
            workEntryRepository = container.workEntryRepository,
            userPreferencesRepository = container.userPreferencesRepository,
            dataMutationCoordinator = container.dataMutationCoordinator,
        ),
    )
    val preferencesViewModel: PreferencesViewModel = viewModel(
        factory = PreferencesViewModel.factory(
            userPreferencesRepository = container.userPreferencesRepository,
            dataMutationCoordinator = container.dataMutationCoordinator,
        ),
    )
    val backupViewModel: BackupViewModel = viewModel(
        factory = BackupViewModel.factory(
            workEntryRepository = container.workEntryRepository,
            userPreferencesRepository = container.userPreferencesRepository,
            backupDocumentSerializer = container.backupDocumentSerializer,
            dataMutationCoordinator = container.dataMutationCoordinator,
        ),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val preferencesState by preferencesViewModel.state.collectAsStateWithLifecycle()
    val backupState by backupViewModel.state.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(AppDestination.Calendar)
    val snackbarHostState = remember { SnackbarHostState() }
    val backupDocumentActions = rememberBackupDocumentActions(backupViewModel)

    fun dismissCurrentDestination() {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
    }

    LaunchedEffect(viewModel, backupViewModel, openTodayRequest) {
        if (openTodayRequest > 0L) {
            while (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
            viewModel.dismissChangeRateSheet()
            backupViewModel.cancelImport()
            viewModel.selectDate(LocalDate.now())
        }
    }

    AppOperationFeedback(
        calendarViewModel = viewModel,
        backupViewModel = backupViewModel,
        snackbarHostState = snackbarHostState,
    )

    val fullScreenDirection = fullScreenNavigationDirection(LocalLayoutDirection.current)

    WorkTimeTheme(themeMode = preferencesState.themeMode) {
        Box(modifier = Modifier.fillMaxSize()) {
            NavDisplay(
                backStack = backStack,
                modifier = Modifier.fillMaxSize(),
                onBack = ::dismissCurrentDestination,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                transitionSpec = {
                    fullScreenEnterTransition(fullScreenDirection) togetherWith
                        ExitTransition.KeepUntilTransitionsFinished
                },
                popTransitionSpec = {
                    EnterTransition.None togetherWith
                        fullScreenExitTransition(fullScreenDirection)
                },
                predictivePopTransitionSpec = { _ ->
                    EnterTransition.None togetherWith
                        fullScreenExitTransition(fullScreenDirection)
                },
                entryProvider = entryProvider {
                    entry<AppDestination.Calendar> {
                        CalendarScreen(
                            state = state,
                            onPreviousMonth = viewModel::previousMonth,
                            onNextMonth = viewModel::nextMonth,
                            onSelectMonth = viewModel::showMonth,
                            onDayClick = viewModel::selectDate,
                            onSettingsClick = {
                                viewModel.dismissEditor()
                                if (backStack.lastOrNull() != AppDestination.Settings) {
                                    backStack.add(AppDestination.Settings)
                                }
                            },
                            onOpenYearSummary = {
                                viewModel.dismissEditor()
                                if (backStack.lastOrNull() !is AppDestination.YearSummary) {
                                    backStack.add(
                                        AppDestination.YearSummary(
                                            initialYear = state.visibleMonth.year,
                                        ),
                                    )
                                }
                            },
                        )
                    }
                    entry<AppDestination.Settings> {
                        val operationErrorMessage = when {
                            preferencesState.saveFailed -> stringResource(R.string.save_settings_failed)
                            backupState.error == BackupOperationError.EXPORT ->
                                stringResource(R.string.backup_export_failed)
                            backupState.error == BackupOperationError.IMPORT ->
                                stringResource(R.string.backup_import_failed)
                            backupState.error == BackupOperationError.IMPORT_ROLLBACK ->
                                stringResource(R.string.backup_import_rollback_failed)
                            else -> null
                        }
                        SettingsScreen(
                            defaultHourlyRateMicros = preferencesState.defaultHourlyRateMicros,
                            themeMode = preferencesState.themeMode,
                            operationErrorMessage = operationErrorMessage,
                            onDismiss = ::dismissCurrentDestination,
                            onThemeChange = preferencesViewModel::updateThemeMode,
                            onRateChange = preferencesViewModel::updateDefaultRate,
                            onOpenChangeRate = { viewModel.openChangeRate(null) },
                            onExportData = backupDocumentActions.exportBackup,
                            onExportCsv = backupDocumentActions.exportCsv,
                            onImportData = backupDocumentActions.importBackup,
                        )
                    }
                    entry<AppDestination.YearSummary>(
                        metadata = metadata {
                            put(NavDisplay.TransitionKey) {
                                yearSummaryEnterTransition() togetherWith
                                    ExitTransition.KeepUntilTransitionsFinished
                            }
                            put(NavDisplay.PopTransitionKey) {
                                EnterTransition.None togetherWith yearSummaryExitTransition()
                            }
                            put(
                                NavDisplay.PredictivePopTransitionKey,
                                { _: Int ->
                                    EnterTransition.None togetherWith yearSummaryExitTransition()
                                },
                            )
                        },
                    ) { destination ->
                        val yearSummaryViewModel: YearSummaryViewModel = viewModel(
                            factory = YearSummaryViewModel.factory(
                                workEntryRepository = container.workEntryRepository,
                                initialYear = destination.initialYear,
                            ),
                        )
                        val yearSummaryState by yearSummaryViewModel.state.collectAsStateWithLifecycle()
                        YearSummaryScreen(
                            selectedYear = yearSummaryState.selectedYear,
                            summaries = yearSummaryState.summaries,
                            onDismiss = ::dismissCurrentDestination,
                            onSelectYear = yearSummaryViewModel::showYear,
                        )
                    }
                },
            )

            AppOverlays(
                calendarState = state,
                preferencesState = preferencesState,
                backupState = backupState,
                calendarViewModel = viewModel,
                backupViewModel = backupViewModel,
                snackbarHostState = snackbarHostState,
            )
        }
    }
}
