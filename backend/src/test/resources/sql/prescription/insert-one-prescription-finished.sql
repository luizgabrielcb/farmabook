INSERT INTO customers (id, name, phone_number, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001', 'Maria Silva', '11999999999', NOW(), NOW());

INSERT INTO prescriptions (id, customer_id, customer_name, status, created_by_id, created_by_name, observations, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000030',
        '00000000-0000-0000-0000-000000000001',
        'Maria Silva',
        'FINISHED',
        '00000000-0000-0000-0000-000000000099',
        'User Teste',
        NULL,
        NOW(),
        NOW());

INSERT INTO prescription_items (id, prescription_id, product, quantity, batch, expiry, status, received_by_id, received_by_name, received_at, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000031',
        '00000000-0000-0000-0000-000000000030',
        'Dipirona 500mg',
        2,
        'LOTE-001',
        '12/2025',
        'RECEIVED',
        '00000000-0000-0000-0000-000000000099',
        'User Teste',
        NOW(),
        NOW(),
        NOW());
