package com.improvingmuslim.android.model

import kotlin.math.ceil

/**
 * Tier 1 catalog search. Pipeline: normalize → tokenize → drop stop-words → stem → expand
 * synonyms/transliterations → score across weighted fields → split into strong "results" and
 * weak "related". Mirrors the website's home-search.js core.
 *
 * Searches titles, speakers, topics, and buried text (descriptions, recaps, takeaways, and a
 * series' episode titles/recaps) — NOT captions/transcripts. Deliberately left for a later
 * tier: fuzzy typo tolerance and broad "related-term" concept groups.
 */
data class SearchResult(val item: HomeFeedItem, val score: Double, val strong: Boolean)

/** Search results split the website's way: [results] matched a headline field (title/speaker/
 *  topic) and are honest hits; [related] only matched buried text. Each is sorted best-first. */
data class SearchOutcome(val results: List<HomeFeedItem>, val related: List<HomeFeedItem>) {
    val isEmpty: Boolean get() = results.isEmpty() && related.isEmpty()
}

fun Catalog.search(query: String): SearchOutcome {
    if (normalizeQuery(query).isEmpty()) return SearchOutcome(emptyList(), emptyList())
    val topicName = topics.associate { it.id to it.name }
    val scored = homeFeed()
        .mapNotNull { assess(it, query, topicName) }
        .sortedByDescending { it.score }
    return SearchOutcome(
        results = scored.filter { it.strong }.map { it.item },
        related = scored.filterNot { it.strong }.map { it.item },
    )
}

private val STOP_WORDS = setOf(
    "the", "of", "a", "an", "in", "on", "and", "or", "for", "to", "with", "about",
    "pbuh", "saw", "sws", "ra", "as", "how", "what", "why", "is", "my", "your",
    "lecture", "lectures", "series", "video", "videos",
)

// Transliteration variants and close synonyms. Any word in a group expands to the whole group,
// so "namaz", "solah", and "prayer" all find "salah".
private val SYNONYM_GROUPS = listOf(
    listOf("salah", "solah", "salat", "salaat", "solat", "salaah", "namaz", "prayer", "prayers", "praying"),
    listOf("dua", "duas", "duaa", "du'a", "supplication", "supplications"),
    listOf("quran", "koran", "quraan", "qur'an"),
    listOf("tafsir", "tafseer", "exegesis"),
    listOf("seerah", "sirah", "sira", "seera", "biography"),
    listOf("hadith", "hadeeth", "ahadith", "hadiths"),
    listOf("sunnah", "sunna"),
    listOf("ramadan", "ramadhan", "ramzan", "ramadaan"),
    listOf("fasting", "sawm", "siyam", "fast"),
    listOf("zakat", "zakah", "zakaat", "charity", "sadaqah", "sadaqa"),
    listOf("hajj", "haj", "pilgrimage", "umrah", "umra"),
    listOf("aqeedah", "aqidah", "aqeeda", "creed", "belief", "beliefs"),
    listOf("fiqh", "jurisprudence", "rulings"),
    listOf("akhirah", "akhira", "hereafter", "afterlife"),
    listOf("jannah", "paradise", "heaven"),
    listOf("jahannam", "hellfire", "hell"),
    listOf("shaytan", "shaitan", "satan", "devil", "iblis"),
    listOf("iman", "eman", "emaan", "imaan", "faith"),
    listOf("tawbah", "tawba", "repentance", "repent", "istighfar"),
    listOf("riba", "interest", "usury"),
    listOf("nikah", "nikaah", "marriage"),
    listOf("prophet", "messenger", "rasul", "rasool", "nabi"),
    listOf("muhammad", "muhammed", "mohammad", "mohammed"),
    listOf("dhikr", "zikr", "thikr", "remembrance"),
    listOf("sabr", "sabar", "patience"),
    listOf("taqwa", "piety"),
    listOf("deen", "din", "religion"),
    listOf("heart", "hearts", "qalb"),
    listOf("akhlaq", "akhlaaq", "manners", "character", "etiquette", "adab"),
    listOf("sahaba", "sahabah", "companions", "companion"),
    listOf("arabic", "arab"),
)

private val SYNONYM_LOOKUP: Map<String, List<String>> = buildMap {
    SYNONYM_GROUPS.forEach { group -> group.forEach { put(it, group) } }
}

private val WHITESPACE = Regex("\\s+")
private val NON_WORD = Regex("[^a-z0-9']+")
private val SUFFIX = Regex("(ers|ies|ing|ed|es|s)$")

private fun normalizeQuery(value: String): String = value.trim().replace(WHITESPACE, " ").lowercase()

private fun textWords(text: String): List<String> = text.split(NON_WORD).filter { it.isNotEmpty() }

private fun stemToken(token: String): String {
    val stem = token.replace(SUFFIX, "")
    return if (stem.length >= 4) stem else token
}

private fun queryTokens(query: String): List<String> =
    textWords(normalizeQuery(query)).filter { it.length > 1 && it !in STOP_WORDS }

private fun tokenVariants(token: String): Set<String> {
    val variants = mutableSetOf(token)
    val stem = stemToken(token)
    variants.add(stem)
    (SYNONYM_LOOKUP[token] ?: SYNONYM_LOOKUP[stem] ?: emptyList()).forEach {
        variants.add(it)
        variants.add(stemToken(it))
    }
    return variants
}

private class Field(val text: String, val weight: Int, val headline: Boolean)

/** Strength of the best match for a token in one field: exact word (1.0) > substring (0.75). */
private fun tokenStrength(variants: Set<String>, fieldText: String, fieldWords: Set<String>): Double {
    if (variants.any { it in fieldWords }) return 1.0
    if (variants.any { it.length >= 4 && fieldText.contains(it) }) return 0.75
    return 0.0
}

private fun buildFields(item: HomeFeedItem, topicName: Map<String, String>): List<Field> {
    fun topics(topic: String?, cats: List<String>): String =
        (cats.mapNotNull { topicName[it] } + listOfNotNull(topic) + cats).joinToString(" ")

    return when (item) {
        is HomeFeedItem.SeriesItem -> {
            val s = item.series
            val rest = buildList {
                s.description?.let { add(it) }
                s.episodes.forEach { ep ->
                    add(ep.title)
                    ep.recap?.let { add(it.take(400)) }
                    addAll(ep.takeaways)
                }
            }.joinToString(" ")
            listOf(
                Field(s.title.lowercase(), 10, true),
                Field(s.speaker.lowercase(), 6, true),
                Field(topics(s.topic, s.categories).lowercase(), 5, true),
                Field(rest.lowercase(), 2, false),
            )
        }
        is HomeFeedItem.LectureItem -> {
            val l = item.lecture
            val rest = buildList {
                l.description?.let { add(it) }
                l.recap?.let { add(it) }
                addAll(l.takeaways)
            }.joinToString(" ")
            listOf(
                Field(l.title.lowercase(), 10, true),
                Field(l.speaker.lowercase(), 6, true),
                Field(topics(l.topic, l.categories).lowercase(), 5, true),
                Field(rest.lowercase(), 2, false),
            )
        }
    }
}

/** Relevance for one item. Returns null when it isn't a match. `strong` = matched a headline
 *  field, so it's an honest result rather than a buried "related" mention. */
private fun assess(item: HomeFeedItem, query: String, topicName: Map<String, String>): SearchResult? {
    val q = normalizeQuery(query)
    val fields = buildFields(item, topicName)
    val fieldWords = fields.map { textWords(it.text).toSet() }
    val fullText = fields.joinToString(" ") { it.text }
    val headlineText = fields.filter { it.headline }.joinToString(" ") { it.text }
    val tokens = queryTokens(query)

    // Query was all stop-words or a single character: fall back to a plain phrase match.
    if (tokens.isEmpty()) {
        if (q.isEmpty() || !fullText.contains(q)) return null
        return SearchResult(item, 1.0, headlineText.contains(q))
    }

    var total = 0.0
    var matched = 0
    var strongTokens = 0
    tokens.forEach { token ->
        val variants = tokenVariants(token)
        var best = 0.0
        var strongHit = false
        fields.forEachIndexed { i, field ->
            val strength = tokenStrength(variants, field.text, fieldWords[i])
            if (strength * field.weight > best) best = strength * field.weight
            if (strength >= 0.75 && field.headline) strongHit = true
        }
        if (best > 0) matched++
        if (strongHit) strongTokens++
        total += best
    }

    // Require at least half the meaningful words to hit, so "money in islam" doesn't match a
    // series that only shares "islam".
    val half = ceil(tokens.size / 2.0).toInt()
    if (matched < half) return null
    if (fullText.contains(q)) total += 8 // exact phrase bonus
    val coverage = matched.toDouble() / tokens.size
    val score = total * (0.3 + 0.7 * coverage * coverage)
    if (score <= 0.0) return null
    val strong = strongTokens >= half || headlineText.contains(q)
    return SearchResult(item, score, strong)
}
