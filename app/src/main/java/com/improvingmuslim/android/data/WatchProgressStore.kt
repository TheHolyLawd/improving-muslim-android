package com.improvingmuslim.android.data

import android.content.Context

/**
 * Per-video playback progress saved locally (SharedPreferences), keyed by PlayableVideo id.
 * Powers the home "Continue learning" card and lets the Watch screen resume where the viewer
 * left off. Local-only for now; can sync to an account later (like [NotesStore]).
 */
object WatchProgressStore {
    private const val PREFS = "watch_progress"
    private const val COMPLETE_RATIO = 0.98f

    data class Progress(
        val key: String,
        val positionMs: Long,
        val durationMs: Long,
        val updatedAt: Long,
        val completed: Boolean,
    ) {
        val percent: Float
            get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

        /** Worth resuming: started, not finished, not essentially at the end. */
        val isResumable: Boolean get() = !completed && positionMs > 0 && percent < COMPLETE_RATIO
    }

    /** Records progress. A near-the-end or explicitly [ended] video is marked completed so it
     *  falls off the Continue learning shelf. No-ops until the duration is known. */
    fun save(context: Context, key: String, positionMs: Long, durationMs: Long, ended: Boolean) {
        if (durationMs <= 0L) return
        val completed = ended || positionMs.toFloat() / durationMs >= COMPLETE_RATIO
        prefs(context).edit()
            .putString(key, "$positionMs|$durationMs|${System.currentTimeMillis()}|$completed")
            .apply()
    }

    fun get(context: Context, key: String): Progress? = parse(key, prefs(context).getString(key, null))

    /** The most recently watched video that's still worth resuming, or null. */
    fun mostRecentResumable(context: Context): Progress? =
        all(context).firstOrNull { it.isResumable }

    /** Every recorded video (finished and unfinished), most recent first. */
    fun all(context: Context): List<Progress> =
        prefs(context).all.keys
            .mapNotNull { get(context, it) }
            .sortedByDescending { it.updatedAt }

    fun remove(context: Context, key: String) {
        prefs(context).edit().remove(key).apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun parse(key: String, raw: String?): Progress? {
        val parts = raw?.split("|") ?: return null
        if (parts.size != 4) return null
        return Progress(
            key = key,
            positionMs = parts[0].toLongOrNull() ?: return null,
            durationMs = parts[1].toLongOrNull() ?: return null,
            updatedAt = parts[2].toLongOrNull() ?: return null,
            completed = parts[3].toBoolean(),
        )
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
