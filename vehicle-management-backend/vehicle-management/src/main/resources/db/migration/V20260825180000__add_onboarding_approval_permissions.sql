-- Dedicated onboarding approval permissions keep reviewer authorization independent from role codes.
-- Default matrix:
--   SYSTEM_ADMIN    -> system-admin and parking-manager onboarding
--   PARKING_MANAGER -> employee and customer onboarding

INSERT INTO iam.permission_modules (
    module_id, code, name, description, created_at, created_by, updated_at, updated_by
) VALUES (
    '4a7d0100-0000-4000-8000-000000000001',
    'ONBOARDING_APPROVAL',
    'Duyệt onboarding',
    'Quản lý quyền xem và phê duyệt hồ sơ onboarding theo từng nhóm đối tượng.',
    now(), NULL, NULL, NULL
);

INSERT INTO iam.permission_actions (
    action_id, code, name, description, created_at, created_by, updated_at, updated_by
) VALUES
    ('4a7d0200-0000-4000-8000-000000000001', 'REVIEW_SYSTEM_ADMIN', 'Duyệt System Admin', 'Xem và phê duyệt hồ sơ onboarding của System Admin.', now(), NULL, NULL, NULL),
    ('4a7d0200-0000-4000-8000-000000000002', 'REVIEW_PARKING_MANAGER', 'Duyệt Parking Manager', 'Xem và phê duyệt hồ sơ onboarding của Parking Manager.', now(), NULL, NULL, NULL),
    ('4a7d0200-0000-4000-8000-000000000003', 'REVIEW_EMPLOYEE', 'Duyệt nhân viên', 'Xem và phê duyệt hồ sơ onboarding của nhân viên và role nội bộ tùy chỉnh.', now(), NULL, NULL, NULL),
    ('4a7d0200-0000-4000-8000-000000000004', 'REVIEW_CUSTOMER', 'Duyệt khách hàng', 'Xem và phê duyệt hồ sơ onboarding của khách hàng.', now(), NULL, NULL, NULL);

INSERT INTO iam.permissions (
    permission_id, permission_code, name, description,
    created_at, created_by, updated_at, updated_by,
    module_id, action_id, scope_id
) VALUES
    (
        '4a7d0300-0000-4000-8000-000000000001',
        'ONBOARDING_APPROVAL_REVIEW_SYSTEM_ADMIN_ALL',
        'Duyệt onboarding System Admin',
        'Cho phép xem và phê duyệt hồ sơ onboarding của System Admin.',
        now(), NULL, NULL, NULL,
        '4a7d0100-0000-4000-8000-000000000001',
        '4a7d0200-0000-4000-8000-000000000001',
        '00000000-0000-0000-0000-000000003001'
    ),
    (
        '4a7d0300-0000-4000-8000-000000000002',
        'ONBOARDING_APPROVAL_REVIEW_PARKING_MANAGER_ALL',
        'Duyệt onboarding Parking Manager',
        'Cho phép xem và phê duyệt hồ sơ onboarding của Parking Manager.',
        now(), NULL, NULL, NULL,
        '4a7d0100-0000-4000-8000-000000000001',
        '4a7d0200-0000-4000-8000-000000000002',
        '00000000-0000-0000-0000-000000003001'
    ),
    (
        '4a7d0300-0000-4000-8000-000000000003',
        'ONBOARDING_APPROVAL_REVIEW_EMPLOYEE_ALL',
        'Duyệt onboarding nhân viên',
        'Cho phép xem và phê duyệt hồ sơ onboarding của nhân viên và role nội bộ tùy chỉnh.',
        now(), NULL, NULL, NULL,
        '4a7d0100-0000-4000-8000-000000000001',
        '4a7d0200-0000-4000-8000-000000000003',
        '00000000-0000-0000-0000-000000003001'
    ),
    (
        '4a7d0300-0000-4000-8000-000000000004',
        'ONBOARDING_APPROVAL_REVIEW_CUSTOMER_ALL',
        'Duyệt onboarding khách hàng',
        'Cho phép xem và phê duyệt hồ sơ onboarding của khách hàng.',
        now(), NULL, NULL, NULL,
        '4a7d0100-0000-4000-8000-000000000001',
        '4a7d0200-0000-4000-8000-000000000004',
        '00000000-0000-0000-0000-000000003001'
    );

INSERT INTO iam.role_permissions (
    id, role_id, permission_id, created_at, created_by, updated_at, updated_by, is_active, is_system
) VALUES
    (
        '4a7d0400-0000-4000-8000-000000000001',
        '00000000-0000-0000-0000-000000000001',
        '4a7d0300-0000-4000-8000-000000000001',
        now(), NULL, NULL, NULL, true, true
    ),
    (
        '4a7d0400-0000-4000-8000-000000000002',
        '00000000-0000-0000-0000-000000000001',
        '4a7d0300-0000-4000-8000-000000000002',
        now(), NULL, NULL, NULL, true, true
    ),
    (
        '4a7d0400-0000-4000-8000-000000000003',
        '00000000-0000-0000-0000-000000000005',
        '4a7d0300-0000-4000-8000-000000000003',
        now(), NULL, NULL, NULL, true, true
    ),
    (
        '4a7d0400-0000-4000-8000-000000000004',
        '00000000-0000-0000-0000-000000000005',
        '4a7d0300-0000-4000-8000-000000000004',
        now(), NULL, NULL, NULL, true, true
    );
