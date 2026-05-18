ALTER TABLE people.customers
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE people.customers
    ADD CONSTRAINT ck_customers_status CHECK (status IN ('ACTIVE', 'INACTIVE'));
