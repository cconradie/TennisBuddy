package com.tennisbuddy.android

import com.tennisbuddy.shared.WatchCommand

/**
 * The seam between the phone app and a smartwatch link. An implementation (for example one
 * built on the Huawei Wear Engine SDK, see docs/watch/huawei-gt2.md) sends match snapshots
 * to the watch and reports button presses made on it.
 */
interface WatchBridge {
    /** Called whenever the match changes. [snapshotJson] is a `WatchProtocol.encode(...)` string. */
    fun sendSnapshot(snapshotJson: String)

    /** Register the function to call when the watch sends a command. May be called from any thread. */
    fun setCommandListener(listener: (WatchCommand) -> Unit)

    fun close()
}

/** Used until a real watch link is added: does nothing. */
object NoWatchBridge : WatchBridge {
    override fun sendSnapshot(snapshotJson: String) = Unit
    override fun setCommandListener(listener: (WatchCommand) -> Unit) = Unit
    override fun close() = Unit
}
