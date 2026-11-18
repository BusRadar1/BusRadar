package pl.busradar.stats;

import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pl.busradar.events.VehiclePositionEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class VehiclePositionConsumer {
    private final VehiclePositionRepository repository;

    // Listens to the topic and receives a batch of events
    @KafkaListener(topics = "vehicle-positions", groupId = "stats")
    public void consumePositions(List<VehiclePositionEvent> events) {

        List<VehiclePositionEvent> validEvents =
                events.stream().filter(Objects::nonNull).toList();

        // ErrorHandlingDeserializer hands us null for a message it could not parse,
        // so whatever the filter removed was unparseable.
        int dropped = events.size() - validEvents.size();
        if (dropped > 0) {
            log.warn("Dropped {} unparseable events out of {}", dropped, events.size());
        }

        // If the whole batch was broken, there's nothing to insert
        if (validEvents.isEmpty()) {
            return;
        }

        // Insert the valid ones
        repository.insertBatch(validEvents);
    }
}
