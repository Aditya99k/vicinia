-- One Postgres instance, one logical database per relational service
-- (DEPLOYMENT.md §2/§3 — schema/access ownership, not physically separate
-- servers). Databases for services not yet built are created ahead of time
-- so this script doesn't need to be touched again per-stage.
CREATE DATABASE auth_db;
CREATE DATABASE user_db;
CREATE DATABASE merchant_db;
CREATE DATABASE inventory_db;
CREATE DATABASE coupon_db;
CREATE DATABASE order_db;
CREATE DATABASE payment_db;
CREATE DATABASE delivery_db;
CREATE DATABASE settlement_db;
