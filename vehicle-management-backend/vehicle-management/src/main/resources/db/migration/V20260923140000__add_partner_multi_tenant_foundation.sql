CREATE TABLE iam.organizations (
    organization_id uuid NOT NULL,
    code character varying(50) NOT NULL,
    name character varying(150) NOT NULL,
    address text,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    CONSTRAINT organizations_pkey PRIMARY KEY (organization_id),
    CONSTRAINT uq_organizations_code UNIQUE (code),
    CONSTRAINT ck_organizations_status CHECK ((status)::text = ANY (ARRAY['ACTIVE'::character varying, 'SUSPENDED'::character varying, 'CLOSED'::character varying]::text[]))
);

CREATE TABLE iam.organization_memberships (
    organization_membership_id uuid NOT NULL,
    organization_id uuid NOT NULL,
    account_id uuid NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    CONSTRAINT organization_memberships_pkey PRIMARY KEY (organization_membership_id),
    CONSTRAINT uq_organization_memberships_organization_account UNIQUE (organization_id, account_id),
    CONSTRAINT ck_organization_memberships_status CHECK ((status)::text = ANY (ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying]::text[])),
    CONSTRAINT fk_organization_memberships_organization FOREIGN KEY (organization_id) REFERENCES iam.organizations(organization_id) ON DELETE CASCADE,
    CONSTRAINT fk_organization_memberships_account FOREIGN KEY (account_id) REFERENCES iam.accounts(account_id) ON DELETE RESTRICT
);

ALTER TABLE parking.parking_lots ADD COLUMN organization_id uuid;

INSERT INTO iam.organizations (
    organization_id, code, name, status, created_at
) VALUES (
    '00000000-0000-0000-0000-000000009001',
    'COPARKING_INTERNAL',
    'CoParking Internal',
    'ACTIVE',
    now()
);

UPDATE parking.parking_lots
SET organization_id = '00000000-0000-0000-0000-000000009001'
WHERE organization_id IS NULL;

ALTER TABLE parking.parking_lots
    ALTER COLUMN organization_id SET NOT NULL,
    ADD CONSTRAINT fk_parking_lots_organization FOREIGN KEY (organization_id) REFERENCES iam.organizations(organization_id) ON DELETE RESTRICT;

ALTER TABLE parking.parking_lots DROP CONSTRAINT parking_lots_code_key;
ALTER TABLE parking.parking_lots ADD CONSTRAINT uq_parking_lots_organization_code UNIQUE (organization_id, code);

CREATE INDEX idx_parking_lots_organization_status ON parking.parking_lots (organization_id, status);
CREATE INDEX idx_organization_memberships_account_status ON iam.organization_memberships (account_id, status);

CREATE TABLE iam.member_parking_lot_scopes (
    member_parking_lot_scope_id uuid NOT NULL,
    organization_membership_id uuid NOT NULL,
    parking_lot_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    CONSTRAINT member_parking_lot_scopes_pkey PRIMARY KEY (member_parking_lot_scope_id),
    CONSTRAINT uq_member_parking_lot_scopes_membership_lot UNIQUE (organization_membership_id, parking_lot_id),
    CONSTRAINT fk_member_parking_lot_scopes_membership FOREIGN KEY (organization_membership_id) REFERENCES iam.organization_memberships(organization_membership_id) ON DELETE CASCADE,
    CONSTRAINT fk_member_parking_lot_scopes_parking_lot FOREIGN KEY (parking_lot_id) REFERENCES parking.parking_lots(parking_lot_id) ON DELETE CASCADE
);

CREATE INDEX idx_member_parking_lot_scopes_parking_lot ON iam.member_parking_lot_scopes (parking_lot_id);

INSERT INTO iam.permission_modules (
    module_id, code, name, description, created_at
) VALUES (
    '00000000-0000-0000-0000-000000001041',
    'ORGANIZATION',
    'Đối tác',
    'Quản lý đối tác sử dụng nền tảng CoParking và phân quyền theo bãi xe.',
    now()
);

INSERT INTO iam.roles (
    role_id, code, name, description, is_system, is_active, created_at
) VALUES (
    '00000000-0000-0000-0000-000000000006',
    'PARTNER_ADMIN',
    'Quản trị đối tác',
    'Đại diện đơn vị đối tác, quản lý các bãi và Parking Manager thuộc đối tác.',
    true,
    true,
    now()
);

INSERT INTO iam.permissions (permission_id, permission_code, name, description, created_at, module_id, action_id, scope_id) VALUES
    ('00000000-0000-0000-0000-000000004101', 'ORGANIZATION_CREATE_ALL', 'Tạo đối tác', 'Cho phép tạo và cấp quyền đối tác trên toàn nền tảng.', now(), '00000000-0000-0000-0000-000000001041', '00000000-0000-0000-0000-000000002001', '00000000-0000-0000-0000-000000003001'),
    ('00000000-0000-0000-0000-000000004102', 'ORGANIZATION_READ_ALL', 'Xem đối tác', 'Cho phép xem danh sách và chi tiết đối tác.', now(), '00000000-0000-0000-0000-000000001041', '00000000-0000-0000-0000-000000002002', '00000000-0000-0000-0000-000000003001'),
    ('00000000-0000-0000-0000-000000004103', 'ORGANIZATION_UPDATE_ALL', 'Cập nhật đối tác', 'Cho phép cập nhật trạng thái và thông tin đối tác.', now(), '00000000-0000-0000-0000-000000001041', '00000000-0000-0000-0000-000000002003', '00000000-0000-0000-0000-000000003001'),
    ('00000000-0000-0000-0000-000000004104', 'ORGANIZATION_MEMBERSHIP_MANAGE_ALL', 'Phân công thành viên đối tác', 'Cho phép gán Parking Manager cho bãi thuộc đối tác.', now(), '00000000-0000-0000-0000-000000001041', '7f4f11d7-2b52-4c20-bf11-0e4200000512', '00000000-0000-0000-0000-000000003001'),
    ('00000000-0000-0000-0000-000000004105', 'PARKING_LOT_CREATE_ALL', 'Tạo bãi xe', 'Cho phép tạo bãi xe thuộc Partner được cấp quyền.', now(), '00000000-0000-0000-0000-000000001018', '00000000-0000-0000-0000-000000002001', '00000000-0000-0000-0000-000000003001'),
    ('00000000-0000-0000-0000-000000004106', 'PARKING_LOT_READ_ALL', 'Xem bãi xe', 'Cho phép xem bãi xe trong phạm vi được cấp.', now(), '00000000-0000-0000-0000-000000001018', '00000000-0000-0000-0000-000000002002', '00000000-0000-0000-0000-000000003001'),
    ('00000000-0000-0000-0000-000000004107', 'PARKING_LOT_UPDATE_ALL', 'Cập nhật bãi xe', 'Cho phép cập nhật thông tin và trạng thái bãi xe trong phạm vi được cấp.', now(), '00000000-0000-0000-0000-000000001018', '00000000-0000-0000-0000-000000002003', '00000000-0000-0000-0000-000000003001');

INSERT INTO iam.role_permissions (id, role_id, permission_id, created_at, is_active, is_system) VALUES
    ('00000000-0000-0000-0000-000000005101', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000004101', now(), true, true),
    ('00000000-0000-0000-0000-000000005102', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000004102', now(), true, true),
    ('00000000-0000-0000-0000-000000005103', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000004103', now(), true, true),
    ('00000000-0000-0000-0000-000000005104', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000004104', now(), true, true),
    ('00000000-0000-0000-0000-000000005105', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000004105', now(), true, true),
    ('00000000-0000-0000-0000-000000005106', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000004106', now(), true, true),
    ('00000000-0000-0000-0000-000000005107', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000004107', now(), true, true),
    ('00000000-0000-0000-0000-000000005108', '00000000-0000-0000-0000-000000000006', 'fbf43219-db7b-4638-9f01-755cc1bf38ce', now(), true, true),
    ('00000000-0000-0000-0000-000000005109', '00000000-0000-0000-0000-000000000006', '4ef49d68-9c43-4a79-848c-a025cd19155e', now(), true, true),
    ('00000000-0000-0000-0000-000000005110', '00000000-0000-0000-0000-000000000006', '847ac31b-1178-4381-b414-95c82d101d2c', now(), true, true),
    ('00000000-0000-0000-0000-000000005111', '00000000-0000-0000-0000-000000000006', '00000000-0000-0000-0000-000000004102', now(), true, true),
    ('00000000-0000-0000-0000-000000005112', '00000000-0000-0000-0000-000000000006', '00000000-0000-0000-0000-000000004104', now(), true, true),
    ('00000000-0000-0000-0000-000000005113', '00000000-0000-0000-0000-000000000006', '00000000-0000-0000-0000-000000004105', now(), true, true),
    ('00000000-0000-0000-0000-000000005114', '00000000-0000-0000-0000-000000000006', '00000000-0000-0000-0000-000000004106', now(), true, true),
    ('00000000-0000-0000-0000-000000005115', '00000000-0000-0000-0000-000000000006', '00000000-0000-0000-0000-000000004107', now(), true, true),
    ('00000000-0000-0000-0000-000000005116', '00000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000004106', now(), true, true);
