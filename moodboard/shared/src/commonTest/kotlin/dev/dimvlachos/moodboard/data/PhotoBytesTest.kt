package dev.dimvlachos.moodboard.data

import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest

class PhotoBytesTest {

    @Test
    fun readsOnTheBackgroundDispatcher() = runTest {
        val background = StandardTestDispatcher(testScheduler)
        var readOn: Any? = null
        val photoBytes =
            PhotoBytes(background) {
                readOn = currentCoroutineContext()[ContinuationInterceptor]
                byteArrayOf(1, 2)
            }
        assertContentEquals(byteArrayOf(1, 2), photoBytes.bytes("files/photos/x.jpg"))
        assertSame(background, readOn)
    }

    @Test
    fun aMissingPhotoIsNull() = runTest {
        val photoBytes = PhotoBytes(StandardTestDispatcher(testScheduler)) { error("missing") }
        assertNull(photoBytes.bytes("files/photos/nope.jpg"))
    }

    @Test
    fun cancellationIsNotMistakenForAMissingPhoto() = runTest {
        val photoBytes =
            PhotoBytes(StandardTestDispatcher(testScheduler)) {
                throw CancellationException("gone")
            }
        assertFailsWith<CancellationException> { photoBytes.bytes("files/photos/x.jpg") }
    }
}
