-- Shared catalog for طبيب السياكل.
-- Apply this migration to the Supabase project before deploying the function.

create table if not exists public.shop_products (
  id uuid primary key default gen_random_uuid(),
  name text not null check (length(trim(name)) > 0),
  image text not null check (length(trim(image)) > 0),
  section text not null check (section in ('parts', 'accessories')),
  bike_type text not null default '' check (bike_type in ('', 'large', 'small')),
  price text not null default '',
  is_active boolean not null default true,
  sort_order integer not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.shop_admins (
  user_id uuid primary key references auth.users(id) on delete cascade,
  created_at timestamptz not null default now()
);

alter table public.shop_products enable row level security;
alter table public.shop_admins enable row level security;

-- Catalog reads are public, but only active products are visible.
drop policy if exists "Anyone can read active shop products" on public.shop_products;
create policy "Anyone can read active shop products"
  on public.shop_products for select
  to anon, authenticated
  using (is_active = true);

-- Only explicitly registered owner accounts can manage products.
drop policy if exists "Shop admins can manage products" on public.shop_products;
create policy "Shop admins can manage products"
  on public.shop_products for all
  to authenticated
  using (exists (select 1 from public.shop_admins a where a.user_id = (select auth.uid())))
  with check (exists (select 1 from public.shop_admins a where a.user_id = (select auth.uid())));

-- Users cannot grant themselves owner access.
drop policy if exists "Admins can read own admin record" on public.shop_admins;
create policy "Admins can read own admin record"
  on public.shop_admins for select
  to authenticated
  using (user_id = (select auth.uid()));

grant select on public.shop_products to anon, authenticated;
grant insert, update, delete on public.shop_products to authenticated;
grant select on public.shop_admins to authenticated;

create index if not exists shop_products_visible_order_idx
  on public.shop_products (is_active, sort_order, created_at);
