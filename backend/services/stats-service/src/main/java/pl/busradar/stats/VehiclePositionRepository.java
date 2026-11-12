package pl.busradar.stats;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pl.busradar.events.VehiclePositionEvent;

@Repository
@RequiredArgsConstructor
public class VehiclePositionRepository {
    // JdbcTemplate because JPA/Hibernate is slow at batch inserts.
    // JdbcTemplate.batchUpdate sends the whole list of inserts to psql in a single network trip.
    private final JdbcTemplate jdbcTemplate;

    public void insertBatch(List<VehiclePositionEvent> events) {
        // Specific column names and the ON CONFLICT DO NOTHING clause
        String sql = """
            INSERT INTO stats.vehicle_positions_raw (
                vehicle_id, vehicle_code, vehicle_service, line, route_id,
                trip_id, scheduled_trip_start, headsign, latitude, longitude,
                speed, direction, delay_seconds, gps_quality, read_at, ingested_at
            ) VALUES (
                ?, ?, ?, ?, ?,
                ?, ?, ?, ?, ?,
                ?, ?, ?, ?, ?, ?
            ) ON CONFLICT DO NOTHING
            """;

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                VehiclePositionEvent event = events.get(i);

                // Map the Java properties to the SQL placeholders (?)
                // 1-5: Required values (NOT NULL)
                ps.setLong(1, event.vehicleId());
                ps.setString(2, event.vehicleCode());
                ps.setString(3, event.vehicleService());
                ps.setString(4, event.line());
                ps.setInt(5, event.routeId());

                // 6-7: might be null
                if (event.tripId() != null) {
                    ps.setInt(6, event.tripId());
                } else {
                    ps.setNull(6, java.sql.Types.INTEGER);
                }

                if (event.scheduledTripStart() != null) {
                    ps.setTimestamp(7, Timestamp.from(event.scheduledTripStart()));
                } else {
                    ps.setNull(7, java.sql.Types.TIMESTAMP);
                }

                // 8-16: Required values (NOT NULL)
                ps.setString(8, event.headsign());
                ps.setDouble(9, event.lat());
                ps.setDouble(10, event.lon());
                ps.setInt(11, event.speed());
                ps.setInt(12, event.direction());
                ps.setInt(13, event.delaySeconds());
                ps.setInt(14, event.gpsQuality());
                ps.setTimestamp(15, Timestamp.from(event.readAt()));
                ps.setTimestamp(16, Timestamp.from(event.ingestedAt()));
            }

            @Override
            public int getBatchSize() {
                return events.size();
            }
        });
    }
}
