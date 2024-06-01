package com.grappenmaker.rawinput

import net.java.games.input.ControllerEnvironment
import net.java.games.input.Mouse
import java.util.concurrent.atomic.AtomicBoolean

object RawInputThread : Thread("AsyncRawInput") {
    init {
        isDaemon = true
    }

    // TODO: reconsider whether atomicity is required
    @JvmStatic
    @Volatile
    var dx = 0f
        get() {
            val temp = field
            field = 0f
            return temp
        }

    @JvmStatic
    @Volatile
    var dy = 0f
        get() {
            val temp = field
            field = 0f
            return temp
        }

    @JvmStatic
    @Suppress("unused")
    fun resetMouse() {
        dx = 0f
        dy = 0f
    }

    private var mice: List<Mouse> = emptyList()
    private var shouldRescan = AtomicBoolean(false)

    private fun rescanMice() = shouldRescan.set(true)

    private fun doRescanMice() {
        // clear the mice first to allow the garbage collector to finalize them
        // there is no guarantee that this happens, but it's not like we can manually
        // unacquire the mice objects anyway
        mice = emptyList()

        val env = Class.forName("net.java.games.input.DefaultControllerEnvironment")
            .getDeclaredConstructor().also { it.isAccessible = true }.newInstance() as ControllerEnvironment

        mice = env.controllers.filterIsInstance<Mouse>()
    }

    private var backoffMs = 100L
    private const val maxBackoff = 5000L

    override fun run() {
        rescanMice()

        // TODO: extract to separate method, so we do not need the labeled while-block?
        outer@ while (true) {
            if (shouldRescan.compareAndSet(true, false)) doRescanMice()

            for (mouse in mice) {
                if (!mouse.poll()) {
                    rescanMice()
                    continue@outer
                }

                // TODO: since about the same code is in place for handling the events sent by winapi/lwjgl/whatever,
                // TODO: should we consider attempting to remove this layer of indirection?
                dx += mouse.x.pollData

                // Minecraft expects a y-flipped input (kind of weird how in game dev, there is consensus on which dir
                // is the x-axis, whereas for the y-axis, different conventions apply?)
                dy -= mouse.y.pollData
            }

            if (mice.isEmpty()) {
                sleep(backoffMs)
                backoffMs = (2L * backoffMs).coerceAtMost(maxBackoff)
                rescanMice()
            }

            sleep(1)
        }
    }
}