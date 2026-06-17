INSERT INTO distributors (id, name, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000020', 'Distribuidora Teste', NOW(), NOW());

INSERT INTO shortage_orders (id, shortage_type, distributor_id, distributor_name, status,
                            created_by_id, created_by_name, ordered_by_id, ordered_by_name, ordered_at,
                            created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000060',
        'WANIA',
        '00000000-0000-0000-0000-000000000020',
        'Distribuidora Teste',
        'ORDERED',
        '00000000-0000-0000-0000-000000000001',
        'User Teste',
        '00000000-0000-0000-0000-000000000001',
        'User Teste',
        NOW(),
        NOW(),
        NOW());

INSERT INTO shortages (id, product, category, quantity, status, shortage_type,
                       created_by_id, created_by_name, ordered_by_id, ordered_by_name, ordered_at,
                       shortage_order_id, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000061',
        'Dipirona 500mg',
        'MEDICAMENTOS',
        10,
        'ORDERED',
        'WANIA',
        '00000000-0000-0000-0000-000000000001',
        'User Teste',
        '00000000-0000-0000-0000-000000000001',
        'User Teste',
        NOW(),
        '00000000-0000-0000-0000-000000000060',
        NOW(),
        NOW());
