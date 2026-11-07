package com.tennisbuddy.shared

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Platform-specific text storage (a file on Android, UserDefaults on iOS). */
interface MatchStorage {
    fun read(): String?
    fun write(text: String)
}

class InMemoryStorage(private var text: String? = null) : MatchStorage {
    override fun read(): String? = text
    override fun write(text: String) {
        this.text = text
    }
}

/** Everything saved on the device: the match being played, and the register of finished or ended matches. */
@Serializable
data class StoreData(
    val current: MatchRecord? = null,
    val history: List<MatchRecord> = emptyList(),
    /** The device owner's name; they are always side A ("you") in the register and statistics. */
    val profileName: String = "",
)

class MatchRepository(private val storage: MatchStorage) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** Never throws: a missing or unreadable file gives an empty store. */
    fun load(): StoreData = try {
        storage.read()?.let { json.decodeFromString(StoreData.serializer(), it) } ?: StoreData()
    } catch (e: Exception) {
        StoreData()
    }

    fun save(data: StoreData) {
        storage.write(json.encodeToString(StoreData.serializer(), data))
    }
}
