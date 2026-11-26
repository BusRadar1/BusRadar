package pl.busradar.tracking;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pl.busradar.events.VehiclePositionEvent;

@Component
public class VehiclePositionListener {

    private final LatestPositionRepository repository;

    public VehiclePositionListener(LatestPositionRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "vehicle-positions")
    public void onVehiclePosition(VehiclePositionEvent event) {
        repository.saveIfNewer(event);
    }
}
