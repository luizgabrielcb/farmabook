DELETE FROM users WHERE name != 'User Teste';
UPDATE users
SET pin_hash = '$2a$04$gMBaXoAuRMITzBR9.KVvqOliIRmtZtyHHLpp4aHs7kOWeAbTIeumO',
    active   = true
WHERE name = 'User Teste';
