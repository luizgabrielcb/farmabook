INSERT INTO users (id, name, pin_hash, role, active, created_at, updated_at)
VALUES (gen_random_uuid(),
        'User Teste',
        '$2a$04$gMBaXoAuRMITzBR9.KVvqOliIRmtZtyHHLpp4aHs7kOWeAbTIeumO',
        'ADMIN',
        true,
        NOW(),
        NOW());