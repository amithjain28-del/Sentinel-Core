package com.sentinel.core.bridge

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URLEncoder

class WebResearchScraper {

    suspend fun searchAndScrape(query: String): String = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val searchUrl = "https://html.duckduckgo.com/html/?q=$encodedQuery"

            val doc = Jsoup.connect(searchUrl).userAgent("Mozilla/5.0").get()
            val results = doc.select(".result__snippet")

            val topResults = results.take(3).map { it.text() }

            if (topResults.isEmpty()) {
                "No results found for '$query'"
            } else {
                "Top research results for '$query':\n" + topResults.joinToString("\n\n")
            }
        } catch (e: Exception) {
            "Error during web research: ${e.message}"
        }
    }
}
