-- Extensão moderna para UUID
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ENUMS (tipagem forte)
CREATE TYPE user_role AS ENUM ('ROLE_USER', 'ROLE_ADMIN');
CREATE TYPE account_status AS ENUM ('ACTIVE', 'BLOCKED', 'CLOSED');
CREATE TYPE transaction_type_enum AS ENUM ('DEBIT','CREDIT','TRANSFER');

-- 1. USERS
CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       full_name VARCHAR(100) NOT NULL,
                       email VARCHAR(150) UNIQUE NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       role user_role DEFAULT 'ROLE_USER',
                       enabled BOOLEAN DEFAULT TRUE,
                       created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 2. ACCOUNTS
CREATE TABLE accounts (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          user_id UUID NOT NULL,
                          account_number VARCHAR(20) UNIQUE NOT NULL,
                          balance NUMERIC(19,2) DEFAULT 0.00,
                          currency VARCHAR(3) DEFAULT 'BRL',
                          status account_status DEFAULT 'ACTIVE',
                          created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

                          CONSTRAINT fk_account_user
                              FOREIGN KEY (user_id)
                                  REFERENCES users(id)
                                  ON DELETE CASCADE,

                          CONSTRAINT check_balance_non_negative
                              CHECK (balance >= 0)
);

-- 3. TRANSACTIONS
CREATE TABLE transactions (
                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              account_id UUID NOT NULL,
                              amount NUMERIC(19,2) NOT NULL,
                              transaction_type transaction_type_enum NOT NULL,
                              description VARCHAR(255),
                              idempotency_key VARCHAR(100),
                              created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

                              CONSTRAINT fk_transaction_account
                                  FOREIGN KEY (account_id)
                                      REFERENCES accounts(id),

                              CONSTRAINT check_amount_positive
                                  CHECK (amount > 0)
);

-- 4. LOGIN ATTEMPTS
CREATE TABLE login_attempts (
                                id BIGSERIAL PRIMARY KEY,
                                email VARCHAR(150) NOT NULL,
                                success BOOLEAN NOT NULL,
                                ip_address VARCHAR(45),
                                attempt_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 5. AUDIT LOGS
CREATE TABLE audit_logs (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            entity_name VARCHAR(50) NOT NULL,
                            entity_id UUID NOT NULL,
                            action VARCHAR(20) NOT NULL,
                            old_value JSONB,
                            new_value JSONB,
                            changed_by UUID,
                            changed_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- ÍNDICES PROFISSIONAIS
CREATE INDEX idx_users_email ON users(email);

CREATE INDEX idx_accounts_user
    ON accounts(user_id);

CREATE INDEX idx_transactions_account
    ON transactions(account_id);

CREATE INDEX idx_transactions_idempotency
    ON transactions(idempotency_key);

CREATE INDEX idx_login_attempts_email
    ON login_attempts(email);

CREATE INDEX idx_audit_entity
    ON audit_logs(entity_name, entity_id);