CREATE SCHEMA IF NOT EXISTS reference;

CREATE TABLE reference.administrative_units (
    administrative_unit_id smallint PRIMARY KEY,
    full_name varchar(255) NOT NULL,
    full_name_en varchar(255),
    short_name varchar(100) NOT NULL,
    short_name_en varchar(100),
    code_name varchar(100) NOT NULL UNIQUE,
    code_name_en varchar(100),
    CONSTRAINT ck_administrative_units_name_not_blank CHECK (btrim(full_name) <> ''),
    CONSTRAINT ck_administrative_units_short_name_not_blank CHECK (btrim(short_name) <> '')
);

CREATE TABLE reference.provinces (
    code varchar(2) PRIMARY KEY,
    name varchar(255) NOT NULL,
    name_en varchar(255),
    full_name varchar(255) NOT NULL,
    full_name_en varchar(255),
    code_name varchar(255) NOT NULL UNIQUE,
    postal_code_prefix varchar(255),
    administrative_unit_id smallint NOT NULL,
    CONSTRAINT fk_provinces_administrative_unit
        FOREIGN KEY (administrative_unit_id)
        REFERENCES reference.administrative_units (administrative_unit_id),
    CONSTRAINT ck_provinces_code_format CHECK (code ~ '^[0-9]{2}$'),
    CONSTRAINT ck_provinces_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT ck_provinces_full_name_not_blank CHECK (btrim(full_name) <> '')
);

CREATE TABLE reference.wards (
    code varchar(5) PRIMARY KEY,
    province_code varchar(2) NOT NULL,
    name varchar(255) NOT NULL,
    name_en varchar(255),
    full_name varchar(255) NOT NULL,
    full_name_en varchar(255),
    code_name varchar(255) NOT NULL,
    postal_code varchar(20),
    administrative_unit_id smallint NOT NULL,
    CONSTRAINT fk_wards_province
        FOREIGN KEY (province_code)
        REFERENCES reference.provinces (code),
    CONSTRAINT fk_wards_administrative_unit
        FOREIGN KEY (administrative_unit_id)
        REFERENCES reference.administrative_units (administrative_unit_id),
    CONSTRAINT ck_wards_code_format CHECK (code ~ '^[0-9]{5}$'),
    CONSTRAINT ck_wards_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT ck_wards_full_name_not_blank CHECK (btrim(full_name) <> '')
);

CREATE INDEX idx_wards_province_code
    ON reference.wards (province_code, code);

CREATE TABLE reference.administrative_dataset_metadata (
    dataset_key varchar(30) PRIMARY KEY,
    dataset_version varchar(30) NOT NULL,
    source_repository varchar(500) NOT NULL,
    source_tag varchar(50) NOT NULL,
    source_file varchar(500) NOT NULL,
    source_sha256 char(64) NOT NULL,
    source_generated_at timestamptz,
    imported_at timestamptz NOT NULL DEFAULT now(),
    administrative_unit_count integer NOT NULL,
    province_count integer NOT NULL,
    ward_count integer NOT NULL,
    license_name varchar(50) NOT NULL,
    license_attribution text NOT NULL,
    CONSTRAINT ck_administrative_dataset_key CHECK (dataset_key = 'CURRENT'),
    CONSTRAINT ck_administrative_dataset_version_not_blank CHECK (btrim(dataset_version) <> ''),
    CONSTRAINT ck_administrative_dataset_sha256 CHECK (source_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_administrative_dataset_counts CHECK (
        administrative_unit_count > 0 AND province_count > 0 AND ward_count > 0
    )
);

CREATE TABLE reference.legacy_provinces (
    code varchar(2) PRIMARY KEY,
    name varchar(255) NOT NULL,
    name_en varchar(255),
    full_name varchar(255) NOT NULL,
    full_name_en varchar(255),
    code_name varchar(255) NOT NULL UNIQUE,
    administrative_unit_id smallint NOT NULL,
    administrative_unit_name varchar(255) NOT NULL,
    CONSTRAINT ck_legacy_provinces_code_format CHECK (code ~ '^[0-9]{2}$'),
    CONSTRAINT ck_legacy_provinces_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT ck_legacy_provinces_full_name_not_blank CHECK (btrim(full_name) <> '')
);

CREATE TABLE reference.legacy_districts (
    code varchar(3) PRIMARY KEY,
    province_code varchar(2) NOT NULL,
    name varchar(255) NOT NULL,
    name_en varchar(255),
    full_name varchar(255) NOT NULL,
    full_name_en varchar(255),
    code_name varchar(255) NOT NULL,
    administrative_unit_id smallint NOT NULL,
    administrative_unit_name varchar(255) NOT NULL,
    CONSTRAINT fk_legacy_districts_province
        FOREIGN KEY (province_code)
        REFERENCES reference.legacy_provinces (code),
    CONSTRAINT ck_legacy_districts_code_format CHECK (code ~ '^[0-9]{3}$'),
    CONSTRAINT ck_legacy_districts_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT ck_legacy_districts_full_name_not_blank CHECK (btrim(full_name) <> '')
);

CREATE INDEX idx_legacy_districts_province_code
    ON reference.legacy_districts (province_code, code);

CREATE TABLE reference.legacy_wards (
    code varchar(5) PRIMARY KEY,
    district_code varchar(3) NOT NULL,
    name varchar(255) NOT NULL,
    name_en varchar(255),
    full_name varchar(255) NOT NULL,
    full_name_en varchar(255),
    code_name varchar(255) NOT NULL,
    administrative_unit_id smallint NOT NULL,
    administrative_unit_name varchar(255) NOT NULL,
    CONSTRAINT fk_legacy_wards_district
        FOREIGN KEY (district_code)
        REFERENCES reference.legacy_districts (code),
    CONSTRAINT ck_legacy_wards_code_format CHECK (code ~ '^[0-9]{5}$'),
    CONSTRAINT ck_legacy_wards_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT ck_legacy_wards_full_name_not_blank CHECK (btrim(full_name) <> '')
);

CREATE INDEX idx_legacy_wards_district_code
    ON reference.legacy_wards (district_code, code);

CREATE TABLE reference.legacy_dataset_metadata (
    dataset_key varchar(30) PRIMARY KEY,
    dataset_version varchar(30) NOT NULL,
    source_repository varchar(500) NOT NULL,
    source_tag varchar(50) NOT NULL,
    source_file varchar(500) NOT NULL,
    source_sha256 char(64) NOT NULL,
    source_generated_at timestamptz,
    imported_at timestamptz NOT NULL DEFAULT now(),
    province_count integer NOT NULL,
    district_count integer NOT NULL,
    ward_count integer NOT NULL,
    license_name varchar(50) NOT NULL,
    license_attribution text NOT NULL,
    CONSTRAINT ck_legacy_dataset_key CHECK (dataset_key = 'LEGACY'),
    CONSTRAINT ck_legacy_dataset_version_not_blank CHECK (btrim(dataset_version) <> ''),
    CONSTRAINT ck_legacy_dataset_sha256 CHECK (source_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_legacy_dataset_counts CHECK (
        province_count > 0 AND district_count > 0 AND ward_count > 0
    )
);

CREATE TABLE reference.legacy_ward_mappings (
    legacy_ward_code varchar(5) NOT NULL,
    current_ward_code varchar(5) NOT NULL,
    mapping_method varchar(30) NOT NULL,
    confidence numeric(5,4) NOT NULL,
    verified boolean NOT NULL DEFAULT false,
    verified_by uuid,
    verified_at timestamptz,
    note text,
    PRIMARY KEY (legacy_ward_code, current_ward_code),
    CONSTRAINT fk_legacy_ward_mappings_legacy_ward
        FOREIGN KEY (legacy_ward_code)
        REFERENCES reference.legacy_wards (code),
    CONSTRAINT fk_legacy_ward_mappings_current_ward
        FOREIGN KEY (current_ward_code)
        REFERENCES reference.wards (code),
    CONSTRAINT fk_legacy_ward_mappings_verified_by
        FOREIGN KEY (verified_by)
        REFERENCES iam.accounts (account_id),
    CONSTRAINT ck_legacy_ward_mappings_method CHECK (
        mapping_method IN ('OFFICIAL', 'MANUAL', 'NAME_EXACT', 'NAME_NORMALIZED', 'SPATIAL', 'COMPOSITE')
    ),
    CONSTRAINT ck_legacy_ward_mappings_confidence CHECK (confidence BETWEEN 0 AND 1),
    CONSTRAINT ck_legacy_ward_mappings_verification CHECK (
        (verified = false AND verified_by IS NULL AND verified_at IS NULL)
        OR (verified = true AND verified_by IS NOT NULL AND verified_at IS NOT NULL)
    )
);

CREATE INDEX idx_legacy_ward_mappings_current_ward
    ON reference.legacy_ward_mappings (current_ward_code);

COMMENT ON TABLE reference.legacy_ward_mappings IS
    'Many-to-many mapping candidates between legacy and current wards; no one-to-one assumption.';
