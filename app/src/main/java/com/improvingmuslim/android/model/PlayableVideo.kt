package com.improvingmuslim.android.model

/**
 * A single video ready to play on the Watch screen, flattened from either a standalone
 * lecture or an episode of a series.
 */
data class PlayableVideo(
    val id: String,
    val title: String,
    val speaker: String,
    /** Topic for a standalone lecture, or "Series title · Episode N" for an episode. */
    val contextLabel: String?,
    /** Human date like "Feb 8, 2021". */
    val publishedLabel: String?,
    val thumbnailURL: String?,
    /** Formatted runtime like "13:34", or null if unknown. */
    val durationLabel: String?,
    /** Topic IDs, used to find related videos. */
    val categories: List<String>,
    val videoURL: String,
    val captionsURL: String?,
    val description: String?,
    val takeaways: List<String>,
    val recap: String?,
)

/** The current video plus what to watch next. */
data class WatchBundle(
    val video: PlayableVideo,
    val upNext: PlayableVideo?,
    val related: List<PlayableVideo>,
)

fun StandaloneLecture.toPlayableVideo(): PlayableVideo? {
    val url = videoURL ?: return null
    return PlayableVideo(
        id = "lecture:$id",
        title = title,
        speaker = speaker,
        contextLabel = topic,
        publishedLabel = formatPublished(published),
        thumbnailURL = thumbnailURL,
        durationLabel = duration?.let { formatDuration(it) },
        categories = categories,
        videoURL = url,
        captionsURL = captionsURL,
        description = description,
        takeaways = takeaways,
        recap = recap,
    )
}

fun episodeToPlayableVideo(series: LectureSeries, episode: Episode): PlayableVideo? {
    val url = episode.videoURL ?: return null
    return PlayableVideo(
        id = "episode:${series.id}:${episode.id}",
        title = episode.title,
        speaker = series.speaker,
        contextLabel = "${series.title} · Episode ${episode.number}",
        publishedLabel = formatPublished(episode.published),
        thumbnailURL = episode.thumbnailURL ?: series.thumbnailURL,
        durationLabel = episode.duration?.let { formatDuration(it) },
        categories = series.categories,
        videoURL = url,
        captionsURL = episode.captionsURL,
        description = episode.description,
        takeaways = episode.takeaways,
        recap = episode.recap,
    )
}

/** Every playable video in the catalog (all available episodes + standalone lectures). */
fun Catalog.allPlayable(): List<PlayableVideo> {
    val episodes = series.flatMap { s ->
        s.episodes.filter { it.isAvailable }.mapNotNull { episodeToPlayableVideo(s, it) }
    }
    val standalone = standaloneLectures.filter { it.isAvailable }.mapNotNull { it.toPlayableVideo() }
    return episodes + standalone
}

/**
 * Builds what the Watch screen needs for [key]: the video itself, the next episode (for a
 * series) or a related pick, and a list of "more like this" sharing a topic.
 */
fun Catalog.buildWatchBundle(key: String): WatchBundle? {
    val pool = allPlayable()
    val current = pool.firstOrNull { it.id == key } ?: return null

    // Prefer the literal next episode when watching a series.
    val nextEpisode = nextEpisodeAfter(key)

    val sameTopic = pool.filter { candidate ->
        candidate.id != current.id &&
            candidate.id != nextEpisode?.id &&
            candidate.categories.any { it in current.categories }
    }

    // Once a series ends (no next episode), "up next" should suggest a video outside that
    // series rather than looping back to an earlier episode of the one just finished.
    val currentSeriesId = key.takeIf { it.startsWith("episode:") }?.split(":")?.getOrNull(1)
    val fallback = sameTopic.firstOrNull {
        currentSeriesId == null || !it.id.startsWith("episode:$currentSeriesId:")
    }

    val upNext = nextEpisode ?: fallback
    val related = sameTopic.filter { it.id != upNext?.id }.take(6)

    return WatchBundle(video = current, upNext = upNext, related = related)
}

private fun Catalog.nextEpisodeAfter(key: String): PlayableVideo? {
    if (!key.startsWith("episode:")) return null
    val parts = key.split(":")
    if (parts.size != 3) return null
    val seriesId = parts[1]
    val episodeId = parts[2]
    val s = series.firstOrNull { it.id == seriesId } ?: return null
    val available = s.episodes.filter { it.isAvailable }
    val index = available.indexOfFirst { it.id == episodeId }
    if (index == -1 || index + 1 >= available.size) return null
    return episodeToPlayableVideo(s, available[index + 1])
}

private val MONTHS = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

/** "2021-02-08" -> "Feb 8, 2021". Returns null if it can't be parsed. */
fun formatPublished(published: String?): String? {
    if (published.isNullOrBlank()) return null
    val parts = published.split("-")
    if (parts.size != 3) return published
    val year = parts[0]
    val month = parts[1].toIntOrNull() ?: return published
    val day = parts[2].toIntOrNull() ?: return published
    if (month !in 1..12) return published
    return "${MONTHS[month - 1]} $day, $year"
}
