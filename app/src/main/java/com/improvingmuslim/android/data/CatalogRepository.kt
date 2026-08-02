package com.improvingmuslim.android.data

import com.improvingmuslim.android.model.Catalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

private const val CATALOG_URL = "https://improvingmuslim.com/api/v1/catalog.json"

class CatalogRepository(
    private val client: OkHttpClient = OkHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun fetchCatalog(): Catalog = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(CATALOG_URL).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Unexpected response ${response.code}")
            val body = response.body?.string() ?: error("Empty response body")
            json.decodeFromString(Catalog.serializer(), body).also { cached = it }
        }
    }

    companion object {
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
