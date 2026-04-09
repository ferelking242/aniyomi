package eu.kanade.tachiyomi.source.internal

import logcat.LogPriority
import logcat.logcat
import java.util.concurrent.ConcurrentHashMap

object InternalSourceRegistry {

    private val sources = ConcurrentHashMap<Long, InternalSource>()

    fun register(source: InternalSource) {
        if (sources.containsKey(source.id)) {
            logcat(LogPriority.WARN) { "Internal source already registered: ${source.name} (${source.id})" }
            return
        }
        sources[source.id] = source
        logcat { "Registered internal source: ${source.name} (${source.id})" }
    }

    fun unregister(sourceId: Long) {
        sources.remove(sourceId)
    }

    fun get(sourceId: Long): InternalSource? = sources[sourceId]

    fun getAll(): List<InternalSource> = sources.values.toList()

    fun contains(sourceId: Long): Boolean = sources.containsKey(sourceId)

    fun count(): Int = sources.size
}
