package com.improvingmuslim.android.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Calendar

/**
 * Daily learning streak, saved locally (SharedPreferences). Mirrors the website's core
 * streak: real lecture playback time counts toward a fixed daily goal; hitting the goal
 * extends the streak, and a missed day resets it. Local-only for now (like notes/progress);
 * can sync to an account later. Freezes, ranks, and the leaderboard are intentionally not
 * ported yet (the leaderboard needs accounts).
 */
object StreakStore {
    private const val PREFS = "study_streak"
    private const val KEY = "state"
    const val TARGET_MINUTES = 15
    private const val TARGET_SECONDS = TARGET_MINUTES * 60
    private const val RETAIN_DAYS = 120

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class DayRecord(val seconds: Int = 0, val completed: Boolean = false)

    @Serializable
    data class State(
        val todayDate: String = "",
        val todaySeconds: Int = 0,
        val current: Int = 0,
        val best: Int = 0,
        val lastCompletedDate: String = "",
        val days: Map<String, DayRecord> = emptyMap(),
    ) {
        val completedToday: Boolean get() = todaySeconds >= TARGET_SECONDS || lastCompletedDate == todayDate
        val todayMinutes: Int get() = todaySeconds / 60
        val progress: Float get() = (todaySeconds.toFloat() / TARGET_SECONDS).coerceIn(0f, 1f)
        val minutesLeft: Int get() = ((TARGET_SECONDS - todaySeconds + 59) / 60).coerceAtLeast(0)
    }

    /** Bumped on every change so the header flame and panel recompose. */
    var revision by mutableIntStateOf(0)
        private set

    /** Current streak, normalized for the calendar (a missed day reads as reset to 0). */
    fun read(context: Context): State = normalize(load(context))

    /** Adds real playback [seconds] toward today's goal, extending the streak on completion. */
    fun recordSeconds(context: Context, seconds: Int): State {
        if (seconds <= 0) return read(context)
        val today = todayKey()
        val base = normalize(load(context))
        val wasComplete = base.completedToday
        val todaySeconds = base.todaySeconds + seconds
        var current = base.current
        var best = base.best
        var lastCompleted = base.lastCompletedDate
        if (!wasComplete && todaySeconds >= TARGET_SECONDS) {
            // Continue yesterday's streak, otherwise start a fresh one at 1.
            current = if (lastCompleted == yesterdayKey()) current + 1 else 1
            best = maxOf(best, current)
            lastCompleted = today
        }
        val completedToday = todaySeconds >= TARGET_SECONDS || lastCompleted == today
        val days = base.days.toMutableMap().apply { put(today, DayRecord(todaySeconds, completedToday)) }
        val next = base.copy(
            todayDate = today,
            todaySeconds = todaySeconds,
            current = current,
            best = best,
            lastCompletedDate = lastCompleted,
            days = trim(days),
        )
        persist(context, next)
        revision++
        return next
    }

    /** Rolls the day over: today's seconds reset, and the streak survives only if the last
     *  completed day was today or yesterday. */
    private fun normalize(s: State): State {
        val today = todayKey()
        if (s.todayDate == today) return s
        val continuous = s.lastCompletedDate == today || s.lastCompletedDate == yesterdayKey()
        return s.copy(todayDate = today, todaySeconds = 0, current = if (continuous) s.current else 0)
    }

    private fun trim(days: Map<String, DayRecord>): Map<String, DayRecord> =
        if (days.size <= RETAIN_DAYS) days
        else days.entries.sortedByDescending { it.key }.take(RETAIN_DAYS).associate { it.key to it.value }

    private fun load(context: Context): State {
        val raw = prefs(context).getString(KEY, null) ?: return State()
        return try {
            json.decodeFromString(State.serializer(), raw)
        } catch (e: Exception) {
            State()
        }
    }

    private fun persist(context: Context, state: State) {
        prefs(context).edit().putString(KEY, json.encodeToString(State.serializer(), state)).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun todayKey(): String = dateKey(Calendar.getInstance())
    private fun yesterdayKey(): String =
        dateKey(Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) })

    private fun dateKey(c: Calendar): String =
        "%04d-%02d-%02d".format(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
}
