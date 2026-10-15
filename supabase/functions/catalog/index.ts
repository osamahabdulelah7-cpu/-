import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Content-Type": "application/json; charset=utf-8",
};

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }
  if (req.method !== "GET") {
    return new Response(JSON.stringify({ error: "Method not allowed" }), {
      status: 405,
      headers: corsHeaders,
    });
  }

  const url = Deno.env.get("SUPABASE_URL");
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
  if (!url || !anonKey) {
    return new Response(JSON.stringify({ error: "Catalog service is not configured" }), {
      status: 503,
      headers: corsHeaders,
    });
  }

  const supabase = createClient(url, anonKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data, error } = await supabase
    .from("shop_products")
    .select("name,image,section,bike_type,price")
    .eq("is_active", true)
    .order("sort_order", { ascending: true })
    .order("created_at", { ascending: true });

  if (error) {
    return new Response(JSON.stringify({ error: "Could not load catalog" }), {
      status: 500,
      headers: corsHeaders,
    });
  }

  const catalog = (data ?? []).map((p) => ({
    name: p.name,
    image: p.image,
    section: p.section,
    bikeType: p.bike_type,
    price: p.price,
  }));

  return new Response(JSON.stringify(catalog), {
    status: 200,
    headers: { ...corsHeaders, "Cache-Control": "no-store" },
  });
});
