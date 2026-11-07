package com.tennisbuddy.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tennisbuddy.shared.MatchController
import com.tennisbuddy.shared.MatchRepository
import com.tennisbuddy.shared.MatchStorage
import com.tennisbuddy.shared.WatchProtocol
import com.tennisbuddy.shared.watchSnapshot
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File

/** Keeps the match controller alive across rotation and stores the register in the app's private files. */
class MatchViewModel(app: Application) : AndroidViewModel(app) {
    val controller = MatchController(
        repository = MatchRepository(FileMatchStorage(File(app.filesDir, "tennisbuddy.json"))),
        nowMillis = { System.currentTimeMillis() },
    )

    /** Swap in a real implementation to show and score the match on a smartwatch. */
    private val watch: WatchBridge = NoWatchBridge

    init {
        // Phone -> watch: push the current match whenever it changes.
        viewModelScope.launch {
            controller.data
                .map { WatchProtocol.encode(watchSnapshot(it.current)) }
                .collect { watch.sendSnapshot(it) }
        }
        // Watch -> phone: run commands on the main thread, where the controller lives.
        watch.setCommandListener { command -> viewModelScope.launch { controller.apply(command) } }
    }

    override fun onCleared() {
        watch.close()
    }
}

class FileMatchStorage(private val file: File) : MatchStorage {
    override fun read(): String? = if (file.exists()) file.readText() else null

    override fun write(text: String) {
        // Write to a temp file then rename, so a crash mid-write cannot corrupt the register.
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) {
            file.writeText(text)
            tmp.delete()
        }
    }
}
