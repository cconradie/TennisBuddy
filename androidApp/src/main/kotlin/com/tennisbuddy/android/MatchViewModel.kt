package com.tennisbuddy.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.tennisbuddy.shared.MatchController
import com.tennisbuddy.shared.MatchRepository
import com.tennisbuddy.shared.MatchStorage
import java.io.File

/** Keeps the match controller alive across rotation and stores the register in the app's private files. */
class MatchViewModel(app: Application) : AndroidViewModel(app) {
    val controller = MatchController(
        repository = MatchRepository(FileMatchStorage(File(app.filesDir, "tennisbuddy.json"))),
        nowMillis = { System.currentTimeMillis() },
    )
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
