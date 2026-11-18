package pl.busradar.ingest.ztm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ZtmVehicleDto(
        String generated,
        String routeShortName,
        Integer tripId,
        Integer routeId,
        String headsign,
        String vehicleCode,
        String vehicleService,
        Long vehicleId,
        Integer speed,
        Integer direction,
        Integer delay,
        String scheduledTripStartTime,
        Double lat,
        Double lon,
        Integer gpsQuality) {}
