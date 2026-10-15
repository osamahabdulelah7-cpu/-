package com.example.bikeshop

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Stores the last successful catalog response on this device.
 * The cache remains available when the device is offline.
 */
object ShopCatalogStore {
    private const val PREFS = "shop_catalog_cache"
    private const val KEY_PRODUCTS = "products_json"

    fun load(context: Context): List<ShopProduct>? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PRODUCTS, null) ?: return null
        return try {
            parse(JSONArray(raw))
        } catch (_: Exception) {
            null
        }
    }

    fun fetchAndCache(context: Context, endpoint: String): List<ShopProduct>? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("apikey", ShopConfig.SUPABASE_PUBLISHABLE_KEY)
            }
            if (connection.responseCode !in 200..299) return null
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val parsed = parse(JSONArray(body))
            // Never overwrite a previously good cache with an empty or invalid response.
            if (parsed.isEmpty()) return null
            val serialized = JSONArray().apply {
                parsed.forEach { product ->
                    put(JSONObject().apply {
                        put("name", product.name)
                        put("image", product.image)
                        put("section", product.section)
                        put("bikeType", product.bikeType)
                        put("price", product.price)
                    })
                }
            }.toString()
            val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PRODUCTS, serialized)
                .commit()
            if (saved) parsed else null
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun parse(array: JSONArray): List<ShopProduct> {
        val result = mutableListOf<ShopProduct>()
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val name = item.optString("name").trim()
            val image = item.optString("image").trim()
            val section = item.optString("section").trim()
            if (name.isBlank() || image.isBlank() || section !in setOf("parts", "accessories")) continue
            result += ShopProduct(
                name = name,
                image = image,
                section = section,
                bikeType = item.optString("bikeType").trim(),
                price = item.optString("price").trim()
            )
        }
        return result
    }
}
