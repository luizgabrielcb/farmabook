INSERT INTO distributors (id, name, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000020', 'Distribuidora Teste', NOW(), NOW());

INSERT INTO shortage_orders (id, shortage_type, distributor_id, distributor_name, status,
                            created_by_id, created_by_name, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000060',
        'WANIA',
        '00000000-0000-0000-0000-000000000020',
        'Distribuidora Teste',
        'PENDING',
        '00000000-0000-0000-0000-000000000001',
        'User Teste',
        NOW(),
        NOW());

INSERT INTO shortages (id, product, category, quantity, status, shortage_type,
                       created_by_id, created_by_name, shortage_order_id, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000061',
        'Dipirona 500mg',
        'MEDICAMENTOS',
        10,
        'PENDING',
        'WANIA',
        '00000000-0000-0000-0000-000000000001',
        'User Teste',
        '00000000-0000-0000-0000-000000000060',
        NOW(),
        NOW());
