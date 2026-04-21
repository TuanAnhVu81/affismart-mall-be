ALTER TABLE orders
    ADD COLUMN referral_link_id BIGINT;

ALTER TABLE orders
    ADD CONSTRAINT fk_orders_referral_link
        FOREIGN KEY (referral_link_id) REFERENCES referral_links (id);

CREATE INDEX idx_orders_referral_link ON orders (referral_link_id);
