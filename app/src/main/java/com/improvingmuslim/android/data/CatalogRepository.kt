package com.improvingmuslim.android.data

import android.content.Context
import com.improvingmuslim.android.model.Catalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

private const val CATALOG_URL = "https://improvingmuslim.com/api/v1/catalog.json"
private const val BUNDLED_CATALOG = "catalog.json"

class CatalogRepository(
    private val client: OkHttpClient = OkHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    /** Prefer the live feed for fresh content; fall back to the build-time snapshot bundled in
     *  assets when the site is unreachable (offline, or the website is down) so the app still
     *  works. Either way the result is [cached] for other screens. */
    suspend fun fetchCatalog(): Catalog = withContext(Dispatchers.IO) {
        val live = runCatching { fetchRemote() }
        live.getOrElse { loadBundled() ?: throw it }.also { cached = it }
    }

    private fun fetchRemote(): Catalog {
        val request = Request.Builder().url(CATALOG_URL).build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Unexpected response ${response.code}")
            val body = response.body?.string() ?: error("Empty response body")
            json.decodeFromString(Catalog.serializer(), body)
        }
    }

    // ponytail: static snapshot, goes stale between builds. Refresh by re-running the website's
    // scripts/generate-mobile-api.js and copying api/v1/catalog.json here. Fine as a fallback.
    private fun loadBundled(): Catalog? {
        val ctx = appContext ?: return null
        return runCatching {
            ctx.assets.open(BUNDLED_CATALOG).use {
                json.decodeFromString(Catalog.serializer(), it.readBytes().decodeToString())
            }
        }.getOrNull()
    }

    companion object {
        /** App context for reading the bundled fallback asset. Set once from MainActivity. */
        @Volatile
        var appContext: Context? = null
        /**
         * The last successfully loaded catalog, shared across screens so the Watch screen
         * can compute "up next" / "more like this" without re-fetching. Populated by the
         * Home screen on launch.
         */
        @Volatile
        var cached: Catalog? = null
            private set
    }
}
