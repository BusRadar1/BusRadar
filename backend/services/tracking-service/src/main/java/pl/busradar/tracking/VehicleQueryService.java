package pl.busradar.tracking;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class VehicleQueryService {

    private final LatestPositionRepository repository;

    public VehicleQueryService(LatestPositionRepository repository) {
        this.repository = repository;
    }

    public List<VehicleResponse> findAll() {
        return repository.findAll().stream()
                .map(event -> new VehicleResponse(
                        event.vehicleId(),
                        event.vehicleCode(),
                        event.line(),
                        event.headsign(),
                        event.lat(),
                        event.lon(),
                        event.direction(),
                        event.speed(),
                        event.delaySeconds(),
                        event.readAt()))
                .toList();
    }
}
