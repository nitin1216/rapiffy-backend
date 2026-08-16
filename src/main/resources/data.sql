-- Alter cart_items: make shop_product_id nullable and add product_variant_id FK
ALTER TABLE cart_items MODIFY COLUMN shop_product_id BIGINT NULL;
ALTER TABLE cart_items ADD COLUMN IF NOT EXISTS product_variant_id BIGINT NULL;
ALTER TABLE cart_items DROP FOREIGN KEY IF EXISTS fk_cart_item_variant;
ALTER TABLE cart_items ADD CONSTRAINT fk_cart_item_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants(id);

