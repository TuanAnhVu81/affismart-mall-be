CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    default_shipping_address TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    bank_info TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'BANNED'))
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
);

CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    sku VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(300) NOT NULL UNIQUE,
    description TEXT,
    price DECIMAL(12, 2) NOT NULL,
    stock_quantity INTEGER NOT NULL DEFAULT 0,
    image_url VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT chk_products_price CHECK (price > 0),
    CONSTRAINT chk_products_stock_quantity CHECK (stock_quantity >= 0)
);

CREATE TABLE affiliate_accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    ref_code VARCHAR(50) NOT NULL UNIQUE,
    promotion_channel VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    commission_rate DECIMAL(5, 2) NOT NULL DEFAULT 10.00,
    balance DECIMAL(12, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_affiliate_accounts_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_affiliate_accounts_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED')),
    CONSTRAINT chk_affiliate_accounts_commission_rate CHECK (commission_rate >= 0 AND commission_rate <= 100),
    CONSTRAINT chk_affiliate_accounts_balance CHECK (balance >= 0)
);

CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    affiliate_account_id BIGINT,
    total_amount DECIMAL(12, 2) NOT NULL,
    discount_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    stripe_session_id VARCHAR(255) UNIQUE,
    shipping_address TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_orders_affiliate_account FOREIGN KEY (affiliate_account_id) REFERENCES affiliate_accounts (id),
    CONSTRAINT chk_orders_total_amount CHECK (total_amount > 0),
    CONSTRAINT chk_orders_discount_amount CHECK (discount_amount >= 0),
    CONSTRAINT chk_orders_status CHECK (status IN ('PENDING', 'PAID', 'CONFIRMED', 'SHIPPED', 'DONE', 'CANCELLED'))
);

CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    price_at_time DECIMAL(12, 2) NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT chk_order_items_price_at_time CHECK (price_at_time > 0)
);

CREATE TABLE referral_links (
    id BIGSERIAL PRIMARY KEY,
    affiliate_account_id BIGINT NOT NULL,
    product_id BIGINT,
    ref_code VARCHAR(20) NOT NULL UNIQUE,
    total_clicks INTEGER NOT NULL DEFAULT 0,
    total_conversions INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_referral_links_affiliate_account FOREIGN KEY (affiliate_account_id) REFERENCES affiliate_accounts (id),
    CONSTRAINT fk_referral_links_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_referral_links_total_clicks CHECK (total_clicks >= 0),
    CONSTRAINT chk_referral_links_total_conversions CHECK (total_conversions >= 0)
);

CREATE TABLE payout_requests (
    id BIGSERIAL PRIMARY KEY,
    affiliate_account_id BIGINT NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    admin_note TEXT,
    resolved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payout_requests_affiliate_account FOREIGN KEY (affiliate_account_id) REFERENCES affiliate_accounts (id),
    CONSTRAINT chk_payout_requests_amount CHECK (amount >= 200000),
    CONSTRAINT chk_payout_requests_status CHECK (status IN ('PENDING', 'APPROVED', 'TRANSFERRED', 'REJECTED'))
);

CREATE TABLE commissions (
    id BIGSERIAL PRIMARY KEY,
    affiliate_account_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL UNIQUE,
    amount DECIMAL(12, 2) NOT NULL,
    rate_snapshot DECIMAL(5, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payout_request_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_commissions_affiliate_account FOREIGN KEY (affiliate_account_id) REFERENCES affiliate_accounts (id),
    CONSTRAINT fk_commissions_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_commissions_payout_request FOREIGN KEY (payout_request_id) REFERENCES payout_requests (id),
    CONSTRAINT chk_commissions_amount CHECK (amount > 0),
    CONSTRAINT chk_commissions_rate_snapshot CHECK (rate_snapshot >= 0 AND rate_snapshot <= 100),
    CONSTRAINT chk_commissions_status CHECK (status IN ('PENDING', 'APPROVED', 'PAID', 'REJECTED'))
);

CREATE TABLE blocked_click_logs (
    id BIGSERIAL PRIMARY KEY,
    ip_address VARCHAR(45) NOT NULL,
    reason VARCHAR(100) NOT NULL,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE recommendation_events (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    session_id VARCHAR(100),
    product_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_recommendation_events_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_recommendation_events_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_recommendation_events_action CHECK (action IN ('VIEW', 'ADD_TO_CART', 'PURCHASE'))
);

CREATE INDEX idx_products_category ON products (category_id);
CREATE INDEX idx_products_slug ON products (slug);
CREATE INDEX idx_products_active ON products (is_active);
CREATE INDEX idx_orders_user ON orders (user_id);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_affiliate ON orders (affiliate_account_id);
CREATE INDEX idx_referral_code ON referral_links (ref_code);
CREATE INDEX idx_commissions_affiliate ON commissions (affiliate_account_id);
CREATE INDEX idx_recommendation_events_user ON recommendation_events (user_id, product_id);
CREATE INDEX idx_recommendation_events_session ON recommendation_events (session_id, product_id);

INSERT INTO roles (name, description)
VALUES
    ('CUSTOMER', 'Customer role'),
    ('AFFILIATE', 'Affiliate role'),
    ('ADMIN', 'Administrator role')
ON CONFLICT (name) DO NOTHING;
