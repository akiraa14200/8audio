package com.akiraa14200.eightaudio

import android.content.Context

/** Small, offline-first persistence layer. No account or server required. */
class AudioPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("eight_audio_preferences", Context.MODE_PRIVATE)

    fun isFavorite(id: String) = prefs.getStringSet("favorites", emptySet()).orEmpty().contains(id)

    fun toggleFavorite(id: String): Boolean {
        val ids = prefs.getStringSet("favorites", emptySet()).orEmpty().toMutableSet()
        val nowFavorite = if (ids.remove(id)) false else { ids.add(id); true }
        prefs.edit().putStringSet("favorites", ids).apply()
        return nowFavorite
    }

    fun favoriteIds(): Set<String> = prefs.getStringSet("favorites", emptySet()).orEmpty().toSet()

    fun getBoolean(key: String, default: Boolean = false) = prefs.getBoolean(key, default)
    fun setBoolean(key: String, value: Boolean) { prefs.edit().putBoolean(key, value).apply() }

    fun saveRecent(track: Track) {
        val encoded = listOf(track.id, track.title, track.artist, track.artwork, track.streamUrl)
            .joinToString("\u001f")
        val list = prefs.getString("recent_tracks", "").orEmpty().split("\u001e")
            .filter { it.isNotBlank() && !it.startsWith(track.id + "\u001f") }
            .toMutableList()
        list.add(0, encoded)
        prefs.edit().putString("recent_tracks", list.take(100).joinToString("\u001e")).apply()
    }

    fun recentTracks(): List<Track> = prefs.getString("recent_tracks", "").orEmpty()
        .split("\u001e").mapNotNull { row ->
            val p = row.split("\u001f")
            if (p.size == 5) Track(p[0], p[1], p[2], p[3], p[4]) else null
        }
}
