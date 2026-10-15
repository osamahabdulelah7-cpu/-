package com.example.bikeshop

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Owner-only catalog API. A signed-in account is accepted only when its user id
 * is explicitly present in public.shop_admins; database RLS remains the authority.
 * Passwords are sent only to Supabase Auth and are never persisted by this class.
 */
data class ShopAdminSession(val accessToken: String, val userId: String)

data class AdminProduct(
    val id: String = "",
    val name: String,
    val image: String,
    val section: String,
    val bikeType: String = "",
    val price: String = "",
    val active: Boolean = true,
    val sortOrder: Int = 0
)

object ShopAdminApi {
    private const val BASE = "https://wccufalnjfmqxosjwoad.supabase.co"
    private const val TIMEOUT_MS = 12000

    fun signIn(email: String, password: String): ShopAdminSession {
        require(email.isNotBlank() && password.isNotBlank()) { "أدخل البريد الإلكتروني وكلمة المرور" }
        val response = request(
            path = "/auth/v1/token?grant_type=password",
            method = "POST",
            body = JSONObject().put("email", email.trim()).put("password", password),
            authenticated = false
        )
        val authObject = response.optJSONObject(0) ?: error("استجابة تسجيل الدخول غير صالحة")
        val token = authObject.optString("access_token")
        val userId = authObject.optJSONObject("user")?.optString("id").orEmpty()
        if (token.isBlank() || userId.isBlank()) error("لم يُرجع الخادم جلسة دخول صالحة")
        val session = ShopAdminSession(token, userId)
        val admins = request(
            path = "/rest/v1/shop_admins?select=user_id&user_id=eq.${encode(userId)}",
            method = "GET",
            session = session
        )
        if (admins.length() == 0) error("هذا الحساب غير مخوّل لإدارة المتجر")
        return session
    }

    fun listProducts(session: ShopAdminSession): List<AdminProduct> {
        val rows = request(
            path = "/rest/v1/shop_products?select=id,name,image,section,bike_type,price,is_active,sort_order&order=sort_order.asc,created_at.asc",
            method = "GET",
            session = session
        )
        return (0 until rows.length()).map { index ->
            val row = rows.getJSONObject(index)
            AdminProduct(
                id = row.optString("id"),
                name = row.optString("name"),
                image = row.optString("image"),
                section = row.optString("section"),
                bikeType = row.optString("bike_type"),
                price = row.optString("price"),
                active = row.optBoolean("is_active", true),
                sortOrder = row.optInt("sort_order", 0)
            )
        }
    }

    fun saveProduct(session: ShopAdminSession, product: AdminProduct): AdminProduct {
        require(product.name.isNotBlank()) { "اسم المنتج مطلوب" }
        require(product.image.isNotBlank()) { "أدخل رابط الصورة أو اسم ملف الصورة" }
        require(product.section == "parts" || product.section == "accessories") { "القسم غير صالح" }
        require(product.bikeType in listOf("", "large", "small")) { "نوع الدراجة غير صالح" }
        val body = JSONObject()
            .put("name", product.name.trim())
            .put("image", product.image.trim())
            .put("section", product.section)
            .put("bike_type", product.bikeType)
            .put("price", product.price.trim())
            .put("is_active", product.active)
            .put("sort_order", product.sortOrder)
        val path = if (product.id.isBlank()) {
            "/rest/v1/shop_products?select=id,name,image,section,bike_type,price,is_active,sort_order"
        } else {
            "/rest/v1/shop_products?id=eq.${encode(product.id)}&select=id,name,image,section,bike_type,price,is_active,sort_order"
        }
        val rows = request(
            path = path,
            method = if (product.id.isBlank()) "POST" else "PATCH",
            body = body,
            session = session,
            prefer = if (product.id.isBlank()) "return=representation" else "return=representation"
        )
        if (rows.length() == 0) error("لم يُرجع الخادم المنتج المحفوظ")
        val row = rows.getJSONObject(0)
        return product.copy(
            id = row.optString("id", product.id),
            name = row.optString("name", product.name),
            image = row.optString("image", product.image),
            section = row.optString("section", product.section),
            bikeType = row.optString("bike_type", product.bikeType),
            price = row.optString("price", product.price),
            active = row.optBoolean("is_active", product.active),
            sortOrder = row.optInt("sort_order", product.sortOrder)
        )
    }

    fun deleteProduct(session: ShopAdminSession, id: String) {
        require(id.isNotBlank()) { "معرّف المنتج غير صالح" }
        request(
            path = "/rest/v1/shop_products?id=eq.${encode(id)}",
            method = "DELETE",
            session = session,
            prefer = "return=minimal"
        )
    }

    private fun request(
        path: String,
        method: String,
        body: JSONObject? = null,
        session: ShopAdminSession? = null,
        authenticated: Boolean = true,
        prefer: String? = null
    ): JSONArray {
        val connection = (URL(BASE + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("apikey", ShopConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Accept", "application/json")
            if (authenticated && session != null) {
                setRequestProperty("Authorization", "Bearer ${session.accessToken}")
            } else {
                setRequestProperty("Authorization", "Bearer ${ShopConfig.SUPABASE_PUBLISHABLE_KEY}")
            }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
            if (prefer != null) setRequestProperty("Prefer", prefer)
        }
        try {
            if (body != null) connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = try {
                    JSONObject(text).optString("message").ifBlank {
                        JSONObject(text).optString("msg").ifBlank { "HTTP $status" }
                    }
                } catch (_: Exception) { "HTTP $status" }
                error(message)
            }
            if (text.isBlank()) return JSONArray()
            return if (text.trimStart().startsWith("[")) JSONArray(text) else JSONArray().put(JSONObject(text))
        } finally {
            connection.disconnect()
        }
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())
}
