package dev.dimvlachos.lab.core.presentation.components.frenchpress

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine

/**
 * Something that finishes once, as a prototype's promise does: [await] suspends until it is
 * [resolve]d, and what waits on it goes on when its [PressScheduler] next runs what is due.
 */
internal class PressJob<T> {
    var done = false
        private set

    private var result: T? = null
    private val waiting = ArrayList<Continuation<T>>(1)
    internal var scheduler: PressScheduler? = null

    fun resolve(value: T) {
        if (done) return
        done = true
        result = value
        val s = scheduler
        for (c in waiting) s?.queue { c.resume(value) }
        waiting.clear()
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun await(): T {
        if (done) return result as T
        return suspendCoroutine { waiting += it }
    }
}

/**
 * The prototype's clock and its `later()`, `animate()`, `wait()` and `until()`, on a simulated time
 * that only [advance] moves, so a sequence runs the same on every device and in tests. Its `async`
 * functions are `suspend` functions started with [launch]; what a resolved promise would run next
 * is queued and run by [drain], after the frame's work, as the browser runs microtasks. Nothing
 * here needs a dispatcher: everything runs on the thread that advances it.
 */
internal class PressScheduler {
    /** The simulated time, in ms. */
    var now = 0.0
        private set

    /** Whether reduced motion is on: an animation then jumps to its end at once. */
    var reduced = false

    private class Timer(val at: Double, val fn: () -> Unit)

    private class Anim(
        val t0: Double,
        val ms: Double,
        val fn: (Float) -> Unit,
        val done: PressJob<Unit>,
    )

    private class Waiter(val cond: () -> Boolean, val done: PressJob<Unit>)

    private val timers = ArrayList<Timer>()
    private val anims = ArrayList<Anim>()
    private val waiters = ArrayList<Waiter>()
    private val micro = ArrayDeque<() -> Unit>()

    /** Whether anything is waiting to happen: a timer, an animation, a condition or a step. */
    val busy: Boolean
        get() =
            timers.isNotEmpty() || anims.isNotEmpty() || waiters.isNotEmpty() || micro.isNotEmpty()

    internal fun queue(fn: () -> Unit) {
        micro.addLast(fn)
    }

    fun <T> job(): PressJob<T> = PressJob<T>().also { it.scheduler = this }

    /** The prototype's `later(ms, fn)`. */
    fun later(ms: Double, fn: () -> Unit) {
        timers += Timer(now + ms, fn)
    }

    /** The prototype's `animate(ms, fn)`, not awaited: [fn] gets 0 to 1 every frame for [ms]. */
    fun animateJob(ms: Double, fn: (Float) -> Unit): PressJob<Unit> {
        val j = job<Unit>()
        if (reduced) {
            fn(1f)
            j.resolve(Unit)
        } else {
            anims += Anim(now, ms, fn, j)
        }
        return j
    }

    suspend fun animate(ms: Double, fn: (Float) -> Unit) = animateJob(ms, fn).await()

    fun waitJob(ms: Double): PressJob<Unit> {
        val j = job<Unit>()
        later(ms) { j.resolve(Unit) }
        return j
    }

    suspend fun wait(ms: Double) = waitJob(ms).await()

    suspend fun until(cond: () -> Boolean) {
        if (cond()) return
        val j = job<Unit>()
        waiters += Waiter(cond, j)
        j.await()
    }

    /** Starts [block] now, running until it first waits, as calling an `async` function does. */
    fun launch(block: suspend () -> Unit) {
        block.startCoroutine(Continuation(EmptyCoroutineContext) { it.getOrThrow() })
    }

    /** Starts [block] and gives back what it returns once it has, as an `async` call's promise. */
    fun <T> async(block: suspend () -> T): PressJob<T> {
        val j = job<T>()
        block.startCoroutine(Continuation(EmptyCoroutineContext) { j.resolve(it.getOrThrow()) })
        return j
    }

    /** Moves the clock on by [ms]: the timers due fire, then every animation is set. */
    fun tick(ms: Double) {
        now += ms
        var i = timers.size - 1
        while (i >= 0) {
            if (i < timers.size && timers[i].at <= now) timers.removeAt(i).fn()
            i--
        }
        i = anims.size - 1
        while (i >= 0) {
            if (i < anims.size) {
                val a = anims[i]
                val p = ((now - a.t0) / a.ms).coerceIn(0.0, 1.0).toFloat()
                a.fn(p)
                if (p >= 1f) {
                    anims.remove(a)
                    a.done.resolve(Unit)
                }
            }
            i--
        }
    }

    /** Resolves the conditions that now hold, as the prototype's waiters after each frame. */
    fun checkWaiters() {
        var i = waiters.size - 1
        while (i >= 0) {
            if (i < waiters.size && waiters[i].cond()) waiters.removeAt(i).done.resolve(Unit)
            i--
        }
    }

    /** Runs what resolved promises queued, and what that queues in turn, until nothing is left. */
    fun drain() {
        while (micro.isNotEmpty()) micro.removeFirst()()
    }

    /** Drops every timer, animation and waiter, as the prototype's `reset()`. */
    fun clear() {
        timers.clear()
        anims.clear()
        waiters.clear()
        micro.clear()
    }
}
