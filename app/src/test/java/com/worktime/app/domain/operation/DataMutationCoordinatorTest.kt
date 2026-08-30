package com.worktime.app.domain.operation

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DataMutationCoordinatorTest {
    @Test
    fun `second mutation waits until first mutation releases lock`() = runTest {
        val coordinator = DataMutationCoordinator()
        val firstStarted = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val secondStarted = CompletableDeferred<Unit>()

        launch {
            coordinator.run {
                firstStarted.complete(Unit)
                releaseFirst.await()
            }
        }
        firstStarted.await()

        launch {
            coordinator.run {
                secondStarted.complete(Unit)
            }
        }
        runCurrent()
        assertFalse(secondStarted.isCompleted)

        releaseFirst.complete(Unit)
        secondStarted.await()
        assertTrue(secondStarted.isCompleted)
    }
}
