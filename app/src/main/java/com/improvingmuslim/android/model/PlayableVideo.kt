package com.improvingmuslim.android.model

/**
 * A single video ready to play on the Watch screen, flattened from either a standalone
 * lecture or the first available episode of a series.
 */
data class PlayableVideo(
    val id: String,
    val title: String,
    val speaker: String,
    /** Topic for a standalone lecture, or "Series title · Episode N" for an episode. */
    val contextLabel: String?,
    /** Human date like "Feb 8, 2021". */
    val publishedLabel: String?,
    val videoURL: String,
    val captionsURL: String?,
    val description: String?,
    val takeaways: List<String>,
    val recap: String?,
)

/**
 * Turns a home-feed card into a playable video, or null if nothing is playable yet
 * (e.g. a series whose episodes haven't been uploaded).
 */
fun HomeFeedItem.toPlayableVideo(): PlayableVideo? = when (this) {
    is HomeFeedItem.LectureItem -> {
        val url = lecture.videoURL ?: return null
        PlayableVideo(
            id = "lecture:${lecture.id}",
            title = lecture.title,
            speaker = lecture.speaker,
            contextLabel = lecture.topic,
            publishedLabel = formatPublished(lecture.published),
            videoURL = url,
            captionsURL = lecture.captionsURL,
            description = lecture.description,
            takeaways = lecture.takeaways,
            recap = lecture.recap,
        )
    }

    is HomeFeedItem.SeriesItem -> {
        val episode = series.episodes.firstOrNull { it.isAvailable } ?: return null
        val url = episode.videoURL ?: return null
        PlayableVideo(
            id = "episode:${series.id}:${episode.id}",
            title = episode.title,
            speaker = series.speaker,
            contextLabel = "${series.title} · Episode ${episode.number}",
            publishedLabel = formatPublished(episode.published),
            videoURL = url,
            captionsURL = episode.captionsURL,
            description = episode.description,
            takeaways = episode.takeaways,
            recap = episode.recap,
        )
    }
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
