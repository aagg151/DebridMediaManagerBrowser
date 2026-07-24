package com.debrid.browser.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONObject

/**
 * Local "collections" (folders) for organizing Real-Debrid torrents.
 *
 * Real-Debrid has no folder concept in its API, so collections are stored on-device:
 * a map of collection name -> set of torrent ids. Deleting a torrent elsewhere just
 * leaves a stale id here, which is harmless (filtering intersects with the live list).
 */
class CollectionsStore(context: Context) {

    private val sp = context.getSharedPreferences("dmm_collections", Context.MODE_PRIVATE)

    /** All collection names, sorted alphabetically. */
    fun names(): List<String> = load().keys().asSequence().toList().sorted()

    /** Torrent ids in a collection. */
    fun idsIn(name: String): Set<String> {
        val obj = load().optJSONArray(name) ?: return emptySet()
        return (0 until obj.length()).mapTo(mutableSetOf()) { obj.getString(it) }
    }

    fun createCollection(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        val json = load()
        if (!json.has(clean)) {
            json.put(clean, org.json.JSONArray())
            save(json)
        }
    }

    fun deleteCollection(name: String) {
        val json = load()
        json.remove(name)
        save(json)
    }

    fun add(name: String, torrentId: String) {
        val json = load()
        val arr = json.optJSONArray(name) ?: org.json.JSONArray().also { json.put(name, it) }
        val existing = (0 until arr.length()).map { arr.getString(it) }
        if (torrentId !in existing) arr.put(torrentId)
        save(json)
    }

    fun remove(name: String, torrentId: String) {
        val json = load()
        val arr = json.optJSONArray(name) ?: return
        val kept = org.json.JSONArray()
        for (i in 0 until arr.length()) {
            val id = arr.getString(i)
            if (id != torrentId) kept.put(id)
        }
        json.put(name, kept)
        save(json)
    }

    /** Collections that currently contain the given torrent id. */
    fun collectionsFor(torrentId: String): List<String> =
        names().filter { torrentId in idsIn(it) }

    private fun load(): JSONObject =
        runCatching { JSONObject(sp.getString(KEY, "{}") ?: "{}") }.getOrDefault(JSONObject())

    private fun save(json: JSONObject) = sp.edit { putString(KEY, json.toString()) }

    companion object {
        private const val KEY = "collections_json"
    }
}
