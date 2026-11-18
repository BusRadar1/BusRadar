-- standard PostgreSQL dialect directly. No XML or YAML

-- The stats schema is already created by infra/postgres/init, so just create the table
CREATE TABLE stats.vehicle_positions_raw (
    vehicle_id BIGINT NOT NULL,
    vehicle_code TEXT NOT NULL,
    vehicle_service TEXT NOT NULL,
    line TEXT NOT NULL,
    route_id INTEGER NOT NULL,
    trip_id INTEGER,
    scheduled_trip_start TIMESTAMPTZ,
    headsign TEXT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL, -- 15 decimal digits
    longitude DOUBLE PRECISION NOT NULL,
    speed INTEGER NOT NULL,
    direction INTEGER NOT NULL,
    delay_seconds INTEGER NOT NULL,
    gps_quality INTEGER NOT NULL,
    read_at TIMESTAMPTZ NOT NULL,
    ingested_at TIMESTAMPTZ NOT NULL,
    
    -- natural key: dedup target for ON CONFLICT DO NOTHING, and it contains
    -- read_at, which daily partitioning will require
    PRIMARY KEY (vehicle_id, read_at)
);

-- Index for querying history efficiently
CREATE INDEX idx_vehicle_positions_raw_read_at 
    ON stats.vehicle_positions_raw (read_at);