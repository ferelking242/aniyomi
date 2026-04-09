package eu.kanade.tachiyomi.source.internal

import logcat.logcat

object InternalSourceLoader {

    fun loadAll(vararg sources: InternalSource) {
        sources.forEach { source ->
            InternalSourceRegistry.register(source)
        }
        logcat { "Loaded ${sources.size} internal source(s)" }
    }

    fun adapters(): List<InternalSourceAdapter> {
        return InternalSourceRegistry.getAll().map { InternalSourceAdapter(it) }
    }
}
