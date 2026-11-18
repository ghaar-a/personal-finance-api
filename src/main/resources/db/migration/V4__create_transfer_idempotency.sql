
CREATE TABLE transfer_idempotency (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id UUID NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,

    source_account_id UUID NOT NULL,
    destination_account_id UUID NOT NULL,

    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_transfer_idempotency_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT fk_transfer_idempotency_source_account
        FOREIGN KEY (source_account_id)
        REFERENCES accounts(id),

    CONSTRAINT fk_transfer_idempotency_destination_account
        FOREIGN KEY (destination_account_id)
        REFERENCES accounts(id),

    CONSTRAINT uk_transfer_idempotency_user_key
        UNIQUE (user_id, idempotency_key),

    CONSTRAINT chk_transfer_idempotency_amount
        CHECK (amount > 0),

    CONSTRAINT chk_transfer_idempotency_different_accounts
        CHECK (source_account_id <> destination_account_id)
);

CREATE INDEX idx_transfer_idempotency_created_at
    ON transfer_idempotency(created_at);
