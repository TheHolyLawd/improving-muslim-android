package com.improvingmuslim.android.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Covers the Tier 1 search engine: field weighting, synonym/transliteration expansion,
 *  stemming, the results/related split, and non-matches. */
class SearchTest {

    private fun lecture(
        id: String,
        title: String,
        speaker: String = "Speaker",
        topic: String? = null,
        categories: List<String> = emptyList(),
        description: String? = null,
        recap: String? = null,
        takeaways: List<String> = emptyList(),
    ) = StandaloneLecture(
        id = id, title = title, speaker = speaker, topic = topic, categories = categories,
        typeLabel = "Lecture", videoURL = "https://v/$id",
        description = description, recap = recap, takeaways = takeaways,
    )

    private fun catalog(vararg lectures: StandaloneLecture) = Catalog(
        schemaVersion = 1, catalogVersion = "t", counts = CatalogCounts(0, 0, 0),
        standaloneLectures = lectures.toList(),
    )

    private fun titles(items: List<HomeFeedItem>) = items.map { it.title }

    @Test fun titleMatchIsAStrongResult() {
        val out = catalog(lecture("1", "The Story of Salah")).search("salah")
        assertTrue("salah", titles(out.results).contains("The Story of Salah"))
    }

    @Test fun transliterationSynonymMatches() {
        // "namaz" and "solah" should both find a Salah lecture via the synonym group.
        val c = catalog(lecture("1", "Enjoy Your Salah"))
        assertTrue("namaz", titles(c.search("namaz").results).contains("Enjoy Your Salah"))
        assertTrue("solah", titles(c.search("solah").results).contains("Enjoy Your Salah"))
    }

    @Test fun buriedMatchIsRelatedNotAResult() {
        // "patience" only appears in the description, not a headline field.
        val out = catalog(
            lecture("1", "Gratitude", description = "A talk on patience and sabr."),
        ).search("patience")
        assertFalse("not a result", titles(out.results).contains("Gratitude"))
        assertTrue("is related", titles(out.related).contains("Gratitude"))
    }

    @Test fun stemmingMatchesSingularAndPlural() {
        val out = catalog(lecture("1", "Daily Reminders")).search("reminder")
        assertTrue(titles(out.results).contains("Daily Reminders"))
    }

    @Test fun unrelatedQueryReturnsNothing() {
        val out = catalog(lecture("1", "The Story of Salah")).search("astronomy")
        assertTrue(out.isEmpty)
    }

    @Test fun rankingPutsTitleHitAboveSpeakerHit() {
        // Same literal word in both (so both get the phrase bonus); the title field's higher
        // weight must still rank the title hit first.
        val c = catalog(
            lecture("1", "Riba Explained", speaker = "Ustadh Ahmad"),
            lecture("2", "Wealth in Islam", speaker = "Riba Ali"),
        )
        val results = titles(c.search("riba").results)
        assertTrue("both matched", results.size == 2)
        assertTrue("title first", results.first() == "Riba Explained")
    }
}
