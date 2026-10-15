package com.example.bikeshop

object ShopConfig {
    // ضع رقم واتساب المحل بصيغة دولية بدون + أو مسافات، مثال: 9677XXXXXXXX
    // اتركه فارغًا إلى أن يتم ضبط رقم المحل.
    const val WHATSAPP_NUMBER = ""

    // Public Supabase Edge Function endpoint for the shared product catalog.
    const val CATALOG_API_URL = "https://wccufalnjfmqxosjwoad.supabase.co/functions/v1/catalog"

    // Publishable/anon key is intended for client apps; database access is protected by RLS.
    const val SUPABASE_PUBLISHABLE_KEY = "sb_publishable_ZngaN5eMZ6qyYXytVKkcPg_KXwqbj86"
}