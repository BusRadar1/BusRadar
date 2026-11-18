package pl.busradar.ingest.ztm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import pl.busradar.events.VehiclePositionEvent;

class ZtmVehicleMapperTest {

    private static final String GENERATED = "2026-11-08T17:16:32Z";
    private static final String TRIP_START = "2026-11-08T17:03:00Z";
    private static final Instant INGESTED_AT = Instant.parse("2026-11-08T17:16:50Z");

    private final ZtmVehicleMapper mapper = new ZtmVehicleMapper();

    @Test
    void mapsAllFields() {
        var dto = vehicle(145989L, GENERATED, 201, TRIP_START, "Orunia  Równa", 3, 35);

        var expected = new VehiclePositionEvent(
                145989L,
                "2810",
                "123-01",
                "123",
                123,
                201,
                Instant.parse(TRIP_START),
                "Orunia  Równa",
                54.33407974243164,
                18.655229568481445,
                20,
                270,
                35,
                3,
                Instant.parse(GENERATED),
                INGESTED_AT);

        assertEquals(expected, mapper.map(dto, INGESTED_AT));
    }

    @Test
    void mapsVehicleWithoutTripAndWithEmptyHeadsign() {
        var dto = vehicle(489L, GENERATED, null, null, "", 3, 0);

        var event = mapper.map(dto, INGESTED_AT);

        assertNull(event.tripId());
        assertNull(event.scheduledTripStart());
        assertEquals("", event.headsign());
        assertEquals(489L, event.vehicleId());
    }

    @Test
    void preservesZeroGpsQualityAndLargeDelay() {
        var dto = vehicle(478L, GENERATED, 201, TRIP_START, "Orunia", 0, 3827);

        var event = mapper.map(dto, INGESTED_AT);

        assertEquals(0, event.gpsQuality());
        assertEquals(3827, event.delaySeconds());
    }

    @Test
    void preservesNegativeDelay() {
        var dto = vehicle(145989L, GENERATED, 201, TRIP_START, "Orunia", 3, -60);

        assertEquals(-60, mapper.map(dto, INGESTED_AT).delaySeconds());
    }

    @Test
    void rejectsMissingVehicleId() {
        var dto = vehicle(null, GENERATED, 201, TRIP_START, "Orunia", 3, 35);

        var exception = assertThrows(IllegalArgumentException.class, () -> mapper.map(dto, INGESTED_AT));

        assertTrue(exception.getMessage().contains("vehicleId"));
    }

    @Test
    void rejectsInvalidGeneratedTimestamp() {
        var dto = vehicle(145989L, "invalid-time", 201, TRIP_START, "Orunia", 3, 35);

        var exception = assertThrows(IllegalArgumentException.class, () -> mapper.map(dto, INGESTED_AT));

        assertTrue(exception.getMessage().contains("generated"));
    }

    @Test
    void rejectsInvalidScheduledTripStartTimestamp() {
        var dto = vehicle(145989L, GENERATED, 201, "invalid-time", "Orunia", 3, 35);

        var exception = assertThrows(IllegalArgumentException.class, () -> mapper.map(dto, INGESTED_AT));

        assertTrue(exception.getMessage().contains("scheduledTripStartTime"));
    }

    private static ZtmVehicleDto vehicle(
            Long vehicleId,
            String generated,
            Integer tripId,
            String scheduledTripStartTime,
            String headsign,
            Integer gpsQuality,
            Integer delay) {
        return new ZtmVehicleDto(
                generated,
                "123",
                tripId,
                123,
                headsign,
                "2810",
                "123-01",
                vehicleId,
                20,
                270,
                delay,
                scheduledTripStartTime,
                54.33407974243164,
                18.655229568481445,
                gpsQuality);
    }
}
