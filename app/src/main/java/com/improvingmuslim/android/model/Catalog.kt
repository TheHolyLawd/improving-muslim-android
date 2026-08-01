package com.improvingmuslim.android.model

import kotlinx.serialization.Serializable

@Serializable
data class Catalog(
    val schemaVersion: Int,
    val catalogVersion: String,
    val counts: CatalogCounts,
    val topics: List<Topic> = emptyList(),
    val speakers: List<Speaker> = emptyList(),
    val series: List<LectureSeries> = emptyList(),
    val standaloneLectures: List<StandaloneLecture> = emptyList(),
)

@Serializable
data class CatalogCounts(
    val series: Int,
    val speakers: Int,
    val availableLectures: Int,
)

@Serializable
data class Topic(
    val id: String,
    val name: String,
    val description: String,
    val aliases: List<String> = emptyList(),
)

@Serializable
data class Speaker(
    val id: String,
    val name: String,
    val imageURL: String? = null,
    val bio: String,
)

@Serializable
data class LectureSeries(
    val id: String,
    val title: String,
    val speakerID: String? = null,
    val speaker: String,
    val topic: String? = null,
    val categories: List<String> = emptyList(),
    val label: String? = null,
    val description: String? = null,
    val thumbnailURL: String? = null,
    val playlistID: String,
    val availableCount: Int,
    val episodeCount: Int,
    val episodes: List<Episode> = emptyList(),
)

@Serializable
data class Episode(
    val id: String,
    val number: Int,
    val title: String,
    val published: String? = null,
    val duration: Int? = null,
    val views: Int? = null,
    val thumbnailURL: String? = null,
    val videoURL: String? = null,
    val captionsURL: String? = null,
    @Serializable(with = LenientNullableStringSerializer::class)
    val statusNote: String? = null,
    @Serializable(with = LenientNullableStringSerializer::class)
    val description: String? = null,
    val takeaways: List<String> = emptyList(),
    @Serializable(with = LenientNullableStringSerializer::class)
    val recap: String? = null,
    val grammarNotes: List<String> = emptyList(),
) {
    val isAvailable: Boolean get() = videoURL != null
}

@Serializable
data class StandaloneLecture(
    val id: String,
    val title: String,
    val speakerID: String? = null,
    val speaker: String,
    val topic: String? = null,
    val categories: List<String> = emptyList(),
    val typeLabel: String,
    val published: String? = null,
    val duration: Int? = null,
    val views: Int? = null,
    val thumbnailURL: String? = null,
    val videoURL: String? = null,
    val captionsURL: String? = null,
    @Serializable(with = LenientNullableStringSerializer::class)
    val description: String? = null,
    val takeaways: List<String> = emptyList(),
    @Serializable(with = LenientNullableStringSerializer::class)
    val recap: String? = null,
    val grammarNotes: List<String> = emptyList(),
) {
    val isAvailable: Boolean get() = videoURL != null
}

/**
 * One card in the home feed. Mirrors the website: a whole series shows as a single
 * card with an episode count, while a standalone lecture shows with its duration.
 */
sealed class HomeFeedItem {
    abstract val id: String
    abstract val title: String
    abstract val speaker: String
    abstract val thumbnailURL: String?
    abstract val categories: List<String>
    /** e.g. "SERIES · SEERAH, PROPHETS" or "LECTURE · QURAN". */
    abstract val categoryLabel: String
    /** Bottom-right badge: episode count for a series, duration for a lecture. */
    abstract val badge: String?
    /** A view metric used only for the "Most viewed" sort. */
    abstract val sortViews: Long

    data class SeriesItem(
        val series: LectureSeries,
        override val categoryLabel: String,
    ) : HomeFeedItem() {
        override val id: String = "series:${series.id}"
        override val title: String = series.title
        override val speaker: String = series.speaker
        override val thumbnailURL: String? = series.thumbnailURL
        override val categories: List<String> = series.categories
        override val badge: String =
            if (series.episodeCount == 1) "1 Episode" else "${series.episodeCount} Episodes"
        override val sortViews: Long = series.episodes.sumOf { (it.views ?: 0).toLong() }
    }

    data class LectureItem(
        val lecture: StandaloneLecture,
        override val categoryLabel: String,
    ) : HomeFeedItem() {
        override val id: String = "lecture:${lecture.id}"
        override val title: String = lecture.title
        override val speaker: String = lecture.speaker
        override val thumbnailURL: String? = lecture.thumbnailURL
        override val categories: List<String> = lecture.categories
        override val badge: String? = lecture.duration?.let { formatDuration(it) }
        override val sortViews: Long = (lecture.views ?: 0).toLong()
    }
}

fun Catalog.homeFeed(): List<HomeFeedItem> {
    val topicName = topics.associate { it.id to it.name }
    fun label(type: String, cats: List<String>): String {
        val names = cats.mapNotNull { topicName[it] }.joinToString(", ") { it.uppercase() }
        return if (names.isEmpty()) type else "$type · $names"
    }

    val seriesItems = series.map {
        HomeFeedItem.SeriesItem(it, label("SERIES", it.categories))
    }
    val lectureItems = standaloneLectures.filter { it.isAvailable }.map {
        HomeFeedItem.LectureItem(it, label("LECTURE", it.categories))
    }
    return seriesItems + lectureItems
}

fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remaining = seconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, remaining)
    } else {
        "%d:%02d".format(minutes, remaining)
    }
}
