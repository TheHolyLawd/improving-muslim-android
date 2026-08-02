package com.improvingmuslim.android.data

import android.content.Context

/**
 * Local, per-lecture notes storage backed by SharedPreferences (keyed by video id).
 * Simple and offline; can be upgraded to cloud sync when accounts exist.
 */
object NotesStore {
    private const val PREFS = "lecture_notes"

    fun load(context: Context, videoId: String): String =
        prefs(context).getString(videoId, "").orEmpty()

    fun save(context: Context, videoId: String, text: String) {
        prefs(context).edit().apply {
            if (text.isBlank()) remove(videoId) else putString(videoId, text)
            apply()
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
