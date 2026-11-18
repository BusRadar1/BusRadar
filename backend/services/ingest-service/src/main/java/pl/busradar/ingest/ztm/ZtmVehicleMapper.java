package pl.busradar.ingest.ztm;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import org.springframework.stereotype.Component;
import pl.busradar.events.VehiclePositionEvent;

@Component
public class ZtmVehicleMapper {

    public VehiclePositionEvent map(ZtmVehicleDto vehicle, Instant ingestedAt) {
        required(vehicle, "vehicle");

        return new VehiclePositionEvent(
                required(vehicle.vehicleId(), "vehicleId"),
                required(vehicle.vehicleCode(), "vehicleCode"),
                required(vehicle.vehicleService(), "vehicleService"),
                required(vehicle.routeShortName(), "routeShortName"),
                required(vehicle.routeId(), "routeId"),
                vehicle.tripId(),
                vehicle.scheduledTripStartTime() == null
                        ? null
                        : parseTimestamp(vehicle.scheduledTripStartTime(), "scheduledTripStartTime"),
                required(vehicle.headsign(), "headsign"),
                required(vehicle.lat(), "lat"),
                required(vehicle.lon(), "lon"),
                required(vehicle.speed(), "speed"),
                required(vehicle.direction(), "direction"),
                required(vehicle.delay(), "delay"),
                required(vehicle.gpsQuality(), "gpsQuality"),
                parseTimestamp(vehicle.generated(), "generated"),
                required(ingestedAt, "ingestedAt"));
    }

    private static <T> T required(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        return value;
    }

    private static Instant parseTimestamp(String value, String field) {
        required(value, field);
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Invalid timestamp in field: " + field, exception);
        }
    }
}
