INSERT INTO customers (id, name, phone_number, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001',
        'Maria Silva',
        '11999999999',
        NOW(),
        NOW());

INSERT INTO orders (id, customer_id, customer_name, status, payment_status,
                    created_by_id, created_by_name, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000010',
        '00000000-0000-0000-0000-000000000001',
        'Maria Silva',
        'PENDING',
        'MAKE_NOTE',
        '00000000-0000-0000-0000-000000000099',
        'User Teste',
        NOW(),
        NOW());

INSERT INTO order_items (id, order_id, product, category, quantity, status, payment_status, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000011',
        '00000000-0000-0000-0000-000000000010',
        'Dipirona 500mg',
        'MEDICAMENTOS',
        5,
        'PENDING',
        'MAKE_NOTE',
        NOW(),
        NOW());
