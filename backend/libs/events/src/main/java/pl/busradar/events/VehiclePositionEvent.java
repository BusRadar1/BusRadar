package pl.busradar.events;

import java.time.Instant;

public record VehiclePositionEvent(
        long vehicleId, // key
        String vehicleCode, // "9107"
        String vehicleService, // "149-11"
        String line, // routeShortName: "149", "M32", "T8"
        int routeId,
        Integer tripId, // nullable
        Instant scheduledTripStart, // nullable
        String headsign, // may be ""
        double lat,
        double lon,
        int speed, // km/h
        int direction, // degrees
        int delaySeconds, // negative = early
        int gpsQuality,
        Instant readAt, // API "generated" (UTC)
        Instant ingestedAt // when we fetched it
        ) {}
