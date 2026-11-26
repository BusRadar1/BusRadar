package pl.busradar.tracking;

import java.time.Instant;

public record VehicleResponse(
        long vehicleId,
        String vehicleCode,
        String line,
        String headsign,
        double lat,
        double lon,
        int direction,
        int speed,
        int delaySeconds,
        Instant readAt) {}
