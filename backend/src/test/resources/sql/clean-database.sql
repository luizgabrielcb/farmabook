-- Shared cleanup for all *ControllerTestIT classes.
-- They run against a single, shared Testcontainers Postgres (singleton container),
-- so every test must start from an identical baseline regardless of what ran before.
-- This wipes all domain tables in foreign-key dependency order, then restores the
-- users table to its seed baseline (a single 'User Teste' with the known PIN/active).

DELETE FROM notifications;
DELETE FROM order_items;
DELETE FROM prescription_items;
DELETE FROM shortages;
DELETE FROM orders;
DELETE FROM prescriptions;
DELETE FROM compoundings;
DELETE FROM shortage_orders;
DELETE FROM distributors;
DELETE FROM compounding_pharmacies;
DELETE FROM customers;

DELETE FROM users WHERE name != 'User Teste';
UPDATE users
SET pin_hash = '$2a$04$gMBaXoAuRMITzBR9.KVvqOliIRmtZtyHHLpp4aHs7kOWeAbTIeumO',
    active   = true
WHERE name = 'User Teste';
