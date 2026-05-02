-- Index to speed up AI training queries filtering by time window
CREATE INDEX IF NOT EXISTS idx_recommendation_events_created_at
    ON recommendation_events (created_at DESC);

-- Trigram indexes for product keyword search using lower(...) LIKE '%keyword%'
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_products_name_trgm
    ON products USING gin (lower(name) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_products_sku_trgm
    ON products USING gin (lower(sku) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_products_slug_trgm
    ON products USING gin (lower(slug) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_categories_name_trgm
    ON categories USING gin (lower(name) gin_trgm_ops);

-- Composite indexes for hot-path order queries:
-- Supports "GET /orders/my" filtered by user with newest-first ordering
CREATE INDEX IF NOT EXISTS idx_orders_user_created_at
    ON orders (user_id, created_at DESC);

-- Supports admin order listing filtered by status with ordering
CREATE INDEX IF NOT EXISTS idx_orders_status_created_at
    ON orders (status, created_at DESC);

-- Composite indexes for affiliate portal queries:
-- Supports commission listing per affiliate filtered by status
CREATE INDEX IF NOT EXISTS idx_commissions_affiliate_status_created_at
    ON commissions (affiliate_account_id, status, created_at DESC);

-- Supports payout request listing per affiliate filtered by status
CREATE INDEX IF NOT EXISTS idx_payout_requests_affiliate_status_created_at
    ON payout_requests (affiliate_account_id, status, created_at DESC);

-- Supports referral link listing per affiliate filtered by active flag
CREATE INDEX IF NOT EXISTS idx_referral_links_affiliate_active_created_at
    ON referral_links (affiliate_account_id, is_active, created_at DESC);

-- Covering indexes for order_items join patterns:
-- Supports fetching order items by order (detail page, stock restore on cancel)
CREATE INDEX IF NOT EXISTS idx_order_items_order_id
    ON order_items (order_id);

-- Supports analytics queries and product stock restore grouped by product
CREATE INDEX IF NOT EXISTS idx_order_items_product_id
    ON order_items (product_id);
