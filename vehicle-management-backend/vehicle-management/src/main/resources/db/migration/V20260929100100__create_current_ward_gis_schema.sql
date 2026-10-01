CREATE TABLE reference.gis_wards (
    ward_code varchar(20) PRIMARY KEY,
    gis_server_id varchar(50),
    area_km2 numeric(12, 5),
    bbox geometry(Polygon, 4326),
    geom geometry(MultiPolygon, 4326) NOT NULL,
    CONSTRAINT fk_gis_wards_ward FOREIGN KEY (ward_code) REFERENCES reference.wards(code)
);

CREATE INDEX idx_gis_wards_bbox_gist ON reference.gis_wards USING gist (bbox);
CREATE INDEX idx_gis_wards_geom_gist ON reference.gis_wards USING gist (geom);

COMMENT ON TABLE reference.gis_wards IS
    'Ward boundaries from vietnamese-provinces-database v5.2.0 (MIT), https://github.com/thanglequoc/vietnamese-provinces-database';
