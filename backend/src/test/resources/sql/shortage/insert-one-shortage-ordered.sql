INSERT INTO shortages (id, product, category, quantity, status, shortage_type, created_by_id, created_by_name,
                       ordered_by_id, ordered_by_name, ordered_at, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001',
        'Dipirona 500mg',
        'MEDICAMENTOS',
        5,
        'ORDERED', 'WANIA',
        '00000000-0000-0000-0000-000000000099',
        'User Teste',
        '00000000-0000-0000-0000-000000000099',
        'User Teste',
        NOW(),
        NOW(),
        NOW());
