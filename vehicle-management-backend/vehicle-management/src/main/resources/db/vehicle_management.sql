-- =========================================================
-- VEHICLE MANAGEMENT DATABASE - POSTGRESQL DESIGN
-- Tách từ database MySQL cũ và mở rộng theo tài liệu phân hệ.
-- Phần 1: Cấu trúc bảng (DDL)
-- Phần 2: Dữ liệu mẫu liên kết theo vòng đời nghiệp vụ
-- =========================================================

-- =========================================================
-- PHẦN 1. CẤU TRÚC BẢNG (DDL)
-- =========================================================

-- Extension dùng cho UUID và email không phân biệt hoa thường.
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS citext;

-- Tạo các schema theo nhóm nghiệp vụ.
CREATE SCHEMA IF NOT EXISTS iam;
CREATE SCHEMA IF NOT EXISTS people;
CREATE SCHEMA IF NOT EXISTS catalog;
CREATE SCHEMA IF NOT EXISTS access_control;
CREATE SCHEMA IF NOT EXISTS parking;
CREATE SCHEMA IF NOT EXISTS billing;
CREATE SCHEMA IF NOT EXISTS operations;
CREATE SCHEMA IF NOT EXISTS hardware;
CREATE SCHEMA IF NOT EXISTS notification;
CREATE SCHEMA IF NOT EXISTS audit;

-- Hàm dùng chung để tự cập nhật updated_at.
CREATE OR REPLACE FUNCTION public.set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =========================================================
-- 1. IAM - Phân quyền, tài khoản và đăng nhập
-- =========================================================

-- Lưu vai trò hệ thống: ADMIN, EMPLOYEE, CUSTOMER, GUEST.
CREATE TABLE iam.roles (
    role_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT ck_roles_code_upper CHECK (code = upper(code))
);

-- Lưu danh mục quyền chi tiết theo module và hành động.
CREATE TABLE iam.permissions (
    permission_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    permission_code VARCHAR(100) NOT NULL UNIQUE,
    module VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT uq_permissions_module_action UNIQUE (module, action),
    CONSTRAINT ck_permissions_code_upper CHECK (permission_code = upper(permission_code)),
    CONSTRAINT ck_permissions_module_upper CHECK (module = upper(module)),
    CONSTRAINT ck_permissions_action_upper CHECK (action = upper(action))
);

-- Lưu hồ sơ con người dùng chung cho admin, nhân viên và khách hàng.
CREATE TABLE people.user_profiles (
    user_profile_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(150) NOT NULL,
    date_of_birth DATE,
    gender VARCHAR(20),
    phone_number VARCHAR(20) UNIQUE,
    address TEXT,
    identify_card VARCHAR(20) UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT ck_user_profiles_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'))
);

-- Lưu tài khoản đăng nhập, tách riêng với hồ sơ cá nhân.
CREATE TABLE iam.accounts (
    account_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_profile_id UUID NOT NULL UNIQUE,
    username VARCHAR(100) NOT NULL UNIQUE,
    email CITEXT NOT NULL UNIQUE,
    role_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_login_at TIMESTAMPTZ,
    failed_login_count INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    password_changed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_accounts_user_profile FOREIGN KEY (user_profile_id) REFERENCES people.user_profiles(user_profile_id) ON DELETE RESTRICT,
    CONSTRAINT fk_accounts_role FOREIGN KEY (role_id) REFERENCES iam.roles(role_id) ON DELETE RESTRICT,
    CONSTRAINT ck_accounts_status CHECK (status IN ('ACTIVE', 'LOCKED', 'DISABLED', 'PENDING'))
);

-- Gán quyền cho từng vai trò.
CREATE TABLE people.user_profile_avatars (
    avatar_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_profile_id UUID NOT NULL,
    object_key VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255),
    content_type VARCHAR(100),
    size_bytes BIGINT,
    checksum_sha256 VARCHAR(64),
    bucket VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    is_current BOOLEAN NOT NULL DEFAULT false,
    uploaded_by_account_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_user_profile_avatars_user_profile FOREIGN KEY (user_profile_id) REFERENCES people.user_profiles(user_profile_id) ON DELETE RESTRICT,
    CONSTRAINT fk_user_profile_avatars_uploaded_by FOREIGN KEY (uploaded_by_account_id) REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT fk_user_profile_avatars_created_by FOREIGN KEY (created_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT fk_user_profile_avatars_updated_by FOREIGN KEY (updated_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT ck_user_profile_avatars_bucket CHECK (bucket IN ('PUBLIC', 'PRIVATE')),
    CONSTRAINT ck_user_profile_avatars_status CHECK (status IN ('ACTIVE', 'REPLACED', 'DELETED')),
    CONSTRAINT ck_user_profile_avatars_current_active CHECK (is_current = false OR status = 'ACTIVE'),
    CONSTRAINT ck_user_profile_avatars_size_non_negative CHECK (size_bytes IS NULL OR size_bytes >= 0)
);

CREATE TABLE iam.role_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id UUID NOT NULL,
    permission_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES iam.roles(role_id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES iam.permissions(permission_id) ON DELETE CASCADE,
    CONSTRAINT uq_role_permissions UNIQUE (role_id, permission_id)
);

-- Lưu lịch sử đổi trạng thái tài khoản.
CREATE TABLE iam.account_status_history (
    account_status_history_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL,
    old_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    reason TEXT,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    changed_by UUID,
    CONSTRAINT fk_account_status_history_account FOREIGN KEY (account_id) REFERENCES iam.accounts(account_id) ON DELETE CASCADE,
    CONSTRAINT fk_account_status_history_changed_by FOREIGN KEY (changed_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT ck_account_status_history_new_status CHECK (new_status IN ('ACTIVE', 'LOCKED', 'DISABLED', 'PENDING'))
);

-- Bổ sung FK audit sau khi accounts đã tồn tại.
ALTER TABLE iam.roles
    ADD CONSTRAINT fk_roles_created_by FOREIGN KEY (created_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_roles_updated_by FOREIGN KEY (updated_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

ALTER TABLE iam.permissions
    ADD CONSTRAINT fk_permissions_created_by FOREIGN KEY (created_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_permissions_updated_by FOREIGN KEY (updated_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

ALTER TABLE people.user_profiles
    ADD CONSTRAINT fk_user_profiles_created_by FOREIGN KEY (created_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_user_profiles_updated_by FOREIGN KEY (updated_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

ALTER TABLE iam.accounts
    ADD CONSTRAINT fk_accounts_created_by FOREIGN KEY (created_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_accounts_updated_by FOREIGN KEY (updated_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

-- =========================================================
-- 2. PEOPLE - Khách hàng và nhân viên
-- =========================================================

-- Lưu thông tin riêng của khách hàng.
CREATE TABLE people.customers (
    customer_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_profile_id UUID NOT NULL UNIQUE,
    customer_code VARCHAR(50) NOT NULL UNIQUE,
    customer_type VARCHAR(20) NOT NULL DEFAULT 'REGISTERED',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_by UUID,
    approved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_customers_user_profile FOREIGN KEY (user_profile_id) REFERENCES people.user_profiles(user_profile_id) ON DELETE RESTRICT,
    CONSTRAINT fk_customers_approved_by FOREIGN KEY (approved_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT ck_customers_type CHECK (customer_type IN ('REGISTERED', 'VIP')),
    CONSTRAINT ck_customers_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_customers_approval_status CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED'))
);

-- Lưu thông tin riêng của nhân viên vận hành.
CREATE TABLE people.employees (
    employee_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_profile_id UUID NOT NULL UNIQUE,
    employee_code VARCHAR(50) NOT NULL UNIQUE,
    job_title VARCHAR(100),
    hired_at DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_employees_user_profile FOREIGN KEY (user_profile_id) REFERENCES people.user_profiles(user_profile_id) ON DELETE RESTRICT,
    CONSTRAINT ck_employees_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'))
);

-- Lưu danh sách xe của khách hàng.
CREATE TABLE people.customer_vehicles (
    customer_vehicle_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    vehicle_type_id UUID NOT NULL,
    license_plate VARCHAR(20) NOT NULL UNIQUE,
    brand VARCHAR(80),
    color VARCHAR(50),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_customer_vehicles_customer FOREIGN KEY (customer_id) REFERENCES people.customers(customer_id) ON DELETE CASCADE,
    CONSTRAINT ck_customer_vehicles_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED'))
);

-- =========================================================
-- 3. CATALOG - Danh mục, loại xe, loại vé và bảng giá
-- =========================================================

-- Lưu loại xe: xe đạp, xe máy, ô tô.
CREATE TABLE catalog.vehicle_types (
    vehicle_type_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID
);

-- Lưu loại vé: vé lượt, vé tháng, VIP.
CREATE TABLE catalog.ticket_types (
    ticket_type_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    duration_days INT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID
);

-- Lưu loại thẻ vật lý: đăng ký, vãng lai, VIP.
CREATE TABLE catalog.card_types (
    card_type_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_return_required BOOLEAN NOT NULL DEFAULT TRUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID
);

-- Lưu bảng giá tổng quát.
CREATE TABLE catalog.price_plans (
    price_plan_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    applies_to VARCHAR(20) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT ck_price_plans_applies_to CHECK (applies_to IN ('VISITOR', 'CUSTOMER', 'ALL'))
);

-- Lưu chi tiết cách tính giá theo loại xe, loại vé, giờ trong ngày.
CREATE TABLE catalog.price_rules (
    price_rule_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    price_plan_id UUID NOT NULL,
    vehicle_type_id UUID NOT NULL,
    ticket_type_id UUID,
    rule_name VARCHAR(150) NOT NULL,
    time_from TIME,
    time_to TIME,
    base_price NUMERIC(12,2) NOT NULL,
    unit VARCHAR(30) NOT NULL DEFAULT 'TURN',
    lost_card_fee NUMERIC(12,2) NOT NULL DEFAULT 0,
    priority INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_price_rules_price_plan FOREIGN KEY (price_plan_id) REFERENCES catalog.price_plans(price_plan_id) ON DELETE CASCADE,
    CONSTRAINT fk_price_rules_vehicle_type FOREIGN KEY (vehicle_type_id) REFERENCES catalog.vehicle_types(vehicle_type_id) ON DELETE RESTRICT,
    CONSTRAINT fk_price_rules_ticket_type FOREIGN KEY (ticket_type_id) REFERENCES catalog.ticket_types(ticket_type_id) ON DELETE SET NULL,
    CONSTRAINT ck_price_rules_unit CHECK (unit IN ('TURN', 'DAY', 'MONTH'))
);

-- Lưu ngày lễ để áp dụng bảng giá đặc biệt.
CREATE TABLE catalog.holiday_calendar (
    holiday_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    holiday_date DATE NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    price_multiplier NUMERIC(5,2) NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID
);

ALTER TABLE people.customer_vehicles
    ADD CONSTRAINT fk_customer_vehicles_vehicle_type FOREIGN KEY (vehicle_type_id) REFERENCES catalog.vehicle_types(vehicle_type_id) ON DELETE RESTRICT;

ALTER TABLE catalog.holiday_calendar
    ADD CONSTRAINT fk_holiday_calendar_created_by FOREIGN KEY (created_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_holiday_calendar_updated_by FOREIGN KEY (updated_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

-- =========================================================
-- 4. ACCESS CONTROL - Thẻ từ, cấp thẻ, vé đăng ký và mất thẻ
-- =========================================================

-- Lưu thẻ RFID/NFC vật lý.
CREATE TABLE access_control.cards (
    card_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    card_number VARCHAR(50) NOT NULL UNIQUE,
    uid VARCHAR(100) NOT NULL UNIQUE,
    card_type_id UUID NOT NULL,
    vehicle_type_id UUID,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    issued_at TIMESTAMPTZ,
    blocked_at TIMESTAMPTZ,
    blocked_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_cards_card_type FOREIGN KEY (card_type_id) REFERENCES catalog.card_types(card_type_id) ON DELETE RESTRICT,
    CONSTRAINT fk_cards_vehicle_type FOREIGN KEY (vehicle_type_id) REFERENCES catalog.vehicle_types(vehicle_type_id) ON DELETE SET NULL,
    CONSTRAINT ck_cards_status CHECK (status IN ('AVAILABLE', 'ASSIGNED', 'IN_USE', 'LOST', 'BLOCKED', 'DAMAGED', 'RETIRED'))
);

-- Lưu vé tháng/vé đăng ký của khách hàng.
CREATE TABLE access_control.subscriptions (
    subscription_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    customer_vehicle_id UUID NOT NULL,
    card_id UUID,
    ticket_type_id UUID NOT NULL,
    price_rule_id UUID,
    effective_from DATE NOT NULL,
    effective_to DATE NOT NULL,
    price NUMERIC(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_by UUID,
    approved_at TIMESTAMPTZ,
    card_receipt_date DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_subscriptions_customer FOREIGN KEY (customer_id) REFERENCES people.customers(customer_id) ON DELETE RESTRICT,
    CONSTRAINT fk_subscriptions_vehicle FOREIGN KEY (customer_vehicle_id) REFERENCES people.customer_vehicles(customer_vehicle_id) ON DELETE RESTRICT,
    CONSTRAINT fk_subscriptions_card FOREIGN KEY (card_id) REFERENCES access_control.cards(card_id) ON DELETE SET NULL,
    CONSTRAINT fk_subscriptions_ticket_type FOREIGN KEY (ticket_type_id) REFERENCES catalog.ticket_types(ticket_type_id) ON DELETE RESTRICT,
    CONSTRAINT fk_subscriptions_price_rule FOREIGN KEY (price_rule_id) REFERENCES catalog.price_rules(price_rule_id) ON DELETE SET NULL,
    CONSTRAINT fk_subscriptions_approved_by FOREIGN KEY (approved_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT ck_subscriptions_status CHECK (status IN ('PENDING', 'ACTIVE', 'EXPIRED', 'CANCELLED', 'REJECTED'))
);

-- Lưu phiếu báo mất thẻ và cách xử lý.
CREATE TABLE access_control.lost_card_reports (
    lost_card_report_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    card_id UUID NOT NULL,
    customer_id UUID,
    parking_session_id UUID,
    notification_time TIMESTAMPTZ NOT NULL,
    time_of_lost TIMESTAMPTZ NOT NULL,
    ticket_price NUMERIC(12,2) NOT NULL DEFAULT 0,
    lost_card_fee NUMERIC(12,2) NOT NULL DEFAULT 0,
    reporter_name VARCHAR(150),
    reporter_phone VARCHAR(20),
    identify_card VARCHAR(20),
    registration_license VARCHAR(50),
    note TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    resolved_by UUID,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_lost_card_reports_card FOREIGN KEY (card_id) REFERENCES access_control.cards(card_id) ON DELETE RESTRICT,
    CONSTRAINT fk_lost_card_reports_customer FOREIGN KEY (customer_id) REFERENCES people.customers(customer_id) ON DELETE SET NULL,
    CONSTRAINT fk_lost_card_reports_resolved_by FOREIGN KEY (resolved_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT ck_lost_card_reports_status CHECK (status IN ('OPEN', 'RESOLVED', 'CANCELLED'))
);

-- =========================================================
-- 5. PARKING - Bãi xe, làn xe, phiên gửi và sự kiện vào ra
-- =========================================================

-- Lưu thông tin bãi xe.
CREATE TABLE parking.parking_lots (
    parking_lot_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    address TEXT,
    total_capacity INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT ck_parking_lots_status CHECK (status IN ('ACTIVE', 'MAINTENANCE', 'CLOSED'))
);

-- Lưu khu vực trong bãi xe.
CREATE TABLE parking.zones (
    zone_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parking_lot_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    vehicle_type_id UUID,
    capacity INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_zones_parking_lot FOREIGN KEY (parking_lot_id) REFERENCES parking.parking_lots(parking_lot_id) ON DELETE CASCADE,
    CONSTRAINT fk_zones_vehicle_type FOREIGN KEY (vehicle_type_id) REFERENCES catalog.vehicle_types(vehicle_type_id) ON DELETE SET NULL,
    CONSTRAINT uq_zones_lot_code UNIQUE (parking_lot_id, code)
);

-- Lưu từng ô/vị trí đỗ xe nếu cần quản lý chỗ trống chi tiết.
CREATE TABLE parking.parking_spaces (
    parking_space_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    zone_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_parking_spaces_zone FOREIGN KEY (zone_id) REFERENCES parking.zones(zone_id) ON DELETE CASCADE,
    CONSTRAINT uq_parking_spaces_zone_code UNIQUE (zone_id, code),
    CONSTRAINT ck_parking_spaces_status CHECK (status IN ('AVAILABLE', 'OCCUPIED', 'RESERVED', 'MAINTENANCE'))
);

-- Lưu làn xe vào/ra.
CREATE TABLE parking.lanes (
    lane_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parking_lot_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_lanes_parking_lot FOREIGN KEY (parking_lot_id) REFERENCES parking.parking_lots(parking_lot_id) ON DELETE CASCADE,
    CONSTRAINT uq_lanes_lot_code UNIQUE (parking_lot_id, code),
    CONSTRAINT ck_lanes_direction CHECK (direction IN ('IN', 'OUT', 'BOTH')),
    CONSTRAINT ck_lanes_status CHECK (status IN ('ACTIVE', 'MAINTENANCE', 'CLOSED'))
);

-- Lưu một phiên gửi xe từ lúc vào đến lúc ra.
CREATE TABLE parking.parking_sessions (
    parking_session_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    card_id UUID NOT NULL,
    customer_id UUID,
    customer_vehicle_id UUID,
    vehicle_type_id UUID NOT NULL,
    parking_space_id UUID,
    license_plate_in VARCHAR(20) NOT NULL,
    license_plate_out VARCHAR(20),
    check_in_time TIMESTAMPTZ NOT NULL,
    check_out_time TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    total_price NUMERIC(12,2),
    price_rule_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_parking_sessions_card FOREIGN KEY (card_id) REFERENCES access_control.cards(card_id) ON DELETE RESTRICT,
    CONSTRAINT fk_parking_sessions_customer FOREIGN KEY (customer_id) REFERENCES people.customers(customer_id) ON DELETE SET NULL,
    CONSTRAINT fk_parking_sessions_vehicle FOREIGN KEY (customer_vehicle_id) REFERENCES people.customer_vehicles(customer_vehicle_id) ON DELETE SET NULL,
    CONSTRAINT fk_parking_sessions_vehicle_type FOREIGN KEY (vehicle_type_id) REFERENCES catalog.vehicle_types(vehicle_type_id) ON DELETE RESTRICT,
    CONSTRAINT fk_parking_sessions_space FOREIGN KEY (parking_space_id) REFERENCES parking.parking_spaces(parking_space_id) ON DELETE SET NULL,
    CONSTRAINT fk_parking_sessions_price_rule FOREIGN KEY (price_rule_id) REFERENCES catalog.price_rules(price_rule_id) ON DELETE SET NULL,
    CONSTRAINT ck_parking_sessions_status CHECK (status IN ('OPEN', 'CLOSED', 'LOST_CARD', 'CANCELLED'))
);

-- Lưu sự kiện vào/ra, ảnh camera và kết quả nhận diện biển số.
CREATE TABLE parking.parking_events (
    parking_event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parking_session_id UUID NOT NULL,
    lane_id UUID NOT NULL,
    event_type VARCHAR(20) NOT NULL,
    event_time TIMESTAMPTZ NOT NULL,
    license_plate_detected VARCHAR(20),
    license_plate_image_path VARCHAR(255),
    person_image_path VARCHAR(255),
    actor_account_id UUID,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_parking_events_session FOREIGN KEY (parking_session_id) REFERENCES parking.parking_sessions(parking_session_id) ON DELETE CASCADE,
    CONSTRAINT fk_parking_events_lane FOREIGN KEY (lane_id) REFERENCES parking.lanes(lane_id) ON DELETE RESTRICT,
    CONSTRAINT fk_parking_events_actor FOREIGN KEY (actor_account_id) REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT ck_parking_events_type CHECK (event_type IN ('CHECK_IN', 'CHECK_OUT', 'MANUAL_REVIEW', 'BARRIER_OPEN'))
);

ALTER TABLE access_control.lost_card_reports
    ADD CONSTRAINT fk_lost_card_reports_session FOREIGN KEY (parking_session_id) REFERENCES parking.parking_sessions(parking_session_id) ON DELETE SET NULL;

ALTER TABLE parking.parking_events
    ADD CONSTRAINT fk_parking_events_created_by FOREIGN KEY (created_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_parking_events_updated_by FOREIGN KEY (updated_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

-- =========================================================
-- 6. BILLING - Hóa đơn và thanh toán
-- =========================================================

-- Lưu hóa đơn cho vé tháng, vé lượt, phí mất thẻ.
CREATE TABLE billing.invoices (
    invoice_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_no VARCHAR(50) NOT NULL UNIQUE,
    customer_id UUID,
    parking_session_id UUID,
    subscription_id UUID,
    lost_card_report_id UUID,
    amount NUMERIC(12,2) NOT NULL,
    discount_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    final_amount NUMERIC(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    issued_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_invoices_customer FOREIGN KEY (customer_id) REFERENCES people.customers(customer_id) ON DELETE SET NULL,
    CONSTRAINT fk_invoices_session FOREIGN KEY (parking_session_id) REFERENCES parking.parking_sessions(parking_session_id) ON DELETE SET NULL,
    CONSTRAINT fk_invoices_subscription FOREIGN KEY (subscription_id) REFERENCES access_control.subscriptions(subscription_id) ON DELETE SET NULL,
    CONSTRAINT fk_invoices_lost_card FOREIGN KEY (lost_card_report_id) REFERENCES access_control.lost_card_reports(lost_card_report_id) ON DELETE SET NULL,
    CONSTRAINT fk_invoices_created_by FOREIGN KEY (created_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT fk_invoices_updated_by FOREIGN KEY (updated_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT ck_invoices_status CHECK (status IN ('UNPAID', 'PAID', 'CANCELLED', 'REFUNDED'))
);

-- Lưu giao dịch thanh toán.
CREATE TABLE billing.payments (
    payment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    transaction_ref VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    paid_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    received_by UUID,
    note TEXT,
    CONSTRAINT fk_payments_invoice FOREIGN KEY (invoice_id) REFERENCES billing.invoices(invoice_id) ON DELETE CASCADE,
    CONSTRAINT fk_payments_received_by FOREIGN KEY (received_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT ck_payments_method CHECK (payment_method IN ('CASH', 'QR', 'BANK_TRANSFER', 'MOMO', 'VNPAY')),
    CONSTRAINT ck_payments_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED'))
);

-- =========================================================
-- 7. OPERATIONS - Ca trực, phê duyệt và hỗ trợ
-- =========================================================

-- Lưu ca trực.
CREATE TABLE operations.shifts (
    shift_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shift_code VARCHAR(50) NOT NULL UNIQUE,
    parking_lot_id UUID NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    opening_cash NUMERIC(12,2) NOT NULL DEFAULT 0,
    closing_cash NUMERIC(12,2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_shifts_parking_lot FOREIGN KEY (parking_lot_id) REFERENCES parking.parking_lots(parking_lot_id) ON DELETE RESTRICT,
    CONSTRAINT fk_shifts_created_by FOREIGN KEY (created_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT ck_shifts_status CHECK (status IN ('OPEN', 'CLOSED', 'CANCELLED'))
);

-- Gán nhân viên vào ca trực.
CREATE TABLE operations.shift_assignments (
    shift_assignment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shift_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    role_in_shift VARCHAR(50) NOT NULL DEFAULT 'OPERATOR',
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_shift_assignments_shift FOREIGN KEY (shift_id) REFERENCES operations.shifts(shift_id) ON DELETE CASCADE,
    CONSTRAINT fk_shift_assignments_employee FOREIGN KEY (employee_id) REFERENCES people.employees(employee_id) ON DELETE RESTRICT,
    CONSTRAINT uq_shift_assignments UNIQUE (shift_id, employee_id)
);

-- Lưu yêu cầu phê duyệt như đăng ký vé tháng.
CREATE TABLE operations.approval_requests (
    approval_request_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_type VARCHAR(50) NOT NULL,
    target_schema VARCHAR(50) NOT NULL,
    target_table VARCHAR(80) NOT NULL,
    target_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_by UUID,
    approved_by UUID,
    approved_at TIMESTAMPTZ,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_approval_requests_requested_by FOREIGN KEY (requested_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT fk_approval_requests_approved_by FOREIGN KEY (approved_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT fk_approval_requests_created_by FOREIGN KEY (created_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT fk_approval_requests_updated_by FOREIGN KEY (updated_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT ck_approval_requests_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'))
);

-- Lưu ticket hỗ trợ của khách hàng.
CREATE TABLE operations.support_ticket_categories (
    category_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT ck_support_ticket_categories_priority CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    CONSTRAINT ck_support_ticket_categories_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

-- Lưu ticket hỗ trợ của khách hàng.
CREATE TABLE operations.support_tickets (
    support_ticket_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID,
    category_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    assigned_to UUID,
    resolved_at TIMESTAMPTZ,
    resolution_note TEXT,
    closed_at TIMESTAMPTZ,
    closed_by UUID,
    reopen_count INTEGER NOT NULL DEFAULT 0,
    last_reopened_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_support_tickets_customer FOREIGN KEY (customer_id) REFERENCES people.customers(customer_id) ON DELETE SET NULL,
    CONSTRAINT fk_support_tickets_category FOREIGN KEY (category_id) REFERENCES operations.support_ticket_categories(category_id) ON DELETE RESTRICT,
    CONSTRAINT fk_support_tickets_assigned_to FOREIGN KEY (assigned_to) REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT fk_support_tickets_closed_by FOREIGN KEY (closed_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT ck_support_tickets_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED')),
    CONSTRAINT ck_support_tickets_reopen_count_non_negative CHECK (reopen_count >= 0)
);

-- Lưu hội thoại vận hành: chat nội bộ, hỗ trợ khách hàng và hội thoại gắn ngữ cảnh nghiệp vụ.
CREATE TABLE operations.chat_conversations (
    conversation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_type VARCHAR(30) NOT NULL,
    title VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    customer_id UUID,
    support_ticket_id UUID,
    owner_account_id UUID,
    assigned_to UUID,
    related_schema VARCHAR(50),
    related_table VARCHAR(80),
    related_id UUID,
    last_message_id UUID,
    last_message_at TIMESTAMPTZ,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_chat_conversations_customer FOREIGN KEY (customer_id) REFERENCES people.customers(customer_id) ON DELETE SET NULL,
    CONSTRAINT fk_chat_conversations_support_ticket FOREIGN KEY (support_ticket_id) REFERENCES operations.support_tickets(support_ticket_id) ON DELETE SET NULL,
    CONSTRAINT fk_chat_conversations_owner_account FOREIGN KEY (owner_account_id) REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT fk_chat_conversations_assigned_to FOREIGN KEY (assigned_to) REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT ck_chat_conversations_type CHECK (conversation_type IN ('INTERNAL_DIRECT', 'INTERNAL_GROUP', 'CUSTOMER_DIRECT', 'SUPPORT_TICKET', 'PARKING_SESSION', 'BILLING', 'LOST_CARD', 'SYSTEM_DIRECT')),
    CONSTRAINT ck_chat_conversations_status CHECK (status IN ('ACTIVE', 'ARCHIVED', 'CLOSED'))
);

CREATE TABLE operations.chat_conversation_members (
    conversation_member_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL,
    account_id UUID NOT NULL,
    member_role VARCHAR(30) NOT NULL DEFAULT 'MEMBER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_read_message_id UUID,
    muted_until TIMESTAMPTZ,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    left_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_chat_members_conversation FOREIGN KEY (conversation_id) REFERENCES operations.chat_conversations(conversation_id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_members_account FOREIGN KEY (account_id) REFERENCES iam.accounts(account_id) ON DELETE CASCADE,
    CONSTRAINT uq_chat_members_conversation_account UNIQUE (conversation_id, account_id),
    CONSTRAINT ck_chat_members_role CHECK (member_role IN ('OWNER', 'MEMBER', 'ASSIGNEE', 'OBSERVER', 'CUSTOMER')),
    CONSTRAINT ck_chat_members_status CHECK (status IN ('ACTIVE', 'LEFT', 'REMOVED', 'BLOCKED'))
);

CREATE TABLE operations.chat_messages (
    message_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL,
    sender_account_id UUID,
    message_type VARCHAR(30) NOT NULL,
    content TEXT,
    reply_to_message_id UUID,
    related_schema VARCHAR(50),
    related_table VARCHAR(80),
    related_id UUID,
    metadata JSONB,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    edited_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_chat_messages_conversation FOREIGN KEY (conversation_id) REFERENCES operations.chat_conversations(conversation_id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_messages_sender FOREIGN KEY (sender_account_id) REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT fk_chat_messages_reply_to FOREIGN KEY (reply_to_message_id) REFERENCES operations.chat_messages(message_id) ON DELETE SET NULL,
    CONSTRAINT ck_chat_messages_type CHECK (message_type IN ('TEXT', 'IMAGE', 'FILE', 'SYSTEM', 'CONTEXT_CARD', 'ACTION_CARD', 'SUPPORT_REQUEST'))
);

ALTER TABLE operations.chat_conversations
    ADD CONSTRAINT fk_chat_conversations_last_message
        FOREIGN KEY (last_message_id) REFERENCES operations.chat_messages(message_id) ON DELETE SET NULL;

ALTER TABLE operations.chat_conversation_members
    ADD CONSTRAINT fk_chat_members_last_read_message
        FOREIGN KEY (last_read_message_id) REFERENCES operations.chat_messages(message_id) ON DELETE SET NULL;

CREATE TABLE operations.chat_message_attachments (
    attachment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL,
    bucket VARCHAR(20) NOT NULL,
    object_key VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255),
    content_type VARCHAR(100),
    size_bytes BIGINT,
    checksum_sha256 VARCHAR(64),
    attachment_type VARCHAR(30) NOT NULL DEFAULT 'IMAGE',
    width INT,
    height INT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_chat_attachments_message FOREIGN KEY (message_id) REFERENCES operations.chat_messages(message_id) ON DELETE CASCADE,
    CONSTRAINT ck_chat_attachments_bucket CHECK (bucket IN ('PUBLIC', 'PRIVATE')),
    CONSTRAINT ck_chat_attachments_type CHECK (attachment_type IN ('IMAGE', 'DOCUMENT', 'AUDIO', 'PARKING_EVIDENCE', 'PAYMENT_PROOF')),
    CONSTRAINT ck_chat_attachments_size_non_negative CHECK (size_bytes IS NULL OR size_bytes >= 0)
);

-- =========================================================
-- 8. HARDWARE - Camera, kiosk, dau doc va barrier
-- =========================================================

-- Lưu thiết bị vật lý trong bãi xe.
CREATE TABLE hardware.devices (
    device_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parking_lot_id UUID NOT NULL,
    lane_id UUID,
    device_code VARCHAR(50) NOT NULL UNIQUE,
    device_type VARCHAR(30) NOT NULL,
    name VARCHAR(150) NOT NULL,
    ip_address VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_heartbeat_at TIMESTAMPTZ,
    config JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_devices_parking_lot FOREIGN KEY (parking_lot_id) REFERENCES parking.parking_lots(parking_lot_id) ON DELETE CASCADE,
    CONSTRAINT fk_devices_lane FOREIGN KEY (lane_id) REFERENCES parking.lanes(lane_id) ON DELETE SET NULL,
    CONSTRAINT ck_devices_type CHECK (device_type IN ('CAMERA', 'KIOSK', 'CARD_READER', 'BARRIER')),
    CONSTRAINT ck_devices_status CHECK (status IN ('ACTIVE', 'OFFLINE', 'MAINTENANCE', 'RETIRED'))
);

-- =========================================================
-- 9. NOTIFICATION - Thong bao va canh bao
-- =========================================================

-- Lưu thông báo gửi cho tài khoản.
CREATE TABLE notification.notifications (
    notification_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID,
    channel VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    sent_at TIMESTAMPTZ,
    read_at TIMESTAMPTZ,
    related_schema VARCHAR(50),
    related_table VARCHAR(80),
    related_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_notifications_account FOREIGN KEY (account_id) REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT fk_notifications_created_by FOREIGN KEY (created_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT fk_notifications_updated_by FOREIGN KEY (updated_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT ck_notifications_channel CHECK (channel IN ('WEB', 'EMAIL', 'PUSH', 'SMS')),
    CONSTRAINT ck_notifications_status CHECK (status IN ('PENDING', 'SENT', 'READ', 'FAILED'))
);

-- =========================================================
-- 10. AUDIT - Lưu vết hệ thống
-- =========================================================

-- Lưu vết hành động quan trọng để truy vết rủi ro.
CREATE TABLE audit.audit_logs (
    audit_log_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_account_id UUID,
    action VARCHAR(100) NOT NULL,
    target_schema VARCHAR(50),
    target_table VARCHAR(80),
    target_id UUID,
    old_data JSONB,
    new_data JSONB,
    ip_address VARCHAR(50),
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_account_id) REFERENCES iam.accounts(account_id) ON DELETE SET NULL
);

ALTER TABLE audit.audit_logs
    ADD CONSTRAINT fk_audit_logs_created_by FOREIGN KEY (created_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_audit_logs_updated_by FOREIGN KEY (updated_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

-- Trigger tự cập nhật updated_at cho các bảng có cột updated_at.
CREATE TRIGGER trg_roles_set_updated_at BEFORE UPDATE ON iam.roles FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_permissions_set_updated_at BEFORE UPDATE ON iam.permissions FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_user_profiles_set_updated_at BEFORE UPDATE ON people.user_profiles FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_user_profile_avatars_set_updated_at BEFORE UPDATE ON people.user_profile_avatars FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_accounts_set_updated_at BEFORE UPDATE ON iam.accounts FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_customers_set_updated_at BEFORE UPDATE ON people.customers FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_employees_set_updated_at BEFORE UPDATE ON people.employees FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_customer_vehicles_set_updated_at BEFORE UPDATE ON people.customer_vehicles FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_vehicle_types_set_updated_at BEFORE UPDATE ON catalog.vehicle_types FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_ticket_types_set_updated_at BEFORE UPDATE ON catalog.ticket_types FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_card_types_set_updated_at BEFORE UPDATE ON catalog.card_types FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_price_plans_set_updated_at BEFORE UPDATE ON catalog.price_plans FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_price_rules_set_updated_at BEFORE UPDATE ON catalog.price_rules FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_holiday_calendar_set_updated_at BEFORE UPDATE ON catalog.holiday_calendar FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_cards_set_updated_at BEFORE UPDATE ON access_control.cards FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_subscriptions_set_updated_at BEFORE UPDATE ON access_control.subscriptions FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_lost_card_reports_set_updated_at BEFORE UPDATE ON access_control.lost_card_reports FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_parking_lots_set_updated_at BEFORE UPDATE ON parking.parking_lots FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_zones_set_updated_at BEFORE UPDATE ON parking.zones FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_parking_spaces_set_updated_at BEFORE UPDATE ON parking.parking_spaces FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_lanes_set_updated_at BEFORE UPDATE ON parking.lanes FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_parking_sessions_set_updated_at BEFORE UPDATE ON parking.parking_sessions FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_parking_events_set_updated_at BEFORE UPDATE ON parking.parking_events FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_invoices_set_updated_at BEFORE UPDATE ON billing.invoices FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_shifts_set_updated_at BEFORE UPDATE ON operations.shifts FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_approval_requests_set_updated_at BEFORE UPDATE ON operations.approval_requests FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_support_ticket_categories_set_updated_at BEFORE UPDATE ON operations.support_ticket_categories FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_support_tickets_set_updated_at BEFORE UPDATE ON operations.support_tickets FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_chat_conversations_set_updated_at BEFORE UPDATE ON operations.chat_conversations FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_chat_conversation_members_set_updated_at BEFORE UPDATE ON operations.chat_conversation_members FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_chat_messages_set_updated_at BEFORE UPDATE ON operations.chat_messages FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_chat_message_attachments_set_updated_at BEFORE UPDATE ON operations.chat_message_attachments FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_devices_set_updated_at BEFORE UPDATE ON hardware.devices FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_notifications_set_updated_at BEFORE UPDATE ON notification.notifications FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_audit_logs_set_updated_at BEFORE UPDATE ON audit.audit_logs FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- Index phục vụ truy vấn thường gặp.
CREATE INDEX idx_accounts_role_id ON iam.accounts(role_id);
CREATE INDEX idx_accounts_status ON iam.accounts(status);
CREATE INDEX idx_role_permissions_role_id ON iam.role_permissions(role_id);
CREATE UNIQUE INDEX uq_user_profile_current_avatar ON people.user_profile_avatars(user_profile_id) WHERE is_current = true;
CREATE INDEX idx_user_profile_avatars_profile ON people.user_profile_avatars(user_profile_id);
CREATE INDEX idx_user_profile_avatars_object_key ON people.user_profile_avatars(object_key);
CREATE INDEX idx_user_profile_avatars_uploaded_by ON people.user_profile_avatars(uploaded_by_account_id);
CREATE INDEX idx_customer_vehicles_customer_id ON people.customer_vehicles(customer_id);
CREATE INDEX idx_subscriptions_customer_id ON access_control.subscriptions(customer_id);
CREATE INDEX idx_cards_status ON access_control.cards(status);
CREATE UNIQUE INDEX ux_lost_card_reports_open_parking_session ON access_control.lost_card_reports(parking_session_id) WHERE parking_session_id IS NOT NULL AND status = 'OPEN';
CREATE INDEX idx_parking_sessions_card_id ON parking.parking_sessions(card_id);
CREATE INDEX idx_parking_sessions_status ON parking.parking_sessions(status);
CREATE INDEX idx_parking_sessions_check_in_time ON parking.parking_sessions(check_in_time);
CREATE INDEX idx_parking_events_session_id ON parking.parking_events(parking_session_id);
CREATE INDEX idx_invoices_status ON billing.invoices(status);
CREATE INDEX idx_payments_invoice_id ON billing.payments(invoice_id);
CREATE UNIQUE INDEX uq_support_ticket_categories_active_code ON operations.support_ticket_categories(code) WHERE status = 'ACTIVE';
CREATE INDEX idx_support_ticket_categories_status ON operations.support_ticket_categories(status);
CREATE INDEX idx_support_ticket_categories_code ON operations.support_ticket_categories(code);
CREATE INDEX idx_support_tickets_category ON operations.support_tickets(category_id);
CREATE INDEX idx_support_tickets_status ON operations.support_tickets(status);
CREATE INDEX idx_support_tickets_customer ON operations.support_tickets(customer_id);
CREATE INDEX idx_support_tickets_assigned_to ON operations.support_tickets(assigned_to);
CREATE INDEX idx_chat_conversations_last_message_at ON operations.chat_conversations(last_message_at DESC, conversation_id DESC);
CREATE INDEX idx_chat_conversations_customer ON operations.chat_conversations(customer_id);
CREATE INDEX idx_chat_conversations_support_ticket ON operations.chat_conversations(support_ticket_id);
CREATE INDEX idx_chat_conversations_related ON operations.chat_conversations(related_schema, related_table, related_id);
CREATE INDEX idx_chat_conversations_assigned_to_status ON operations.chat_conversations(assigned_to, status);
CREATE INDEX idx_chat_members_account_status ON operations.chat_conversation_members(account_id, status);
CREATE INDEX idx_chat_members_conversation_status ON operations.chat_conversation_members(conversation_id, status);
CREATE INDEX idx_chat_members_conversation_account_status ON operations.chat_conversation_members(conversation_id, account_id, status);
CREATE INDEX idx_chat_messages_conversation_created_at ON operations.chat_messages(conversation_id, created_at DESC, message_id DESC);
CREATE INDEX idx_chat_messages_sender_created_at ON operations.chat_messages(sender_account_id, created_at DESC);
CREATE INDEX idx_chat_messages_related ON operations.chat_messages(related_schema, related_table, related_id);
CREATE INDEX idx_chat_attachments_message ON operations.chat_message_attachments(message_id);
CREATE INDEX idx_chat_attachments_object_key ON operations.chat_message_attachments(object_key);
CREATE INDEX idx_notifications_account_id ON notification.notifications(account_id);
CREATE INDEX idx_approval_requests_request_type_status ON operations.approval_requests(request_type, status);
CREATE INDEX idx_approval_requests_target_lookup ON operations.approval_requests(target_schema, target_table, target_id);
CREATE INDEX idx_audit_logs_target ON audit.audit_logs(target_schema, target_table, target_id);

-- =========================================================
-- PHẦN 2. DỮ LIỆU MẪU (SAMPLE DATA)
-- =========================================================

-- Dữ liệu mẫu: vai trò hệ thống.
INSERT INTO iam.roles (role_id, code, name, description, is_system)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'ADMIN', 'Quản trị viên', 'Toàn quyền cấu hình hệ thống, phân quyền và báo cáo.', TRUE),
    ('00000000-0000-0000-0000-000000000002', 'EMPLOYEE', 'Nhân viên vận hành', 'Vận hành cổng vào ra, xử lý thanh toán và hỗ trợ khách hàng.', TRUE),
    ('00000000-0000-0000-0000-000000000003', 'CUSTOMER', 'Khách hàng', 'Quản lý hồ sơ, xe, vé đăng ký và lịch sử gửi xe của chính mình.', TRUE),
    ('00000000-0000-0000-0000-000000000004', 'GUEST', 'Khách vãng lai', 'Xem thông tin công khai và đăng ký tài khoản.', TRUE);

-- Dữ liệu mẫu: quyền theo các module chính.
INSERT INTO iam.permissions (permission_id, permission_code, module, action, name, description)
VALUES
    ('00000000-0000-0000-0000-000000000101', 'VIEW_DASHBOARD', 'DASHBOARD', 'READ', 'Xem dashboard', 'Xem doanh thu, lưu lượng xe và tỉ lệ lấp đầy.'),
    ('00000000-0000-0000-0000-000000000102', 'MANAGE_ACCOUNT', 'ACCOUNT', 'MANAGE', 'Quản lý tài khoản', 'Thêm, sửa, khóa tài khoản.'),
    ('00000000-0000-0000-0000-000000000103', 'MANAGE_ROLE', 'ROLE', 'MANAGE', 'Quản lý vai trò', 'Quản lý vai trò và gán quyền.'),
    ('00000000-0000-0000-0000-000000000104', 'MANAGE_PRICE', 'PRICE', 'MANAGE', 'Quản lý bảng giá', 'Cấu hình bảng giá theo loại xe và thời gian.'),
    ('00000000-0000-0000-0000-000000000105', 'MANAGE_CARD', 'CARD', 'MANAGE', 'Quản lý thẻ', 'Nhập kho, cấp phát, khóa và thu hồi thẻ.'),
    ('00000000-0000-0000-0000-000000000106', 'OPERATE_PARKING_GATE', 'PARKING', 'OPERATE_GATE', 'Vận hành cổng', 'Quẹt thẻ, đối chiếu biển số, mở barrier.'),
    ('00000000-0000-0000-0000-000000000107', 'PROCESS_PAYMENT', 'PAYMENT', 'PROCESS', 'Xử lý thanh toán', 'Thu tiền mặt, QR hoặc kênh online.'),
    ('00000000-0000-0000-0000-000000000108', 'APPROVE_SUBSCRIPTION', 'SUBSCRIPTION', 'APPROVE', 'Duyệt vé đăng ký', 'Duyệt hồ sơ đăng ký vé tháng.'),
    ('00000000-0000-0000-0000-000000000109', 'VIEW_OWN_PROFILE', 'PROFILE', 'READ_OWN', 'Xem hồ sơ cá nhân', 'Khách hàng xem hồ sơ của chính mình.'),
    ('00000000-0000-0000-0000-000000000110', 'VIEW_OWN_PARKING_HISTORY', 'PARKING_HISTORY', 'READ_OWN', 'Xem lịch sử gửi xe', 'Khách hàng xem các phiên gửi xe.'),
    ('00000000-0000-0000-0000-000000000111', 'SEND_SUPPORT_TICKET', 'SUPPORT', 'CREATE', 'Gửi yêu cầu hỗ trợ', 'Khách hàng gửi ticket hỗ trợ.'),
    ('00000000-0000-0000-0000-000000000112', 'VIEW_PUBLIC_PRICE', 'PUBLIC_PRICE', 'READ', 'Xem bảng giá công khai', 'Khách vãng lai xem bảng giá.');

-- Dữ liệu mẫu: gán quyền cho vai trò.
INSERT INTO iam.role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000001'::uuid, permission_id FROM iam.permissions;

INSERT INTO iam.role_permissions (role_id, permission_id)
VALUES
    ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000106'),
    ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000107'),
    ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000105'),
    ('00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000109'),
    ('00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000110'),
    ('00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000111'),
    ('00000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000112');

-- Dữ liệu mẫu: hồ sơ người dùng.
INSERT INTO people.user_profiles (user_profile_id, full_name, date_of_birth, gender, phone_number, address, identify_card, status)
VALUES
    ('10000000-0000-0000-0000-000000000001', 'Nguyễn Văn Admin', '1988-01-10', 'Nam', '0901000001', 'TP.HCM', '079188000001', 'ACTIVE'),
    ('10000000-0000-0000-0000-000000000002', 'Trần Thị Nhân Viên', '1996-06-15', 'Nữ', '0901000002', 'TP.HCM', '079196000002', 'ACTIVE'),
    ('10000000-0000-0000-0000-000000000003', 'Võ Văn Tú', '2003-09-19', 'Nam', '0901000003', 'Tân Phú, TP.HCM', '079203000003', 'ACTIVE');

-- Dữ liệu mẫu: tài khoản đăng nhập.
INSERT INTO iam.accounts (account_id, user_profile_id, username, email, role_id, status, password_changed_at)
VALUES
    ('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'admin', 'admin@parking.local', '00000000-0000-0000-0000-000000000001', 'ACTIVE', now()),
    ('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002', 'employee01', 'employee01@parking.local', '00000000-0000-0000-0000-000000000002', 'ACTIVE', now()),
    ('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000003', 'vovantu', 'tu.customer@example.com', '00000000-0000-0000-0000-000000000003', 'ACTIVE', now());

-- Dữ liệu mẫu: khách hàng và nhân viên.
INSERT INTO people.customers (customer_id, user_profile_id, customer_code, customer_type, approval_status, approved_by, approved_at)
VALUES
    ('30000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000003', 'CUS-0001', 'REGISTERED', 'APPROVED', '20000000-0000-0000-0000-000000000001', '2026-05-01 08:00:00+07');

INSERT INTO people.employees (employee_id, user_profile_id, employee_code, job_title, hired_at, status)
VALUES
    ('31000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002', 'EMP-0001', 'Nhân viên cổng vào ra', '2026-04-01', 'ACTIVE');

-- Dữ liệu mẫu: danh mục loại xe, vé, thẻ và bảng giá.
INSERT INTO catalog.vehicle_types (vehicle_type_id, code, name, description)
VALUES
    ('40000000-0000-0000-0000-000000000001', 'BICYCLE', 'Xe đạp', 'Phương tiện không động cơ.'),
    ('40000000-0000-0000-0000-000000000002', 'MOTORBIKE', 'Xe máy', 'Xe hai bánh động cơ.'),
    ('40000000-0000-0000-0000-000000000003', 'CAR', 'Ô tô', 'Ô tô cá nhân.');

INSERT INTO catalog.ticket_types (ticket_type_id, code, name, description, duration_days)
VALUES
    ('41000000-0000-0000-0000-000000000001', 'MONTHLY', 'Vé tháng', 'Dành cho khách đăng ký tháng.', 30),
    ('41000000-0000-0000-0000-000000000002', 'DAILY', 'Vé ngày', 'Dành cho khách vãng lai.', 1),
    ('41000000-0000-0000-0000-000000000003', 'VIP', 'Vé VIP', 'Dành cho khách hàng ưu tiên.', 30);

INSERT INTO catalog.card_types (card_type_id, code, name, description)
VALUES
    ('42000000-0000-0000-0000-000000000001', 'REGISTERED', 'Thẻ đăng ký', 'Thẻ cấp cho khách có vé tháng.'),
    ('42000000-0000-0000-0000-000000000002', 'VISITOR', 'Thẻ vãng lai', 'Thẻ phát cho khách gửi theo lượt.');

INSERT INTO catalog.price_plans (price_plan_id, code, name, applies_to, effective_from, description)
VALUES
    ('43000000-0000-0000-0000-000000000001', 'VISITOR-2026', 'Bảng giá khách vãng lai 2026', 'VISITOR', '2026-01-01', 'Giá theo lượt cho khách vãng lai.'),
    ('43000000-0000-0000-0000-000000000002', 'MONTHLY-2026', 'Bảng giá vé tháng 2026', 'CUSTOMER', '2026-01-01', 'Giá vé tháng theo loại xe.');

INSERT INTO catalog.price_rules (price_rule_id, price_plan_id, vehicle_type_id, ticket_type_id, rule_name, time_from, time_to, base_price, unit, lost_card_fee, priority)
VALUES
    ('44000000-0000-0000-0000-000000000001', '43000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000002', '41000000-0000-0000-0000-000000000002', 'Xe máy vãng lai ban ngày', '06:00', '18:00', 5000, 'TURN', 100000, 10),
    ('44000000-0000-0000-0000-000000000002', '43000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000003', '41000000-0000-0000-0000-000000000002', 'Ô tô vãng lai ban ngày', '06:00', '18:00', 30000, 'TURN', 200000, 10),
    ('44000000-0000-0000-0000-000000000003', '43000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', '41000000-0000-0000-0000-000000000001', 'Xe máy vé tháng', NULL, NULL, 140000, 'MONTH', 100000, 20);

-- Dữ liệu mẫu: xe của khách hàng.
INSERT INTO people.customer_vehicles (customer_vehicle_id, customer_id, vehicle_type_id, license_plate, brand, color, is_default)
VALUES
    ('50000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000002', '60K8-2301', 'Honda', 'Đen', TRUE);

-- Dữ liệu mẫu: bãi xe, khu, ô đỗ và làn xe.
INSERT INTO parking.parking_lots (parking_lot_id, code, name, address, total_capacity)
VALUES
    ('60000000-0000-0000-0000-000000000001', 'LOT-HCMUTE', 'Bãi xe HCMUTE', 'Số 1 Võ Văn Ngân, TP Thủ Đức', 500);

INSERT INTO parking.zones (zone_id, parking_lot_id, code, name, vehicle_type_id, capacity)
VALUES
    ('61000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000001', 'MOTO-A', 'Khu xe máy A', '40000000-0000-0000-0000-000000000002', 300);

INSERT INTO parking.parking_spaces (parking_space_id, zone_id, code, status)
VALUES
    ('62000000-0000-0000-0000-000000000001', '61000000-0000-0000-0000-000000000001', 'A-001', 'OCCUPIED'),
    ('62000000-0000-0000-0000-000000000002', '61000000-0000-0000-0000-000000000001', 'A-002', 'AVAILABLE');

INSERT INTO parking.lanes (lane_id, parking_lot_id, code, name, direction)
VALUES
    ('63000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000001', 'IN-01', 'Làn vào 01', 'IN'),
    ('63000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000001', 'OUT-01', 'Làn ra 01', 'OUT');

-- Dữ liệu mẫu: thiết bị trên làn xe.
INSERT INTO hardware.devices (device_id, parking_lot_id, lane_id, device_code, device_type, name, ip_address, status, last_heartbeat_at, config)
VALUES
    ('64000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000001', '63000000-0000-0000-0000-000000000001', 'CAM-IN-01', 'CAMERA', 'Camera làn vào 01', '192.168.1.10', 'ACTIVE', now(), '{"resolution":"1080p"}'),
    ('64000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000001', '63000000-0000-0000-0000-000000000002', 'READER-OUT-01', 'CARD_READER', 'Đầu đọc làn ra 01', '192.168.1.21', 'ACTIVE', now(), '{"port":"COM3"}');

-- Dữ liệu mẫu: thẻ, vé tháng và phiếu phê duyệt.
INSERT INTO access_control.cards (card_id, card_number, uid, card_type_id, vehicle_type_id, status, issued_at)
VALUES
    ('70000000-0000-0000-0000-000000000001', 'C001', 'RFID-REGISTERED-001', '42000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000002', 'ASSIGNED', '2026-05-01 08:10:00+07'),
    ('70000000-0000-0000-0000-000000000002', 'V001', 'RFID-VISITOR-001', '42000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', 'IN_USE', '2026-05-14 07:30:00+07');

INSERT INTO access_control.subscriptions (subscription_id, customer_id, customer_vehicle_id, card_id, ticket_type_id, price_rule_id, effective_from, effective_to, price, status, approved_by, approved_at, card_receipt_date)
VALUES
    ('71000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', '41000000-0000-0000-0000-000000000001', '44000000-0000-0000-0000-000000000003', '2026-05-01', '2026-05-31', 140000, 'ACTIVE', '20000000-0000-0000-0000-000000000001', '2026-05-01 08:05:00+07', '2026-05-01');

INSERT INTO operations.approval_requests (approval_request_id, request_type, target_schema, target_table, target_id, status, requested_by, approved_by, approved_at, note)
VALUES
    ('72000000-0000-0000-0000-000000000001', 'SUBSCRIPTION_REGISTER', 'access_control', 'subscriptions', '71000000-0000-0000-0000-000000000001', 'APPROVED', '20000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000001', '2026-05-01 08:05:00+07', 'Hồ sơ hợp lệ.');

-- Dữ liệu mẫu: ca trực của nhân viên.
INSERT INTO operations.shifts (shift_id, shift_code, parking_lot_id, start_time, status, opening_cash, created_by)
VALUES
    ('80000000-0000-0000-0000-000000000001', 'SHIFT-20260514-MORNING', '60000000-0000-0000-0000-000000000001', '2026-05-14 06:00:00+07', 'OPEN', 500000, '20000000-0000-0000-0000-000000000002');

INSERT INTO operations.shift_assignments (shift_assignment_id, shift_id, employee_id, role_in_shift)
VALUES
    ('81000000-0000-0000-0000-000000000001', '80000000-0000-0000-0000-000000000001', '31000000-0000-0000-0000-000000000001', 'OPERATOR');

-- Dữ liệu mẫu: vòng đời phiên gửi xe vãng lai đã vào và đã ra.
INSERT INTO parking.parking_sessions (parking_session_id, card_id, vehicle_type_id, parking_space_id, license_plate_in, license_plate_out, check_in_time, check_out_time, status, total_price, price_rule_id, created_by, updated_by)
VALUES
    ('90000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', '62000000-0000-0000-0000-000000000001', '59B1-67890', '59B1-67890', '2026-05-14 07:30:00+07', '2026-05-14 10:15:00+07', 'CLOSED', 5000, '44000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002');

INSERT INTO parking.parking_events (parking_event_id, parking_session_id, lane_id, event_type, event_time, license_plate_detected, license_plate_image_path, person_image_path, actor_account_id, note)
VALUES
    ('91000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000001', '63000000-0000-0000-0000-000000000001', 'CHECK_IN', '2026-05-14 07:30:00+07', '59B1-67890', 'images/checkin/59B1-67890-plate.jpg', 'images/checkin/59B1-67890-person.jpg', '20000000-0000-0000-0000-000000000002', 'Xe vãng lai vào bãi.'),
    ('91000000-0000-0000-0000-000000000002', '90000000-0000-0000-0000-000000000001', '63000000-0000-0000-0000-000000000002', 'CHECK_OUT', '2026-05-14 10:15:00+07', '59B1-67890', 'images/checkout/59B1-67890-plate.jpg', 'images/checkout/59B1-67890-person.jpg', '20000000-0000-0000-0000-000000000002', 'Biển số khớp, cho ra cổng.');

-- Dữ liệu mẫu: hóa đơn và thanh toán cho phiên gửi xe vãng lai.
INSERT INTO billing.invoices (invoice_id, invoice_no, parking_session_id, amount, final_amount, status, issued_at, paid_at, created_by)
VALUES
    ('a0000000-0000-0000-0000-000000000001', 'INV-20260514-0001', '90000000-0000-0000-0000-000000000001', 5000, 5000, 'PAID', '2026-05-14 10:15:00+07', '2026-05-14 10:16:00+07', '20000000-0000-0000-0000-000000000002');

INSERT INTO billing.payments (payment_id, invoice_id, payment_method, amount, transaction_ref, status, paid_at, received_by, note)
VALUES
    ('a1000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'CASH', 5000, 'CASH-20260514-0001', 'SUCCESS', '2026-05-14 10:16:00+07', '20000000-0000-0000-0000-000000000002', 'Thu tiền mặt tại cổng ra.');

-- Dữ liệu mẫu: mất thẻ sau một phiên gửi xe.
INSERT INTO access_control.lost_card_reports (lost_card_report_id, card_id, parking_session_id, notification_time, time_of_lost, ticket_price, lost_card_fee, reporter_name, reporter_phone, identify_card, note, status, resolved_by, resolved_at, created_by)
VALUES
    ('b0000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000002', '90000000-0000-0000-0000-000000000001', '2026-05-14 10:20:00+07', '2026-05-14 10:00:00+07', 5000, 100000, 'Nguyễn Văn Khách', '0901999999', '079199999999', 'Khách báo làm mất thẻ sau khi checkout, đã đối chiếu ảnh camera.', 'RESOLVED', '20000000-0000-0000-0000-000000000002', '2026-05-14 10:25:00+07', '20000000-0000-0000-0000-000000000002');

-- Dữ liệu mẫu: ticket hỗ trợ và thông báo.
INSERT INTO operations.support_ticket_categories (category_id, code, name, description, priority, status)
VALUES
    ('c1000000-0000-0000-0000-000000000001', 'LOST_CARD', 'Mất thẻ xe', 'Khách hàng mất thẻ xe hoặc không xuất trình được thẻ khi ra bãi.', 'HIGH', 'ACTIVE'),
    ('c1000000-0000-0000-0000-000000000002', 'WRONG_FEE', 'Khiếu nại phí gửi xe', 'Khách hàng phản ánh bị tính sai phí gửi xe.', 'HIGH', 'ACTIVE'),
    ('c1000000-0000-0000-0000-000000000003', 'VEHICLE_DAMAGE', 'Xe hư hỏng/trầy xước', 'Khách hàng phản ánh xe bị trầy xước, hư hỏng hoặc mất tài sản.', 'URGENT', 'ACTIVE'),
    ('c1000000-0000-0000-0000-000000000004', 'CARD_NOT_WORKING', 'Thẻ không hoạt động', 'Thẻ gửi xe không quét được hoặc không sử dụng được.', 'NORMAL', 'ACTIVE'),
    ('c1000000-0000-0000-0000-000000000005', 'SUBSCRIPTION_PROBLEM', 'Vấn đề vé đăng ký', 'Vé tháng/quý/năm/miễn phí không hoạt động hoặc cần hỗ trợ.', 'NORMAL', 'ACTIVE'),
    ('c1000000-0000-0000-0000-000000000006', 'PAYMENT_PROBLEM', 'Vấn đề thanh toán', 'Thanh toán lỗi, đã thanh toán nhưng chưa ghi nhận hoặc cần kiểm tra giao dịch.', 'HIGH', 'ACTIVE'),
    ('c1000000-0000-0000-0000-000000000007', 'PARKING_HISTORY_REQUEST', 'Yêu cầu tra cứu lịch sử gửi xe', 'Khách hàng cần tra cứu lịch sử gửi xe.', 'LOW', 'ACTIVE'),
    ('c1000000-0000-0000-0000-000000000008', 'PROFILE_OR_VEHICLE_UPDATE', 'Cập nhật hồ sơ/xe', 'Khách hàng cần hỗ trợ cập nhật thông tin cá nhân hoặc phương tiện.', 'LOW', 'ACTIVE'),
    ('c1000000-0000-0000-0000-000000000009', 'STAFF_COMPLAINT', 'Khiếu nại nhân viên', 'Khách hàng phản ánh thái độ hoặc cách xử lý của nhân viên.', 'HIGH', 'ACTIVE'),
    ('c1000000-0000-0000-0000-000000000010', 'OTHER', 'Vấn đề khác', 'Yêu cầu hỗ trợ khác chưa thuộc nhóm cố định.', 'NORMAL', 'ACTIVE');

INSERT INTO operations.support_tickets (support_ticket_id, customer_id, category_id, title, content, status, assigned_to, created_by)
VALUES
    ('c0000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', 'c1000000-0000-0000-0000-000000000005', 'Hỏi về gia hạn vé tháng', 'Khách hàng muốn gia hạn vé tháng cho biển số 60K8-2301.', 'OPEN', '20000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000003');

INSERT INTO notification.notifications (notification_id, account_id, channel, title, message, status, sent_at, related_schema, related_table, related_id)
VALUES
    ('d0000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000003', 'WEB', 'Vé tháng sap het han', 'Vé tháng của biển số 60K8-2301 sẽ hết hạn vào ngày 2026-05-31.', 'SENT', '2026-05-25 08:00:00+07', 'access_control', 'subscriptions', '71000000-0000-0000-0000-000000000001');

-- Dữ liệu mẫu: audit log cho hành động quan trọng.
INSERT INTO audit.audit_logs (audit_log_id, actor_account_id, action, target_schema, target_table, target_id, new_data, ip_address)
VALUES
    ('e0000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'APPROVE_SUBSCRIPTION', 'access_control', 'subscriptions', '71000000-0000-0000-0000-000000000001', '{"status":"ACTIVE","approved_by":"admin"}', '127.0.0.1'),
    ('e0000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002', 'PROCESS_PAYMENT', 'billing', 'payments', 'a1000000-0000-0000-0000-000000000001', '{"amount":5000,"method":"CASH"}', '127.0.0.1');
