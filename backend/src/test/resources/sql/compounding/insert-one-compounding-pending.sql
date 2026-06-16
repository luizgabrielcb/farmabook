INSERT INTO customers (id, name, phone_number, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001',
        'Maria Silva',
        '11999999999',
        NOW(),
        NOW());

INSERT INTO compounding_pharmacies (id, name, city, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000060',
        'Farmácia Magistral Central',
        'São Paulo',
        NOW(),
        NOW());

INSERT INTO compoundings (id, quantity, customer_id, customer_name, pharmacy_id, pharmacy_name, pharmacy_city,
                          value, observations, status, payment_status, created_by_id, created_by_name,
                          created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000070',
        2,
        '00000000-0000-0000-0000-000000000001',
        'Maria Silva',
        '00000000-0000-0000-0000-000000000060',
        'Farmácia Magistral Central',
        'São Paulo',
        150.00,
        'Sem observações',
        'PENDING',
        'TO_PAY',
        '00000000-0000-0000-0000-000000000099',
        'User Teste',
        NOW(),
        NOW());
