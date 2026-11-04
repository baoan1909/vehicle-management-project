CREATE TABLE parking.parking_lot_reviews (
    review_id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    organization_id uuid NOT NULL,
    parking_lot_id uuid NOT NULL,
    customer_id uuid NOT NULL,
    rating smallint NOT NULL,
    comment varchar(2000),
    source_parking_session_id uuid,
    source_payment_id uuid,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamptz,
    updated_by uuid,
    CONSTRAINT uq_parking_lot_reviews_customer_lot UNIQUE (customer_id, parking_lot_id),
    CONSTRAINT ck_parking_lot_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT ck_parking_lot_reviews_single_source
        CHECK (num_nonnulls(source_parking_session_id, source_payment_id) = 1),
    CONSTRAINT fk_parking_lot_reviews_customer
        FOREIGN KEY (customer_id) REFERENCES people.customers(customer_id) ON DELETE RESTRICT,
    CONSTRAINT fk_parking_lot_reviews_lot_tenant
        FOREIGN KEY (organization_id, parking_lot_id)
        REFERENCES parking.parking_lots(organization_id, parking_lot_id) ON DELETE RESTRICT,
    CONSTRAINT fk_parking_lot_reviews_session
        FOREIGN KEY (source_parking_session_id)
        REFERENCES parking.parking_sessions(parking_session_id) ON DELETE RESTRICT,
    CONSTRAINT fk_parking_lot_reviews_payment
        FOREIGN KEY (source_payment_id) REFERENCES billing.payments(payment_id) ON DELETE RESTRICT
);

CREATE INDEX idx_parking_lot_reviews_lot_created
    ON parking.parking_lot_reviews(parking_lot_id, created_at DESC, review_id DESC);
CREATE INDEX idx_parking_lot_reviews_org_lot_created
    ON parking.parking_lot_reviews(organization_id, parking_lot_id, created_at DESC);
CREATE INDEX idx_parking_lot_reviews_lot_rating
    ON parking.parking_lot_reviews(parking_lot_id, rating);

INSERT INTO iam.permission_modules (module_id, code, name, description, created_at)
SELECT gen_random_uuid(), 'PARKING_LOT_REVIEW', 'Đánh giá bãi xe',
       'Đánh giá của khách hàng sau giao dịch tại bãi xe.', now()
WHERE NOT EXISTS (SELECT 1 FROM iam.permission_modules WHERE code = 'PARKING_LOT_REVIEW');

WITH permission_seed(permission_code, name, description, action_code, scope_code) AS (
    VALUES
        ('PARKING_LOT_REVIEW_CREATE_OWN', 'Tạo đánh giá bãi xe',
         'Cho phép khách hàng tạo đánh giá cho bãi xe đã phát sinh giao dịch.', 'CREATE', 'OWN'),
        ('PARKING_LOT_REVIEW_UPDATE_OWN', 'Cập nhật đánh giá bãi xe',
         'Cho phép khách hàng cập nhật đánh giá bãi xe của mình.', 'UPDATE', 'OWN'),
        ('PARKING_LOT_REVIEW_READ_ALL', 'Xem đánh giá bãi xe',
         'Cho phép xem đánh giá trong phạm vi bãi xe được quản lý hoặc phân công.', 'READ', 'ALL')
)
INSERT INTO iam.permissions (
    permission_id, permission_code, name, description, created_at,
    module_id, action_id, scope_id
)
SELECT gen_random_uuid(), seed.permission_code, seed.name, seed.description, now(),
       module.module_id, action.action_id, scope.scope_id
FROM permission_seed seed
JOIN iam.permission_modules module ON module.code = 'PARKING_LOT_REVIEW'
JOIN iam.permission_actions action ON action.code = seed.action_code
JOIN iam.permission_scopes scope ON scope.code = seed.scope_code
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO iam.role_permissions (id, role_id, permission_id, created_at, is_active, is_system)
SELECT gen_random_uuid(), role.role_id, permission.permission_id, now(), true, true
FROM iam.roles role
JOIN iam.permissions permission ON (
    role.code = 'CUSTOMER'
    AND permission.permission_code IN (
        'PARKING_LOT_REVIEW_CREATE_OWN', 'PARKING_LOT_REVIEW_UPDATE_OWN'
    )
) OR (
    role.code IN ('PARTNER_ADMIN', 'PARKING_MANAGER', 'EMPLOYEE')
    AND permission.permission_code = 'PARKING_LOT_REVIEW_READ_ALL'
)
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = true, updated_at = now();
