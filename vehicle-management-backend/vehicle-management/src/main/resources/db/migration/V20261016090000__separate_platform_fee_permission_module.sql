-- Keep platform-fee permissions isolated from the WALLET IAM module.
-- V20261014100000 reused the WALLET module UUID, so its module insert was ignored
-- and the six platform-fee permissions were attached to WALLET instead.

INSERT INTO iam.permission_modules (
    module_id,
    code,
    name,
    description,
    created_at
)
VALUES (
    '00000000-0000-0000-0000-000000001046',
    'PLATFORM_FEE',
    'Phí sàn',
    'Quản trị các loại phí sàn áp dụng toàn hệ thống cho giao dịch mua vé.',
    now()
)
ON CONFLICT DO NOTHING;

UPDATE iam.permissions
SET module_id = '00000000-0000-0000-0000-000000001046'
WHERE permission_code IN (
    'PLATFORM_FEE_CREATE_ALL',
    'PLATFORM_FEE_READ_ALL',
    'PLATFORM_FEE_UPDATE_ALL',
    'PLATFORM_FEE_ACTIVATE_ALL',
    'PLATFORM_FEE_ARCHIVE_ALL',
    'PLATFORM_FEE_PREVIEW_ALL'
);
