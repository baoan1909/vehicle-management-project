-- Canonical catalog and pricing reference data agreed by the team.
-- Future price changes must be introduced by a new migration instead of editing this file.

INSERT INTO catalog.vehicle_types (
    vehicle_type_id, code, name, description, is_active
)
VALUES
    ('40000000-0000-0000-0000-000000000001', 'BICYCLE', 'Xe đạp', 'Phương tiện không động cơ.', TRUE),
    ('40000000-0000-0000-0000-000000000002', 'MOTORBIKE', 'Xe máy', 'Xe hai bánh động cơ.', TRUE),
    ('40000000-0000-0000-0000-000000000003', 'CAR', 'Ô tô', 'Ô tô cá nhân.', TRUE),
    ('f603ac17-26d1-45ef-b5a1-98af3581952b', 'LIGHT_TRUCK', 'Xe tải nhỏ', 'Xe tải nhỏ, xe bán tải hoặc phương tiện chở hàng nhẹ.', TRUE),
    ('86149ae9-06c2-47f1-8f3e-627702fcbe2f', 'OTHER', 'Xe khác', 'Các loại phương tiện khác.', TRUE)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    is_active = EXCLUDED.is_active,
    updated_at = now();

INSERT INTO catalog.ticket_types (
    ticket_type_id, code, name, description, duration_days, status
)
VALUES
    ('41000000-0000-0000-0000-000000000001', 'MONTHLY', 'Vé tháng', 'Dành cho khách đăng ký tháng.', 30, 'ACTIVE'),
    ('41000000-0000-0000-0000-000000000002', 'DAILY', 'Vé ngày', 'Vé gửi xe theo ngày hoặc theo lượt trong ngày.', 1, 'ACTIVE'),
    ('d7591c53-b8f5-41e0-92ea-bcb9af8c826a', 'QUARTERLY', 'Vé quý', 'Vé gửi xe theo quý.', 90, 'ACTIVE'),
    ('457ec2af-a5ef-4e18-8ac5-1ac203b2768a', 'YEARLY', 'Vé năm', 'Vé gửi xe theo năm.', 365, 'ACTIVE'),
    ('3aba3b12-5949-439a-9cf7-4cd0d0d23fa9', 'FREE', 'Vé miễn phí', 'Vé miễn phí theo chính sách hỗ trợ.', 180, 'ACTIVE')
ON CONFLICT (code) WHERE status = 'ACTIVE' DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    duration_days = EXCLUDED.duration_days,
    updated_at = now();

INSERT INTO catalog.card_types (
    card_type_id, code, name, description, is_return_required, is_active
)
VALUES
    ('42000000-0000-0000-0000-000000000001', 'REGISTERED', 'Thẻ đăng ký', 'Thẻ cấp cho khách có vé tháng.', FALSE, TRUE),
    ('42000000-0000-0000-0000-000000000002', 'VISITOR', 'Thẻ vãng lai', 'Thẻ phát cho khách gửi theo lượt.', TRUE, TRUE)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    is_return_required = EXCLUDED.is_return_required,
    is_active = EXCLUDED.is_active,
    updated_at = now();

INSERT INTO catalog.price_plans (
    price_plan_id, code, name, description, applies_to,
    effective_from, effective_to, is_active
)
VALUES
    ('43000000-0000-0000-0000-000000000001', 'VISITOR-2026', 'Bảng giá khách vãng lai 2026', 'Bảng giá áp dụng cho khách gửi xe vãng lai trong năm 2026.', 'VISITOR', DATE '2026-01-01', DATE '2026-12-31', TRUE),
    ('f84cc55a-904b-4c0d-9195-e9e4f7ae6f9d', 'CUSTOMER-2026', 'Bảng giá khách đăng ký 2026', 'Bảng giá áp dụng cho khách đăng ký vé trong năm 2026.', 'CUSTOMER', DATE '2026-01-01', DATE '2026-12-31', TRUE)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    applies_to = EXCLUDED.applies_to,
    effective_from = EXCLUDED.effective_from,
    effective_to = EXCLUDED.effective_to,
    is_active = EXCLUDED.is_active,
    updated_at = now();

WITH rule_data (
    price_rule_id, price_plan_code, vehicle_type_code, ticket_type_code,
    rule_name, time_from, time_to, base_price, unit,
    lost_card_fee, priority, is_active
) AS (
    VALUES
        -- Visitor prices: day and night for every canonical vehicle type.
        ('04b7e67d-3712-4577-8bae-4a5f4eac7e27'::UUID, 'VISITOR-2026', 'BICYCLE', 'DAILY', 'Xe đạp vãng lai ban ngày', TIME '06:00:00', TIME '19:59:59', 2000, 'TURN', 50000, 10, TRUE),
        ('34052cf9-91b9-471e-91d3-c61644465788'::UUID, 'VISITOR-2026', 'BICYCLE', 'DAILY', 'Xe đạp vãng lai ban đêm', TIME '20:00:00', TIME '05:59:59', 4000, 'TURN', 50000, 10, TRUE),
        ('c9b4a882-19f0-41eb-8608-b3385f0ec22c'::UUID, 'VISITOR-2026', 'MOTORBIKE', 'DAILY', 'Xe máy vãng lai ban ngày', TIME '06:00:00', TIME '19:59:59', 4000, 'TURN', 50000, 10, TRUE),
        ('aed4c1f4-e876-48dc-bfad-5160cd162dc9'::UUID, 'VISITOR-2026', 'MOTORBIKE', 'DAILY', 'Xe máy vãng lai ban đêm', TIME '20:00:00', TIME '05:59:59', 8000, 'TURN', 50000, 10, TRUE),
        ('e180b20e-2375-446e-b271-bda2abe90a32'::UUID, 'VISITOR-2026', 'CAR', 'DAILY', 'Ô tô vãng lai ban ngày', TIME '06:00:00', TIME '19:59:59', 20000, 'TURN', 50000, 10, TRUE),
        ('ea5d5579-fc3d-45f4-8023-72dfa7a2263a'::UUID, 'VISITOR-2026', 'CAR', 'DAILY', 'Ô tô vãng lai ban đêm', TIME '20:00:00', TIME '05:59:59', 30000, 'TURN', 50000, 10, TRUE),
        ('b405387f-1fc3-4e3b-9129-8d90c2d22d7e'::UUID, 'VISITOR-2026', 'LIGHT_TRUCK', 'DAILY', 'Xe tải nhỏ vãng lai ban ngày', TIME '06:00:00', TIME '19:59:59', 25000, 'TURN', 50000, 10, TRUE),
        ('7e9811cd-7ec0-417e-8d5c-effff6a79fbd'::UUID, 'VISITOR-2026', 'LIGHT_TRUCK', 'DAILY', 'Xe tải nhỏ vãng lai ban đêm', TIME '20:00:00', TIME '05:59:59', 35000, 'TURN', 50000, 10, TRUE),
        ('c4059f69-3f33-4b73-a985-570dcf9eeb85'::UUID, 'VISITOR-2026', 'OTHER', 'DAILY', 'Xe khác vãng lai ban ngày', TIME '06:00:00', TIME '19:59:59', 10000, 'TURN', 50000, 10, TRUE),
        ('ca20711b-e399-496f-a69e-b674099adf10'::UUID, 'VISITOR-2026', 'OTHER', 'DAILY', 'Xe khác vãng lai ban đêm', TIME '20:00:00', TIME '05:59:59', 15000, 'TURN', 50000, 10, TRUE),

        -- Subscription prices: monthly, quarterly, yearly and free for every vehicle type.
        ('5886a020-d2b3-47bf-bc60-024b9d1e383f'::UUID, 'CUSTOMER-2026', 'BICYCLE', 'MONTHLY', 'Xe đạp đăng ký tháng', NULL, NULL, 40000, 'MONTH', 50000, 20, TRUE),
        ('678f67b9-b513-4db4-96fe-c7ade3423817'::UUID, 'CUSTOMER-2026', 'BICYCLE', 'QUARTERLY', 'Xe đạp đăng ký quý', NULL, NULL, 110000, 'MONTH', 50000, 20, TRUE),
        ('6882c88d-1dec-497f-af32-fa2b6cb81d50'::UUID, 'CUSTOMER-2026', 'BICYCLE', 'YEARLY', 'Xe đạp đăng ký năm', NULL, NULL, 400000, 'MONTH', 50000, 20, TRUE),
        ('0066dc64-74d4-4b81-859c-7246663bc0b4'::UUID, 'CUSTOMER-2026', 'BICYCLE', 'FREE', 'Xe đạp vé miễn phí', NULL, NULL, 0, 'MONTH', 50000, 20, TRUE),
        ('cd00d20d-4e34-4c21-8aed-9136b7487c40'::UUID, 'CUSTOMER-2026', 'MOTORBIKE', 'MONTHLY', 'Xe máy đăng ký tháng', NULL, NULL, 80000, 'MONTH', 50000, 20, TRUE),
        ('0c75ca0b-6384-49a0-9100-79771b5744cc'::UUID, 'CUSTOMER-2026', 'MOTORBIKE', 'QUARTERLY', 'Xe máy đăng ký quý', NULL, NULL, 220000, 'MONTH', 50000, 20, TRUE),
        ('05783a68-6f6f-41db-8e3a-9b8ec21efbe4'::UUID, 'CUSTOMER-2026', 'MOTORBIKE', 'YEARLY', 'Xe máy đăng ký năm', NULL, NULL, 800000, 'MONTH', 50000, 20, TRUE),
        ('38d75293-820e-41c8-90d8-466043f65ab3'::UUID, 'CUSTOMER-2026', 'MOTORBIKE', 'FREE', 'Xe máy vé miễn phí', NULL, NULL, 0, 'MONTH', 50000, 20, TRUE),
        ('fd5e993c-61dd-4702-9375-69d584d06b1d'::UUID, 'CUSTOMER-2026', 'CAR', 'MONTHLY', 'Ô tô đăng ký tháng', NULL, NULL, 600000, 'MONTH', 50000, 20, TRUE),
        ('82f9e8b0-20e9-45b4-a97d-2414e421432b'::UUID, 'CUSTOMER-2026', 'CAR', 'QUARTERLY', 'Ô tô đăng ký quý', NULL, NULL, 1700000, 'MONTH', 50000, 20, TRUE),
        ('f0f1fedc-30e1-4aee-ba0d-b882c438966a'::UUID, 'CUSTOMER-2026', 'CAR', 'YEARLY', 'Ô tô đăng ký năm', NULL, NULL, 6500000, 'MONTH', 50000, 20, TRUE),
        ('dc8a2d73-ffef-4d87-9daf-07ecde0f2f03'::UUID, 'CUSTOMER-2026', 'CAR', 'FREE', 'Ô tô vé miễn phí', NULL, NULL, 0, 'MONTH', 50000, 20, TRUE),
        ('d5d9a304-f3fe-4ab0-aad1-5fc4620e08c2'::UUID, 'CUSTOMER-2026', 'LIGHT_TRUCK', 'MONTHLY', 'Xe tải nhỏ đăng ký tháng', NULL, NULL, 800000, 'MONTH', 50000, 20, TRUE),
        ('3f9ccc91-f49f-4c5e-9cce-333a786e18e7'::UUID, 'CUSTOMER-2026', 'LIGHT_TRUCK', 'QUARTERLY', 'Xe tải nhỏ đăng ký quý', NULL, NULL, 2300000, 'MONTH', 50000, 20, TRUE),
        ('cca21be5-b1ec-4635-80df-c4644db7f58f'::UUID, 'CUSTOMER-2026', 'LIGHT_TRUCK', 'YEARLY', 'Xe tải nhỏ đăng ký năm', NULL, NULL, 8800000, 'MONTH', 50000, 20, TRUE),
        ('12a9f191-8175-4749-90ee-aa5c062f880c'::UUID, 'CUSTOMER-2026', 'LIGHT_TRUCK', 'FREE', 'Xe tải nhỏ vé miễn phí', NULL, NULL, 0, 'MONTH', 50000, 20, TRUE),
        ('1cda6908-3020-4021-86c6-18018753f4ba'::UUID, 'CUSTOMER-2026', 'OTHER', 'MONTHLY', 'Xe khác đăng ký tháng', NULL, NULL, 200000, 'MONTH', 50000, 20, TRUE),
        ('7e56081c-8084-4975-aa8f-96c976f17abf'::UUID, 'CUSTOMER-2026', 'OTHER', 'QUARTERLY', 'Xe khác đăng ký quý', NULL, NULL, 560000, 'MONTH', 50000, 20, TRUE),
        ('14513440-4846-4dce-a89a-d7f822b12ccc'::UUID, 'CUSTOMER-2026', 'OTHER', 'YEARLY', 'Xe khác đăng ký năm', NULL, NULL, 2000000, 'MONTH', 50000, 20, TRUE),
        ('1e2793e1-7daa-478d-bb55-53f96bd5faa1'::UUID, 'CUSTOMER-2026', 'OTHER', 'FREE', 'Xe khác vé miễn phí', NULL, NULL, 0, 'MONTH', 50000, 20, TRUE)
)
INSERT INTO catalog.price_rules (
    price_rule_id, price_plan_id, vehicle_type_id, ticket_type_id,
    rule_name, time_from, time_to, base_price, unit,
    lost_card_fee, priority, is_active
)
SELECT
    rule.price_rule_id,
    plan.price_plan_id,
    vehicle_type.vehicle_type_id,
    ticket_type.ticket_type_id,
    rule.rule_name,
    rule.time_from,
    rule.time_to,
    rule.base_price,
    rule.unit,
    rule.lost_card_fee,
    rule.priority,
    rule.is_active
FROM rule_data rule
JOIN catalog.price_plans plan
    ON plan.code = rule.price_plan_code
JOIN catalog.vehicle_types vehicle_type
    ON vehicle_type.code = rule.vehicle_type_code
JOIN catalog.ticket_types ticket_type
    ON ticket_type.code = rule.ticket_type_code
   AND ticket_type.status = 'ACTIVE'
ON CONFLICT (price_rule_id) DO UPDATE
SET price_plan_id = EXCLUDED.price_plan_id,
    vehicle_type_id = EXCLUDED.vehicle_type_id,
    ticket_type_id = EXCLUDED.ticket_type_id,
    rule_name = EXCLUDED.rule_name,
    time_from = EXCLUDED.time_from,
    time_to = EXCLUDED.time_to,
    base_price = EXCLUDED.base_price,
    unit = EXCLUDED.unit,
    lost_card_fee = EXCLUDED.lost_card_fee,
    priority = EXCLUDED.priority,
    is_active = EXCLUDED.is_active,
    updated_at = now();

DO $$
DECLARE
    canonical_vehicle_type_count INTEGER;
    canonical_ticket_type_count INTEGER;
    canonical_price_plan_count INTEGER;
    visitor_rule_count INTEGER;
    customer_rule_count INTEGER;
BEGIN
    SELECT count(*) INTO canonical_vehicle_type_count
    FROM catalog.vehicle_types
    WHERE code IN ('BICYCLE', 'MOTORBIKE', 'CAR', 'LIGHT_TRUCK', 'OTHER')
      AND is_active = TRUE;

    SELECT count(*) INTO canonical_ticket_type_count
    FROM catalog.ticket_types
    WHERE code IN ('DAILY', 'MONTHLY', 'QUARTERLY', 'YEARLY', 'FREE')
      AND status = 'ACTIVE';

    SELECT count(*) INTO canonical_price_plan_count
    FROM catalog.price_plans
    WHERE code IN ('VISITOR-2026', 'CUSTOMER-2026')
      AND is_active = TRUE;

    SELECT count(*) INTO visitor_rule_count
    FROM catalog.price_rules price_rule
    JOIN catalog.price_plans price_plan
        ON price_plan.price_plan_id = price_rule.price_plan_id
    WHERE price_plan.code = 'VISITOR-2026'
      AND price_rule.is_active = TRUE;

    SELECT count(*) INTO customer_rule_count
    FROM catalog.price_rules price_rule
    JOIN catalog.price_plans price_plan
        ON price_plan.price_plan_id = price_rule.price_plan_id
    WHERE price_plan.code = 'CUSTOMER-2026'
      AND price_rule.is_active = TRUE;

    IF canonical_vehicle_type_count <> 5
       OR canonical_ticket_type_count <> 5
       OR canonical_price_plan_count <> 2
       OR visitor_rule_count <> 10
       OR customer_rule_count <> 20 THEN
        RAISE EXCEPTION
            'Invalid catalog seed counts: vehicle_types=%, ticket_types=%, price_plans=%, visitor_rules=%, customer_rules=%',
            canonical_vehicle_type_count,
            canonical_ticket_type_count,
            canonical_price_plan_count,
            visitor_rule_count,
            customer_rule_count;
    END IF;
END $$;
