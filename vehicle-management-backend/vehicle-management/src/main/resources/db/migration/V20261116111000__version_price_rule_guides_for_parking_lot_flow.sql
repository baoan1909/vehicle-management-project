CREATE TEMP TABLE price_rule_guide_upgrade (
    guide_code varchar(120) PRIMARY KEY,
    guide_version_id uuid NOT NULL,
    title varchar(255) NOT NULL,
    summary text NOT NULL,
    deep_link varchar(1000) NOT NULL
) ON COMMIT DROP;

INSERT INTO price_rule_guide_upgrade VALUES
('PRICE_RULE_SETUP', md5('price-rule-guide:PRICE_RULE_SETUP:2')::uuid,
 'Thiết lập bảng giá theo bãi',
 'Chọn bãi, khởi tạo bản nháp từ mẫu rồi rà soát và kích hoạt từng quy tắc giá.',
 '/admin/price-rules'),
('WORKFLOW_RULE_DRAFT', md5('price-rule-guide:WORKFLOW_RULE_DRAFT:2')::uuid,
 'Từ bảng giá nháp theo bãi đến phủ đủ dịch vụ',
 'Mỗi bãi có bộ quy tắc giá độc lập; catalog xe, vé và kế hoạch giá vẫn dùng chung trong Partner.',
 '/admin/price-rules');

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM price_rule_guide_upgrade upgrade
        LEFT JOIN notification.user_guides guide ON guide.code = upgrade.guide_code
        WHERE guide.guide_id IS NULL
    ) THEN
        RAISE EXCEPTION 'Price-rule guide upgrade references an unknown guide';
    END IF;
END $$;

INSERT INTO notification.user_guide_versions (
    guide_version_id, guide_id, version_number, title, summary, deep_link,
    audience_mode, published_at
)
SELECT upgrade.guide_version_id, guide.guide_id, 2, upgrade.title, upgrade.summary,
       upgrade.deep_link, previous.audience_mode, now()
FROM price_rule_guide_upgrade upgrade
JOIN notification.user_guides guide ON guide.code = upgrade.guide_code
JOIN LATERAL (
    SELECT version.*
    FROM notification.user_guide_versions version
    WHERE version.guide_id = guide.guide_id
    ORDER BY version.version_number DESC
    LIMIT 1
) previous ON true
ON CONFLICT (guide_id, version_number) DO NOTHING;

INSERT INTO notification.user_guide_steps (
    step_id, guide_version_id, display_order, title, body, anchor_key, placement, collapse_ms
) VALUES
(md5('price-rule-guide:PRICE_RULE_SETUP:2:1')::uuid, md5('price-rule-guide:PRICE_RULE_SETUP:2')::uuid, 1,
 '1. Chọn bãi xe', 'Bảng giá thuộc bãi. Chọn đúng bãi trong phạm vi được phân công trước khi xem hoặc thay đổi quy tắc.',
 'PRICING.RULE.PARKING_LOT', 'BOTTOM', 0),
(md5('price-rule-guide:PRICE_RULE_SETUP:2:2')::uuid, md5('price-rule-guide:PRICE_RULE_SETUP:2')::uuid, 2,
 '2. Khởi tạo bảng giá nháp', 'Sao chép mẫu nội bộ thành bộ nháp độc lập của bãi. Thao tác có thể chạy lại an toàn và không tự kích hoạt giá.',
 'PRICING.RULE.INITIALIZE_DRAFTS', 'BOTTOM', 0),
(md5('price-rule-guide:PRICE_RULE_SETUP:2:3')::uuid, md5('price-rule-guide:PRICE_RULE_SETUP:2')::uuid, 3,
 '3. Chọn kế hoạch', 'Chọn kế hoạch dùng chung của Partner đúng nhóm VISITOR hoặc CUSTOMER.',
 'PRICING.RULE.PLAN', 'LEFT', 0),
(md5('price-rule-guide:PRICE_RULE_SETUP:2:4')::uuid, md5('price-rule-guide:PRICE_RULE_SETUP:2')::uuid, 4,
 '4. Hoàn thiện quy tắc', 'Kiểm tra loại xe, loại vé, giá, đơn vị, khung giờ và ưu tiên trong chính bãi đang chọn.',
 'PRICING.RULE.VALIDATION', 'LEFT', 0),
(md5('price-rule-guide:PRICE_RULE_SETUP:2:5')::uuid, md5('price-rule-guide:PRICE_RULE_SETUP:2')::uuid, 5,
 '5. Lưu nháp và kích hoạt có chủ đích', 'Lưu không làm giá có hiệu lực. Chỉ kích hoạt sau khi dependency và coverage của bãi đã hợp lệ.',
 'PRICING.RULE.SUBMIT', 'LEFT', 0),
(md5('price-rule-guide:WORKFLOW_RULE_DRAFT:2:1')::uuid, md5('price-rule-guide:WORKFLOW_RULE_DRAFT:2')::uuid, 1,
 '1. Chọn đúng bãi', 'Price Rule là cấu hình theo bãi; người quản lý được phân công có thể thao tác trong đúng scope của mình.',
 'PRICING.RULE.PARKING_LOT', 'BOTTOM', 0),
(md5('price-rule-guide:WORKFLOW_RULE_DRAFT:2:2')::uuid, md5('price-rule-guide:WORKFLOW_RULE_DRAFT:2')::uuid, 2,
 '2. Khởi tạo hoặc tạo nháp', 'Khởi tạo từ mẫu tạo các bản nháp riêng cho bãi và idempotent. Không có quy tắc cấp Partner để dùng chung giữa các bãi.',
 'PRICING.RULE.INITIALIZE_DRAFTS', 'BOTTOM', 0),
(md5('price-rule-guide:WORKFLOW_RULE_DRAFT:2:3')::uuid, md5('price-rule-guide:WORKFLOW_RULE_DRAFT:2')::uuid, 3,
 '3. Hoàn thành dependency', 'Xe, vé và kế hoạch thuộc catalog dùng chung của Partner; enrollment và coverage được đánh giá riêng cho bãi.',
 'WORKFLOW.PRICING.RULES', 'BOTTOM', 0),
(md5('price-rule-guide:WORKFLOW_RULE_DRAFT:2:4')::uuid, md5('price-rule-guide:WORKFLOW_RULE_DRAFT:2')::uuid, 4,
 '4. Kích hoạt và kiểm tra readiness', 'Chỉ quy tắc ACTIVE của đúng bãi mới thỏa readiness và được dùng để tính phí tại bãi đó.',
 'WORKFLOW.PRICING.RULES', 'BOTTOM', 0)
ON CONFLICT (guide_version_id, display_order) DO NOTHING;

INSERT INTO notification.user_guide_audiences (
    audience_id, guide_version_id, target_type, role_id, permission_id
)
SELECT gen_random_uuid(), upgrade.guide_version_id, audience.target_type,
       audience.role_id, audience.permission_id
FROM price_rule_guide_upgrade upgrade
JOIN notification.user_guides guide ON guide.code = upgrade.guide_code
JOIN LATERAL (
    SELECT version.*
    FROM notification.user_guide_versions version
    WHERE version.guide_id = guide.guide_id
    ORDER BY version.version_number DESC
    LIMIT 1
) previous ON true
JOIN notification.user_guide_audiences audience
  ON audience.guide_version_id = previous.guide_version_id
ON CONFLICT DO NOTHING;

UPDATE notification.user_guides guide
SET current_version = 2,
    updated_at = now()
FROM price_rule_guide_upgrade upgrade
WHERE guide.code = upgrade.guide_code;
