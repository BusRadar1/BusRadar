package pl.busradar.ingest.poller;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.busradar.events.VehiclePositionEvent;
import pl.busradar.ingest.config.KafkaTopicConfig;
import pl.busradar.ingest.ztm.ZtmClient;

@ConditionalOnProperty(name = "ingest.polling.enabled", havingValue = "true", matchIfMissing = true)
@Component
public class VehiclePositionPoller {

    private static final Logger log = LoggerFactory.getLogger(VehiclePositionPoller.class);

    private final ZtmClient ztmClient;
    private final KafkaTemplate<String, VehiclePositionEvent> kafkaTemplate;

    public VehiclePositionPoller(ZtmClient ztmClient, KafkaTemplate<String, VehiclePositionEvent> kafkaTemplate) {
        this.ztmClient = ztmClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${ingest.poll-interval:20s}")
    public void pollVehiclePositions() {
        List<VehiclePositionEvent> events;
        try {
            events = ztmClient.fetchVehiclePositions();
        } catch (Exception e) {
            log.error("Failed to fetch vehicle positions from ZTM API", e);
            return;
        }

        if (events == null || events.isEmpty()) {
            log.debug("No vehicle positions returned from ZTM API");
            return;
        }

        for (VehiclePositionEvent event : events) {
            try {
                kafkaTemplate
                        .send(KafkaTopicConfig.VEHICLE_POSITIONS_TOPIC, String.valueOf(event.vehicleId()), event)
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("Failed to publish vehicleId={}", event.vehicleId(), ex);
                            }
                        });
            } catch (Exception e) {
                log.error("Failed to enqueue message for vehicleId={}", event.vehicleId(), e);
            }
        }

        log.debug("Dispatched {} vehicle position events to Kafka", events.size());
    }
}
