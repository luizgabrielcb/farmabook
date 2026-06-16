INSERT INTO customers (id, name, phone_number, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001',
        'Maria Silva',
        '11999999999',
        NOW(),
        NOW());

INSERT INTO orders (id, customer_id, customer_name, status, created_by_id, created_by_name, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000010',
        '00000000-0000-0000-0000-000000000001',
        'Maria Silva',
        'RECEIVED',
        '00000000-0000-0000-0000-000000000099',
        'User Teste',
        NOW(),
        NOW());

INSERT INTO notifications (id, order_id, customer_id, customer_phone, customer_name, message, link, sent_at, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000020',
        '00000000-0000-0000-0000-000000000010',
        '00000000-0000-0000-0000-000000000001',
        '5511999999999',
        'Maria Silva',
        'Boa tarde, Maria Silva! Tudo bem? A sua encomenda acabou de chegar aqui na farmácia.',
        'https://wa.me/5511999999999?text=Boa+tarde%2C+Maria+Silva%21+Tudo+bem%3F+A+sua+encomenda+acabou+de+chegar+aqui+na+farm%C3%A1cia.',
        NOW(),
        NOW(),
        NOW());
