-- =========================================================
-- PARKING LEVELS, SPACES, LAYOUT VERSIONS, ALLOCATIONS
-- Phase 1: Foundation for per-space management and 2D/3D layout
-- =========================================================

-- 1. parking_levels: multi-level support (ground, underground, multi-story)
CREATE TABLE parking.parking_levels (
    parking_level_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parking_lot_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    elevation NUMERIC(8,2),
    floor_height NUMERIC(8,2),
    canvas_width NUMERIC(10,2) NOT NULL DEFAULT 10000,
    canvas_height NUMERIC(10,2) NOT NULL DEFAULT 10000,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_parking_levels_parking_lot FOREIGN KEY (parking_lot_id) REFERENCES parking.parking_lots(parking_lot_id) ON DELETE CASCADE,
    CONSTRAINT uq_parking_levels_lot_code UNIQUE (parking_lot_id, code),
    CONSTRAINT ck_parking_levels_status CHECK (status IN ('ACTIVE', 'MAINTENANCE', 'CLOSED')),
    CONSTRAINT ck_parking_levels_canvas_positive CHECK (canvas_width > 0 AND canvas_height > 0)
);

-- 2. Extend zones: add level reference, tracking_mode, layout_status, layout_version
ALTER TABLE parking.zones
    ADD COLUMN parking_level_id UUID,
    ADD COLUMN tracking_mode VARCHAR(20) NOT NULL DEFAULT 'CAPACITY',
    ADD COLUMN layout_status VARCHAR(20) NOT NULL DEFAULT 'NOT_CONFIGURED',
    ADD COLUMN layout_version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE parking.zones
    ADD CONSTRAINT fk_zones_parking_level FOREIGN KEY (parking_level_id) REFERENCES parking.parking_levels(parking_level_id) ON DELETE SET NULL,
    ADD CONSTRAINT ck_zones_tracking_mode CHECK (tracking_mode IN ('CAPACITY', 'SPACE')),
    ADD CONSTRAINT ck_zones_layout_status CHECK (layout_status IN ('NOT_CONFIGURED', 'DRAFT', 'PUBLISHED'));

-- Migrate existing zones to first level of their parking lot
-- Create a default level for each existing parking lot and assign zones to it
INSERT INTO parking.parking_levels (parking_lot_id, code, name, display_order, canvas_width, canvas_height, status, created_at, created_by)
SELECT
    pl.parking_lot_id,
    'L1',
    'Tầng 1',
    1,
    10000,
    10000,
    'ACTIVE',
    now(),
    pl.created_by
FROM parking.parking_lots pl
WHERE NOT EXISTS (
    SELECT 1 FROM parking.parking_levels plvl WHERE plvl.parking_lot_id = pl.parking_lot_id
);

UPDATE parking.zones z
SET parking_level_id = (
    SELECT plvl.parking_level_id
    FROM parking.parking_levels plvl
    WHERE plvl.parking_lot_id = z.parking_lot_id
    ORDER BY plvl.display_order
    LIMIT 1
)
WHERE z.parking_level_id IS NULL;

-- 3. Create parking_spaces table (if not exists from baseline) and extend with new columns
CREATE TABLE IF NOT EXISTS parking.parking_spaces (
    parking_space_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    zone_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    vehicle_type_id UUID,
    lifecycle_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    status_source VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    manual_occupancy_type VARCHAR(20),
    status_reason TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    x NUMERIC(10,2),
    y NUMERIC(10,2),
    width NUMERIC(10,2),
    height NUMERIC(10,2),
    rotation NUMERIC(6,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_parking_spaces_zone FOREIGN KEY (zone_id) REFERENCES parking.zones(zone_id) ON DELETE CASCADE,
    CONSTRAINT fk_parking_spaces_vehicle_type FOREIGN KEY (vehicle_type_id) REFERENCES catalog.vehicle_types(vehicle_type_id) ON DELETE SET NULL,
    CONSTRAINT uq_parking_spaces_zone_code UNIQUE (zone_id, code),
    CONSTRAINT ck_parking_spaces_status CHECK (status IN ('AVAILABLE', 'OCCUPIED', 'RESERVED', 'MAINTENANCE')),
    CONSTRAINT ck_parking_spaces_lifecycle_status CHECK (lifecycle_status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT ck_parking_spaces_status_source CHECK (status_source IN ('MANUAL', 'AUTOMATIC')),
    CONSTRAINT ck_parking_spaces_manual_occupancy_type CHECK (manual_occupancy_type IS NULL OR manual_occupancy_type IN ('VISITOR', 'REGISTERED')),
    CONSTRAINT ck_parking_spaces_geometry_positive CHECK (width IS NULL OR width > 0),
    CONSTRAINT ck_parking_spaces_geometry_positive_height CHECK (height IS NULL OR height > 0)
);

-- 4. parking_layout_versions: versioned layouts per zone
CREATE TABLE parking.parking_layout_versions (
    layout_version_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    zone_id UUID NOT NULL,
    version BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    published_at TIMESTAMPTZ,
    published_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_parking_layout_versions_zone FOREIGN KEY (zone_id) REFERENCES parking.zones(zone_id) ON DELETE CASCADE,
    CONSTRAINT uq_parking_layout_versions_zone_version UNIQUE (zone_id, version),
    CONSTRAINT ck_parking_layout_versions_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
);

-- 5. parking_space_layout_items: geometry snapshot per layout version
CREATE TABLE parking.parking_space_layout_items (
    layout_item_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    layout_version_id UUID NOT NULL,
    parking_space_id UUID NOT NULL,
    x NUMERIC(10,2) NOT NULL,
    y NUMERIC(10,2) NOT NULL,
    width NUMERIC(10,2) NOT NULL,
    height NUMERIC(10,2) NOT NULL,
    rotation NUMERIC(6,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_parking_space_layout_items_version FOREIGN KEY (layout_version_id) REFERENCES parking.parking_layout_versions(layout_version_id) ON DELETE CASCADE,
    CONSTRAINT fk_parking_space_layout_items_space FOREIGN KEY (parking_space_id) REFERENCES parking.parking_spaces(parking_space_id) ON DELETE CASCADE,
    CONSTRAINT uq_parking_space_layout_items_version_space UNIQUE (layout_version_id, parking_space_id),
    CONSTRAINT ck_parking_space_layout_items_geometry_positive CHECK (width > 0 AND height > 0)
);

-- 6. parking_layout_elements: non-space visual elements (lines, text, obstacles, ramps, etc.)
CREATE TABLE parking.parking_layout_elements (
    layout_element_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    layout_version_id UUID NOT NULL,
    element_type VARCHAR(50) NOT NULL,
    geometry JSONB NOT NULL,
    style JSONB,
    label TEXT,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_parking_layout_elements_version FOREIGN KEY (layout_version_id) REFERENCES parking.parking_layout_versions(layout_version_id) ON DELETE CASCADE
);

-- 7. parking_space_allocations: subscription-to-space mapping with time windows
CREATE TABLE parking.parking_space_allocations (
    allocation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parking_space_id UUID NOT NULL,
    subscription_id UUID NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE NOT NULL,
    hold_expires_at TIMESTAMPTZ,
    allocation_status VARCHAR(20) NOT NULL DEFAULT 'HELD',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_parking_space_allocations_space FOREIGN KEY (parking_space_id) REFERENCES parking.parking_spaces(parking_space_id) ON DELETE CASCADE,
    CONSTRAINT fk_parking_space_allocations_subscription FOREIGN KEY (subscription_id) REFERENCES access_control.subscriptions(subscription_id) ON DELETE CASCADE,
    CONSTRAINT uq_parking_space_allocations_space_time UNIQUE (parking_space_id, effective_from, effective_to) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT ck_parking_space_allocations_status CHECK (allocation_status IN ('HELD', 'ACTIVE', 'RELEASED', 'CANCELLED')),
    CONSTRAINT ck_parking_space_allocations_dates CHECK (effective_to >= effective_from)
);

-- 8. Extend parking_sessions: add parking_space_id (if not exists), add subscription_id
ALTER TABLE parking.parking_sessions
    ADD COLUMN IF NOT EXISTS parking_space_id UUID,
    ADD COLUMN subscription_id UUID;

ALTER TABLE parking.parking_sessions
    ADD CONSTRAINT fk_parking_sessions_subscription FOREIGN KEY (subscription_id) REFERENCES access_control.subscriptions(subscription_id) ON DELETE SET NULL;

-- Prevent multiple open sessions for the same space
CREATE UNIQUE INDEX uq_parking_sessions_open_space
    ON parking.parking_sessions (parking_space_id)
    WHERE status = 'OPEN' AND parking_space_id IS NOT NULL;

-- Indexes for performance
CREATE INDEX idx_parking_levels_parking_lot ON parking.parking_levels(parking_lot_id);
CREATE INDEX idx_parking_levels_status ON parking.parking_levels(status);
CREATE INDEX idx_zones_parking_level ON parking.zones(parking_level_id);
CREATE INDEX idx_zones_tracking_mode ON parking.zones(tracking_mode);
CREATE INDEX idx_zones_layout_status ON parking.zones(layout_status);
CREATE INDEX idx_parking_spaces_zone ON parking.parking_spaces(zone_id);
CREATE INDEX idx_parking_spaces_status ON parking.parking_spaces(status);
CREATE INDEX idx_parking_spaces_lifecycle_status ON parking.parking_spaces(lifecycle_status);
CREATE INDEX idx_parking_spaces_version ON parking.parking_spaces(version);
CREATE INDEX idx_parking_layout_versions_zone ON parking.parking_layout_versions(zone_id);
CREATE INDEX idx_parking_layout_versions_status ON parking.parking_layout_versions(status);
CREATE INDEX idx_parking_space_layout_items_version ON parking.parking_space_layout_items(layout_version_id);
CREATE INDEX idx_parking_space_layout_items_space ON parking.parking_space_layout_items(parking_space_id);
CREATE INDEX idx_parking_layout_elements_version ON parking.parking_layout_elements(layout_version_id);
CREATE INDEX idx_parking_space_allocations_space ON parking.parking_space_allocations(parking_space_id);
CREATE INDEX idx_parking_space_allocations_subscription ON parking.parking_space_allocations(subscription_id);
CREATE INDEX idx_parking_space_allocations_status ON parking.parking_space_allocations(allocation_status);
CREATE INDEX idx_parking_space_allocations_dates ON parking.parking_space_allocations(effective_from, effective_to);
CREATE INDEX idx_parking_sessions_space ON parking.parking_sessions(parking_space_id);
CREATE INDEX idx_parking_sessions_subscription ON parking.parking_sessions(subscription_id);

-- Triggers for updated_at
CREATE TRIGGER trg_parking_levels_set_updated_at BEFORE UPDATE ON parking.parking_levels FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_parking_layout_versions_set_updated_at BEFORE UPDATE ON parking.parking_layout_versions FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_parking_space_layout_items_set_updated_at BEFORE UPDATE ON parking.parking_space_layout_items FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_parking_layout_elements_set_updated_at BEFORE UPDATE ON parking.parking_layout_elements FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_parking_space_allocations_set_updated_at BEFORE UPDATE ON parking.parking_space_allocations FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();