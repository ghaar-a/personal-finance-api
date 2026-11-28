-- Align NOT NULL constraints with the JPA entity and the existing column defaults.
UPDATE accounts SET balance = 0.00 WHERE balance IS NULL;
UPDATE accounts SET currency = 'BRL' WHERE currency IS NULL;
UPDATE accounts SET status = 'ACTIVE' WHERE status IS NULL;

ALTER TABLE accounts
    ALTER COLUMN balance SET NOT NULL,
    ALTER COLUMN currency SET NOT NULL,
    ALTER COLUMN status SET NOT NULL;