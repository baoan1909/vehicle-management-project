ALTER TABLE notification.user_guides DROP CONSTRAINT IF EXISTS ck_user_guides_display_type;
ALTER TABLE notification.user_guides ADD CONSTRAINT ck_user_guides_display_type CHECK (display_type IN (
    'MODAL', 'BOTTOM_SHEET', 'COACH_MARK', 'TOOLTIP', 'POPOVER', 'BANNER',
    'INLINE_CARD', 'CALLOUT', 'JOURNEY'
));

CREATE TABLE IF NOT EXISTS notification.user_guide_journey_step_configs (
    step_id UUID PRIMARY KEY,
    guide_version_id UUID NOT NULL,
    step_key VARCHAR(120) NOT NULL,
    readiness_key VARCHAR(120) NOT NULL,
    route_template VARCHAR(1000),
    action_label VARCHAR(100),
    linked_guide_id UUID,
    state_content JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT fk_user_guide_journey_step_config_step FOREIGN KEY (step_id)
        REFERENCES notification.user_guide_steps(step_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_guide_journey_step_config_version FOREIGN KEY (guide_version_id)
        REFERENCES notification.user_guide_versions(guide_version_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_guide_journey_step_config_linked_guide FOREIGN KEY (linked_guide_id)
        REFERENCES notification.user_guides(guide_id) ON DELETE RESTRICT,
    CONSTRAINT uq_user_guide_journey_step_config_version_key UNIQUE (guide_version_id, step_key),
    CONSTRAINT ck_user_guide_journey_step_key_upper CHECK (step_key ~ '^[A-Z0-9_-]+$'),
    CONSTRAINT ck_user_guide_journey_readiness_key_upper CHECK (readiness_key ~ '^[A-Z0-9_-]+$')
);

CREATE INDEX IF NOT EXISTS idx_user_guide_journey_step_config_version
    ON notification.user_guide_journey_step_configs(guide_version_id);
CREATE INDEX IF NOT EXISTS idx_user_guide_journey_step_config_linked
    ON notification.user_guide_journey_step_configs(linked_guide_id);

CREATE TEMP TABLE journey_guide_seed (
    guide_code VARCHAR(120) NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary TEXT NOT NULL,
    audience_mode VARCHAR(30) NOT NULL,
    permission_codes TEXT[] NOT NULL,
    priority INTEGER NOT NULL
) ON COMMIT DROP;

INSERT INTO journey_guide_seed VALUES
('POST_ONBOARDING_PARTNER_JOURNEY', 'Lộ trình từ thiết lập đến vận hành',
 'Theo dõi dữ liệu thực tế từ tạo bãi, cấu hình dịch vụ đến check-in/check-out.',
 'PERMISSION_ALL', ARRAY['PARKING_LOT_CREATE_ALL','PRICE_PLAN_CREATE_ALL'], 1),
('POST_ONBOARDING_MANAGER_JOURNEY', 'Lộ trình bàn giao và vận hành bãi',
 'Hoàn thiện các dependency tại bãi được phân công trước khi vận hành.',
 'PERMISSION_ALL', ARRAY['PARKING_LOT_READ_ALL','PARKING_SPACE_CONFIGURE_ALL'], 2),
('POST_ONBOARDING_EMPLOYEE_JOURNEY', 'Lộ trình chuẩn bị ca và vận hành',
 'Kiểm tra phân công, ca và capability riêng cho xe vào/xe ra.',
 'PERMISSION_ANY', ARRAY['PARKING_SESSION_CHECK_IN_ALL','PARKING_SESSION_CHECK_OUT_ALL'], 3);

CREATE TEMP TABLE journey_step_seed (
    display_order INTEGER NOT NULL,
    step_key VARCHAR(120) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    route_template VARCHAR(1000),
    action_label VARCHAR(100),
    linked_guide_code VARCHAR(120),
    state_content JSONB NOT NULL
) ON COMMIT DROP;

INSERT INTO journey_step_seed VALUES
(1, 'ACCOUNT_ACCESS', 'Tài khoản và onboarding', 'Kiểm tra trạng thái tài khoản và quyền truy cập hiện hành.',
 '/admin/profile', 'Xem hồ sơ', NULL,
 '{"COMPLETED":{"description":"Tài khoản và onboarding đang hợp lệ."},"BLOCKED":{"description":"Tài khoản hoặc onboarding chưa đáp ứng điều kiện.","ownerHint":"Quản trị tài khoản"}}'),
(2, 'PARKING_SCOPE', 'Phạm vi bãi xe', 'Kiểm tra organization và bãi được phân công.',
 '/admin/parking-structure?action=create', 'Thiết lập bãi', 'PARKING_LOT_CREATE',
 '{"COMPLETED":{"description":"Đã có bãi trong phạm vi hiện tại."},"ACTIONABLE":{"description":"Bạn có thể tạo bãi đầu tiên.","actionLabel":"Tạo bãi"},"WAITING_FOR_PERMISSION":{"description":"Cần được phân công bãi hoặc organization.","ownerHint":"Người quản lý organization"}}'),
(3, 'CATALOG_PRICING', 'Loại xe, loại vé và giá', 'Chuẩn bị catalog và cấu hình giá cho dịch vụ tại bãi.',
 '/admin/vehicle', 'Cấu hình dịch vụ', 'CATALOG_VEHICLE_TYPE_SETUP',
 '{"COMPLETED":{"description":"Catalog và giá đã đáp ứng dịch vụ tại bãi."},"DEPENDENCY_REQUIRED":{"description":"Cần hoàn thiện loại xe, loại vé hoặc giá công bố.","actionLabel":"Cấu hình dịch vụ"},"WAITING_FOR_PERMISSION":{"description":"Cần người có quyền catalog hoặc giá xử lý.","ownerHint":"Quản trị catalog/giá"}}'),
(4, 'PARKING_TOPOLOGY', 'Cấu trúc, làn và layout', 'Chuẩn bị zone, cổng, làn IN/OUT và layout cần thiết.',
 '/admin/parking-structure?lot={PARKING_LOT_ID}', 'Thiết lập cấu trúc', 'PARKING_STRUCTURE_SETUP',
 '{"COMPLETED":{"description":"Cấu trúc bãi đã đáp ứng điều kiện."},"DEPENDENCY_REQUIRED":{"description":"Cần zone, cổng, làn IN/OUT và layout đã xuất bản.","actionLabel":"Thiết lập cấu trúc"},"WAITING_FOR_PERMISSION":{"description":"Cần người có quyền topology/layout xử lý.","ownerHint":"Quản trị cấu trúc bãi"}}'),
(5, 'PARKING_ACTIVATION', 'Readiness và kích hoạt bãi', 'Đánh giá điều kiện và kích hoạt hoặc gửi yêu cầu kích hoạt.',
 '/admin/parking-lots?lotId={PARKING_LOT_ID}', 'Kiểm tra kích hoạt', 'PARKING_LOT_ACTIVATION',
 '{"COMPLETED":{"description":"Bãi đang hoạt động."},"ACTIONABLE":{"description":"Bãi đã đủ readiness để kích hoạt.","actionLabel":"Kích hoạt bãi"},"DEPENDENCY_REQUIRED":{"description":"Bãi còn thiếu địa chỉ, catalog/giá hoặc topology."},"WAITING_FOR_PERMISSION":{"description":"Cần người có quyền kích hoạt xử lý.","ownerHint":"Quản trị bãi"}}'),
(6, 'WORKFORCE_SHIFT', 'Phân công và ca vận hành', 'Kiểm tra phân công, scope và ca OPEN tại bãi.',
 '/admin/shifts', 'Xem ca vận hành', NULL,
 '{"COMPLETED":{"description":"Điều kiện phân công và ca hiện tại hợp lệ."},"BLOCKED":{"description":"Phân công, scope hoặc ca OPEN chưa đáp ứng.","ownerHint":"Quản lý nhân sự/ca"}}'),
(7, 'CHECK_IN', 'Xe vào', 'Thực hiện check-in đúng bãi và làn IN.',
 '/admin/swipe', 'Thực hiện xe vào', 'PARKING_CHECK_IN_FIRST_RUN',
 '{"ACTIONABLE":{"description":"Có thể thực hiện check-in tại bãi đã chọn.","actionLabel":"Mở xe vào"},"COMPLETED":{"description":"Điều kiện check-in đã sẵn sàng."},"BLOCKED":{"description":"Bãi hoặc ca chưa sẵn sàng cho check-in."},"WAITING_FOR_PERMISSION":{"description":"Tài khoản chưa có quyền check-in.","ownerHint":"Người có quyền vận hành xe vào"}}'),
(8, 'FIND_SESSION', 'Tìm phiên đang mở', 'Tìm đúng phiên trước khi chuẩn bị xe ra.',
 '/admin/swipe/sessions', 'Tìm phiên', NULL,
 '{"ACTIONABLE":{"description":"Có thể tìm phiên đang mở."},"BLOCKED":{"description":"Bãi hoặc ca chưa sẵn sàng."},"WAITING_FOR_PERMISSION":{"description":"Tài khoản chưa có quyền đọc/vận hành phiên."}}'),
(9, 'PREPARE_CHECK_OUT', 'Chuẩn bị xe ra và tính phí', 'Đối chiếu dữ liệu và tính phí trước checkout.',
 '/admin/swipe', 'Chuẩn bị xe ra', 'PARKING_CHECK_OUT_FIRST_RUN',
 '{"ACTIONABLE":{"description":"Có thể chuẩn bị checkout tại bãi đã chọn."},"BLOCKED":{"description":"Bãi hoặc ca chưa sẵn sàng cho checkout."},"WAITING_FOR_PERMISSION":{"description":"Tài khoản chưa có quyền checkout."}}'),
(10, 'COLLECT_PAYMENT', 'Ghi nhận thanh toán', 'Ghi nhận thanh toán khi hóa đơn yêu cầu.',
 '/admin/invoices', 'Xem hóa đơn', NULL,
 '{"ACTIONABLE":{"description":"Có thể ghi nhận thanh toán."},"NOT_APPLICABLE":{"description":"Bước này không bắt buộc với mọi phiên."},"BLOCKED":{"description":"Bãi hoặc ca chưa sẵn sàng."},"WAITING_FOR_PERMISSION":{"description":"Tài khoản chưa có quyền thanh toán.","ownerHint":"Thu ngân hoặc người có quyền thanh toán"}}'),
(11, 'COMPLETE_CHECK_OUT', 'Hoàn tất xe ra', 'Hoàn tất checkout sau khi đối chiếu và thanh toán hợp lệ.',
 '/admin/swipe', 'Hoàn tất xe ra', 'PARKING_CHECK_OUT_FIRST_RUN',
 '{"ACTIONABLE":{"description":"Có thể hoàn tất checkout."},"BLOCKED":{"description":"Bãi, ca hoặc dependency checkout chưa sẵn sàng."},"WAITING_FOR_PERMISSION":{"description":"Tài khoản chưa có quyền checkout."}}'),
(12, 'EXCEPTIONS', 'Mất thẻ và sai lệch biển số', 'Xử lý ngoại lệ theo đúng capability, không bypass.',
 '/admin/lost-card-reports', 'Xử lý ngoại lệ', NULL,
 '{"ACTIONABLE":{"description":"Có thể xử lý ngoại lệ theo quyền hiện tại."},"NOT_APPLICABLE":{"description":"Chưa phát sinh ngoại lệ cần xử lý."},"WAITING_FOR_PERMISSION":{"description":"Cần người có quyền lost-card hoặc review/override.","ownerHint":"Người xử lý ngoại lệ"}}');

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM notification.user_guide_features WHERE code = 'OPERATIONS_ONBOARDING') THEN
        RAISE EXCEPTION 'OPERATIONS_ONBOARDING feature must exist before journey seed';
    END IF;
    IF EXISTS (
        SELECT 1 FROM journey_guide_seed g
        CROSS JOIN LATERAL unnest(g.permission_codes) p(code)
        LEFT JOIN iam.permissions permission ON permission.permission_code = p.code
        WHERE permission.permission_id IS NULL
    ) THEN
        RAISE EXCEPTION 'Journey seed references an unknown permission';
    END IF;
    IF EXISTS (
        SELECT 1 FROM journey_step_seed s
        LEFT JOIN notification.user_guides linked ON linked.code = s.linked_guide_code
        WHERE s.linked_guide_code IS NOT NULL AND linked.guide_id IS NULL
    ) THEN
        RAISE EXCEPTION 'Journey seed references an unknown linked coach mark';
    END IF;
END $$;

INSERT INTO notification.user_guides (
    guide_id, feature_id, code, status, display_type, replay_policy, context_key,
    priority, display_order, start_at, current_version
)
SELECT md5('vehicle-journey-guide:' || seed.guide_code)::uuid, feature.feature_id,
       seed.guide_code, 'PUBLISHED', 'JOURNEY', 'MANUAL_ONLY', '/ADMIN/HEADER',
       seed.priority, seed.priority, TIMESTAMPTZ '2026-01-01 00:00:00+07', 1
FROM journey_guide_seed seed
JOIN notification.user_guide_features feature ON feature.code = 'OPERATIONS_ONBOARDING'
ON CONFLICT (code) DO NOTHING;

INSERT INTO notification.user_guide_versions (
    guide_version_id, guide_id, version_number, title, summary, deep_link,
    audience_mode, published_at
)
SELECT md5('vehicle-journey-version:' || seed.guide_code || ':1')::uuid,
       guide.guide_id, 1, seed.title, seed.summary, '/admin/dashboard',
       seed.audience_mode, TIMESTAMPTZ '2026-11-16 00:00:00+07'
FROM journey_guide_seed seed
JOIN notification.user_guides guide ON guide.code = seed.guide_code
ON CONFLICT (guide_id, version_number) DO NOTHING;

INSERT INTO notification.user_guide_steps (
    step_id, guide_version_id, display_order, title, body, placement, collapse_ms
)
SELECT md5('vehicle-journey-step:' || guide_seed.guide_code || ':' || step_seed.step_key)::uuid,
       version.guide_version_id, step_seed.display_order, step_seed.title, step_seed.body, 'CENTER', 0
FROM journey_guide_seed guide_seed
JOIN notification.user_guides guide ON guide.code = guide_seed.guide_code
JOIN notification.user_guide_versions version ON version.guide_id = guide.guide_id AND version.version_number = 1
CROSS JOIN journey_step_seed step_seed
ON CONFLICT (guide_version_id, display_order) DO NOTHING;

INSERT INTO notification.user_guide_journey_step_configs (
    step_id, guide_version_id, step_key, readiness_key, route_template,
    action_label, linked_guide_id, state_content
)
SELECT step.step_id, version.guide_version_id, step_seed.step_key, step_seed.step_key,
       step_seed.route_template, step_seed.action_label, linked.guide_id, step_seed.state_content
FROM journey_guide_seed guide_seed
JOIN notification.user_guides guide ON guide.code = guide_seed.guide_code
JOIN notification.user_guide_versions version ON version.guide_id = guide.guide_id AND version.version_number = 1
CROSS JOIN journey_step_seed step_seed
JOIN notification.user_guide_steps step ON step.guide_version_id = version.guide_version_id
    AND step.display_order = step_seed.display_order
LEFT JOIN notification.user_guides linked ON linked.code = step_seed.linked_guide_code
ON CONFLICT (step_id) DO NOTHING;

INSERT INTO notification.user_guide_audiences (audience_id, guide_version_id, target_type, permission_id)
SELECT md5('vehicle-journey-audience:' || seed.guide_code || ':' || required.permission_code)::uuid,
       version.guide_version_id, 'PERMISSION', permission.permission_id
FROM journey_guide_seed seed
JOIN notification.user_guides guide ON guide.code = seed.guide_code
JOIN notification.user_guide_versions version ON version.guide_id = guide.guide_id AND version.version_number = 1
CROSS JOIN LATERAL unnest(seed.permission_codes) required(permission_code)
JOIN iam.permissions permission ON permission.permission_code = required.permission_code
ON CONFLICT DO NOTHING;
