package dev.dimvlachos.lab.core.demo

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import kotlin.time.Duration
import org.jetbrains.compose.resources.StringResource

/**
 * A demo's scripted state; [recording] when the recorder is capturing it as a clip, [replay] while
 * the user watches the script again, until they stop it.
 */
@Stable
internal class DemoState(val recording: Boolean = false, replay: Boolean = false) : DemoController {
    override var selectedIndex: Int by mutableIntStateOf(0)
        private set

    var replay: Boolean by mutableStateOf(replay)
        private set

    /** Stopped: the demo is the user's to play with again. */
    fun endReplay() {
        replay = false
    }

    private var scrollHandler: (suspend (Float) -> Unit)? = null
    private var pageDragHandler: (suspend (PageDrag) -> Unit)? = null
    private var wipeHandler: (suspend (List<Offset>, Duration) -> Unit)? = null
    private var dripHandler: (suspend (Offset, Float) -> Unit)? = null
    private var mistHandler: (suspend (Duration) -> Unit)? = null
    private var sendHandler: (suspend (StringResource, Duration) -> Unit)? = null
    private var clearHandler: (suspend () -> Unit)? = null
    private var cordHandler: (suspend (Dp, Duration, Dp) -> Unit)? = null
    private var magnetDragHandler: (suspend (String, List<Offset>, Duration) -> Unit)? = null
    private var magnetReleaseHandler: (suspend (String, Duration) -> Unit)? = null
    private var magnetTapHandler: (suspend (String) -> Unit)? = null
    private var photoTapHandler: (suspend (String) -> Unit)? = null
    private var refreshPullHandler: (suspend (Dp, Duration) -> Unit)? = null

    override fun select(index: Int) {
        selectedIndex = index
    }

    override suspend fun scrollBy(px: Float) {
        scrollHandler?.invoke(px)
    }

    override suspend fun dragPage(drag: PageDrag) {
        pageDragHandler?.invoke(drag)
    }

    override suspend fun wipe(path: List<Offset>, duration: Duration) {
        wipeHandler?.invoke(path, duration)
    }

    override suspend fun drip(at: Offset, length: Float) {
        dripHandler?.invoke(at, length)
    }

    override suspend fun mist(duration: Duration) {
        mistHandler?.invoke(duration)
    }

    override suspend fun sendMessage(text: StringResource, typing: Duration) {
        sendHandler?.invoke(text, typing)
    }

    override suspend fun clearMessages() {
        clearHandler?.invoke()
    }

    override suspend fun pullCord(down: Dp, duration: Duration, across: Dp) {
        cordHandler?.invoke(down, duration, across)
    }

    override suspend fun dragMagnet(tag: String, path: List<Offset>, duration: Duration) {
        magnetDragHandler?.invoke(tag, path, duration)
    }

    override suspend fun releaseMagnet(tag: String, duration: Duration) {
        magnetReleaseHandler?.invoke(tag, duration)
    }

    override suspend fun tapMagnet(tag: String) {
        magnetTapHandler?.invoke(tag)
    }

    override suspend fun tapPhoto(id: String) {
        photoTapHandler?.invoke(id)
    }

    override suspend fun pullToRefresh(distance: Dp, duration: Duration) {
        refreshPullHandler?.invoke(distance, duration)
    }

    fun setScrollHandler(handler: (suspend (Float) -> Unit)?) {
        scrollHandler = handler
    }

    fun setPageDragHandler(handler: (suspend (PageDrag) -> Unit)?) {
        pageDragHandler = handler
    }

    fun setWipeHandler(handler: (suspend (List<Offset>, Duration) -> Unit)?) {
        wipeHandler = handler
    }

    fun setDripHandler(handler: (suspend (Offset, Float) -> Unit)?) {
        dripHandler = handler
    }

    fun setMistHandler(handler: (suspend (Duration) -> Unit)?) {
        mistHandler = handler
    }

    fun setSendHandler(handler: (suspend (StringResource, Duration) -> Unit)?) {
        sendHandler = handler
    }

    fun setClearHandler(handler: (suspend () -> Unit)?) {
        clearHandler = handler
    }

    fun setCordHandler(handler: (suspend (Dp, Duration, Dp) -> Unit)?) {
        cordHandler = handler
    }

    fun setMagnetDragHandler(handler: (suspend (String, List<Offset>, Duration) -> Unit)?) {
        magnetDragHandler = handler
    }

    fun setMagnetReleaseHandler(handler: (suspend (String, Duration) -> Unit)?) {
        magnetReleaseHandler = handler
    }

    fun setMagnetTapHandler(handler: (suspend (String) -> Unit)?) {
        magnetTapHandler = handler
    }

    fun setPhotoTapHandler(handler: (suspend (String) -> Unit)?) {
        photoTapHandler = handler
    }

    fun setRefreshPullHandler(handler: (suspend (Dp, Duration) -> Unit)?) {
        refreshPullHandler = handler
    }
}
