package dev.dimvlachos.moodboard.data

/** Not thread-safe: callers stay on the main thread. */
internal class LruCache<K, V>(private val capacity: Int) {
    private val entries = LinkedHashMap<K, V>()

    operator fun get(key: K): V? {
        val value = entries.remove(key) ?: return null
        entries[key] = value
        return value
    }

    operator fun set(key: K, value: V) {
        entries.remove(key)
        entries[key] = value
        if (entries.size > capacity) entries.remove(entries.keys.first())
    }

    inline fun getOrPut(key: K, compute: () -> V): V = get(key) ?: compute().also { set(key, it) }
}
