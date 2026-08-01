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

sealed class LectureItem {
    abstract val id: String
    abstract val title: String
    abstract val speaker: String
    abstract val context: String
    abstract val thumbnailURL: String?
    abstract val duration: Int?

    data class FromEpisode(val series: LectureSeries, val episode: Episode) : LectureItem() {
        override val id: String = "episode:${series.id}:${episode.id}"
        override val title: String = episode.title
        override val speaker: String = series.speaker
        override val context: String = "${series.title} · Episode ${episode.number}"
        override val thumbnailURL: String? = episode.thumbnailURL
        override val duration: Int? = episode.duration
    }

    data class FromStandalone(val lecture: StandaloneLecture) : LectureItem() {
        override val id: String = "standalone:${lecture.id}"
        override val title: String = lecture.title
        override val speaker: String = lecture.speaker
        override val context: String = lecture.topic ?: lecture.typeLabel
        override val thumbnailURL: String? = lecture.thumbnailURL
        override val duration: Int? = lecture.duration
    }
}

fun Catalog.playableItems(): List<LectureItem> {
    val episodes = series.flatMap { s ->
        s.episodes.filter { it.isAvailable }.map { LectureItem.FromEpisode(s, it) }
    }
    val standalone = standaloneLectures.filter { it.isAvailable }.map { LectureItem.FromStandalone(it) }
    return episodes + standalone
}
