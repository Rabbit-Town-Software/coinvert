package com.rabbittownsoftware.coinvert

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.PairSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.core.content.edit

/**
 * CurrencyRepository handles data fetching and caching for the currency converter.
 * It retrieves live and historical exchange rates from an external API,
 * and uses SharedPreferences to store fallback/cache data locally.
 *
 * @param context Application context used to initialize SharedPreferences.
 */
class CurrencyRepository(context: Context)
{
    // JSON parser configured to ignore unknown fields
    private val json = Json { ignoreUnknownKeys = true }

    // Local cache
    private val sharedPrefs = context.getSharedPreferences("currency_prefs", Context.MODE_PRIVATE)
    private val LATEST_RATES_KEY = "latest_rates_json"
    private val CURRENCY_LIST_KEY = "available_currencies"

    /**
     * Fetches the latest exchange rates for the given base currency.
     * Falls back to cache if the API fails or data is malformed.
     */
    suspend fun getLatestRates(base: String): Map<String, Double>?
    {
        return withContext(Dispatchers.IO)
        {
            try
            {
                val url = "https://cdn.jsdelivr.net/npm/@fawazahmed0/" +
                        "currency-api@latest/v1/currencies/${base.lowercase()}.json"

                val request = Request.Builder().url(url).build()
                val response = OkHttpClient().newCall(request).execute()
                val body = response.body?.string()?.trim()

                // Validate response before parsing
                if (!response.isSuccessful || body == null || !body.startsWith("{"))
                {
                    Log.e("CurrencyRepository", "Invalid latest response")
                    return@withContext loadCachedLatestRates()
                }

                val jsonElement = json.parseToJsonElement(body).jsonObject
                val nested = jsonElement[base.lowercase()]?.jsonObject ?: return@withContext null

                val rates = nested.mapValues { it.value.jsonPrimitive.doubleOrNull ?: 0.0 }

                // Save latest results to cache
                cacheLatestRates(rates)
                cacheCurrencyList(rates.keys)

                rates
            }
            catch (e: Exception)
            {
                Log.e("CurrencyRepository", "Error loading latest rates: ${e.message}")
                loadCachedLatestRates()
            }
        }
    }

    /**
     * Fetches historical exchange rate data for a given base-target pair over a number of past days.
     * Caches results for future use. Skips dates with failed responses.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getHistoricalRates(base: String, target: String, days: Int): List<Pair<String, Double>>?
    {
        return withContext(Dispatchers.IO)
        {
            val cacheKey = getHistoricalKey(base, target, days)

            // Try loading from cache first
            loadCachedHistoricalRates(cacheKey)?.let { return@withContext it }

            val endDate = LocalDate.now().minusDays(2) // slight buffer to avoid today's incomplete data
            val formatter = DateTimeFormatter.ISO_DATE
            val client = OkHttpClient()
            val results = mutableListOf<Pair<String, Double>>()

            for (i in 0 until days)
            {
                val date = endDate.minusDays(i.toLong()).format(formatter)
                val url = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@${date}" +
                        "/v1/currencies/${base.lowercase()}.json"

                try
                {
                    val request = Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()
                    val body = response.body?.string()?.trim() ?: continue

                    if (!body.startsWith("{")) continue

                    val jsonObject = json.parseToJsonElement(body).jsonObject
                    val nested = jsonObject[base.lowercase()]?.jsonObject
                    val value = nested?.get(target.lowercase())?.jsonPrimitive?.doubleOrNull

                    if (value != null)
                    {
                        results.add(date to value)
                    }
                }
                catch (_: Exception)
                {
                    // Ignore individual failures (e.g. missing date)
                }
            }

            // Sort by date ascending and cache
            results.sortedBy { it.first }.also()
            {
                cacheHistoricalRates(cacheKey, it)
            }
        }
    }

    /**
     * Stores the latest exchange rates in SharedPreferences as JSON.
     */
    private fun cacheLatestRates(rates: Map<String, Double>)
    {
        try
        {
            val jsonString = json.encodeToString(
                MapSerializer(String.serializer(), Double.serializer()),
                rates
            )
            sharedPrefs.edit { putString(LATEST_RATES_KEY, jsonString) }
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    /**
     * Loads previously cached latest exchange rates.
     */
    private fun loadCachedLatestRates(): Map<String, Double>?
    {
        return try
        {
            val jsonString = sharedPrefs.getString(LATEST_RATES_KEY, null) ?: return null
            json.decodeFromString(
                MapSerializer(String.serializer(), Double.serializer()),
                jsonString
            )
        }
        catch (e: Exception)
        {
            e.printStackTrace()
            null
        }
    }

    /**
     * Constructs a unique cache key for a historical base/target/days combo.
     */
    private fun getHistoricalKey(base: String, target: String, days: Int): String
    {
        return "historical_${base.lowercase()}_${target.lowercase()}_${days}"
    }

    /**
     * Stores historical exchange rate results in SharedPreferences.
     */
    private fun cacheHistoricalRates(key: String, rates: List<Pair<String, Double>>)
    {
        try
        {
            val jsonString = json.encodeToString(
                ListSerializer(
                    PairSerializer(String.serializer(), Double.serializer())
                ),
                rates
            )
            sharedPrefs.edit { putString(key, jsonString) }
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    /**
     * Loads cached historical exchange rates from SharedPreferences.
     */
    private fun loadCachedHistoricalRates(key: String): List<Pair<String, Double>>?
    {
        return try
        {
            val jsonString = sharedPrefs.getString(key, null) ?: return null
            json.decodeFromString(
                ListSerializer(
                    PairSerializer(String.serializer(), Double.serializer())
                ),
                jsonString
            )
        }
        catch (e: Exception)
        {
            e.printStackTrace()
            null
        }
    }

    /**
     * Saves the list of available currencies for offline use.
     */
    private fun cacheCurrencyList(currencies: Set<String>)
    {
        sharedPrefs.edit { putStringSet(CURRENCY_LIST_KEY, currencies) }
    }

    /**
     * Loads the cached currency list or returns an empty list.
     */
    fun loadCachedCurrencyList(): List<String>
    {
        return sharedPrefs.getStringSet(CURRENCY_LIST_KEY, emptySet())?.toList() ?: emptyList()
    }
}
